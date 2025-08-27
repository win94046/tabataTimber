package org.tabata.timber.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.tabata.timber.domain.models.WorkoutSession

/**
 * Repository interface for managing workout session history and statistics.
 * Handles workout tracking, progress analysis, and performance metrics.
 */
interface WorkoutHistoryRepository {
    
    /**
     * Gets all workout sessions, ordered by start time (newest first)
     * @return Flow of all workout sessions
     */
    fun getAllWorkouts(): Flow<List<WorkoutSession>>
    
    /**
     * Gets completed workout sessions only
     * @return Flow of completed workout sessions
     */
    fun getCompletedWorkouts(): Flow<List<WorkoutSession>>
    
    /**
     * Gets a specific workout session by ID
     * @param id The workout session ID
     * @return The workout session if found, null otherwise
     */
    suspend fun getWorkoutById(id: String): WorkoutSession?
    
    /**
     * Gets workouts within a specific date range
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of workouts in the date range
     */
    suspend fun getWorkoutsByDateRange(startDate: LocalDate, endDate: LocalDate): List<WorkoutSession>
    
    /**
     * Gets workouts for a specific timer configuration
     * @param configurationId The timer configuration ID
     * @return Flow of workouts using the specified configuration
     */
    fun getWorkoutsByConfiguration(configurationId: String): Flow<List<WorkoutSession>>
    
    /**
     * Gets the most recent workouts
     * @param limit Maximum number of workouts to return
     * @return List of recent workout sessions
     */
    suspend fun getRecentWorkouts(limit: Int = 10): List<WorkoutSession>
    
    /**
     * Creates a new workout session (typically when starting a workout)
     * @param session The workout session to create
     * @return Result with the created session or error
     */
    suspend fun createWorkoutSession(session: WorkoutSession): Result<WorkoutSession>
    
    /**
     * Updates an existing workout session (progress updates, completion, etc.)
     * @param session The updated workout session
     * @return Result indicating success or failure
     */
    suspend fun updateWorkoutSession(session: WorkoutSession): Result<WorkoutSession>
    
    /**
     * Updates workout session progress (for real-time updates during workout)
     * @param sessionId The session ID
     * @param completedCycles Current completed cycles
     * @param completedSets Current completed sets
     * @param completionPercentage Current completion percentage
     * @return Result indicating success or failure
     */
    suspend fun updateWorkoutProgress(
        sessionId: String,
        completedCycles: Int,
        completedSets: Int,
        completionPercentage: Float
    ): Result<Unit>
    
    /**
     * Marks a workout session as completed
     * @param sessionId The session ID
     * @param endTime When the workout ended
     * @param totalDuration Total workout duration in seconds
     * @return Result indicating success or failure
     */
    suspend fun completeWorkoutSession(
        sessionId: String, 
        endTime: Instant,
        totalDuration: Int
    ): Result<Unit>
    
    /**
     * Deletes a workout session
     * @param id The session ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteWorkoutSession(id: String): Result<Unit>
    
    /**
     * Deletes old workout sessions (for cleanup/privacy)
     * @param olderThan Delete sessions older than this date
     * @return Result with number of deleted sessions or error
     */
    suspend fun deleteOldWorkouts(olderThan: Instant): Result<Int>
    
    /**
     * Gets comprehensive workout statistics
     * @return Overall workout statistics
     */
    suspend fun getWorkoutStatistics(): WorkoutStatistics
    
    /**
     * Gets workout statistics for a specific time period
     * @param period The time period to analyze
     * @return Statistics for the specified period
     */
    suspend fun getWorkoutStatisticsForPeriod(period: StatisticsPeriod): WorkoutStatistics
    
    /**
     * Gets weekly workout statistics
     * @param weeksCount Number of weeks to include (default: 12 weeks)
     * @return List of weekly statistics
     */
    suspend fun getWeeklyStatistics(weeksCount: Int = 12): List<WeeklyWorkoutStats>
    
    /**
     * Gets monthly workout statistics
     * @param monthsCount Number of months to include (default: 12 months)
     * @return List of monthly statistics
     */
    suspend fun getMonthlyStatistics(monthsCount: Int = 12): List<MonthlyWorkoutStats>
    
    /**
     * Gets current workout streak (consecutive days with at least one workout)
     * @return Current streak in days
     */
    suspend fun getCurrentWorkoutStreak(): Int
    
    /**
     * Gets best workout streak ever achieved
     * @return Best streak in days
     */
    suspend fun getBestWorkoutStreak(): Int
    
    /**
     * Gets average workout duration across all completed workouts
     * @return Average duration in seconds
     */
    suspend fun getAverageWorkoutDuration(): Int
    
    /**
     * Gets total workout time across all completed sessions
     * @return Total time in seconds
     */
    suspend fun getTotalWorkoutTime(): Long
    
    /**
     * Gets total calories burned across all workouts
     * @return Total calories burned
     */
    suspend fun getTotalCaloriesBurned(): Int
    
    /**
     * Exports workout history for backup purposes
     * @return Result with serialized workout data or error
     */
    suspend fun exportWorkoutHistory(): Result<String>
    
    /**
     * Imports workout history from backup data
     * @param data The serialized workout data
     * @param replaceExisting Whether to replace existing data
     * @return Result indicating success with import count or error
     */
    suspend fun importWorkoutHistory(data: String, replaceExisting: Boolean = false): Result<Int>
}

/**
 * Overall workout statistics
 */
data class WorkoutStatistics(
    val totalWorkouts: Int = 0,
    val completedWorkouts: Int = 0,
    val totalDuration: Long = 0,  // in seconds
    val averageCompletionPercentage: Float = 0f,
    val totalCaloriesBurned: Int = 0,
    val averageRating: Float = 0f,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

/**
 * Weekly workout statistics
 */
data class WeeklyWorkoutStats(
    val weekStart: LocalDate,
    val workoutCount: Int = 0,
    val totalDuration: Long = 0,  // in seconds
    val averageCompletionPercentage: Float = 0f,
    val totalCaloriesBurned: Int = 0
)

/**
 * Monthly workout statistics
 */
data class MonthlyWorkoutStats(
    val month: String,  // YYYY-MM format
    val workoutCount: Int = 0,
    val totalDuration: Long = 0,  // in seconds
    val averageCompletionPercentage: Float = 0f,
    val totalCaloriesBurned: Int = 0
)

/**
 * Time periods for statistics analysis
 */
enum class StatisticsPeriod {
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    ALL_TIME
}

/**
 * Workout progress update data
 */
data class WorkoutProgress(
    val sessionId: String,
    val completedCycles: Int,
    val completedSets: Int,
    val completionPercentage: Float,
    val timestamp: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
)