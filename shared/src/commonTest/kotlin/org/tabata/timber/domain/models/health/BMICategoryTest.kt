package org.tabata.timber.domain.models.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BMICategoryTest {
    
    @Test
    fun `fromBMI correctly categorizes BMI values`() {
        assertEquals(BMICategory.UNDERWEIGHT, BMICategory.fromBMI(17.0))
        assertEquals(BMICategory.UNDERWEIGHT, BMICategory.fromBMI(18.4))
        
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(18.5))
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(22.0))
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(24.9))
        
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(25.0))
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(27.5))
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(29.9))
        
        assertEquals(BMICategory.OBESE_CLASS_I, BMICategory.fromBMI(30.0))
        assertEquals(BMICategory.OBESE_CLASS_I, BMICategory.fromBMI(32.5))
        assertEquals(BMICategory.OBESE_CLASS_I, BMICategory.fromBMI(34.9))
        
        assertEquals(BMICategory.OBESE_CLASS_II, BMICategory.fromBMI(35.0))
        assertEquals(BMICategory.OBESE_CLASS_II, BMICategory.fromBMI(37.5))
        assertEquals(BMICategory.OBESE_CLASS_II, BMICategory.fromBMI(39.9))
        
        assertEquals(BMICategory.OBESE_CLASS_III, BMICategory.fromBMI(40.0))
        assertEquals(BMICategory.OBESE_CLASS_III, BMICategory.fromBMI(45.0))
        assertEquals(BMICategory.OBESE_CLASS_III, BMICategory.fromBMI(50.0))
    }
    
    @Test
    fun `BMI ranges are correctly defined`() {
        assertEquals(0.0, BMICategory.UNDERWEIGHT.minBMI)
        assertEquals(18.49, BMICategory.UNDERWEIGHT.maxBMI)
        
        assertEquals(18.5, BMICategory.NORMAL.minBMI)
        assertEquals(24.99, BMICategory.NORMAL.maxBMI)
        
        assertEquals(25.0, BMICategory.OVERWEIGHT.minBMI)
        assertEquals(29.99, BMICategory.OVERWEIGHT.maxBMI)
        
        assertEquals(30.0, BMICategory.OBESE_CLASS_I.minBMI)
        assertEquals(34.99, BMICategory.OBESE_CLASS_I.maxBMI)
        
        assertEquals(35.0, BMICategory.OBESE_CLASS_II.minBMI)
        assertEquals(39.99, BMICategory.OBESE_CLASS_II.maxBMI)
        
        assertEquals(40.0, BMICategory.OBESE_CLASS_III.minBMI)
        assertEquals(Double.MAX_VALUE, BMICategory.OBESE_CLASS_III.maxBMI)
    }
    
    @Test
    fun `health risk levels are correctly assigned`() {
        assertEquals(HealthRisk.MODERATE, BMICategory.UNDERWEIGHT.healthRisk)
        assertEquals(HealthRisk.LOW, BMICategory.NORMAL.healthRisk)
        assertEquals(HealthRisk.MODERATE, BMICategory.OVERWEIGHT.healthRisk)
        assertEquals(HealthRisk.HIGH, BMICategory.OBESE_CLASS_I.healthRisk)
        assertEquals(HealthRisk.VERY_HIGH, BMICategory.OBESE_CLASS_II.healthRisk)
        assertEquals(HealthRisk.EXTREMELY_HIGH, BMICategory.OBESE_CLASS_III.healthRisk)
    }
    
    @Test
    fun `display names are in Chinese`() {
        assertEquals("體重過輕", BMICategory.UNDERWEIGHT.displayName)
        assertEquals("正常體重", BMICategory.NORMAL.displayName)
        assertEquals("體重過重", BMICategory.OVERWEIGHT.displayName)
        assertEquals("輕度肥胖", BMICategory.OBESE_CLASS_I.displayName)
        assertEquals("中度肥胖", BMICategory.OBESE_CLASS_II.displayName)
        assertEquals("重度肥胖", BMICategory.OBESE_CLASS_III.displayName)
    }
    
    @Test
    fun `getHealthRecommendations returns appropriate advice`() {
        val underweightRecs = BMICategory.UNDERWEIGHT.getHealthRecommendations()
        assertTrue(underweightRecs.any { it.contains("醫師") })
        assertTrue(underweightRecs.any { it.contains("營養") })
        
        val normalRecs = BMICategory.NORMAL.getHealthRecommendations()
        assertTrue(normalRecs.any { it.contains("維持") })
        
        val overweightRecs = BMICategory.OVERWEIGHT.getHealthRecommendations()
        assertTrue(overweightRecs.any { it.contains("減重") })
        
        val obeseRecs = BMICategory.OBESE_CLASS_III.getHealthRecommendations()
        assertTrue(obeseRecs.any { it.contains("醫師") })
        assertTrue(obeseRecs.any { it.contains("減重") })
    }
    
    @Test
    fun `getExerciseRecommendations returns appropriate exercises`() {
        val underweightExercises = BMICategory.UNDERWEIGHT.getExerciseRecommendations()
        assertTrue(underweightExercises.any { it.contains("阻力") || it.contains("重量") })
        
        val normalExercises = BMICategory.NORMAL.getExerciseRecommendations()
        assertTrue(normalExercises.any { it.contains("Tabata") })
        
        val overweightExercises = BMICategory.OVERWEIGHT.getExerciseRecommendations()
        assertTrue(overweightExercises.any { it.contains("有氧") })
        
        val obeseExercises = BMICategory.OBESE_CLASS_III.getExerciseRecommendations()
        assertTrue(obeseExercises.any { it.contains("輕度") || it.contains("水中") })
    }
    
    @Test
    fun `getIdealWeightRange calculates correct range for normal BMI`() {
        val heightInMeters = 1.75 // 175 cm
        val (minWeight, maxWeight) = BMICategory.NORMAL.getIdealWeightRange(heightInMeters)
        
        // BMI 18.5-24.99 for 1.75m height
        val expectedMinWeight = 18.5 * 1.75 * 1.75
        val expectedMaxWeight = 24.99 * 1.75 * 1.75
        
        assertEquals(expectedMinWeight, minWeight, 0.01)
        assertEquals(expectedMaxWeight, maxWeight, 0.01)
    }
    
    @Test
    fun `getIdealWeightRange returns normal range for non-normal categories`() {
        val heightInMeters = 1.70
        val underweightRange = BMICategory.UNDERWEIGHT.getIdealWeightRange(heightInMeters)
        val overweightRange = BMICategory.OVERWEIGHT.getIdealWeightRange(heightInMeters)
        val normalRange = BMICategory.NORMAL.getIdealWeightRange(heightInMeters)
        
        // All should return the same normal BMI range
        assertEquals(normalRange.first, underweightRange.first, 0.01)
        assertEquals(normalRange.second, underweightRange.second, 0.01)
        assertEquals(normalRange.first, overweightRange.first, 0.01)
        assertEquals(normalRange.second, overweightRange.second, 0.01)
    }
    
    @Test
    fun `getAllCategories returns all categories`() {
        val allCategories = BMICategory.getAllCategories()
        assertEquals(6, allCategories.size)
        assertEquals(BMICategory.values().toList(), allCategories)
    }
    
    @Test
    fun `getHealthyCategories returns only normal category`() {
        val healthyCategories = BMICategory.getHealthyCategories()
        assertEquals(listOf(BMICategory.NORMAL), healthyCategories)
    }
    
    @Test
    fun `getRiskCategories returns all non-normal categories`() {
        val riskCategories = BMICategory.getRiskCategories()
        val expectedRiskCategories = listOf(
            BMICategory.UNDERWEIGHT,
            BMICategory.OVERWEIGHT,
            BMICategory.OBESE_CLASS_I,
            BMICategory.OBESE_CLASS_II,
            BMICategory.OBESE_CLASS_III
        )
        assertEquals(expectedRiskCategories, riskCategories)
    }
    
    @Test
    fun `recommended colors are valid hex codes`() {
        BMICategory.values().forEach { category ->
            val color = category.recommendedColor
            assertTrue(color.startsWith("#"))
            assertEquals(7, color.length) // #RRGGBB format
            
            // Verify it's a valid hex string
            val hexPart = color.substring(1)
            hexPart.forEach { char ->
                assertTrue(char.isDigit() || char.uppercaseChar() in 'A'..'F')
            }
        }
    }
    
    @Test
    fun `all categories have unique colors`() {
        val colors = BMICategory.values().map { it.recommendedColor }
        val uniqueColors = colors.distinct()
        assertEquals(colors.size, uniqueColors.size)
    }
    
    @Test
    fun `health risk enum has correct values`() {
        val risks = HealthRisk.values()
        assertEquals(5, risks.size)
        
        assertEquals("低風險", HealthRisk.LOW.displayName)
        assertEquals("中等風險", HealthRisk.MODERATE.displayName)
        assertEquals("高風險", HealthRisk.HIGH.displayName)
        assertEquals("極高風險", HealthRisk.VERY_HIGH.displayName)
        assertEquals("嚴重風險", HealthRisk.EXTREMELY_HIGH.displayName)
    }
}