package org.tabata.timber.domain.models.subscription

/**
 * Represents the current usage status and limits for features
 */
data class FeatureLimitStatus(
    val feature: PremiumFeature,
    val currentUsage: Int,
    val maxLimit: Int,
    val resetPeriod: ResetPeriod = ResetPeriod.NONE,
    val nextResetDate: kotlinx.datetime.Instant? = null,
    val isUnlimited: Boolean = false
) {
    
    /**
     * Calculates the remaining usage before hitting the limit
     */
    fun getRemainingUsage(): Int {
        return if (isUnlimited) Int.MAX_VALUE else maxOf(0, maxLimit - currentUsage)
    }
    
    /**
     * Checks if the usage limit has been reached
     */
    fun isLimitReached(): Boolean {
        return !isUnlimited && currentUsage >= maxLimit
    }
    
    /**
     * Calculates the usage percentage (0.0 to 1.0)
     */
    fun getUsagePercentage(): Float {
        return if (isUnlimited) 0f else {
            if (maxLimit == 0) 1f else (currentUsage.toFloat() / maxLimit.toFloat()).coerceIn(0f, 1f)
        }
    }
    
    /**
     * Checks if usage is approaching the limit (>= 80%)
     */
    fun isApproachingLimit(): Boolean {
        return !isUnlimited && getUsagePercentage() >= 0.8f
    }
    
    /**
     * Returns a user-friendly status message
     */
    fun getStatusMessage(): String {
        return when {
            isUnlimited -> "無限制"
            isLimitReached() -> "已達到限制 ($currentUsage/$maxLimit)"
            isApproachingLimit() -> "即將達到限制 ($currentUsage/$maxLimit)"
            else -> "使用中 ($currentUsage/$maxLimit)"
        }
    }
    
    /**
     * Returns the status severity level
     */
    fun getStatusSeverity(): StatusSeverity {
        return when {
            isUnlimited -> StatusSeverity.SUCCESS
            isLimitReached() -> StatusSeverity.ERROR
            isApproachingLimit() -> StatusSeverity.WARNING
            else -> StatusSeverity.INFO
        }
    }
    
    /**
     * Creates a new status with incremented usage
     */
    fun withIncrementedUsage(increment: Int = 1): FeatureLimitStatus {
        return copy(currentUsage = (currentUsage + increment).coerceAtLeast(0))
    }
    
    /**
     * Creates a new status with reset usage
     */
    fun withResetUsage(): FeatureLimitStatus {
        return copy(currentUsage = 0)
    }
    
    /**
     * Checks if the limit should be reset based on the reset period
     */
    fun shouldReset(currentTime: kotlinx.datetime.Instant = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
        return nextResetDate?.let { currentTime >= it } ?: false
    }
    
    companion object {
        /**
         * Creates an unlimited feature status for premium users
         */
        fun createUnlimited(feature: PremiumFeature): FeatureLimitStatus {
            return FeatureLimitStatus(
                feature = feature,
                currentUsage = 0,
                maxLimit = Int.MAX_VALUE,
                isUnlimited = true
            )
        }
        
        /**
         * Creates a limited feature status for free users
         */
        fun createLimited(
            feature: PremiumFeature,
            maxLimit: Int,
            currentUsage: Int = 0,
            resetPeriod: ResetPeriod = ResetPeriod.NONE
        ): FeatureLimitStatus {
            return FeatureLimitStatus(
                feature = feature,
                currentUsage = currentUsage,
                maxLimit = maxLimit,
                resetPeriod = resetPeriod,
                nextResetDate = calculateNextResetDate(resetPeriod),
                isUnlimited = false
            )
        }
        
        /**
         * Creates a blocked feature status (limit = 0)
         */
        fun createBlocked(feature: PremiumFeature): FeatureLimitStatus {
            return FeatureLimitStatus(
                feature = feature,
                currentUsage = 0,
                maxLimit = 0,
                isUnlimited = false
            )
        }
        
        private fun calculateNextResetDate(resetPeriod: ResetPeriod): kotlinx.datetime.Instant? {
            val now = kotlinx.datetime.Clock.System.now()
            return when (resetPeriod) {
                ResetPeriod.NONE -> null
                ResetPeriod.DAILY -> {
                    // Reset at midnight
                    val tomorrow = now.plus(kotlinx.datetime.DateTimeUnit.DAY, 1)
                    kotlinx.datetime.Instant.fromEpochSeconds(tomorrow.epochSeconds - (tomorrow.epochSeconds % 86400))
                }
                ResetPeriod.WEEKLY -> {
                    // Reset on Monday at midnight
                    now.plus(kotlinx.datetime.DateTimeUnit.DAY, 7)
                }
                ResetPeriod.MONTHLY -> {
                    // Reset on the first of next month
                    now.plus(kotlinx.datetime.DateTimeUnit.MONTH, 1)
                }
            }
        }
    }
}

/**
 * Defines when feature limits reset
 */
enum class ResetPeriod {
    NONE,       // Never resets
    DAILY,      // Resets daily at midnight
    WEEKLY,     // Resets weekly on Monday
    MONTHLY     // Resets monthly on the 1st
}

/**
 * Severity levels for status display
 */
enum class StatusSeverity {
    SUCCESS,    // Green - unlimited or well within limits
    INFO,       // Blue - normal usage
    WARNING,    // Orange - approaching limits
    ERROR       // Red - limit reached
}