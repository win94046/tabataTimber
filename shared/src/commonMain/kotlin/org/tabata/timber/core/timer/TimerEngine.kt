package org.tabata.timber.core.timer

import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for managing Tabata timer functionality
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
     * Current remaining time in seconds
     */
    val remainingTime: StateFlow<Int>
    
    /**
     * Whether the timer engine is currently active
     */
    val isActive: StateFlow<Boolean>
    
    /**
     * Starts a new Tabata session with the given configuration
     */
    suspend fun startSession(config: TabataConfig): Result<Unit>
    
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