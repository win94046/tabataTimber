package org.tabata.timber.core.subscription

import kotlinx.coroutines.flow.StateFlow

/**
 * Available subscription tiers
 */
enum class SubscriptionTier {
    FREE,           // Basic free tier with limited features
    PREMIUM,        // Premium subscription with full features
    LIFETIME        // One-time purchase for lifetime access
}

/**
 * Available premium features
 */
enum class PremiumFeature {
    CUSTOM_CONFIGURATIONS,  // Create and save custom Tabata configurations
    ADVANCED_STATISTICS,    // Detailed workout statistics and history
    CUSTOM_SOUNDS,         // Upload and use custom audio files
    EXPORT_DATA,           // Export workout data
    CLOUD_SYNC,            // Sync data across devices
    UNLIMITED_SESSIONS     // No limit on workout sessions
}

/**
 * Subscription status information
 */
data class SubscriptionStatus(
    val tier: SubscriptionTier,
    val isActive: Boolean,
    val expirationDate: Long? = null,  // Timestamp in milliseconds
    val availableFeatures: Set<PremiumFeature>
)

/**
 * Core interface for managing subscription and premium features
 * This requires platform-specific implementations for App Store/Play Store integration
 */
interface SubscriptionManager {
    
    /**
     * Current subscription status
     */
    val subscriptionStatus: StateFlow<SubscriptionStatus>
    
    /**
     * Whether subscription services are available (network, store connectivity)
     */
    val isSubscriptionAvailable: StateFlow<Boolean>
    
    /**
     * Initializes the subscription manager
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Refreshes the current subscription status from the platform store
     */
    suspend fun refreshSubscriptionStatus(): Result<Unit>
    
    /**
     * Checks if a specific premium feature is available
     */
    fun hasFeature(feature: PremiumFeature): Boolean
    
    /**
     * Launches the premium upgrade flow
     */
    suspend fun launchUpgradeFlow(): Result<Unit>
    
    /**
     * Restores previous purchases (important for iOS)
     */
    suspend fun restorePurchases(): Result<Unit>
    
    /**
     * Gets available subscription products from the store
     */
    suspend fun getAvailableProducts(): Result<List<SubscriptionProduct>>
    
    /**
     * Purchases a specific subscription product
     */
    suspend fun purchaseProduct(productId: String): Result<Unit>
}

/**
 * Represents a subscription product from the platform store
 */
data class SubscriptionProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val tier: SubscriptionTier
)