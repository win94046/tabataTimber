package org.tabata.timber.domain.models.subscription

/**
 * Interface for managing feature access based on subscription status
 */
interface FeatureGateManager {
    
    /**
     * Checks if a user has access to a specific premium feature
     */
    fun hasFeatureAccess(feature: PremiumFeature): Boolean
    
    /**
     * Gets the current limit status for a specific feature
     */
    fun getFeatureLimitStatus(feature: PremiumFeature): FeatureLimitStatus
    
    /**
     * Attempts to use a feature, returning true if allowed
     * Automatically increments usage counters if successful
     */
    suspend fun tryUseFeature(feature: PremiumFeature): FeatureUsageResult
    
    /**
     * Gets the current subscription status
     */
    fun getSubscriptionStatus(): SubscriptionStatus
    
    /**
     * Updates the subscription status
     */
    suspend fun updateSubscriptionStatus(status: SubscriptionStatus)
    
    /**
     * Resets usage counters for features that have expired limits
     */
    suspend fun resetExpiredLimits()
    
    /**
     * Gets all available features and their current status
     */
    fun getAllFeatureStatuses(): Map<PremiumFeature, FeatureLimitStatus>
    
    /**
     * Checks if user should be shown upgrade prompts for a feature
     */
    fun shouldShowUpgradePrompt(feature: PremiumFeature): Boolean
}

/**
 * Result of attempting to use a feature
 */
sealed class FeatureUsageResult {
    
    /**
     * Feature usage was allowed
     */
    object Allowed : FeatureUsageResult()
    
    /**
     * Feature usage was blocked due to limits or lack of access
     */
    data class Blocked(
        val reason: BlockReason,
        val message: String,
        val suggestedAction: SuggestedAction? = null
    ) : FeatureUsageResult()
    
    /**
     * Feature usage was allowed but user is approaching limits
     */
    data class AllowedWithWarning(
        val warning: String,
        val remainingUsage: Int
    ) : FeatureUsageResult()
}

/**
 * Reasons why feature usage might be blocked
 */
enum class BlockReason {
    REQUIRES_PREMIUM,           // Feature requires premium subscription
    LIMIT_REACHED,              // Usage limit has been reached
    SUBSCRIPTION_EXPIRED,       // Subscription has expired
    PAYMENT_FAILED,             // Payment failed, access temporarily blocked
    FEATURE_DISABLED,           // Feature is temporarily disabled
    TRIAL_ENDED                 // Trial period has ended
}

/**
 * Suggested actions when feature is blocked
 */
enum class SuggestedAction {
    UPGRADE_TO_PREMIUM,         // Upgrade to premium subscription
    RENEW_SUBSCRIPTION,         // Renew expired subscription
    UPDATE_PAYMENT_METHOD,      // Update payment information
    START_TRIAL,                // Start free trial
    WAIT_FOR_RESET,             // Wait for limit reset
    CONTACT_SUPPORT             // Contact customer support
}

/**
 * Basic implementation of FeatureGateManager
 */
class BasicFeatureGateManager(
    private var subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Free(),
    private val featureLimits: MutableMap<PremiumFeature, FeatureLimitStatus> = mutableMapOf()
) : FeatureGateManager {
    
    override fun hasFeatureAccess(feature: PremiumFeature): Boolean {
        return subscriptionStatus.hasPremiumAccess() || isFeatureIncludedInFree(feature)
    }
    
    override fun getFeatureLimitStatus(feature: PremiumFeature): FeatureLimitStatus {
        return if (subscriptionStatus.hasPremiumAccess()) {
            // Premium users get unlimited access
            FeatureLimitStatus.createUnlimited(feature)
        } else {
            // Free users get limited access
            featureLimits[feature] ?: getDefaultFreeLimit(feature)
        }
    }
    
    override suspend fun tryUseFeature(feature: PremiumFeature): FeatureUsageResult {
        val limitStatus = getFeatureLimitStatus(feature)
        
        // Check if user has access to the feature
        if (!hasFeatureAccess(feature) && !isFeatureIncludedInFree(feature)) {
            return FeatureUsageResult.Blocked(
                reason = when (subscriptionStatus) {
                    is SubscriptionStatus.Expired -> BlockReason.SUBSCRIPTION_EXPIRED
                    is SubscriptionStatus.PaymentFailed -> BlockReason.PAYMENT_FAILED
                    is SubscriptionStatus.Trial -> if ((subscriptionStatus as SubscriptionStatus.Trial).isActive()) 
                        BlockReason.REQUIRES_PREMIUM else BlockReason.TRIAL_ENDED
                    else -> BlockReason.REQUIRES_PREMIUM
                },
                message = "此功能需要高級版訂閱",
                suggestedAction = SuggestedAction.UPGRADE_TO_PREMIUM
            )
        }
        
        // Check usage limits for free users
        if (!subscriptionStatus.hasPremiumAccess()) {
            if (limitStatus.isLimitReached()) {
                return FeatureUsageResult.Blocked(
                    reason = BlockReason.LIMIT_REACHED,
                    message = "已達到${feature.displayName}的使用限制",
                    suggestedAction = if (limitStatus.resetPeriod != ResetPeriod.NONE) 
                        SuggestedAction.WAIT_FOR_RESET else SuggestedAction.UPGRADE_TO_PREMIUM
                )
            }
            
            // Increment usage counter
            val newStatus = limitStatus.withIncrementedUsage()
            featureLimits[feature] = newStatus
            
            // Check if approaching limit
            if (newStatus.isApproachingLimit()) {
                return FeatureUsageResult.AllowedWithWarning(
                    warning = "您即將達到${feature.displayName}的使用限制",
                    remainingUsage = newStatus.getRemainingUsage()
                )
            }
        }
        
        return FeatureUsageResult.Allowed
    }
    
    override fun getSubscriptionStatus(): SubscriptionStatus = subscriptionStatus
    
    override suspend fun updateSubscriptionStatus(status: SubscriptionStatus) {
        subscriptionStatus = status
        
        // If upgraded to premium, reset all limits to unlimited
        if (status.hasPremiumAccess()) {
            PremiumFeature.values().forEach { feature ->
                featureLimits[feature] = FeatureLimitStatus.createUnlimited(feature)
            }
        }
    }
    
    override suspend fun resetExpiredLimits() {
        val currentTime = kotlinx.datetime.Clock.System.now()
        featureLimits.replaceAll { feature, status ->
            if (status.shouldReset(currentTime)) {
                status.withResetUsage()
            } else {
                status
            }
        }
    }
    
    override fun getAllFeatureStatuses(): Map<PremiumFeature, FeatureLimitStatus> {
        return PremiumFeature.values().associateWith { getFeatureLimitStatus(it) }
    }
    
    override fun shouldShowUpgradePrompt(feature: PremiumFeature): Boolean {
        if (subscriptionStatus.hasPremiumAccess()) return false
        
        val limitStatus = getFeatureLimitStatus(feature)
        return limitStatus.isApproachingLimit() || limitStatus.isLimitReached()
    }
    
    private fun isFeatureIncludedInFree(feature: PremiumFeature): Boolean {
        // Some features might be partially available in free tier
        return when (feature) {
            PremiumFeature.NO_ADS -> false  // Never included in free
            else -> false // Most features are premium-only
        }
    }
    
    private fun getDefaultFreeLimit(feature: PremiumFeature): FeatureLimitStatus {
        return when (feature) {
            PremiumFeature.CUSTOM_TIMERS -> FeatureLimitStatus.createLimited(
                feature = feature,
                maxLimit = 3,
                resetPeriod = ResetPeriod.NONE
            )
            PremiumFeature.UNLIMITED_HISTORY -> FeatureLimitStatus.createLimited(
                feature = feature,
                maxLimit = 10,
                resetPeriod = ResetPeriod.NONE
            )
            PremiumFeature.WEIGHT_TRACKING -> FeatureLimitStatus.createLimited(
                feature = feature,
                maxLimit = 5,
                resetPeriod = ResetPeriod.WEEKLY
            )
            PremiumFeature.DATA_EXPORT -> FeatureLimitStatus.createLimited(
                feature = feature,
                maxLimit = 1,
                resetPeriod = ResetPeriod.MONTHLY
            )
            else -> FeatureLimitStatus.createBlocked(feature)
        }
    }
}