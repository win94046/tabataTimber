package org.tabata.timber.domain.models.health

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utility object for BMI calculations and related health metrics
 */
object BMICalculator {
    
    /**
     * Calculates BMI from weight and height
     * @param weightInKg Weight in kilograms
     * @param heightInMeters Height in meters
     * @return BMI value, null if invalid input
     */
    fun calculateBMI(weightInKg: Double, heightInMeters: Double): Double? {
        return if (weightInKg > 0 && heightInMeters > 0) {
            weightInKg / (heightInMeters * heightInMeters)
        } else null
    }
    
    /**
     * Calculates BMI from weight and height with units
     * @param weight Weight value
     * @param weightUnit Weight unit
     * @param height Height value  
     * @param heightUnit Height unit
     * @return BMI value, null if invalid input
     */
    fun calculateBMI(
        weight: Double, 
        weightUnit: WeightUnit, 
        height: Double, 
        heightUnit: HeightUnit
    ): Double? {
        val weightInKg = weightUnit.toKilograms(weight)
        val heightInMeters = heightUnit.toMeters(height)
        
        return calculateBMI(weightInKg, heightInMeters)
    }
    
    /**
     * Calculates BMI category from BMI value
     */
    fun getBMICategory(bmi: Double): BMICategory {
        return BMICategory.fromBMI(bmi)
    }
    
    /**
     * Calculates ideal weight range for given height
     * @param heightInMeters Height in meters
     * @return Pair of (minimum ideal weight, maximum ideal weight) in kg
     */
    fun getIdealWeightRange(heightInMeters: Double): Pair<Double, Double>? {
        return if (heightInMeters > 0) {
            val minWeight = 18.5 * heightInMeters.pow(2)
            val maxWeight = 24.9 * heightInMeters.pow(2)
            Pair(minWeight, maxWeight)
        } else null
    }
    
    /**
     * Calculates ideal weight range with specified units
     */
    fun getIdealWeightRange(
        height: Double, 
        heightUnit: HeightUnit, 
        targetWeightUnit: WeightUnit
    ): Pair<Double, Double>? {
        val heightInMeters = heightUnit.toMeters(height)
        val rangeInKg = getIdealWeightRange(heightInMeters) ?: return null
        
        val minWeight = targetWeightUnit.fromKilograms(rangeInKg.first)
        val maxWeight = targetWeightUnit.fromKilograms(rangeInKg.second)
        
        return Pair(minWeight, maxWeight)
    }
    
    /**
     * Calculates weight needed to reach a target BMI
     * @param targetBMI Target BMI value
     * @param heightInMeters Height in meters
     * @return Target weight in kg, null if invalid input
     */
    fun getWeightForTargetBMI(targetBMI: Double, heightInMeters: Double): Double? {
        return if (targetBMI > 0 && heightInMeters > 0) {
            targetBMI * heightInMeters.pow(2)
        } else null
    }
    
    /**
     * Calculates body surface area (BSA) using the Mosteller formula
     * @param weightInKg Weight in kilograms
     * @param heightInCm Height in centimeters
     * @return BSA in square meters, null if invalid input
     */
    fun calculateBodySurfaceArea(weightInKg: Double, heightInCm: Double): Double? {
        return if (weightInKg > 0 && heightInCm > 0) {
            kotlin.math.sqrt((weightInKg * heightInCm) / 3600)
        } else null
    }
    
    /**
     * Calculates basal metabolic rate (BMR) using the Mifflin-St Jeor equation
     * @param weightInKg Weight in kilograms
     * @param heightInCm Height in centimeters
     * @param ageInYears Age in years
     * @param gender Gender (affects BMR calculation)
     * @return BMR in calories per day, null if invalid input
     */
    fun calculateBMR(
        weightInKg: Double, 
        heightInCm: Double, 
        ageInYears: Int, 
        gender: Gender
    ): Double? {
        return if (weightInKg > 0 && heightInCm > 0 && ageInYears > 0) {
            val baseBMR = 10 * weightInKg + 6.25 * heightInCm - 5 * ageInYears
            when (gender) {
                Gender.MALE -> baseBMR + 5
                Gender.FEMALE -> baseBMR - 161
            }
        } else null
    }
    
    /**
     * Calculates total daily energy expenditure (TDEE) from BMR and activity level
     * @param bmr Basal metabolic rate
     * @param activityLevel Physical activity level
     * @return TDEE in calories per day
     */
    fun calculateTDEE(bmr: Double, activityLevel: ActivityLevel): Double {
        return bmr * activityLevel.multiplier
    }
    
    /**
     * Formats BMI value for display
     * @param bmi BMI value
     * @param decimals Number of decimal places
     * @return Formatted BMI string
     */
    fun formatBMI(bmi: Double, decimals: Int = 1): String {
        return String.format("%.${decimals}f", bmi)
    }
    
    /**
     * Gets BMI health assessment with detailed information
     * @param bmi BMI value
     * @return Detailed BMI assessment
     */
    fun getBMIAssessment(bmi: Double): BMIAssessment {
        val category = getBMICategory(bmi)
        
        return BMIAssessment(
            bmi = bmi,
            category = category,
            healthRisk = category.healthRisk,
            description = category.description,
            recommendations = category.getHealthRecommendations(),
            exerciseRecommendations = category.getExerciseRecommendations()
        )
    }
    
    /**
     * Calculates weight change needed to reach normal BMI range
     * @param currentBMI Current BMI
     * @param heightInMeters Height in meters
     * @return WeightChangeRecommendation with target weights and changes needed
     */
    fun getWeightChangeRecommendation(
        currentBMI: Double, 
        heightInMeters: Double
    ): WeightChangeRecommendation? {
        if (currentBMI <= 0 || heightInMeters <= 0) return null
        
        val currentWeight = currentBMI * heightInMeters.pow(2)
        val normalMinWeight = 18.5 * heightInMeters.pow(2)
        val normalMaxWeight = 24.9 * heightInMeters.pow(2)
        
        val recommendation = when {
            currentBMI < 18.5 -> {
                // Underweight - need to gain
                val targetWeight = (normalMinWeight + normalMaxWeight) / 2
                val changeNeeded = targetWeight - currentWeight
                WeightChangeRecommendation(
                    currentWeight = currentWeight,
                    targetWeightMin = normalMinWeight,
                    targetWeightMax = normalMaxWeight,
                    recommendedTargetWeight = targetWeight,
                    weightChangeNeeded = changeNeeded,
                    changeDirection = WeightChangeDirection.GAIN,
                    timelineWeeks = (changeNeeded / 0.25).roundToInt().coerceAtLeast(4) // 0.25kg per week gain
                )
            }
            currentBMI > 25.0 -> {
                // Overweight - need to lose
                val targetWeight = normalMaxWeight
                val changeNeeded = currentWeight - targetWeight
                WeightChangeRecommendation(
                    currentWeight = currentWeight,
                    targetWeightMin = normalMinWeight,
                    targetWeightMax = normalMaxWeight,
                    recommendedTargetWeight = targetWeight,
                    weightChangeNeeded = -changeNeeded,
                    changeDirection = WeightChangeDirection.LOSE,
                    timelineWeeks = (changeNeeded / 0.5).roundToInt().coerceAtLeast(4) // 0.5kg per week loss
                )
            }
            else -> {
                // Normal range - maintain
                WeightChangeRecommendation(
                    currentWeight = currentWeight,
                    targetWeightMin = normalMinWeight,
                    targetWeightMax = normalMaxWeight,
                    recommendedTargetWeight = currentWeight,
                    weightChangeNeeded = 0.0,
                    changeDirection = WeightChangeDirection.MAINTAIN,
                    timelineWeeks = 0
                )
            }
        }
        
        return recommendation
    }
    
    /**
     * Validates height and weight values
     * @param weight Weight value
     * @param weightUnit Weight unit
     * @param height Height value
     * @param heightUnit Height unit
     * @return List of validation errors, empty if valid
     */
    fun validateInputs(
        weight: Double, 
        weightUnit: WeightUnit, 
        height: Double, 
        heightUnit: HeightUnit
    ): List<String> {
        val errors = mutableListOf<String>()
        
        // Convert to standard units for validation
        val weightInKg = weightUnit.toKilograms(weight)
        val heightInM = heightUnit.toMeters(height)
        
        // Weight validation
        if (weightInKg < 1.0 || weightInKg > 1000.0) {
            errors.add("體重必須在 1kg 到 1000kg 之間")
        }
        
        // Height validation
        if (heightInM < 0.5 || heightInM > 3.0) {
            errors.add("身高必須在 50cm 到 300cm 之間")
        }
        
        return errors
    }
}

/**
 * Height units supported for BMI calculations
 */
enum class HeightUnit(
    val symbol: String,
    val displayName: String
) {
    METERS("m", "公尺"),
    CENTIMETERS("cm", "公分"),
    FEET("ft", "英尺"),
    INCHES("in", "英吋");
    
    /**
     * Converts height value to meters
     */
    fun toMeters(height: Double): Double {
        return when (this) {
            METERS -> height
            CENTIMETERS -> height / 100.0
            FEET -> height * 0.3048
            INCHES -> height * 0.0254
        }
    }
    
    /**
     * Converts height from meters to this unit
     */
    fun fromMeters(heightInMeters: Double): Double {
        return when (this) {
            METERS -> heightInMeters
            CENTIMETERS -> heightInMeters * 100.0
            FEET -> heightInMeters / 0.3048
            INCHES -> heightInMeters / 0.0254
        }
    }
}

/**
 * Gender enumeration for BMR calculations
 */
enum class Gender(val displayName: String) {
    MALE("男性"),
    FEMALE("女性")
}

/**
 * Physical activity levels for TDEE calculation
 */
enum class ActivityLevel(
    val displayName: String,
    val description: String,
    val multiplier: Double
) {
    SEDENTARY(
        displayName = "久坐",
        description = "很少或沒有運動",
        multiplier = 1.2
    ),
    
    LIGHTLY_ACTIVE(
        displayName = "輕度活動", 
        description = "輕度運動/一週1-3天",
        multiplier = 1.375
    ),
    
    MODERATELY_ACTIVE(
        displayName = "中度活動",
        description = "中度運動/一週3-5天", 
        multiplier = 1.55
    ),
    
    VERY_ACTIVE(
        displayName = "高度活動",
        description = "高強度運動/一週6-7天",
        multiplier = 1.725
    ),
    
    EXTREMELY_ACTIVE(
        displayName = "極度活動",
        description = "非常高強度運動/體力勞動工作",
        multiplier = 1.9
    )
}

/**
 * Comprehensive BMI assessment
 */
data class BMIAssessment(
    val bmi: Double,
    val category: BMICategory,
    val healthRisk: HealthRisk,
    val description: String,
    val recommendations: List<String>,
    val exerciseRecommendations: List<String>
)

/**
 * Weight change recommendations
 */
data class WeightChangeRecommendation(
    val currentWeight: Double,              // Current weight in kg
    val targetWeightMin: Double,            // Minimum target weight in kg
    val targetWeightMax: Double,            // Maximum target weight in kg
    val recommendedTargetWeight: Double,    // Recommended target weight in kg
    val weightChangeNeeded: Double,         // Weight change needed in kg (+ for gain, - for loss)
    val changeDirection: WeightChangeDirection,
    val timelineWeeks: Int                  // Estimated timeline in weeks
) {
    
    /**
     * Gets a user-friendly summary message
     */
    fun getSummaryMessage(): String {
        return when (changeDirection) {
            WeightChangeDirection.GAIN -> 
                "建議增重 ${String.format("%.1f", kotlin.math.abs(weightChangeNeeded))} 公斤，預計需要 $timelineWeeks 週"
            WeightChangeDirection.LOSE -> 
                "建議減重 ${String.format("%.1f", kotlin.math.abs(weightChangeNeeded))} 公斤，預計需要 $timelineWeeks 週"
            WeightChangeDirection.MAINTAIN -> 
                "您的體重在健康範圍內，建議維持目前體重"
        }
    }
    
    /**
     * Gets weekly weight change target
     */
    fun getWeeklyTarget(): Double {
        return if (timelineWeeks > 0) weightChangeNeeded / timelineWeeks else 0.0
    }
}

/**
 * Weight change directions
 */
enum class WeightChangeDirection(val displayName: String) {
    GAIN("增重"),
    LOSE("減重"), 
    MAINTAIN("維持")
}