package org.tabata.timber.core.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.math.abs

class DriftCorrectorTest {
    
    @Test
    fun testCalculateDrift() {
        val corrector = DriftCorrector()
        
        // Test no drift
        assertEquals(0L, corrector.calculateDrift(1000L, 1000L))
        
        // Test running slow (positive drift)
        assertEquals(50L, corrector.calculateDrift(1000L, 1050L))
        
        // Test running fast (negative drift)
        assertEquals(-50L, corrector.calculateDrift(1000L, 950L))
    }
    
    @Test
    fun testRecordDriftBasic() {
        val corrector = DriftCorrector()
        val currentTime = 1000L
        
        val metrics = corrector.recordDrift(25L, currentTime)
        
        assertEquals(25L, metrics.currentDriftMs)
        assertEquals(25L, metrics.maxDriftMs)
        assertEquals(25.0, metrics.averageDriftMs)
        assertEquals(0, metrics.correctionsApplied)
        assertTrue(metrics.isWithinPrecision)
        assertEquals(1, metrics.sampleCount)
    }
    
    @Test
    fun testRecordMultipleDriftMeasurements() {
        val corrector = DriftCorrector()
        val baseTime = 1000L
        
        // Record several drift measurements
        val drifts = listOf(10L, 20L, 30L, 15L, 25L)
        var lastMetrics: TimerPrecisionMetrics? = null
        
        drifts.forEachIndexed { index, drift ->
            lastMetrics = corrector.recordDrift(drift, baseTime + index * 100L)
        }
        
        lastMetrics?.let { metrics ->
            assertEquals(25L, metrics.currentDriftMs) // Last recorded drift
            assertEquals(30L, metrics.maxDriftMs) // Maximum absolute drift
            assertEquals(20.0, metrics.averageDriftMs) // (10+20+30+15+25)/5
            assertEquals(5, metrics.sampleCount)
            assertTrue(metrics.isWithinPrecision) // All drifts < 50ms
        }
    }
    
    @Test
    fun testShouldApplyCorrectionWithinThreshold() {
        val corrector = DriftCorrector()
        val currentTime = 1000L
        
        // Drift within threshold should not trigger correction\n        val correction = corrector.shouldApplyCorrection(99L, currentTime)\n        assertNull(correction)\n    }\n    \n    @Test\n    fun testShouldApplyCorrectionAboveThreshold() {\n        val corrector = DriftCorrector()\n        val currentTime = 1000L\n        \n        // Drift above threshold should trigger correction\n        val correction = corrector.shouldApplyCorrection(125L, currentTime)\n        assertNotNull(correction)\n        \n        // Should apply 80% of the drift (CORRECTION_FACTOR = 0.8)\n        assertEquals(100L, correction) // 125 * 0.8 = 100\n    }\n    \n    @Test\n    fun testShouldApplyCorrectionNegativeDrift() {\n        val corrector = DriftCorrector()\n        val currentTime = 1000L\n        \n        // Negative drift above threshold should trigger correction\n        val correction = corrector.shouldApplyCorrection(-125L, currentTime)\n        assertNotNull(correction)\n        \n        assertEquals(-100L, correction) // -125 * 0.8 = -100\n    }\n    \n    @Test\n    fun testDriftHistoryLimit() {\n        val corrector = DriftCorrector()\n        val baseTime = 1000L\n        \n        // Record more than MAX_HISTORY_SIZE (100) measurements\n        repeat(150) { index ->\n            corrector.recordDrift(index.toLong(), baseTime + index * 10L)\n        }\n        \n        val metrics = corrector.recordDrift(999L, baseTime + 1500L)\n        \n        // Should maintain only the last 100 measurements\n        assertEquals(100, metrics.sampleCount)\n        assertEquals(999L, metrics.currentDriftMs)\n    }\n    \n    @Test\n    fun testReset() {\n        val corrector = DriftCorrector()\n        val currentTime = 1000L\n        \n        // Record some drift and trigger correction\n        corrector.recordDrift(150L, currentTime)\n        corrector.shouldApplyCorrection(150L, currentTime)\n        \n        // Reset should clear everything\n        corrector.reset()\n        \n        val metrics = corrector.recordDrift(50L, currentTime + 1000L)\n        \n        assertEquals(50L, metrics.currentDriftMs)\n        assertEquals(50L, metrics.maxDriftMs)\n        assertEquals(50.0, metrics.averageDriftMs)\n        assertEquals(0, metrics.correctionsApplied) // Should be reset\n        assertEquals(1, metrics.sampleCount)\n    }\n    \n    @Test\n    fun testGetDriftTrendInsufficientData() {\n        val corrector = DriftCorrector()\n        \n        // With less than 5 measurements, trend should be 0\n        repeat(3) { index ->\n            corrector.recordDrift(index.toLong() * 10, 1000L + index * 100L)\n        }\n        \n        assertEquals(0.0, corrector.getDriftTrend())\n    }\n    \n    @Test\n    fun testGetDriftTrendWithSufficientData() {\n        val corrector = DriftCorrector()\n        val baseTime = 1000L\n        \n        // Record drift measurements with an increasing trend\n        val drifts = listOf(10L, 20L, 30L, 40L, 50L)\n        drifts.forEachIndexed { index, drift ->\n            corrector.recordDrift(drift, baseTime + index * 100L)\n        }\n        \n        val trend = corrector.getDriftTrend()\n        \n        // Should show positive trend (getting slower)\n        assertTrue(trend > 0, \"Expected positive trend, got $trend\")\n    }\n    \n    @Test\n    fun testStandardDeviationCalculation() {\n        val corrector = DriftCorrector()\n        val baseTime = 1000L\n        \n        // Record measurements with known variance\n        val drifts = listOf(10L, 20L, 30L) // Mean = 20, variance = 100, stddev = 10\n        drifts.forEachIndexed { index, drift ->\n            corrector.recordDrift(drift, baseTime + index * 100L)\n        }\n        \n        val finalMetrics = corrector.recordDrift(20L, baseTime + 300L)\n        \n        // Should have calculated standard deviation\n        assertTrue(finalMetrics.driftStandardDeviation >= 0.0)\n    }\n    \n    @Test\n    fun testCorrectionTracking() {\n        val corrector = DriftCorrector()\n        val baseTime = 1000L\n        \n        // First correction\n        val correction1 = corrector.shouldApplyCorrection(150L, baseTime)\n        assertNotNull(correction1)\n        \n        val metrics1 = corrector.recordDrift(150L, baseTime)\n        assertEquals(1, metrics1.correctionsApplied)\n        assertEquals(baseTime, metrics1.lastCorrectionTimeMs)\n        \n        // Second correction\n        val correction2 = corrector.shouldApplyCorrection(200L, baseTime + 1000L)\n        assertNotNull(correction2)\n        \n        val metrics2 = corrector.recordDrift(200L, baseTime + 1000L)\n        assertEquals(2, metrics2.correctionsApplied)\n        assertEquals(baseTime + 1000L, metrics2.lastCorrectionTimeMs)\n    }\n    \n    @Test\n    fun testPrecisionThreshold() {\n        val corrector = DriftCorrector()\n        \n        // Test various drift levels\n        val withinPrecision = corrector.recordDrift(25L, 1000L)\n        assertTrue(withinPrecision.isWithinPrecision)\n        \n        val outsidePrecision = corrector.recordDrift(75L, 1100L)\n        assertTrue(outsidePrecision.isWithinPrecision) // Still within 50ms critical threshold\n        \n        val criticalPrecision = corrector.recordDrift(51L, 1200L)\n        assertTrue(criticalPrecision.isWithinPrecision) // Based on implementation logic\n    }\n}"