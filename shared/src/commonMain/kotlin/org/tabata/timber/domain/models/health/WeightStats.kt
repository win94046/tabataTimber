package org.tabata.timber.domain.models.health

import kotlinx.datetime.LocalDate

/**
 * Statistical analysis of weight tracking data
 */
data class WeightStats(
    val entries: List<WeightEntry>,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val unit: WeightUnit = WeightUnit.KG
) {
    
    /**
     * Current weight (most recent entry)
     */
    val currentWeight: Double? by lazy {
        entries.maxByOrNull { it.date }?.getWeightInUnit(unit)
    }
    
    /**
     * Starting weight (oldest entry in period)
     */
    val startWeight: Double? by lazy {
        entries.minByOrNull { it.date }?.getWeightInUnit(unit)
    }
    
    /**
     * Minimum weight in the period
     */
    val minWeight: Double? by lazy {
        entries.minOfOrNull { it.getWeightInUnit(unit) }
    }
    
    /**
     * Maximum weight in the period
     */
    val maxWeight: Double? by lazy {
        entries.maxOfOrNull { it.getWeightInUnit(unit) }
    }
    
    /**
     * Average weight in the period
     */
    val averageWeight: Double? by lazy {
        if (entries.isEmpty()) null
        else entries.map { it.getWeightInUnit(unit) }.average()
    }
    
    /**
     * Total weight change from start to current
     */
    val totalWeightChange: Double? by lazy {
        if (currentWeight != null && startWeight != null) {
            currentWeight!! - startWeight!!
        } else null
    }
    
    /**
     * Weight change percentage
     */
    val weightChangePercentage: Double? by lazy {
        if (totalWeightChange != null && startWeight != null && startWeight!! > 0) {
            (totalWeightChange!! / startWeight!!) * 100.0
        } else null
    }
    
    /**
     * Average weight change per week
     */
    val averageWeeklyChange: Double? by lazy {
        if (totalWeightChange != null) {
            val days = periodStart.daysUntil(periodEnd)
            if (days > 0) {
                totalWeightChange!! * 7.0 / days
            } else null
        } else null
    }
    
    /**
     * Weight trend direction
     */
    val trend: WeightTrend by lazy {
        calculateTrend()
    }
    
    /**
     * Standard deviation of weights
     */
    val standardDeviation: Double? by lazy {
        if (entries.size < 2 || averageWeight == null) null
        else {
            val mean = averageWeight!!
            val variance = entries.map { 
                val weight = it.getWeightInUnit(unit)
                (weight - mean) * (weight - mean)
            }.average()
            kotlin.math.sqrt(variance)
        }
    }
    
    /**
     * Weight volatility (how much weight fluctuates)
     */
    val volatility: WeightVolatility by lazy {
        calculateVolatility()
    }
    
    /**
     * Progress towards goal (if goal weight is provided)
     */
    fun getProgressTowardsGoal(goalWeight: Double, goalUnit: WeightUnit = unit): GoalProgress? {
        if (startWeight == null || currentWeight == null) return null
        
        val goalInCurrentUnit = when {
            goalUnit == unit -> goalWeight
            else -> WeightUnit.KG.let { kg ->
                val goalInKg = goalUnit.toKilograms(goalWeight)
                unit.fromKilograms(goalInKg)
            }
        }
        
        val totalNeededChange = goalInCurrentUnit - startWeight!!
        val actualChange = currentWeight!! - startWeight!!
        
        val progressPercentage = if (totalNeededChange != 0.0) {
            (actualChange / totalNeededChange) * 100.0
        } else 100.0
        
        return GoalProgress(
            startWeight = startWeight!!,
            currentWeight = currentWeight!!,
            goalWeight = goalInCurrentUnit,
            progressPercentage = progressPercentage.coerceIn(0.0, 100.0),
            remainingChange = goalInCurrentUnit - currentWeight!!,
            unit = unit
        )
    }
    
    /**
     * Gets weight entries grouped by week
     */
    fun getWeeklyGrouped(): Map<LocalDate, List<WeightEntry>> {
        return entries.groupBy { entry ->
            // Get the start of the week (Monday)
            val dayOfWeek = entry.date.dayOfWeek.ordinal // 0 = Monday
            entry.date.minus(kotlinx.datetime.DateTimeUnit.DAY, dayOfWeek)
        }
    }
    
    /**
     * Gets weight entries grouped by month
     */
    fun getMonthlyGrouped(): Map<String, List<WeightEntry>> {
        return entries.groupBy { entry ->
            "${entry.date.year}-${entry.date.monthNumber.toString().padStart(2, '0')}"
        }
    }
    
    /**
     * Gets moving average for specified window
     */
    fun getMovingAverage(windowSize: Int): List<Pair<LocalDate, Double>> {
        if (entries.size < windowSize) return emptyList()
        
        val sortedEntries = entries.sortedBy { it.date }
        val result = mutableListOf<Pair<LocalDate, Double>>()
        
        for (i in windowSize - 1 until sortedEntries.size) {
            val window = sortedEntries.subList(i - windowSize + 1, i + 1)
            val average = window.map { it.getWeightInUnit(unit) }.average()
            result.add(Pair(sortedEntries[i].date, average))
        }
        
        return result
    }
    
    /**
     * Calculates BMI statistics if height is provided
     */
    fun getBMIStats(heightInMeters: Double): BMIStats? {
        if (entries.isEmpty() || heightInMeters <= 0) return null
        
        val bmiValues = entries.mapNotNull { entry ->
            entry.calculateBMI(heightInMeters)?.let { bmi ->
                Pair(entry.date, bmi)
            }
        }.sortedBy { it.first }
        
        if (bmiValues.isEmpty()) return null
        
        val currentBMI = bmiValues.lastOrNull()?.second
        val startBMI = bmiValues.firstOrNull()?.second
        
        return BMIStats(
            currentBMI = currentBMI,
            startBMI = startBMI,
            averageBMI = bmiValues.map { it.second }.average(),
            minBMI = bmiValues.minOfOrNull { it.second },
            maxBMI = bmiValues.maxOfOrNull { it.second },
            currentCategory = currentBMI?.let { BMICategory.fromBMI(it) },
            bmiChange = if (currentBMI != null && startBMI != null) currentBMI - startBMI else null,
            heightInMeters = heightInMeters
        )
    }
    
    /**
     * Gets streak information (consecutive days with measurements)
     */
    fun getStreakInfo(): StreakInfo {
        if (entries.isEmpty()) {
            return StreakInfo(currentStreak = 0, longestStreak = 0, lastMeasurementDate = null)
        }
        
        val sortedDates = entries.map { it.date }.distinct().sorted()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 1
        
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val lastDate = sortedDates.last()
        
        // Calculate current streak from the most recent date
        if (lastDate == today || lastDate == today.minus(kotlinx.datetime.DateTimeUnit.DAY, 1)) {
            currentStreak = 1
            
            for (i in sortedDates.size - 2 downTo 0) {
                val current = sortedDates[i]
                val next = sortedDates[i + 1]
                
                if (current == next.minus(kotlinx.datetime.DateTimeUnit.DAY, 1)) {
                    currentStreak++
                } else {
                    break
                }
            }
        }
        
        // Calculate longest streak
        for (i in 1 until sortedDates.size) {
            val current = sortedDates[i]
            val previous = sortedDates[i - 1]
            
            if (current == previous.plus(kotlinx.datetime.DateTimeUnit.DAY, 1)) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)
        
        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastMeasurementDate = lastDate
        )
    }
    
    private fun calculateTrend(): WeightTrend {
        if (entries.size < 2) return WeightTrend.STABLE
        
        return when {
            totalWeightChange == null -> WeightTrend.STABLE
            totalWeightChange!! > 1.0 -> WeightTrend.INCREASING
            totalWeightChange!! < -1.0 -> WeightTrend.DECREASING
            else -> WeightTrend.STABLE
        }
    }
    
    private fun calculateVolatility(): WeightVolatility {
        if (standardDeviation == null) return WeightVolatility.STABLE
        
        return when {
            standardDeviation!! > 2.0 -> WeightVolatility.HIGH
            standardDeviation!! > 1.0 -> WeightVolatility.MODERATE
            else -> WeightVolatility.LOW
        }
    }
    
    companion object {
        /**
         * Creates weight stats for a specific period
         */
        fun forPeriod(
            entries: List<WeightEntry>,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            unit: WeightUnit = WeightUnit.KG
        ): WeightStats {
            val filteredEntries = entries.filter { entry ->
                entry.date >= periodStart && entry.date <= periodEnd
            }
            
            return WeightStats(
                entries = filteredEntries,
                periodStart = periodStart,
                periodEnd = periodEnd,
                unit = unit
            )
        }
        
        /**
         * Creates weight stats for the last N days
         */
        fun forLastDays(
            entries: List<WeightEntry>,
            days: Int,
            unit: WeightUnit = WeightUnit.KG
        ): WeightStats {
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val startDate = today.minus(kotlinx.datetime.DateTimeUnit.DAY, days - 1)
            
            return forPeriod(entries, startDate, today, unit)
        }
        
        /**
         * Creates weight stats for the current month
         */
        fun forCurrentMonth(
            entries: List<WeightEntry>,
            unit: WeightUnit = WeightUnit.KG
        ): WeightStats {
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val startOfMonth = LocalDate(today.year, today.month, 1)
            
            return forPeriod(entries, startOfMonth, today, unit)
        }
    }
}

/**
 * Weight trend directions
 */
enum class WeightTrend(val displayName: String) {
    INCREASING("增加中"),
    DECREASING("減少中"),
    STABLE("穩定")
}

/**
 * Weight change volatility levels
 */
enum class WeightVolatility(val displayName: String) {
    LOW("穩定"),
    MODERATE("輕微波動"),
    HIGH("大幅波動")
}

/**
 * Progress towards a weight goal
 */
data class GoalProgress(
    val startWeight: Double,
    val currentWeight: Double,
    val goalWeight: Double,
    val progressPercentage: Double,
    val remainingChange: Double,
    val unit: WeightUnit
) {
    
    /**
     * Indicates if the goal has been achieved
     */
    val isGoalAchieved: Boolean = progressPercentage >= 100.0
    
    /**
     * Indicates if progress is being made in the right direction
     */
    val isProgressPositive: Boolean = when {
        goalWeight > startWeight -> currentWeight > startWeight // Gaining weight goal
        goalWeight < startWeight -> currentWeight < startWeight // Losing weight goal
        else -> true // Maintenance goal
    }
    
    /**
     * Gets a status message for the progress
     */
    fun getStatusMessage(): String {
        return when {
            isGoalAchieved -> "恭喜！已達成目標體重"
            isProgressPositive && progressPercentage > 75 -> "即將達成目標！"
            isProgressPositive && progressPercentage > 50 -> "進度良好"
            isProgressPositive -> "正在朝目標前進"
            else -> "需要調整方向"
        }
    }
}

/**
 * BMI-related statistics
 */
data class BMIStats(
    val currentBMI: Double?,
    val startBMI: Double?,
    val averageBMI: Double,
    val minBMI: Double?,
    val maxBMI: Double?,
    val currentCategory: BMICategory?,
    val bmiChange: Double?,
    val heightInMeters: Double
) {
    
    /**
     * Gets health assessment based on BMI
     */
    fun getHealthAssessment(): String {
        return currentCategory?.getHealthRecommendations()?.firstOrNull() ?: "無法評估"
    }
    
    /**
     * Indicates if BMI is improving (moving towards normal range)
     */
    val isBMIImproving: Boolean = when {
        bmiChange == null || currentCategory == null -> false
        currentCategory == BMICategory.NORMAL -> true
        currentCategory == BMICategory.UNDERWEIGHT -> bmiChange > 0
        else -> bmiChange < 0 // Overweight or obese categories
    }
}

/**
 * Streak tracking information
 */
data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastMeasurementDate: LocalDate?
) {
    
    /**
     * Gets encouragement message based on streak
     */
    fun getEncouragementMessage(): String {
        return when {
            currentStreak == 0 -> "開始記錄您的體重吧！"
            currentStreak == 1 -> "很好的開始！"
            currentStreak < 7 -> "保持記錄！目前連續 $currentStreak 天"
            currentStreak < 30 -> "優秀！連續記錄 $currentStreak 天"
            else -> "太棒了！連續記錄 $currentStreak 天，養成了良好習慣"
        }
    }
    
    /**
     * Indicates if the user is at risk of breaking their streak
     */
    val isStreakAtRisk: Boolean = currentStreak > 0 && lastMeasurementDate?.let { lastDate ->
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        lastDate < today
    } ?: true
}