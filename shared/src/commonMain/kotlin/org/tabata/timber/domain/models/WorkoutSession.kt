package org.tabata.timber.domain.models

import kotlinx.datetime.Instant

/**
 * Represents a completed or in-progress workout session
 */
data class WorkoutSession(
    val id: String,
    val config: TabataConfig,
    val startTime: Instant,
    val endTime: Instant? = null,
    val currentSet: Int = 1,
    val currentCycle: Int = 1,
    val currentState: TimerState = TimerState.IDLE,
    val remainingTime: Int = 0,      // Remaining time in current period (seconds)
    val isCompleted: Boolean = false
) {
    
    /**
     * Calculates the total elapsed time of the session
     */
    fun getElapsedTime(): Long {
        val end = endTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
        return end.epochSeconds - startTime.epochSeconds
    }
    
    /**
     * Calculates the progress percentage (0.0 to 1.0)
     */
    fun getProgress(): Float {
        if (isCompleted) return 1.0f
        
        val totalCycles = config.cycles * config.sets
        val completedCycles = (currentSet - 1) * config.cycles + (currentCycle - 1)
        
        return if (totalCycles > 0) {
            completedCycles.toFloat() / totalCycles.toFloat()
        } else {
            0.0f
        }
    }
}