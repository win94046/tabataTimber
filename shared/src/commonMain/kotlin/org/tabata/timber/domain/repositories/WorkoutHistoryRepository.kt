package org.tabata.timber.domain.repositories

import org.tabata.timber.domain.models.TimerConfiguration

/**
 * Repository interface for managing workout history and statistics
 */
interface WorkoutHistoryRepository {
    /**
     * Save a completed workout session
     * @param session Workout session to save
     */
    suspend fun saveWorkoutSession(session: WorkoutSession)
    
    /**
     * Get workout history with optional limit
     * @param limit Maximum number of sessions to return
     */
    suspend fun getWorkoutHistory(limit: Int = 50): List<WorkoutSession>
    
    /**
     * Get workout statistics for a specific period
     * @param period Statistics period to calculate
     */
    suspend fun getWorkoutStats(period: StatsPeriod): WorkoutStats
    
    /**
     * Get workout sessions within a date range
     * @param startDate Start date (timestamp)
     * @param endDate End date (timestamp)
     */
    suspend fun getWorkoutsByDateRange(startDate: Long, endDate: Long): List<WorkoutSession>
}

/**
 * Workout session data model
 */
data class WorkoutSession(
    val id: Long = 0,
    val presetId: Long,
    val presetName: String,
    val startTime: Long,
    val endTime: Long?,
    val completed: Boolean,
    val totalDuration: Long,
    val backgroundTime: Long = 0,
    val driftCorrections: Int = 0
)

/**
 * Workout statistics data model
 */
data class WorkoutStats(
    val totalSessions: Int,
    val completedSessions: Int,
    val totalDuration: Long,
    val averageDuration: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val period: StatsPeriod
)

/**
 * Statistics calculation periods
 */
enum class StatsPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
    ALL_TIME
}