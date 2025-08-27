package org.tabata.timber.core.timer

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledFuture

/**
 * Android implementation of PlatformTimer using ScheduledExecutorService
 * Provides high-precision timing for Android platform
 */
actual class PlatformTimer {
    
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { thread ->
        thread.apply {
            name = "TabataTimer-Precision"
            priority = Thread.MAX_PRIORITY // Highest priority for timing accuracy
        }
    }
    
    private var scheduledFuture: ScheduledFuture<*>? = null
    private var _isRunning = false
    
    actual val isRunning: Boolean get() = _isRunning
    
    actual fun start(intervalMs: Long, callback: () -> Unit) {
        stop() // Stop any existing timer
        
        _isRunning = true
        scheduledFuture = executor.scheduleAtFixedRate(
            {
                try {
                    if (_isRunning) {
                        callback()
                    }
                } catch (e: Exception) {
                    // Log error but don't crash timer
                    println("Timer callback error: ${e.message}")
                }
            },
            0L, // Initial delay
            intervalMs, // Period
            TimeUnit.MILLISECONDS
        )
    }
    
    actual fun stop() {
        _isRunning = false
        scheduledFuture?.cancel(false)
        scheduledFuture = null
    }
    
    actual fun getCurrentMonotonicTimeNanos(): Long {
        // Use System.nanoTime() for monotonic clock
        return System.nanoTime()
    }
    
    actual fun getCurrentSystemTimeMs(): Long {
        // Use System.currentTimeMillis() for system time
        return System.currentTimeMillis()
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        stop()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}