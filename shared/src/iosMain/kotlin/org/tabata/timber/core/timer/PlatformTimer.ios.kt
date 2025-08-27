package org.tabata.timber.core.timer

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.QuartzCore.CACurrentMediaTime
import kotlin.native.concurrent.ThreadLocal

/**
 * iOS implementation of PlatformTimer using NSTimer
 * Provides high-precision timing for iOS platform
 */
actual class PlatformTimer {
    
    @ThreadLocal
    companion object {
        private var timerInstances = mutableMapOf<PlatformTimer, NSTimer>()
    }
    
    private var nsTimer: NSTimer? = null
    private var callback: (() -> Unit)? = null
    private var _isRunning = false
    
    actual val isRunning: Boolean get() = _isRunning
    
    actual fun start(intervalMs: Long, callback: () -> Unit) {
        stop() // Stop any existing timer
        
        this.callback = callback
        val intervalSeconds = intervalMs.toDouble() / 1000.0
        
        nsTimer = NSTimer.scheduledTimerWithTimeInterval(
            ti = intervalSeconds,
            repeats = true,
            block = { _ ->
                try {
                    if (_isRunning) {
                        this.callback?.invoke()
                    }
                } catch (e: Exception) {
                    println("Timer callback error: ${e.message}")
                }\n            }\n        )\n        \n        _isRunning = true\n        nsTimer?.let { timerInstances[this] = it }\n    }\n    \n    actual fun stop() {\n        _isRunning = false\n        nsTimer?.invalidate()\n        nsTimer = null\n        timerInstances.remove(this)\n        callback = null\n    }\n    \n    actual fun getCurrentMonotonicTimeNanos(): Long {\n        // Use CACurrentMediaTime() for monotonic time on iOS\n        // CACurrentMediaTime returns seconds, convert to nanoseconds\n        return (CACurrentMediaTime() * 1_000_000_000.0).toLong()\n    }\n    \n    actual fun getCurrentSystemTimeMs(): Long {\n        // Use NSDate for system time\n        return (NSDate().timeIntervalSince1970 * 1000.0).toLong()\n    }\n}"