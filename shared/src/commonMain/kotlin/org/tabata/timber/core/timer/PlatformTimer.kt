package org.tabata.timber.core.timer

/**
 * Platform-specific timer implementation for high-precision timing
 * Uses platform-optimized timing mechanisms for best accuracy
 */
expect class PlatformTimer {
    
    /**
     * Starts the platform timer with specified interval
     * 
     * @param intervalMs Timer tick interval in milliseconds
     * @param callback Function called on each timer tick
     */
    fun start(intervalMs: Long, callback: () -> Unit)
    
    /**
     * Stops the platform timer
     */
    fun stop()
    
    /**
     * Returns current monotonic time in nanoseconds
     * Used as base for precise time measurements
     */
    fun getCurrentMonotonicTimeNanos(): Long
    
    /**
     * Returns current system time in milliseconds
     */
    fun getCurrentSystemTimeMs(): Long
    
    /**
     * Returns true if timer is currently running
     */
    val isRunning: Boolean
}