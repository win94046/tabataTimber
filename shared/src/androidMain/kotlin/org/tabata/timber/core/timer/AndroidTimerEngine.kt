package org.tabata.timber.core.timer

import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration
import kotlinx.coroutines.flow.StateFlow

/**
 * Android implementation of TimerEngine using PreciseTimerEngine
 * Provides high-precision timing with drift correction for Android platform
 */
class AndroidTimerEngine : TimerEngine {
    
    private val platformTimer = PlatformTimer()
    private val preciseEngine = PreciseTimerEngine(platformTimer)
    
    // Delegate all StateFlow properties to PreciseTimerEngine
    override val currentSession: StateFlow<WorkoutSession?> = preciseEngine.currentSession
    override val timerState: StateFlow<TimerState> = preciseEngine.timerState
    override val currentPhase: StateFlow<PhaseType> = preciseEngine.currentPhase
    override val remainingTimeMs: StateFlow<Long> = preciseEngine.remainingTimeMs
    override val remainingTime: StateFlow<Int> = preciseEngine.remainingTime
    override val elapsedTimeMs: StateFlow<Long> = preciseEngine.elapsedTimeMs
    override val isActive: StateFlow<Boolean> = preciseEngine.isActive
    override val currentConfiguration: StateFlow<TimerConfiguration?> = preciseEngine.currentConfiguration
    override val currentCycle: StateFlow<Int> = preciseEngine.currentCycle
    override val currentSet: StateFlow<Int> = preciseEngine.currentSet
    override val precisionMetrics: StateFlow<TimerPrecisionMetrics> = preciseEngine.precisionMetrics
    
    // Delegate all operations to PreciseTimerEngine
    override suspend fun startSession(config: TabataConfig): Result<Unit> {
        return preciseEngine.startSession(config)
    }
    
    override suspend fun startSession(config: TimerConfiguration): Result<Unit> {
        return preciseEngine.startSession(config)
    }
    
    override suspend fun pauseSession(): Result<Unit> {
        return preciseEngine.pauseSession()
    }
    
    override suspend fun resumeSession(): Result<Unit> {
        return preciseEngine.resumeSession()
    }
    
    override suspend fun stopSession(): Result<Unit> {
        return preciseEngine.stopSession()
    }
    
    override suspend fun resetTimer(): Result<Unit> {
        return preciseEngine.resetTimer()
    }
    
    override suspend fun skipToNext(): Result<Unit> {
        return preciseEngine.skipToNext()
    }
    
    override suspend fun skipToPrevious(): Result<Unit> {
        return preciseEngine.skipToPrevious()
    }
    
    /**
     * Clean up Android-specific resources
     */
    fun cleanup() {
        (platformTimer as? org.tabata.timber.core.timer.PlatformTimer)?.let { timer ->
            // Access Android-specific cleanup if available
            try {
                timer.javaClass.getDeclaredMethod("shutdown")?.invoke(timer)
            } catch (e: Exception) {
                // Cleanup method not available, ignore
            }
        }
    }
}