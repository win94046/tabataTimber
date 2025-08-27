package org.tabata.timber.core.timer

import kotlinx.coroutines.flow.StateFlow
import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration

/**
 * High-level controller for timer operations with state validation
 * Provides safe operations with comprehensive error handling and validation
 */
class TimerController(
    private val timerEngine: TimerEngine
) {
    
    // Expose timer state for UI observation
    val timerState: StateFlow<TimerState> = timerEngine.timerState
    val currentPhase: StateFlow<PhaseType> = timerEngine.currentPhase
    val remainingTimeMs: StateFlow<Long> = timerEngine.remainingTimeMs
    val remainingTime: StateFlow<Int> = timerEngine.remainingTime
    val elapsedTimeMs: StateFlow<Long> = timerEngine.elapsedTimeMs
    val isActive: StateFlow<Boolean> = timerEngine.isActive
    val currentCycle: StateFlow<Int> = timerEngine.currentCycle
    val currentSet: StateFlow<Int> = timerEngine.currentSet
    val precisionMetrics: StateFlow<TimerPrecisionMetrics> = timerEngine.precisionMetrics
    
    /**
     * Result of timer operation with detailed error information
     */
    sealed class TimerOperationResult {
        object Success : TimerOperationResult()
        data class Error(
            val message: String,
            val errorCode: TimerErrorCode,
            val cause: Throwable? = null
        ) : TimerOperationResult()
    }
    
    /**
     * Error codes for timer operations
     */
    enum class TimerErrorCode {
        INVALID_STATE,
        INVALID_CONFIGURATION,
        SESSION_NOT_FOUND,
        OPERATION_NOT_ALLOWED,
        INTERNAL_ERROR
    }
    
    /**
     * Starts a new timer session with state validation
     * 
     * @param config Timer configuration
     * @return Operation result with validation
     */
    suspend fun startSession(config: TimerConfiguration): TimerOperationResult {
        // Validate current state
        val currentState = timerEngine.timerState.value
        if (currentState != TimerState.IDLE) {
            return TimerOperationResult.Error(
                "Cannot start session in state: $currentState. Must be IDLE.",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        // Validate configuration
        return when (val validation = config.validate()) {
            is TimerConfiguration.ValidationResult.Invalid -> {
                TimerOperationResult.Error(
                    "Configuration validation failed: ${validation.errors.joinToString()}",
                    TimerErrorCode.INVALID_CONFIGURATION
                )
            }
            is TimerConfiguration.ValidationResult.Valid -> {
                try {
                    val result = timerEngine.startSession(config)
                    if (result.isSuccess) {
                        TimerOperationResult.Success
                    } else {
                        TimerOperationResult.Error(
                            "Failed to start session: ${result.exceptionOrNull()?.message}",
                            TimerErrorCode.INTERNAL_ERROR,
                            result.exceptionOrNull()
                        )
                    }
                } catch (e: Exception) {
                    TimerOperationResult.Error(
                        "Unexpected error starting session: ${e.message}",
                        TimerErrorCode.INTERNAL_ERROR,
                        e
                    )
                }
            }
        }
    }
    
    /**
     * Starts a session with legacy TabataConfig
     */
    suspend fun startSession(config: TabataConfig): TimerOperationResult {
        return startSession(TimerConfiguration.fromTabataConfig(config))
    }
    
    /**
     * Pauses the current session with state validation
     */
    suspend fun pauseSession(): TimerOperationResult {
        val currentState = timerEngine.timerState.value
        
        if (!timerEngine.isActive.value) {
            return TimerOperationResult.Error(
                "No active session to pause",
                TimerErrorCode.SESSION_NOT_FOUND
            )
        }
        
        if (currentState == TimerState.PAUSED) {
            return TimerOperationResult.Error(
                "Session is already paused",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        if (currentState == TimerState.FINISHED) {
            return TimerOperationResult.Error(
                "Cannot pause finished session",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        return try {
            val result = timerEngine.pauseSession()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to pause session: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error pausing session: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Resumes a paused session with state validation
     */
    suspend fun resumeSession(): TimerOperationResult {
        val currentState = timerEngine.timerState.value
        
        if (currentState != TimerState.PAUSED) {
            return TimerOperationResult.Error(
                "Cannot resume session in state: $currentState. Must be PAUSED.",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        return try {
            val result = timerEngine.resumeSession()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to resume session: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error resuming session: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Stops the current session
     */
    suspend fun stopSession(): TimerOperationResult {
        if (!timerEngine.isActive.value && timerEngine.timerState.value == TimerState.IDLE) {
            return TimerOperationResult.Error(
                "No active session to stop",
                TimerErrorCode.SESSION_NOT_FOUND
            )
        }
        
        return try {
            val result = timerEngine.stopSession()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to stop session: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error stopping session: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Resets the timer to initial state
     */
    suspend fun resetTimer(): TimerOperationResult {
        return try {
            val result = timerEngine.resetTimer()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to reset timer: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error resetting timer: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Skips to the next phase with validation
     */
    suspend fun skipToNext(): TimerOperationResult {
        val currentState = timerEngine.timerState.value
        val currentPhase = timerEngine.currentPhase.value
        
        if (!timerEngine.isActive.value) {
            return TimerOperationResult.Error(
                "Cannot skip phase when session is not active",
                TimerErrorCode.SESSION_NOT_FOUND
            )
        }
        
        if (currentState == TimerState.PAUSED) {
            return TimerOperationResult.Error(
                "Cannot skip phase while paused. Resume session first.",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        if (currentState == TimerState.FINISHED) {
            return TimerOperationResult.Error(
                "Cannot skip phase in finished session",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        if (currentPhase == PhaseType.COOLDOWN) {
            return TimerOperationResult.Error(
                "Cannot skip cooldown phase",
                TimerErrorCode.OPERATION_NOT_ALLOWED
            )
        }
        
        return try {
            val result = timerEngine.skipToNext()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to skip to next phase: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error skipping to next phase: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Skips to the previous phase with validation
     */
    suspend fun skipToPrevious(): TimerOperationResult {
        val currentState = timerEngine.timerState.value
        
        if (!timerEngine.isActive.value) {
            return TimerOperationResult.Error(
                "Cannot skip phase when session is not active",
                TimerErrorCode.SESSION_NOT_FOUND
            )
        }
        
        if (currentState == TimerState.PAUSED) {
            return TimerOperationResult.Error(
                "Cannot skip phase while paused. Resume session first.",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        if (currentState == TimerState.FINISHED) {
            return TimerOperationResult.Error(
                "Cannot skip phase in finished session",
                TimerErrorCode.INVALID_STATE
            )
        }
        
        return try {
            val result = timerEngine.skipToPrevious()
            if (result.isSuccess) {
                TimerOperationResult.Success
            } else {
                TimerOperationResult.Error(
                    "Failed to skip to previous phase: ${result.exceptionOrNull()?.message}",
                    TimerErrorCode.INTERNAL_ERROR,
                    result.exceptionOrNull()
                )
            }
        } catch (e: Exception) {
            TimerOperationResult.Error(
                "Unexpected error skipping to previous phase: ${e.message}",
                TimerErrorCode.INTERNAL_ERROR,
                e
            )
        }
    }
    
    /**
     * Gets current session information
     */
    fun getCurrentSessionInfo(): SessionInfo? {
        val session = timerEngine.currentSession.value ?: return null
        val config = timerEngine.currentConfiguration.value ?: return null
        
        return SessionInfo(
            sessionId = session.id,
            configuration = config,
            currentPhase = timerEngine.currentPhase.value,
            currentState = timerEngine.timerState.value,
            elapsedTimeMs = timerEngine.elapsedTimeMs.value,
            remainingTimeMs = timerEngine.remainingTimeMs.value,
            currentCycle = timerEngine.currentCycle.value,
            currentSet = timerEngine.currentSet.value,
            precisionMetrics = timerEngine.precisionMetrics.value
        )
    }
    
    /**
     * Information about current timer session
     */
    data class SessionInfo(
        val sessionId: String,
        val configuration: TimerConfiguration,
        val currentPhase: PhaseType,
        val currentState: TimerState,
        val elapsedTimeMs: Long,
        val remainingTimeMs: Long,
        val currentCycle: Int,
        val currentSet: Int,
        val precisionMetrics: TimerPrecisionMetrics
    ) {
        /**
         * Calculates session progress percentage
         */
        fun getProgressPercent(): Double {
            val totalDurationMs = with(configuration) {
                (warmupDuration + cooldownDuration + 
                 (workDuration + restDuration) * cycles * sets + 
                 setBreakDuration * (sets - 1).coerceAtLeast(0)) * 1000L
            }
            
            return if (totalDurationMs > 0) {
                (elapsedTimeMs.toDouble() / totalDurationMs * 100).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        }
        
        /**
         * Gets formatted time remaining string
         */
        fun getFormattedRemainingTime(): String {
            val seconds = remainingTimeMs / 1000
            val minutes = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", minutes, secs)
        }
    }
}