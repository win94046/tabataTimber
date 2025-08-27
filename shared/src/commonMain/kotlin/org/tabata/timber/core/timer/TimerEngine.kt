package org.tabata.timber.core.timer

import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for managing high-precision Tabata timer functionality
 * Provides <50ms accuracy for timing operations with drift correction
 * This is expected to have platform-specific implementations
 */
interface TimerEngine {
    
    /**
     * Current workout session state
     */
    val currentSession: StateFlow<WorkoutSession?>
    
    /**
     * Current timer state
     */
    val timerState: StateFlow<TimerState>
    
    /**
     * Current phase type (WARMUP, WORK, REST, SET_BREAK, COOLDOWN)
     */
    val currentPhase: StateFlow<PhaseType>
    
    /**
     * Current remaining time in milliseconds (for higher precision)
     */
    val remainingTimeMs: StateFlow<Long>
    
    /**
     * Current remaining time in seconds (legacy compatibility)
     */
    val remainingTime: StateFlow<Int>
    
    /**
     * Total elapsed time since session start in milliseconds
     */
    val elapsedTimeMs: StateFlow<Long>
    
    /**
     * Whether the timer engine is currently active
     */
    val isActive: StateFlow<Boolean>
    
    /**
     * Current timer configuration
     */
    val currentConfiguration: StateFlow<TimerConfiguration?>
    
    /**
     * Current cycle number (1-based)
     */
    val currentCycle: StateFlow<Int>
    
    /**
     * Current set number (1-based)
     */
    val currentSet: StateFlow<Int>
    
    /**
     * Timer precision metrics (drift, accuracy)
     */
    val precisionMetrics: StateFlow<TimerPrecisionMetrics>
    
    /**
     * Starts a new Tabata session with the given configuration
     */
    suspend fun startSession(config: TabataConfig): Result<Unit>
    
    /**
     * Starts a new session with enhanced timer configuration
     */
    suspend fun startSession(config: TimerConfiguration): Result<Unit>
    
    /**
     * Pauses the current session
     */
    suspend fun pauseSession(): Result<Unit>
    
    /**
     * Resumes a paused session
     */
    suspend fun resumeSession(): Result<Unit>
    
    /**
     * Stops the current session completely
     */
    suspend fun stopSession(): Result<Unit>
    
    /**
     * Resets the timer to initial state
     */
    suspend fun resetTimer(): Result<Unit>
    
    /**
     * Manually advances to the next period (work -> rest or rest -> work)
     * Useful for testing or manual control
     */
    suspend fun skipToNext(): Result<Unit>
    
    /**
     * Manually goes back to the previous period
     * Useful for corrections during workout
     */
    suspend fun skipToPrevious(): Result<Unit>
}