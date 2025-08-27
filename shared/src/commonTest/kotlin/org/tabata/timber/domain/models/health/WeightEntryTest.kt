package org.tabata.timber.domain.models.health

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeightEntryTest {
    
    private val testDate = LocalDate(2022, 1, 1)
    
    @Test
    fun `create generates valid weight entry`() {
        val entry = WeightEntry.create(
            weight = 70.0,
            unit = WeightUnit.KG,
            notes = "Morning measurement"
        )
        
        assertNotNull(entry.id)
        assertEquals(70.0, entry.weight)
        assertEquals(WeightUnit.KG, entry.unit)
        assertEquals("Morning measurement", entry.notes)
        assertEquals(MeasurementSource.MANUAL, entry.source)
        assertTrue(entry.id.startsWith("weight_"))
    }
    
    @Test
    fun `validate returns Valid for correct entry`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val result = entry.validate()
        
        assertTrue(result is WeightEntry.ValidationResult.Valid)
    }
    
    @Test
    fun `validate catches weight too low`() {
        val entry = WeightEntry.create(0.5, WeightUnit.KG)
        val result = entry.validate()
        
        assertTrue(result is WeightEntry.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("體重必須") })
    }
    
    @Test
    fun `validate catches weight too high`() {
        val entry = WeightEntry.create(1500.0, WeightUnit.KG)
        val result = entry.validate()
        
        assertTrue(result is WeightEntry.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("體重必須") })
    }
    
    @Test
    fun `validate catches invalid body fat percentage`() {
        val entry = WeightEntry.create(
            weight = 70.0,
            unit = WeightUnit.KG,
            bodyFatPercentage = 150.0
        )
        val result = entry.validate()
        
        assertTrue(result is WeightEntry.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("體脂肪率") })
    }
    
    @Test
    fun `validate catches invalid muscle mass`() {
        val entry = WeightEntry.create(
            weight = 70.0,
            unit = WeightUnit.KG,
            muscleMass = 80.0 // More than total weight
        )
        val result = entry.validate()
        
        assertTrue(result is WeightEntry.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("肌肉量") })
    }
    
    @Test
    fun `getWeightInKg converts correctly`() {
        val kgEntry = WeightEntry.create(70.0, WeightUnit.KG)
        assertEquals(70.0, kgEntry.getWeightInKg(), 0.01)
        
        val lbEntry = WeightEntry.create(154.32, WeightUnit.LB) // ~70kg
        assertEquals(70.0, lbEntry.getWeightInKg(), 0.1)
        
        val stEntry = WeightEntry.create(11.02, WeightUnit.ST) // ~70kg
        assertEquals(70.0, stEntry.getWeightInKg(), 0.1)
    }
    
    @Test
    fun `getWeightInUnit converts to different units`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        
        assertEquals(70.0, entry.getWeightInUnit(WeightUnit.KG), 0.01)
        assertEquals(154.32, entry.getWeightInUnit(WeightUnit.LB), 0.1)
        assertEquals(11.02, entry.getWeightInUnit(WeightUnit.ST), 0.1)
    }
    
    @Test
    fun `getFormattedWeight formats correctly`() {
        val entry = WeightEntry.create(70.5, WeightUnit.KG)
        
        assertEquals("70.5 kg", entry.getFormattedWeight())
        assertEquals("70.5 kg", entry.getFormattedWeight(WeightUnit.KG, 1))
        assertEquals("70 kg", entry.getFormattedWeight(WeightUnit.KG, 0))
        
        val lbFormatted = entry.getFormattedWeight(WeightUnit.LB, 1)
        assertTrue(lbFormatted.endsWith(" lb"))
        assertTrue(lbFormatted.contains("155."))
    }
    
    @Test
    fun `calculateBMI calculates correctly`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val bmi = entry.calculateBMI(1.75)
        
        assertNotNull(bmi)
        assertEquals(22.86, bmi, 0.01)
    }
    
    @Test
    fun `calculateBMI with zero height returns null`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val bmi = entry.calculateBMI(0.0)
        
        assertNull(bmi)
    }
    
    @Test
    fun `getBMICategory returns correct category`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val category = entry.getBMICategory(1.75)
        
        assertEquals(BMICategory.NORMAL, category)
    }
    
    @Test
    fun `getBMICategory with invalid height returns null`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val category = entry.getBMICategory(0.0)
        
        assertNull(category)
    }
    
    @Test
    fun `getBodyCompositionSummary returns summary when data available`() {
        val entry = WeightEntry.create(
            weight = 70.0,
            unit = WeightUnit.KG,
            bodyFatPercentage = 15.0,
            muscleMass = 35.0
        )
        val summary = entry.getBodyCompositionSummary()
        
        assertNotNull(summary)
        assertEquals(70.0, summary.totalWeight)
        assertEquals(15.0, summary.bodyFatPercentage)
        assertEquals(35.0, summary.muscleMass)
    }
    
    @Test
    fun `getBodyCompositionSummary returns null when no data available`() {
        val entry = WeightEntry.create(70.0, WeightUnit.KG)
        val summary = entry.getBodyCompositionSummary()
        
        assertNull(summary)
    }
    
    @Test
    fun `WeightUnit enum has correct properties`() {
        assertEquals("kg", WeightUnit.KG.symbol)
        assertEquals("公斤", WeightUnit.KG.displayName)
        assertTrue(WeightUnit.KG.isMetric)
        
        assertEquals("lb", WeightUnit.LB.symbol)
        assertEquals("磅", WeightUnit.LB.displayName)
        assertFalse(WeightUnit.LB.isMetric)
        
        assertEquals("st", WeightUnit.ST.symbol)
        assertEquals("英石", WeightUnit.ST.displayName)
        assertFalse(WeightUnit.ST.isMetric)
    }
    
    @Test
    fun `WeightUnit conversions work correctly`() {
        // KG conversions
        assertEquals(70.0, WeightUnit.KG.toKilograms(70.0), 0.01)
        assertEquals(70.0, WeightUnit.KG.fromKilograms(70.0), 0.01)
        
        // LB conversions (1 lb ≈ 0.453592 kg)
        assertEquals(70.0, WeightUnit.LB.toKilograms(154.32), 0.1)
        assertEquals(154.32, WeightUnit.LB.fromKilograms(70.0), 0.1)
        
        // ST conversions (1 st ≈ 6.35029 kg)
        assertEquals(70.0, WeightUnit.ST.toKilograms(11.02), 0.1)
        assertEquals(11.02, WeightUnit.ST.fromKilograms(70.0), 0.1)
    }
    
    @Test
    fun `MeasurementSource enum has correct display names`() {
        assertEquals("手動輸入", MeasurementSource.MANUAL.displayName)
        assertEquals("智能體重計", MeasurementSource.SMART_SCALE.displayName)
        assertEquals("健身應用程式", MeasurementSource.FITNESS_APP.displayName)
        assertEquals("健康應用程式", MeasurementSource.HEALTH_APP.displayName)
        assertEquals("資料匯入", MeasurementSource.IMPORT.displayName)
    }
    
    @Test
    fun `BodyCompositionSummary calculates fat mass correctly`() {
        val summary = BodyCompositionSummary(
            totalWeight = 70.0,
            bodyFatPercentage = 15.0
        )
        
        val fatMass = summary.getFatMass()
        assertNotNull(fatMass)
        assertEquals(10.5, fatMass, 0.01) // 15% of 70kg
    }
    
    @Test
    fun `BodyCompositionSummary calculates lean mass correctly`() {
        val summary = BodyCompositionSummary(
            totalWeight = 70.0,
            bodyFatPercentage = 15.0
        )
        
        val leanMass = summary.getLeanMass()
        assertNotNull(leanMass)
        assertEquals(59.5, leanMass, 0.01) // 85% of 70kg
    }
    
    @Test
    fun `BodyCompositionSummary returns null when no body fat percentage`() {
        val summary = BodyCompositionSummary(totalWeight = 70.0)
        
        assertNull(summary.getFatMass())
        assertNull(summary.getLeanMass())
    }
    
    @Test
    fun `BodyCompositionSummary getAssessment returns appropriate messages`() {
        val lowFatSummary = BodyCompositionSummary(totalWeight = 70.0, bodyFatPercentage = 8.0)
        assertEquals("體脂率偏低", lowFatSummary.getAssessment())
        
        val goodFatSummary = BodyCompositionSummary(totalWeight = 70.0, bodyFatPercentage = 15.0)
        assertEquals("體脂率良好", goodFatSummary.getAssessment())
        
        val normalFatSummary = BodyCompositionSummary(totalWeight = 70.0, bodyFatPercentage = 25.0)
        assertEquals("體脂率正常", normalFatSummary.getAssessment())
        
        val highFatSummary = BodyCompositionSummary(totalWeight = 70.0, bodyFatPercentage = 35.0)
        assertEquals("體脂率偏高", highFatSummary.getAssessment())
        
        val noFatDataSummary = BodyCompositionSummary(totalWeight = 70.0)
        assertEquals("基本體重記錄", noFatDataSummary.getAssessment())
    }
    
    @Test
    fun `validation constants are properly defined`() {
        assertTrue(WeightEntry.MIN_WEIGHT_KG > 0)
        assertTrue(WeightEntry.MAX_WEIGHT_KG > WeightEntry.MIN_WEIGHT_KG)
        assertEquals(1.0, WeightEntry.MIN_WEIGHT_KG)
        assertEquals(1000.0, WeightEntry.MAX_WEIGHT_KG)
    }
}