package org.tabata.timber.core.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerPrecisionMetricsTest {
    
    @Test
    fun testInitialMetrics() {
        val metrics = TimerPrecisionMetrics.initial()
        
        assertEquals(0L, metrics.currentDriftMs)
        assertEquals(0L, metrics.maxDriftMs)
        assertEquals(0.0, metrics.averageDriftMs)
        assertEquals(0, metrics.correctionsApplied)
        assertEquals(0L, metrics.lastCorrectionTimeMs)
        assertTrue(metrics.isWithinPrecision)
        assertEquals(0, metrics.sampleCount)
        assertEquals(0.0, metrics.driftStandardDeviation)
    }
    
    @Test
    fun testNeedsCorrectionThreshold() {
        val withinThreshold = TimerPrecisionMetrics(currentDriftMs = 99L)
        assertFalse(withinThreshold.needsCorrection())
        
        val aboveThreshold = TimerPrecisionMetrics(currentDriftMs = 101L)
        assertTrue(aboveThreshold.needsCorrection())
        
        val negativeAboveThreshold = TimerPrecisionMetrics(currentDriftMs = -101L)
        assertTrue(negativeAboveThreshold.needsCorrection())
    }
    
    @Test
    fun testCriticalPrecisionThreshold() {
        val withinPrecision = TimerPrecisionMetrics(currentDriftMs = 49L)
        assertFalse(withinPrecision.isCriticalPrecision())
        
        val criticalPrecision = TimerPrecisionMetrics(currentDriftMs = 51L)
        assertTrue(criticalPrecision.isCriticalPrecision())
        
        val negativeCriticalPrecision = TimerPrecisionMetrics(currentDriftMs = -51L)
        assertTrue(negativeCriticalPrecision.isCriticalPrecision())
    }
    
    @Test
    fun testPrecisionConstants() {
        assertEquals(100L, TimerPrecisionMetrics.CORRECTION_THRESHOLD_MS)
        assertEquals(50L, TimerPrecisionMetrics.CRITICAL_PRECISION_MS)
    }
    
    @Test
    fun testIsWithinPrecisionCalculation() {
        val withinPrecision = TimerPrecisionMetrics(
            currentDriftMs = 25L,
            isWithinPrecision = true
        )
        assertTrue(withinPrecision.isWithinPrecision)
        
        val outsidePrecision = TimerPrecisionMetrics(
            currentDriftMs = 75L,
            isWithinPrecision = false
        )
        assertFalse(outsidePrecision.isWithinPrecision)
    }
    
    @Test
    fun testMetricsWithCorrections() {
        val metrics = TimerPrecisionMetrics(
            currentDriftMs = 150L,
            maxDriftMs = 200L,
            averageDriftMs = 125.5,
            correctionsApplied = 3,
            lastCorrectionTimeMs = 1000L,
            isWithinPrecision = false,
            sampleCount = 10,
            driftStandardDeviation = 45.2
        )
        
        assertTrue(metrics.needsCorrection())
        assertTrue(metrics.isCriticalPrecision())
        assertFalse(metrics.isWithinPrecision)
        assertEquals(3, metrics.correctionsApplied)
        assertEquals(1000L, metrics.lastCorrectionTimeMs)
    }
}