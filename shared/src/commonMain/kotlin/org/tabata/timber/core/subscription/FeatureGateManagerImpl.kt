package org.tabata.timber.core.subscription

/**
 * Implementation of feature access control manager
 */
class FeatureGateManagerImpl(
    private val subscriptionManager: SubscriptionManager
) : FeatureGateManager {
    
    companion object {
        private const val FREE_PRESET_LIMIT = 1
        private val FREE_SOUND_EFFECTS = setOf(
            org.tabata.timber.core.audio.SoundType.PHASE_START,
            org.tabata.timber.core.audio.SoundType.PHASE_END
        )
    }
    
    override fun canAccessFeature(feature: PremiumFeature): Boolean {
        return when (feature) {
            PremiumFeature.UNLIMITED_PRESETS -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
            PremiumFeature.ADVANCED_AUDIO_EFFECTS -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
            PremiumFeature.WEIGHT_TRACKING -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
            PremiumFeature.ADVANCED_STATISTICS -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
            PremiumFeature.WEARABLE_SUPPORT -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
            PremiumFeature.HEALTH_INTEGRATION -> {
                subscriptionManager.subscriptionStatus.value.isActive
            }
        }
    }
    
    override fun getFeatureLimitStatus(feature: PremiumFeature): FeatureLimitStatus {
        return when (feature) {
            PremiumFeature.UNLIMITED_PRESETS -> {
                if (subscriptionManager.subscriptionStatus.value.isActive) {
                    FeatureLimitStatus.Available
                } else {
                    FeatureLimitStatus.LimitedUsage(remaining = FREE_PRESET_LIMIT, total = FREE_PRESET_LIMIT)
                }
            }
            else -> {
                if (subscriptionManager.subscriptionStatus.value.isActive) {
                    FeatureLimitStatus.Available
                } else {
                    FeatureLimitStatus.RequiresPremium
                }
            }
        }
    }
    
    override suspend fun showUpgradePrompt(feature: PremiumFeature) {
        // Implementation will be added when UI layer is implemented
        // This will show platform-specific upgrade prompts
    }
    
    override fun getRemainingFreeUsage(feature: PremiumFeature): Int? {
        return when (feature) {
            PremiumFeature.UNLIMITED_PRESETS -> {
                if (!subscriptionManager.subscriptionStatus.value.isActive) {
                    FREE_PRESET_LIMIT
                } else {
                    null // Unlimited for premium users
                }
            }
            else -> null // Other features are all-or-nothing
        }
    }
}