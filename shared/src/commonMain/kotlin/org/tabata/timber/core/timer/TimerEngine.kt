package org.tabata.timber.core.timer

import kotlinx.coroutines.flow.StateFlow
import org.tabata.timber.domain.models.TimerConfiguration
import org.tabata.timber.domain.models.TimerState
import kotlin.time.Duration

/**
 * Core timer engine interface for managing interval training sessions.
 * Provides precise timing with drift correction and background time handling.
 */
interface TimerEngine {
    /**
     * Current timer state as a reactive flow
     */
    val timerState: StateFlow<TimerState>
    
    /**
     * Start a timer session with the given configuration
     */
    suspend fun start(configuration: TimerConfiguration)
    
    /**
     * Pause the current timer session
     */
    suspend fun pause()
    
    /**
     * Resume a paused timer session
     */
    suspend fun resume()
    
    /**
     * Stop the current timer session
     */
    suspend fun stop()
    
    /**
     * Skip to the next phase in the current session
     */
    suspend fun skipPhase()
    
    /**
     * Correct timer state after returning from background
     * @param backgroundDuration Time spent in background
     */
    suspend fun correctTimeFromBackground(backgroundDuration: Duration)
}