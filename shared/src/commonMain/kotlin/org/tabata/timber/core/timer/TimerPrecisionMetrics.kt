package org.tabata.timber.core.timer

/**
 * Metrics for tracking timer precision and accuracy
 * Used for monitoring drift and ensuring <50ms accuracy requirements
 */
data class TimerPrecisionMetrics(
    /**
     * Current timing drift in milliseconds
     * Positive values indicate timer is running slow, negative values indicate running fast
     */
    val currentDriftMs: Long = 0L,
    
    /**
     * Maximum observed drift during current session
     */
    val maxDriftMs: Long = 0L,
    
    /**
     * Average drift over the session
     */
    val averageDriftMs: Double = 0.0,
    
    /**
     * Number of drift corrections applied
     */
    val correctionsApplied: Int = 0,
    
    /**
     * Last correction timestamp
     */
    val lastCorrectionTimeMs: Long = 0L,
    
    /**
     * Whether timing is within acceptable precision (<50ms for short term, <100ms for long term)
     */
    val isWithinPrecision: Boolean = true,
    
    /**
     * Sample count for calculating averages
     */
    val sampleCount: Int = 0,
    
    /**
     * Standard deviation of drift measurements
     */
    val driftStandardDeviation: Double = 0.0
) {
    
    /**
     * Returns true if timer needs drift correction
     * Correction threshold: >100ms for long-running sessions
     */
    fun needsCorrection(): Boolean = kotlin.math.abs(currentDriftMs) > CORRECTION_THRESHOLD_MS
    
    /**
     * Returns true if precision is critical (short-term accuracy)
     */
    fun isCriticalPrecision(): Boolean = kotlin.math.abs(currentDriftMs) > CRITICAL_PRECISION_MS
    
    companion object {
        const val CORRECTION_THRESHOLD_MS = 100L  // Apply correction when drift > 100ms
        const val CRITICAL_PRECISION_MS = 50L     // Critical precision requirement
        
        /**
         * Creates initial metrics for a new session
         */
        fun initial() = TimerPrecisionMetrics()
    }
}