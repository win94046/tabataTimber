package org.tabata.timber.domain.models.subscription

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureGateManagerTest {
    
    private lateinit var featureGateManager: BasicFeatureGateManager
    private val currentTime = kotlinx.datetime.Instant.fromEpochSeconds(1640995200) // 2022-01-01
    private val futureTime = kotlinx.datetime.Instant.fromEpochSeconds(1672531200)  // 2023-01-01
    
    @BeforeTest
    fun setup() {
        featureGateManager = BasicFeatureGateManager()
    }
    
    @Test
    fun `free user has limited access to premium features`() {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        // Should not have access to most premium features
        assertFalse(featureGateManager.hasFeatureAccess(PremiumFeature.UNLIMITED_SETS))
        assertFalse(featureGateManager.hasFeatureAccess(PremiumFeature.PREMIUM_SOUNDS))
        assertFalse(featureGateManager.hasFeatureAccess(PremiumFeature.DETAILED_ANALYTICS))
        
        // Should not have access to NO_ADS
        assertFalse(featureGateManager.hasFeatureAccess(PremiumFeature.NO_ADS))
    }
    
    @Test
    fun `premium user has access to all premium features`() {
        val premiumStatus = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager = BasicFeatureGateManager(premiumStatus)
        
        // Should have access to all premium features
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.UNLIMITED_SETS))
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.PREMIUM_SOUNDS))
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.DETAILED_ANALYTICS))
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.NO_ADS))
    }
    
    @Test
    fun `trial user has access to premium features`() {
        val trialStatus = SubscriptionStatus.Trial(
            trialId = "trial_123",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager = BasicFeatureGateManager(trialStatus)
        
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.UNLIMITED_SETS))
        assertTrue(featureGateManager.hasFeatureAccess(PremiumFeature.PREMIUM_SOUNDS))
    }
    
    @Test
    fun `expired user without grace period loses premium access`() {
        val expiredStatus = SubscriptionStatus.Expired(
            lastSubscriptionId = "sub_123",
            lastPlanId = "premium_monthly",
            expiryDate = currentTime.minus(kotlinx.datetime.DateTimeUnit.DAY, 1)
        )
        featureGateManager = BasicFeatureGateManager(expiredStatus)
        
        assertFalse(featureGateManager.hasFeatureAccess(PremiumFeature.UNLIMITED_SETS))
    }
    
    @Test
    fun `getFeatureLimitStatus returns unlimited for premium users`() {
        val premiumStatus = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager = BasicFeatureGateManager(premiumStatus)
        
        val limitStatus = featureGateManager.getFeatureLimitStatus(PremiumFeature.CUSTOM_TIMERS)
        
        assertTrue(limitStatus.isUnlimited)
        assertEquals(Int.MAX_VALUE, limitStatus.maxLimit)
    }
    
    @Test
    fun `getFeatureLimitStatus returns limited for free users`() {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        val limitStatus = featureGateManager.getFeatureLimitStatus(PremiumFeature.CUSTOM_TIMERS)
        
        assertFalse(limitStatus.isUnlimited)
        assertEquals(3, limitStatus.maxLimit) // Default limit for custom timers
        assertEquals(0, limitStatus.currentUsage)
    }
    
    @Test
    fun `tryUseFeature allows usage for premium users`() = runTest {
        val premiumStatus = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager = BasicFeatureGateManager(premiumStatus)
        
        val result = featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        
        assertTrue(result is FeatureUsageResult.Allowed)
    }
    
    @Test
    fun `tryUseFeature blocks non-premium users for premium features`() = runTest {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        val result = featureGateManager.tryUseFeature(PremiumFeature.PREMIUM_SOUNDS)
        
        assertTrue(result is FeatureUsageResult.Blocked)
        assertEquals(BlockReason.REQUIRES_PREMIUM, result.reason)
        assertEquals(SuggestedAction.UPGRADE_TO_PREMIUM, result.suggestedAction)
    }
    
    @Test
    fun `tryUseFeature tracks usage for free users with limits`() = runTest {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        // Use feature multiple times
        repeat(3) {
            val result = featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
            assertTrue(result is FeatureUsageResult.Allowed || result is FeatureUsageResult.AllowedWithWarning)
        }
        
        // Fourth usage should be blocked (limit is 3)
        val result = featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        assertTrue(result is FeatureUsageResult.Blocked)
        assertEquals(BlockReason.LIMIT_REACHED, result.reason)
    }
    
    @Test
    fun `tryUseFeature shows warning when approaching limit`() = runTest {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        // Use feature to get close to limit (3rd usage out of 3 max)
        repeat(2) {
            featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        }
        
        val result = featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        assertTrue(result is FeatureUsageResult.AllowedWithWarning)
        assertEquals(0, result.remainingUsage)
    }
    
    @Test
    fun `updateSubscriptionStatus upgrades limits for premium users`() = runTest {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        // Start as free user
        val initialLimitStatus = featureGateManager.getFeatureLimitStatus(PremiumFeature.CUSTOM_TIMERS)
        assertFalse(initialLimitStatus.isUnlimited)
        
        // Upgrade to premium
        val premiumStatus = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager.updateSubscriptionStatus(premiumStatus)
        
        // Should now have unlimited access
        val upgradedLimitStatus = featureGateManager.getFeatureLimitStatus(PremiumFeature.CUSTOM_TIMERS)
        assertTrue(upgradedLimitStatus.isUnlimited)
    }
    
    @Test
    fun `getAllFeatureStatuses returns all features`() {
        val statuses = featureGateManager.getAllFeatureStatuses()
        
        assertEquals(PremiumFeature.values().size, statuses.size)
        PremiumFeature.values().forEach { feature ->
            assertTrue(statuses.containsKey(feature))
        }
    }
    
    @Test
    fun `shouldShowUpgradePrompt returns false for premium users`() {
        val premiumStatus = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = currentTime,
            expiryDate = futureTime
        )
        featureGateManager = BasicFeatureGateManager(premiumStatus)
        
        assertFalse(featureGateManager.shouldShowUpgradePrompt(PremiumFeature.CUSTOM_TIMERS))
    }
    
    @Test
    fun `shouldShowUpgradePrompt returns true when approaching limit`() = runTest {
        val freeStatus = SubscriptionStatus.Free()
        featureGateManager = BasicFeatureGateManager(freeStatus)
        
        // Use feature to approach limit
        repeat(3) { // Use all 3 allowed custom timers
            featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        }
        
        assertTrue(featureGateManager.shouldShowUpgradePrompt(PremiumFeature.CUSTOM_TIMERS))
    }
    
    @Test
    fun `resetExpiredLimits resets appropriate counters`() = runTest {
        // This test would need more complex setup with time-based limits
        // For now, just ensure it doesn't crash
        featureGateManager.resetExpiredLimits()
        
        // Verify the manager is still functional
        val result = featureGateManager.tryUseFeature(PremiumFeature.CUSTOM_TIMERS)
        assertTrue(result is FeatureUsageResult.Allowed)
    }
    
    @Test
    fun `different subscription states return correct block reasons`() = runTest {
        // Test expired subscription
        val expiredStatus = SubscriptionStatus.Expired(
            lastSubscriptionId = "sub_123",
            lastPlanId = "premium_monthly", 
            expiryDate = currentTime.minus(kotlinx.datetime.DateTimeUnit.DAY, 1)
        )
        featureGateManager.updateSubscriptionStatus(expiredStatus)
        
        val expiredResult = featureGateManager.tryUseFeature(PremiumFeature.PREMIUM_SOUNDS)
        assertTrue(expiredResult is FeatureUsageResult.Blocked)
        assertEquals(BlockReason.SUBSCRIPTION_EXPIRED, expiredResult.reason)
        
        // Test payment failed
        val paymentFailedStatus = SubscriptionStatus.PaymentFailed(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            lastSuccessfulPayment = currentTime.minus(kotlinx.datetime.DateTimeUnit.DAY, 1),
            nextRetryDate = currentTime.plus(kotlinx.datetime.DateTimeUnit.DAY, 1)
        )
        featureGateManager.updateSubscriptionStatus(paymentFailedStatus)
        
        val paymentFailedResult = featureGateManager.tryUseFeature(PremiumFeature.PREMIUM_SOUNDS)
        assertTrue(paymentFailedResult is FeatureUsageResult.Allowed) // Still allowed during retry period
        
        // Test expired trial
        val expiredTrialStatus = SubscriptionStatus.Trial(
            trialId = "trial_123",
            startDate = currentTime.minus(kotlinx.datetime.DateTimeUnit.DAY, 8),
            expiryDate = currentTime.minus(kotlinx.datetime.DateTimeUnit.DAY, 1)
        )
        featureGateManager.updateSubscriptionStatus(expiredTrialStatus)
        
        val expiredTrialResult = featureGateManager.tryUseFeature(PremiumFeature.PREMIUM_SOUNDS)
        assertTrue(expiredTrialResult is FeatureUsageResult.Blocked)
        assertEquals(BlockReason.REQUIRES_PREMIUM, expiredTrialResult.reason) // Trial ended becomes requires premium
    }
}