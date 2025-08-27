package org.tabata.timber.domain.models.health

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BMICalculatorTest {
    
    @Test
    fun `calculateBMI with valid inputs returns correct value`() {
        val bmi = BMICalculator.calculateBMI(70.0, 1.75)
        assertNotNull(bmi)
        assertEquals(22.86, bmi, 0.01)
    }
    
    @Test
    fun `calculateBMI with zero weight returns null`() {
        val bmi = BMICalculator.calculateBMI(0.0, 1.75)
        assertNull(bmi)
    }
    
    @Test
    fun `calculateBMI with zero height returns null`() {
        val bmi = BMICalculator.calculateBMI(70.0, 0.0)
        assertNull(bmi)
    }
    
    @Test
    fun `calculateBMI with negative inputs returns null`() {
        assertNull(BMICalculator.calculateBMI(-70.0, 1.75))
        assertNull(BMICalculator.calculateBMI(70.0, -1.75))
    }
    
    @Test
    fun `calculateBMI with units converts correctly`() {
        // Test kg and cm
        val bmiMetric = BMICalculator.calculateBMI(70.0, WeightUnit.KG, 175.0, HeightUnit.CENTIMETERS)
        assertNotNull(bmiMetric)
        assertEquals(22.86, bmiMetric, 0.01)
        
        // Test lb and inches (154.32 lb ≈ 70 kg, 68.9 inches ≈ 175 cm)
        val bmiImperial = BMICalculator.calculateBMI(154.32, WeightUnit.LB, 68.9, HeightUnit.INCHES)
        assertNotNull(bmiImperial)
        assertEquals(22.86, bmiImperial, 0.1) // Allow for conversion rounding
    }
    
    @Test
    fun `getBMICategory returns correct category`() {
        assertEquals(BMICategory.UNDERWEIGHT, BMICalculator.getBMICategory(17.0))
        assertEquals(BMICategory.NORMAL, BMICalculator.getBMICategory(22.0))
        assertEquals(BMICategory.OVERWEIGHT, BMICalculator.getBMICategory(27.0))
        assertEquals(BMICategory.OBESE_CLASS_I, BMICalculator.getBMICategory(32.0))
    }
    
    @Test
    fun `getIdealWeightRange returns correct range`() {
        val range = BMICalculator.getIdealWeightRange(1.75)
        assertNotNull(range)
        
        // BMI 18.5-24.9 for 1.75m height
        val expectedMin = 18.5 * 1.75 * 1.75
        val expectedMax = 24.9 * 1.75 * 1.75
        
        assertEquals(expectedMin, range.first, 0.01)
        assertEquals(expectedMax, range.second, 0.01)
    }
    
    @Test
    fun `getIdealWeightRange with zero height returns null`() {
        val range = BMICalculator.getIdealWeightRange(0.0)
        assertNull(range)
    }
    
    @Test
    fun `getIdealWeightRange with units converts correctly`() {
        val rangeMetric = BMICalculator.getIdealWeightRange(175.0, HeightUnit.CENTIMETERS, WeightUnit.KG)
        assertNotNull(rangeMetric)
        
        val rangeLb = BMICalculator.getIdealWeightRange(175.0, HeightUnit.CENTIMETERS, WeightUnit.LB)
        assertNotNull(rangeLb)
        
        // Convert first result to pounds and compare
        val convertedMin = WeightUnit.LB.fromKilograms(rangeMetric.first)
        val convertedMax = WeightUnit.LB.fromKilograms(rangeMetric.second)
        
        assertEquals(convertedMin, rangeLb.first, 0.1)
        assertEquals(convertedMax, rangeLb.second, 0.1)
    }
    
    @Test
    fun `getWeightForTargetBMI calculates correctly`() {
        val targetWeight = BMICalculator.getWeightForTargetBMI(22.0, 1.75)
        assertNotNull(targetWeight)
        
        val expectedWeight = 22.0 * 1.75 * 1.75
        assertEquals(expectedWeight, targetWeight, 0.01)
    }
    
    @Test
    fun `getWeightForTargetBMI with invalid inputs returns null`() {
        assertNull(BMICalculator.getWeightForTargetBMI(0.0, 1.75))
        assertNull(BMICalculator.getWeightForTargetBMI(22.0, 0.0))
        assertNull(BMICalculator.getWeightForTargetBMI(-22.0, 1.75))
    }
    
    @Test
    fun `calculateBodySurfaceArea calculates correctly`() {
        val bsa = BMICalculator.calculateBodySurfaceArea(70.0, 175.0)
        assertNotNull(bsa)
        
        // Mosteller formula: sqrt((weight * height) / 3600)
        val expected = kotlin.math.sqrt((70.0 * 175.0) / 3600.0)
        assertEquals(expected, bsa, 0.01)
    }
    
    @Test
    fun `calculateBodySurfaceArea with invalid inputs returns null`() {
        assertNull(BMICalculator.calculateBodySurfaceArea(0.0, 175.0))
        assertNull(BMICalculator.calculateBodySurfaceArea(70.0, 0.0))
        assertNull(BMICalculator.calculateBodySurfaceArea(-70.0, 175.0))
    }
    
    @Test
    fun `calculateBMR calculates correctly for male`() {
        val bmr = BMICalculator.calculateBMR(70.0, 175.0, 30, Gender.MALE)
        assertNotNull(bmr)
        
        // Mifflin-St Jeor equation for males: 10*weight + 6.25*height - 5*age + 5
        val expected = 10 * 70.0 + 6.25 * 175.0 - 5 * 30 + 5
        assertEquals(expected, bmr, 0.01)
    }
    
    @Test
    fun `calculateBMR calculates correctly for female`() {
        val bmr = BMICalculator.calculateBMR(60.0, 165.0, 25, Gender.FEMALE)
        assertNotNull(bmr)
        
        // Mifflin-St Jeor equation for females: 10*weight + 6.25*height - 5*age - 161
        val expected = 10 * 60.0 + 6.25 * 165.0 - 5 * 25 - 161
        assertEquals(expected, bmr, 0.01)
    }
    
    @Test
    fun `calculateBMR with invalid inputs returns null`() {
        assertNull(BMICalculator.calculateBMR(0.0, 175.0, 30, Gender.MALE))
        assertNull(BMICalculator.calculateBMR(70.0, 0.0, 30, Gender.MALE))
        assertNull(BMICalculator.calculateBMR(70.0, 175.0, 0, Gender.MALE))
    }
    
    @Test
    fun `calculateTDEE multiplies BMR correctly`() {
        val bmr = 1600.0
        
        assertEquals(1920.0, BMICalculator.calculateTDEE(bmr, ActivityLevel.SEDENTARY), 0.01)
        assertEquals(2200.0, BMICalculator.calculateTDEE(bmr, ActivityLevel.LIGHTLY_ACTIVE), 0.01)
        assertEquals(2480.0, BMICalculator.calculateTDEE(bmr, ActivityLevel.MODERATELY_ACTIVE), 0.01)
        assertEquals(2760.0, BMICalculator.calculateTDEE(bmr, ActivityLevel.VERY_ACTIVE), 0.01)
        assertEquals(3040.0, BMICalculator.calculateTDEE(bmr, ActivityLevel.EXTREMELY_ACTIVE), 0.01)
    }
    
    @Test
    fun `formatBMI formats correctly`() {
        assertEquals("22.9", BMICalculator.formatBMI(22.86, 1))
        assertEquals("22.86", BMICalculator.formatBMI(22.8634, 2))
        assertEquals("23", BMICalculator.formatBMI(22.86, 0))
    }
    
    @Test
    fun `getBMIAssessment returns complete assessment`() {
        val assessment = BMICalculator.getBMIAssessment(22.0)
        
        assertEquals(22.0, assessment.bmi)
        assertEquals(BMICategory.NORMAL, assessment.category)
        assertEquals(HealthRisk.LOW, assessment.healthRisk)
        assertTrue(assessment.recommendations.isNotEmpty())
        assertTrue(assessment.exerciseRecommendations.isNotEmpty())
    }
    
    @Test
    fun `getWeightChangeRecommendation for underweight person`() {
        val heightInMeters = 1.75
        val underweightBMI = 17.0
        
        val recommendation = BMICalculator.getWeightChangeRecommendation(underweightBMI, heightInMeters)
        assertNotNull(recommendation)
        
        assertEquals(WeightChangeDirection.GAIN, recommendation.changeDirection)
        assertTrue(recommendation.weightChangeNeeded > 0)
        assertTrue(recommendation.timelineWeeks > 0)
        assertTrue(recommendation.getSummaryMessage().contains("增重"))
    }
    
    @Test
    fun `getWeightChangeRecommendation for overweight person`() {
        val heightInMeters = 1.75
        val overweightBMI = 27.0
        
        val recommendation = BMICalculator.getWeightChangeRecommendation(overweightBMI, heightInMeters)
        assertNotNull(recommendation)
        
        assertEquals(WeightChangeDirection.LOSE, recommendation.changeDirection)
        assertTrue(recommendation.weightChangeNeeded < 0)
        assertTrue(recommendation.timelineWeeks > 0)
        assertTrue(recommendation.getSummaryMessage().contains("減重"))
    }
    
    @Test
    fun `getWeightChangeRecommendation for normal weight person`() {
        val heightInMeters = 1.75
        val normalBMI = 22.0
        
        val recommendation = BMICalculator.getWeightChangeRecommendation(normalBMI, heightInMeters)
        assertNotNull(recommendation)
        
        assertEquals(WeightChangeDirection.MAINTAIN, recommendation.changeDirection)
        assertEquals(0.0, recommendation.weightChangeNeeded, 0.01)
        assertEquals(0, recommendation.timelineWeeks)
        assertTrue(recommendation.getSummaryMessage().contains("維持"))
    }
    
    @Test
    fun `getWeightChangeRecommendation with invalid inputs returns null`() {
        assertNull(BMICalculator.getWeightChangeRecommendation(0.0, 1.75))
        assertNull(BMICalculator.getWeightChangeRecommendation(22.0, 0.0))
        assertNull(BMICalculator.getWeightChangeRecommendation(-22.0, 1.75))
    }
    
    @Test
    fun `validateInputs catches invalid weight`() {
        val errors = BMICalculator.validateInputs(0.5, WeightUnit.KG, 175.0, HeightUnit.CENTIMETERS)
        assertTrue(errors.any { it.contains("體重") })
        
        val tooHeavyErrors = BMICalculator.validateInputs(1500.0, WeightUnit.KG, 175.0, HeightUnit.CENTIMETERS)
        assertTrue(tooHeavyErrors.any { it.contains("體重") })
    }
    
    @Test
    fun `validateInputs catches invalid height`() {
        val tooShortErrors = BMICalculator.validateInputs(70.0, WeightUnit.KG, 30.0, HeightUnit.CENTIMETERS)
        assertTrue(tooShortErrors.any { it.contains("身高") })
        
        val tooTallErrors = BMICalculator.validateInputs(70.0, WeightUnit.KG, 350.0, HeightUnit.CENTIMETERS)
        assertTrue(tooTallErrors.any { it.contains("身高") })
    }
    
    @Test
    fun `validateInputs returns empty list for valid inputs`() {
        val errors = BMICalculator.validateInputs(70.0, WeightUnit.KG, 175.0, HeightUnit.CENTIMETERS)
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun `HeightUnit conversions work correctly`() {
        val heightInM = 1.75
        
        assertEquals(1.75, HeightUnit.METERS.toMeters(heightInM), 0.01)
        assertEquals(175.0, HeightUnit.CENTIMETERS.toMeters(175.0), 0.01)
        assertEquals(1.75, HeightUnit.FEET.toMeters(5.741), 0.01) // 5.741 ft ≈ 1.75m
        assertEquals(1.75, HeightUnit.INCHES.toMeters(68.9), 0.01) // 68.9 in ≈ 1.75m
        
        assertEquals(1.75, HeightUnit.METERS.fromMeters(heightInM), 0.01)
        assertEquals(175.0, HeightUnit.CENTIMETERS.fromMeters(heightInM), 0.01)
        assertEquals(5.741, HeightUnit.FEET.fromMeters(heightInM), 0.01)
        assertEquals(68.9, HeightUnit.INCHES.fromMeters(heightInM), 0.1)
    }
    
    @Test
    fun `ActivityLevel multipliers are reasonable`() {
        assertTrue(ActivityLevel.SEDENTARY.multiplier < ActivityLevel.LIGHTLY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.LIGHTLY_ACTIVE.multiplier < ActivityLevel.MODERATELY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.MODERATELY_ACTIVE.multiplier < ActivityLevel.VERY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.VERY_ACTIVE.multiplier < ActivityLevel.EXTREMELY_ACTIVE.multiplier)
        
        // Multipliers should be reasonable (between 1.0 and 2.5)
        ActivityLevel.values().forEach { level ->
            assertTrue(level.multiplier >= 1.0)
            assertTrue(level.multiplier <= 2.5)
        }
    }
    
    @Test
    fun `WeightChangeRecommendation getWeeklyTarget calculates correctly`() {
        val recommendation = WeightChangeRecommendation(
            currentWeight = 80.0,
            targetWeightMin = 65.0,
            targetWeightMax = 80.0,
            recommendedTargetWeight = 75.0,
            weightChangeNeeded = -5.0, // Lose 5kg
            changeDirection = WeightChangeDirection.LOSE,
            timelineWeeks = 10
        )
        
        assertEquals(-0.5, recommendation.getWeeklyTarget(), 0.01)
    }
}