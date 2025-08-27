package org.tabata.timber.domain.models.health

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Represents a single weight measurement entry
 */
data class WeightEntry(
    val id: String,
    val weight: Double,                     // Weight value
    val unit: WeightUnit,                   // Unit of measurement
    val date: LocalDate,                    // Date of measurement
    val time: Instant,                      // Exact timestamp of measurement
    val notes: String = "",                 // Optional notes
    val bodyFatPercentage: Double? = null,  // Optional body fat percentage
    val muscleMass: Double? = null,         // Optional muscle mass in kg
    val waterPercentage: Double? = null,    // Optional water percentage
    val visceralFat: Double? = null,        // Optional visceral fat rating
    val boneMass: Double? = null,           // Optional bone mass in kg
    val source: MeasurementSource = MeasurementSource.MANUAL
) {
    
    /**
     * Validation result for weight entry
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
    
    /**
     * Validates the weight entry
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Weight validation
        val weightInKg = when (unit) {
            WeightUnit.KG -> weight
            WeightUnit.LB -> weight * 0.453592
            WeightUnit.ST -> weight * 6.35029
        }
        
        if (weightInKg < MIN_WEIGHT_KG || weightInKg > MAX_WEIGHT_KG) {
            errors.add("體重必須在 ${MIN_WEIGHT_KG}kg 到 ${MAX_WEIGHT_KG}kg 之間")
        }
        
        // Body fat percentage validation
        bodyFatPercentage?.let { bf ->
            if (bf < 0.0 || bf > 100.0) {
                errors.add("體脂肪率必須在 0% 到 100% 之間")
            }
        }
        
        // Water percentage validation
        waterPercentage?.let { water ->
            if (water < 0.0 || water > 100.0) {
                errors.add("水分率必須在 0% 到 100% 之間")
            }
        }
        
        // Muscle mass validation
        muscleMass?.let { muscle ->
            if (muscle < 0.0 || muscle > weightInKg) {
                errors.add("肌肉量不能超過總體重")
            }
        }
        
        // Bone mass validation
        boneMass?.let { bone ->
            if (bone < 0.0 || bone > weightInKg * 0.2) { // Bone mass typically < 20% of body weight
                errors.add("骨量數值異常")
            }
        }
        
        // Visceral fat validation
        visceralFat?.let { vf ->
            if (vf < 0.0 || vf > 60.0) { // Typical visceral fat rating range
                errors.add("內臟脂肪等級必須在 0 到 60 之間")
            }
        }
        
        // Date validation
        val now = kotlinx.datetime.Clock.System.now()
        if (time > now) {
            errors.add("測量時間不能是未來時間")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Converts weight to kilograms
     */
    fun getWeightInKg(): Double {
        return when (unit) {
            WeightUnit.KG -> weight
            WeightUnit.LB -> weight * 0.453592
            WeightUnit.ST -> weight * 6.35029
        }
    }
    
    /**
     * Converts weight to specified unit
     */
    fun getWeightInUnit(targetUnit: WeightUnit): Double {
        val weightInKg = getWeightInKg()
        return when (targetUnit) {
            WeightUnit.KG -> weightInKg
            WeightUnit.LB -> weightInKg / 0.453592
            WeightUnit.ST -> weightInKg / 6.35029
        }
    }
    
    /**
     * Returns a formatted weight string with unit
     */
    fun getFormattedWeight(targetUnit: WeightUnit = unit, decimals: Int = 1): String {
        val weightValue = getWeightInUnit(targetUnit)
        return "${String.format("%.${decimals}f", weightValue)} ${targetUnit.symbol}"
    }
    
    /**
     * Calculates BMI if height is provided (in meters)
     */
    fun calculateBMI(heightInMeters: Double): Double? {
        return if (heightInMeters > 0) {
            val weightInKg = getWeightInKg()
            weightInKg / (heightInMeters * heightInMeters)
        } else null
    }
    
    /**
     * Gets BMI category if height is provided
     */
    fun getBMICategory(heightInMeters: Double): BMICategory? {
        return calculateBMI(heightInMeters)?.let { BMICategory.fromBMI(it) }
    }
    
    /**
     * Returns body composition summary if available
     */
    fun getBodyCompositionSummary(): BodyCompositionSummary? {
        return if (bodyFatPercentage != null || muscleMass != null || waterPercentage != null) {
            BodyCompositionSummary(
                totalWeight = getWeightInKg(),
                bodyFatPercentage = bodyFatPercentage,
                muscleMass = muscleMass,
                waterPercentage = waterPercentage,
                visceralFat = visceralFat,
                boneMass = boneMass
            )
        } else null
    }
    
    companion object {
        const val MIN_WEIGHT_KG = 1.0      // 1 kg minimum
        const val MAX_WEIGHT_KG = 1000.0   // 1000 kg maximum
        
        /**
         * Creates a new weight entry with current timestamp
         */
        fun create(
            weight: Double,
            unit: WeightUnit,
            date: LocalDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault()),
            notes: String = "",
            bodyFatPercentage: Double? = null,
            muscleMass: Double? = null,
            waterPercentage: Double? = null,
            visceralFat: Double? = null,
            boneMass: Double? = null,
            source: MeasurementSource = MeasurementSource.MANUAL
        ): WeightEntry {
            return WeightEntry(
                id = generateId(),
                weight = weight,
                unit = unit,
                date = date,
                time = kotlinx.datetime.Clock.System.now(),
                notes = notes,
                bodyFatPercentage = bodyFatPercentage,
                muscleMass = muscleMass,
                waterPercentage = waterPercentage,
                visceralFat = visceralFat,
                boneMass = boneMass,
                source = source
            )
        }
        
        private fun generateId(): String {
            return "weight_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * Weight units supported by the app
 */
enum class WeightUnit(
    val symbol: String,
    val displayName: String,
    val isMetric: Boolean
) {
    KG(
        symbol = "kg",
        displayName = "公斤",
        isMetric = true
    ),
    
    LB(
        symbol = "lb",
        displayName = "磅",
        isMetric = false
    ),
    
    ST(
        symbol = "st",
        displayName = "英石",
        isMetric = false
    );
    
    /**
     * Converts a weight value from this unit to kilograms
     */
    fun toKilograms(weight: Double): Double {
        return when (this) {
            KG -> weight
            LB -> weight * 0.453592
            ST -> weight * 6.35029
        }
    }
    
    /**
     * Converts a weight value from kilograms to this unit
     */
    fun fromKilograms(weightInKg: Double): Double {
        return when (this) {
            KG -> weightInKg
            LB -> weightInKg / 0.453592
            ST -> weightInKg / 6.35029
        }
    }
}

/**
 * Source of the weight measurement
 */
enum class MeasurementSource(
    val displayName: String
) {
    MANUAL("手動輸入"),
    SMART_SCALE("智能體重計"),
    FITNESS_APP("健身應用程式"),
    HEALTH_APP("健康應用程式"),
    IMPORT("資料匯入")
}

/**
 * Body composition summary
 */
data class BodyCompositionSummary(
    val totalWeight: Double,                // Total weight in kg
    val bodyFatPercentage: Double? = null,
    val muscleMass: Double? = null,         // In kg
    val waterPercentage: Double? = null,
    val visceralFat: Double? = null,        // Rating
    val boneMass: Double? = null            // In kg
) {
    
    /**
     * Calculates fat mass if body fat percentage is available
     */
    fun getFatMass(): Double? {
        return bodyFatPercentage?.let { totalWeight * (it / 100.0) }
    }
    
    /**
     * Calculates lean mass (muscle + bone + organs)
     */
    fun getLeanMass(): Double? {
        return bodyFatPercentage?.let { totalWeight * (1 - it / 100.0) }
    }
    
    /**
     * Returns body composition assessment
     */
    fun getAssessment(): String {
        return when {
            bodyFatPercentage == null -> "基本體重記錄"
            bodyFatPercentage < 10 -> "體脂率偏低"
            bodyFatPercentage < 20 -> "體脂率良好"
            bodyFatPercentage < 30 -> "體脂率正常"
            else -> "體脂率偏高"
        }
    }
}