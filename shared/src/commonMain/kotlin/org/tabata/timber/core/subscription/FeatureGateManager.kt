package org.tabata.timber.core.subscription

/**
 * Feature access control manager for premium features
 */
interface FeatureGateManager {
    /**
     * Check if a feature is accessible to the current user
     * @param feature Premium feature to check
     */
    fun canAccessFeature(feature: PremiumFeature): Boolean
    
    /**
     * Get the current limit status for a feature
     * @param feature Premium feature to check
     */
    fun getFeatureLimitStatus(feature: PremiumFeature): FeatureLimitStatus
    
    /**
     * Show upgrade prompt for a premium feature
     * @param feature Feature that requires premium access
     */
    suspend fun showUpgradePrompt(feature: PremiumFeature)
    
    /**
     * Get remaining free usage for a limited feature
     * @param feature Feature to check usage for
     */
    fun getRemainingFreeUsage(feature: PremiumFeature): Int?
}

/**
 * Feature limit status for free tier users
 */
sealed class FeatureLimitStatus {
    object Available : FeatureLimitStatus()
    object RequiresPremium : FeatureLimitStatus()
    data class LimitedUsage(val remaining: Int, val total: Int) : FeatureLimitStatus()
}