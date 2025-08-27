package org.tabata.timber.domain.models.subscription

import kotlinx.datetime.Instant

/**
 * Represents the current subscription status of a user
 */
sealed class SubscriptionStatus {
    
    /**
     * Free tier with limited features
     */
    data class Free(
        val trialUsed: Boolean = false,
        val trialEndDate: Instant? = null
    ) : SubscriptionStatus()
    
    /**
     * Active premium subscription
     */
    data class Premium(
        val subscriptionId: String,
        val planId: String,
        val startDate: Instant,
        val expiryDate: Instant,
        val autoRenew: Boolean = true,
        val paymentProvider: PaymentProvider = PaymentProvider.UNKNOWN
    ) : SubscriptionStatus() {
        
        /**
         * Checks if the subscription is currently active
         */
        fun isActive(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
            return currentTime < expiryDate
        }
        
        /**
         * Checks if the subscription expires within the specified number of days
         */
        fun isExpiringWithinDays(days: Int, currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
            val daysInSeconds = days * 24 * 60 * 60L
            val expiryThreshold = currentTime.epochSeconds + daysInSeconds
            return expiryDate.epochSeconds <= expiryThreshold
        }
        
        /**
         * Returns the number of days until expiry
         */
        fun getDaysUntilExpiry(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Long {
            val secondsUntilExpiry = expiryDate.epochSeconds - currentTime.epochSeconds
            return maxOf(0L, secondsUntilExpiry / (24 * 60 * 60))
        }
    }
    
    /**
     * Trial subscription (limited time free premium access)
     */
    data class Trial(
        val trialId: String,
        val startDate: Instant,
        val expiryDate: Instant,
        val originalPlanId: String? = null
    ) : SubscriptionStatus() {
        
        /**
         * Checks if the trial is currently active
         */
        fun isActive(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
            return currentTime < expiryDate
        }
        
        /**
         * Returns the number of days remaining in the trial
         */
        fun getDaysRemaining(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Long {
            val secondsRemaining = expiryDate.epochSeconds - currentTime.epochSeconds
            return maxOf(0L, secondsRemaining / (24 * 60 * 60))
        }
    }
    
    /**
     * Expired subscription that needs renewal
     */
    data class Expired(
        val lastSubscriptionId: String?,
        val lastPlanId: String?,
        val expiryDate: Instant,
        val gracePeriodEndDate: Instant? = null
    ) : SubscriptionStatus() {
        
        /**
         * Checks if still in grace period
         */
        fun isInGracePeriod(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
            return gracePeriodEndDate?.let { currentTime < it } ?: false
        }
    }
    
    /**
     * Cancelled subscription (user actively cancelled)
     */
    data class Cancelled(
        val subscriptionId: String,
        val planId: String,
        val cancellationDate: Instant,
        val expiryDate: Instant,
        val cancellationReason: CancellationReason = CancellationReason.USER_CANCELLED
    ) : SubscriptionStatus() {
        
        /**
         * Checks if subscription is still valid until expiry
         */
        fun isValidUntilExpiry(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
            return currentTime < expiryDate
        }
    }
    
    /**
     * Payment failed - awaiting retry or user action
     */
    data class PaymentFailed(
        val subscriptionId: String,
        val planId: String,
        val lastSuccessfulPayment: Instant,
        val nextRetryDate: Instant?,
        val retryCount: Int = 0,
        val maxRetries: Int = 3
    ) : SubscriptionStatus() {
        
        /**
         * Checks if max retries have been exceeded
         */
        fun hasExceededMaxRetries(): Boolean = retryCount >= maxRetries
    }
    
    /**
     * Returns whether this status provides premium access
     */
    fun hasPremiumAccess(currentTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())): Boolean {
        return when (this) {
            is Free -> false
            is Premium -> isActive(currentTime)
            is Trial -> isActive(currentTime)
            is Expired -> isInGracePeriod(currentTime)
            is Cancelled -> isValidUntilExpiry(currentTime)
            is PaymentFailed -> true // Still provide access during payment retry period
        }
    }
    
    /**
     * Returns the subscription tier
     */
    fun getSubscriptionTier(): SubscriptionTier {
        return when (this) {
            is Free -> SubscriptionTier.FREE
            is Premium, is Trial, is PaymentFailed -> SubscriptionTier.PREMIUM
            is Expired -> if (isInGracePeriod()) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
            is Cancelled -> if (isValidUntilExpiry()) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
        }
    }
    
    /**
     * Returns a user-friendly status description
     */
    fun getDisplayStatus(): String {
        return when (this) {
            is Free -> if (trialUsed) "免費版" else "免費版 (可試用)"
            is Premium -> "高級版"
            is Trial -> "試用版"
            is Expired -> if (isInGracePeriod()) "已到期 (寬限期)" else "已到期"
            is Cancelled -> "已取消"
            is PaymentFailed -> "付款失敗"
        }
    }
}

/**
 * Subscription tiers available in the app
 */
enum class SubscriptionTier {
    FREE,
    PREMIUM
}

/**
 * Payment providers supported
 */
enum class PaymentProvider {
    GOOGLE_PLAY,
    APP_STORE,
    STRIPE,
    UNKNOWN
}

/**
 * Reasons for subscription cancellation
 */
enum class CancellationReason {
    USER_CANCELLED,
    PAYMENT_FAILED,
    REFUND_REQUESTED,
    POLICY_VIOLATION,
    OTHER
}