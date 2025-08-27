package org.tabata.timber.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import org.tabata.timber.domain.models.health.*

/**
 * Repository interface for managing weight tracking and health data.
 * Handles weight entries, user profile, and health analytics.
 */
interface WeightTrackingRepository {
    
    /**
     * Gets all weight entries, ordered by date (newest first)
     * @return Flow of all weight entries
     */
    fun getAllWeightEntries(): Flow<List<WeightEntry>>
    
    /**
     * Gets a specific weight entry by ID
     * @param id The weight entry ID
     * @return The weight entry if found, null otherwise
     */
    suspend fun getWeightEntryById(id: String): WeightEntry?
    
    /**
     * Gets weight entries within a specific date range
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of weight entries in the date range
     */
    suspend fun getWeightEntriesByDateRange(startDate: LocalDate, endDate: LocalDate): List<WeightEntry>
    
    /**
     * Gets the most recent weight entries
     * @param limit Maximum number of entries to return
     * @return List of recent weight entries
     */
    suspend fun getRecentWeightEntries(limit: Int = 10): List<WeightEntry>
    
    /**
     * Gets the latest weight entry
     * @return The most recent weight entry, null if none exists
     */
    suspend fun getLatestWeightEntry(): WeightEntry?
    
    /**
     * Gets weight entries for a specific month
     * @param year The year
     * @param month The month (1-12)
     * @return List of weight entries for the specified month
     */
    suspend fun getWeightEntriesForMonth(year: Int, month: Int): List<WeightEntry>
    
    /**
     * Gets weight entries for a specific year
     * @param year The year
     * @return List of weight entries for the specified year
     */
    suspend fun getWeightEntriesForYear(year: Int): List<WeightEntry>
    
    /**
     * Creates a new weight entry
     * @param entry The weight entry to create
     * @return Result with the created entry or error
     */
    suspend fun createWeightEntry(entry: WeightEntry): Result<WeightEntry>
    
    /**
     * Updates an existing weight entry
     * @param entry The updated weight entry
     * @return Result indicating success or failure
     */
    suspend fun updateWeightEntry(entry: WeightEntry): Result<WeightEntry>
    
    /**
     * Deletes a weight entry
     * @param id The entry ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteWeightEntry(id: String): Result<Unit>
    
    /**
     * Deletes old weight entries (for cleanup/privacy)
     * @param olderThan Delete entries older than this date
     * @return Result with number of deleted entries or error
     */
    suspend fun deleteOldWeightEntries(olderThan: LocalDate): Result<Int>
    
    /**
     * Gets weight statistics and trends
     * @return Comprehensive weight statistics
     */
    suspend fun getWeightStatistics(): WeightStatistics
    
    /**
     * Gets weight trend data for charting
     * @param period The period to analyze
     * @return List of weight trend data points
     */
    suspend fun getWeightTrend(period: StatisticsPeriod = StatisticsPeriod.ALL_TIME): List<WeightTrendPoint>
    
    /**
     * Gets monthly weight averages
     * @param monthsCount Number of months to include
     * @return List of monthly weight averages
     */
    suspend fun getMonthlyWeightAverages(monthsCount: Int = 12): List<MonthlyWeightAverage>
    
    /**
     * Gets weekly weight progress
     * @param weeksCount Number of weeks to include
     * @return List of weekly weight progress
     */
    suspend fun getWeeklyWeightProgress(weeksCount: Int = 12): List<WeeklyWeightProgress>
    
    /**
     * Gets entries with body composition data only
     * @return Flow of entries that have body composition measurements
     */
    fun getEntriesWithBodyComposition(): Flow<List<WeightEntry>>
    
    /**
     * Gets body composition trend data
     * @return List of body composition trend data points
     */
    suspend fun getBodyCompositionTrend(): List<BodyCompositionTrendPoint>
    
    /**
     * Gets user health profile
     * @return The user's health profile
     */
    suspend fun getUserProfile(): UserProfile?
    
    /**
     * Updates user health profile
     * @param profile The updated user profile
     * @return Result indicating success or failure
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile>
    
    /**
     * Gets BMI data with calculated values
     * @return Flow of BMI data points (requires height in profile)
     */
    fun getBMIData(): Flow<List<BMIDataPoint>>
    
    /**
     * Calculates target weight recommendations based on BMI and user goals
     * @param targetBMI The target BMI (optional, defaults to healthy range)
     * @return Weight recommendations or null if height not set
     */
    suspend fun getWeightRecommendations(targetBMI: Double? = null): WeightRecommendations?
    
    /**
     * Validates a weight entry before saving
     * @param entry The weight entry to validate
     * @return Result indicating validation success or failure
     */
    suspend fun validateWeightEntry(entry: WeightEntry): Result<Unit>
    
    /**
     * Exports weight tracking data for backup purposes
     * @return Result with serialized weight data or error
     */
    suspend fun exportWeightData(): Result<String>
    
    /**
     * Imports weight tracking data from backup
     * @param data The serialized weight data
     * @param replaceExisting Whether to replace existing data
     * @return Result indicating success with import count or error
     */
    suspend fun importWeightData(data: String, replaceExisting: Boolean = false): Result<Int>
}

/**
 * User health profile data
 */
data class UserProfile(
    val heightCm: Double? = null,
    val birthDate: LocalDate? = null,
    val gender: Gender? = null,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val targetWeightKg: Double? = null,
    val preferredUnit: WeightUnit = WeightUnit.KG
)

/**
 * User gender options
 */
enum class Gender {
    MALE, FEMALE, OTHER
}

/**
 * Activity level options for calorie calculations
 */
enum class ActivityLevel {
    SEDENTARY,      // Little to no exercise
    LIGHT,          // Light exercise 1-3 days/week
    MODERATE,       // Moderate exercise 3-5 days/week
    ACTIVE,         // Heavy exercise 6-7 days/week
    VERY_ACTIVE     // Very heavy exercise, physical job, or 2x/day training
}

/**
 * Weight statistics and analytics
 */
data class WeightStatistics(
    val totalEntries: Int = 0,
    val minWeight: Double = 0.0,
    val maxWeight: Double = 0.0,
    val averageWeight: Double = 0.0,
    val weightRange: Double = 0.0,
    val currentWeight: Double? = null,
    val startingWeight: Double? = null,
    val weightChange: Double? = null,
    val weightChangePercentage: Double? = null,
    val averageWeeklyChange: Double? = null,
    val averageMonthlyChange: Double? = null
)

/**
 * Weight trend data point for charting
 */
data class WeightTrendPoint(
    val date: LocalDate,
    val weight: Double,
    val previousWeight: Double?,
    val weightChange: Double?
)

/**
 * Monthly weight average
 */
data class MonthlyWeightAverage(
    val month: String,  // YYYY-MM format
    val averageWeight: Double,
    val minWeight: Double,
    val maxWeight: Double,
    val entryCount: Int
)

/**
 * Weekly weight progress
 */
data class WeeklyWeightProgress(
    val weekStart: LocalDate,
    val averageWeight: Double,
    val minWeight: Double,
    val maxWeight: Double,
    val entryCount: Int
)

/**
 * Body composition trend data point
 */
data class BodyCompositionTrendPoint(
    val date: LocalDate,
    val weight: Double,
    val bodyFatPercentage: Double? = null,
    val muscleMass: Double? = null,
    val waterPercentage: Double? = null,
    val visceralFat: Double? = null,
    val boneMass: Double? = null
)

/**
 * BMI data point with calculated values
 */
data class BMIDataPoint(
    val id: String,
    val date: LocalDate,
    val weight: Double,  // in kg
    val height: Double,  // in cm
    val bmi: Double,
    val category: BMICategory
)

/**
 * Weight recommendations based on health goals
 */
data class WeightRecommendations(
    val currentWeight: Double,  // in kg
    val currentBMI: Double,
    val currentCategory: BMICategory,
    val targetWeight: Double,   // in kg
    val targetBMI: Double,
    val targetCategory: BMICategory,
    val weightToLose: Double?,  // positive if need to lose weight
    val weightToGain: Double?,  // positive if need to gain weight
    val recommendedWeeklyChange: Double,  // kg per week
    val estimatedWeeksToTarget: Int?
)