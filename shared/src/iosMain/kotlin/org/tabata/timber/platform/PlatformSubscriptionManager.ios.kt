package org.tabata.timber.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tabata.timber.core.subscription.*

/**
 * iOS implementation of SubscriptionManager
 * Uses StoreKit 2 for subscription management
 */
actual class PlatformSubscriptionManager : SubscriptionManager {
    
    private val _subscriptionStatus = MutableStateFlow(
        SubscriptionStatus(
            isActive = false,
            productId = null,
            expiryDate = null,
            lastVerified = System.currentTimeMillis()
        )
    )
    
    override val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus
    
    override suspend fun checkSubscriptionStatus(): SubscriptionStatus {
        // TODO: Implement StoreKit 2 subscription check
        return _subscriptionStatus.value
    }
    
    override suspend fun purchaseSubscription(productId: String): PurchaseResult {
        // TODO: Implement StoreKit 2 purchase flow
        return PurchaseResult.Error("Not implemented yet")
    }
    
    override suspend fun restorePurchases(): RestoreResult {
        // TODO: Implement StoreKit 2 restore purchases
        return RestoreResult.NoRestorablePurchases
    }
    
    override suspend fun handleSubscriptionExpiry(): ExpiryHandlingResult {
        // TODO: Implement subscription expiry handling
        return ExpiryHandlingResult.GracefulDowngrade
    }
    
    override fun isFeatureAvailable(feature: PremiumFeature): Boolean {
        return _subscriptionStatus.value.isActive
    }
    
    override fun isInGracePeriod(): Boolean {
        val status = _subscriptionStatus.value
        val now = System.currentTimeMillis()
        return status.gracePeriodEnd?.let { it > now } ?: false
    }
}