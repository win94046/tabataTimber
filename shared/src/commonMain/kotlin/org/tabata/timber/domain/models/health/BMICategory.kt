package org.tabata.timber.domain.models.health

/**
 * BMI categories based on WHO (World Health Organization) standards
 */
enum class BMICategory(
    val displayName: String,
    val description: String,
    val minBMI: Double,
    val maxBMI: Double,
    val healthRisk: HealthRisk,
    val recommendedColor: String
) {
    UNDERWEIGHT(
        displayName = "體重過輕",
        description = "BMI 低於 18.5",
        minBMI = 0.0,
        maxBMI = 18.49,
        healthRisk = HealthRisk.MODERATE,
        recommendedColor = "#42A5F5" // Blue
    ),
    
    NORMAL(
        displayName = "正常體重",
        description = "BMI 18.5 - 24.9",
        minBMI = 18.5,
        maxBMI = 24.99,
        healthRisk = HealthRisk.LOW,
        recommendedColor = "#66BB6A" // Green
    ),
    
    OVERWEIGHT(
        displayName = "體重過重",
        description = "BMI 25.0 - 29.9",
        minBMI = 25.0,
        maxBMI = 29.99,
        healthRisk = HealthRisk.MODERATE,
        recommendedColor = "#FFA726" // Orange
    ),
    
    OBESE_CLASS_I(
        displayName = "輕度肥胖",
        description = "BMI 30.0 - 34.9",
        minBMI = 30.0,
        maxBMI = 34.99,
        healthRisk = HealthRisk.HIGH,
        recommendedColor = "#FF7043" // Deep Orange
    ),
    
    OBESE_CLASS_II(
        displayName = "中度肥胖",
        description = "BMI 35.0 - 39.9",
        minBMI = 35.0,
        maxBMI = 39.99,
        healthRisk = HealthRisk.VERY_HIGH,
        recommendedColor = "#EF5350" // Red
    ),
    
    OBESE_CLASS_III(
        displayName = "重度肥胖",
        description = "BMI ≥ 40.0",
        minBMI = 40.0,
        maxBMI = Double.MAX_VALUE,
        healthRisk = HealthRisk.EXTREMELY_HIGH,
        recommendedColor = "#D32F2F" // Dark Red
    );
    
    /**
     * Returns health recommendations based on BMI category
     */
    fun getHealthRecommendations(): List<String> {
        return when (this) {
            UNDERWEIGHT -> listOf(
                "建議諮詢醫師或營養師",
                "增加營養攝取",
                "考慮進行阻力訓練增加肌肉量",
                "定期監測體重變化"
            )
            NORMAL -> listOf(
                "維持目前的健康體重",
                "持續均衡飲食",
                "保持規律運動習慣",
                "定期健康檢查"
            )
            OVERWEIGHT -> listOf(
                "建議減重 3-5 公斤",
                "增加有氧運動頻率",
                "控制飲食份量",
                "考慮諮詢營養師"
            )
            OBESE_CLASS_I -> listOf(
                "建議減重 5-10 公斤",
                "每週至少 150 分鐘中等強度運動",
                "諮詢醫師制定減重計劃",
                "監測血壓、血糖等指標"
            )
            OBESE_CLASS_II -> listOf(
                "強烈建議減重 10-15 公斤",
                "諮詢醫師制定綜合減重方案",
                "定期監測心血管健康指標",
                "考慮專業減重機構協助"
            )
            OBESE_CLASS_III -> listOf(
                "緊急需要減重",
                "立即諮詢醫師",
                "可能需要醫療介入",
                "密切監測健康狀況",
                "考慮減重手術選項"
            )
        }
    }
    
    /**
     * Returns fitness exercise recommendations
     */
    fun getExerciseRecommendations(): List<String> {
        return when (this) {
            UNDERWEIGHT -> listOf(
                "阻力訓練（重量訓練）",
                "肌力建構運動",
                "適度有氧運動",
                "瑜伽或伸展運動"
            )
            NORMAL -> listOf(
                "維持多樣化運動",
                "有氧運動 + 肌力訓練",
                "Tabata 高強度間歇訓練",
                "戶外活動和運動"
            )
            OVERWEIGHT, OBESE_CLASS_I -> listOf(
                "低衝擊有氧運動",
                "快走或慢跑",
                "游泳或水中運動",
                "Tabata 訓練（調整強度）"
            )
            OBESE_CLASS_II, OBESE_CLASS_III -> listOf(
                "從輕度運動開始",
                "水中運動（關節友善）",
                "坐姿運動",
                "在專業指導下進行運動"
            )
        }
    }
    
    /**
     * Returns the ideal weight range for the given height (in meters)
     */
    fun getIdealWeightRange(heightInMeters: Double): Pair<Double, Double> {
        return when (this) {
            NORMAL -> {
                val minWeight = 18.5 * heightInMeters * heightInMeters
                val maxWeight = 24.99 * heightInMeters * heightInMeters
                Pair(minWeight, maxWeight)
            }
            else -> {
                // For non-normal categories, return the normal weight range
                val minWeight = 18.5 * heightInMeters * heightInMeters
                val maxWeight = 24.99 * heightInMeters * heightInMeters
                Pair(minWeight, maxWeight)
            }
        }
    }
    
    companion object {
        /**
         * Determines BMI category from BMI value
         */
        fun fromBMI(bmi: Double): BMICategory {
            return values().first { bmi >= it.minBMI && bmi <= it.maxBMI }
        }
        
        /**
         * Returns all categories for display purposes
         */
        fun getAllCategories(): List<BMICategory> = values().toList()
        
        /**
         * Returns categories that are considered healthy
         */
        fun getHealthyCategories(): List<BMICategory> {
            return listOf(NORMAL)
        }
        
        /**
         * Returns categories that indicate health risks
         */
        fun getRiskCategories(): List<BMICategory> {
            return listOf(UNDERWEIGHT, OVERWEIGHT, OBESE_CLASS_I, OBESE_CLASS_II, OBESE_CLASS_III)
        }
    }
}

/**
 * Health risk levels associated with BMI categories
 */
enum class HealthRisk(
    val displayName: String,
    val description: String
) {
    LOW(
        displayName = "低風險",
        description = "健康風險較低"
    ),
    
    MODERATE(
        displayName = "中等風險", 
        description = "有一定健康風險，建議注意"
    ),
    
    HIGH(
        displayName = "高風險",
        description = "健康風險較高，建議採取行動"
    ),
    
    VERY_HIGH(
        displayName = "極高風險",
        description = "健康風險極高，強烈建議尋求專業協助"
    ),
    
    EXTREMELY_HIGH(
        displayName = "嚴重風險",
        description = "嚴重健康風險，需要立即醫療關注"
    )
}