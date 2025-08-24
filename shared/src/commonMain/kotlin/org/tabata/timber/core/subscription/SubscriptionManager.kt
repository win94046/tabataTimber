package org.tabata.timber.core.subscription

import kotlinx.coroutines.flow.StateFlow

/**
 * Subscription management interface for premium features.
 * Handles subscription status, purchases, and feature access control.
 */
interface SubscriptionManager {
    /**
     * Current subscription status as a reactive flow
     */
    val subscriptionStatus: StateFlow<SubscriptionStatus>
    
    /**
     * Check current subscription status
     */
    suspend fun checkSubscriptionStatus(): SubscriptionStatus
    
    /**
     * Purchase a subscription
     * @param productId Product identifier for the subscription
     */
    suspend fun purchaseSubscription(productId: String): PurchaseResult
    
    /**
     * Restore previous purchases
     */
    suspend fun restorePurchases(): RestoreResult
    
    /**
     * Handle subscription expiry scenarios
     */
    suspend fun handleSubscriptionExpiry(): ExpiryHandlingResult
    
    /**
     * Check if a specific feature is available
     * @param feature Premium feature to check
     */
    fun isFeatureAvailable(feature: PremiumFeature): Boolean
    
    /**
     * Check if user is in grace period after subscription expiry
     */
    fun isInGracePeriod(): Boolean
}

/**
 * Current subscription status
 */
data class SubscriptionStatus(
    val isActive: Boolean,
    val productId: String?,
    val expiryDate: Long?,
    val lastVerified: Long,
    val gracePeriodEnd: Long? = null,
    val needsRestore: Boolean = false
)

/**
 * Premium features available in the app
 */
enum class PremiumFeature {
    UNLIMITED_PRESETS,
    ADVANCED_AUDIO_EFFECTS,
    WEIGHT_TRACKING,
    ADVANCED_STATISTICS,
    WEARABLE_SUPPORT,
    HEALTH_INTEGRATION
}

/**
 * Purchase operation result
 */
sealed class PurchaseResult {
    object Success : PurchaseResult()
    object Cancelled : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

/**
 * Restore purchases result
 */
sealed class RestoreResult {
    object Success : RestoreResult()
    object NoRestorablePurchases : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

/**
 * Subscription expiry handling result
 */
sealed class ExpiryHandlingResult {
    object GracefulDowngrade : ExpiryHandlingResult()
    object GracePeriodExtended : ExpiryHandlingResult()
    data class RestoreRequired(val message: String) : ExpiryHandlingResult()
}