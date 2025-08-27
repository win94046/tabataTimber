package org.tabata.timber.core.timer

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles drift detection and correction for precise timing
 * Monitors timing accuracy and applies corrections when drift exceeds thresholds
 */
class DriftCorrector {
    
    private val driftHistory = mutableListOf<Long>()
    private var totalDrift = 0L
    private var correctionCount = 0
    private var lastCorrectionTime = 0L
    
    /**
     * Calculates timing drift between expected and actual elapsed time
     * 
     * @param expectedElapsedMs Expected time that should have elapsed
     * @param actualElapsedMs Actual time elapsed according to system clock
     * @return Drift in milliseconds (positive = running slow, negative = running fast)
     */
    fun calculateDrift(expectedElapsedMs: Long, actualElapsedMs: Long): Long {
        return actualElapsedMs - expectedElapsedMs
    }
    
    /**
     * Records a drift measurement and updates metrics
     * 
     * @param driftMs Current drift measurement
     * @param currentTimeMs Current system time for correction tracking
     * @return Updated precision metrics
     */
    fun recordDrift(driftMs: Long, currentTimeMs: Long): TimerPrecisionMetrics {
        driftHistory.add(driftMs)
        totalDrift += driftMs
        
        // Keep history size manageable (last 100 measurements)
        if (driftHistory.size > MAX_HISTORY_SIZE) {
            val removed = driftHistory.removeAt(0)
            totalDrift -= removed
        }
        
        val maxDrift = driftHistory.maxByOrNull { abs(it) } ?: 0L
        val avgDrift = if (driftHistory.isNotEmpty()) totalDrift.toDouble() / driftHistory.size else 0.0
        val stdDev = calculateStandardDeviation()
        
        return TimerPrecisionMetrics(
            currentDriftMs = driftMs,
            maxDriftMs = abs(maxDrift),
            averageDriftMs = avgDrift,
            correctionsApplied = correctionCount,
            lastCorrectionTimeMs = lastCorrectionTime,
            isWithinPrecision = abs(driftMs) <= TimerPrecisionMetrics.CRITICAL_PRECISION_MS,
            sampleCount = driftHistory.size,
            driftStandardDeviation = stdDev
        )
    }
    
    /**
     * Determines if correction is needed and calculates correction amount
     * 
     * @param currentDriftMs Current measured drift
     * @param currentTimeMs Current system time
     * @return Correction amount in milliseconds, or null if no correction needed
     */
    fun shouldApplyCorrection(currentDriftMs: Long, currentTimeMs: Long): Long? {
        return if (abs(currentDriftMs) > TimerPrecisionMetrics.CORRECTION_THRESHOLD_MS) {
            // Apply correction - adjust by 80% of drift to avoid oscillation
            val correction = (currentDriftMs * CORRECTION_FACTOR).toLong()
            lastCorrectionTime = currentTimeMs
            correctionCount++
            correction
        } else {
            null
        }
    }
    
    /**
     * Resets drift tracking for new session
     */
    fun reset() {
        driftHistory.clear()
        totalDrift = 0L
        correctionCount = 0
        lastCorrectionTime = 0L
    }
    
    /**
     * Calculates standard deviation of drift measurements
     */
    private fun calculateStandardDeviation(): Double {
        if (driftHistory.size < 2) return 0.0
        
        val mean = totalDrift.toDouble() / driftHistory.size
        val variance = driftHistory.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    /**
     * Gets current drift trend (positive = getting slower, negative = getting faster)
     */
    fun getDriftTrend(): Double {
        if (driftHistory.size < 5) return 0.0
        
        // Calculate trend over last 5 measurements
        val recent = driftHistory.takeLast(5)
        val oldAvg = recent.take(2).average()
        val newAvg = recent.drop(3).average()
        
        return newAvg - oldAvg
    }
    
    companion object {
        private const val MAX_HISTORY_SIZE = 100
        private const val CORRECTION_FACTOR = 0.8  // Apply 80% of measured drift to avoid overcorrection
    }
}