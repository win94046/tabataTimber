package org.tabata.timber.domain.models.subscription

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionStatusTest {
    
    private val currentTime = Instant.fromEpochSeconds(1640995200) // 2022-01-01 00:00:00 UTC
    private val pastTime = Instant.fromEpochSeconds(1609459200)    // 2021-01-01 00:00:00 UTC
    private val futureTime = Instant.fromEpochSeconds(1672531200)  // 2023-01-01 00:00:00 UTC
    
    @Test
    fun `Free status has no premium access`() {
        val status = SubscriptionStatus.Free()
        
        assertFalse(status.hasPremiumAccess(currentTime))
        assertEquals(SubscriptionTier.FREE, status.getSubscriptionTier())
        assertEquals("免費版", status.getDisplayStatus())
    }
    
    @Test
    fun `Free status with trial used shows correct display`() {
        val status = SubscriptionStatus.Free(
            trialUsed = true,
            trialEndDate = pastTime
        )
        
        assertEquals("免費版", status.getDisplayStatus())
    }
    
    @Test
    fun `Premium status with active subscription has premium access`() {
        val status = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = pastTime,
            expiryDate = futureTime
        )
        
        assertTrue(status.hasPremiumAccess(currentTime))
        assertTrue(status.isActive(currentTime))
        assertEquals(SubscriptionTier.PREMIUM, status.getSubscriptionTier())
        assertEquals("高級版", status.getDisplayStatus())
    }
    
    @Test
    fun `Premium status with expired subscription has no premium access`() {
        val status = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = pastTime,
            expiryDate = pastTime.plus(kotlinx.datetime.DateTimeUnit.MONTH, 1)
        )
        
        assertFalse(status.hasPremiumAccess(currentTime))
        assertFalse(status.isActive(currentTime))
    }
    
    @Test
    fun `Premium status calculates days until expiry correctly`() {
        val expiryIn10Days = currentTime.plus(kotlinx.datetime.DateTimeUnit.DAY, 10)
        val status = SubscriptionStatus.Premium(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            startDate = pastTime,
            expiryDate = expiryIn10Days
        )
        
        assertEquals(10L, status.getDaysUntilExpiry(currentTime))
        assertTrue(status.isExpiringWithinDays(15, currentTime))
        assertFalse(status.isExpiringWithinDays(5, currentTime))
    }
    
    @Test
    fun `Trial status with active trial has premium access`() {
        val status = SubscriptionStatus.Trial(
            trialId = "trial_123",
            startDate = pastTime,
            expiryDate = futureTime
        )
        
        assertTrue(status.hasPremiumAccess(currentTime))
        assertTrue(status.isActive(currentTime))
        assertEquals(SubscriptionTier.PREMIUM, status.getSubscriptionTier())
        assertEquals("試用版", status.getDisplayStatus())
    }
    
    @Test
    fun `Trial status calculates days remaining correctly`() {
        val expiryIn7Days = currentTime.plus(kotlinx.datetime.DateTimeUnit.DAY, 7)
        val status = SubscriptionStatus.Trial(
            trialId = "trial_123",
            startDate = pastTime,
            expiryDate = expiryIn7Days
        )
        
        assertEquals(7L, status.getDaysRemaining(currentTime))
    }
    
    @Test
    fun `Trial status with expired trial has no premium access`() {
        val status = SubscriptionStatus.Trial(
            trialId = "trial_123",
            startDate = pastTime,
            expiryDate = pastTime.plus(kotlinx.datetime.DateTimeUnit.DAY, 7)
        )
        
        assertFalse(status.hasPremiumAccess(currentTime))
        assertFalse(status.isActive(currentTime))
    }
    
    @Test
    fun `Expired status without grace period has no premium access`() {
        val status = SubscriptionStatus.Expired(
            lastSubscriptionId = "sub_123",
            lastPlanId = "premium_monthly",
            expiryDate = pastTime
        )
        
        assertFalse(status.hasPremiumAccess(currentTime))
        assertFalse(status.isInGracePeriod(currentTime))
        assertEquals(SubscriptionTier.FREE, status.getSubscriptionTier())
        assertEquals("已到期", status.getDisplayStatus())
    }
    
    @Test
    fun `Expired status with active grace period has premium access`() {
        val status = SubscriptionStatus.Expired(
            lastSubscriptionId = "sub_123",
            lastPlanId = "premium_monthly",
            expiryDate = pastTime,
            gracePeriodEndDate = futureTime
        )
        
        assertTrue(status.hasPremiumAccess(currentTime))
        assertTrue(status.isInGracePeriod(currentTime))
        assertEquals(SubscriptionTier.PREMIUM, status.getSubscriptionTier())
        assertEquals("已到期 (寬限期)", status.getDisplayStatus())
    }
    
    @Test
    fun `Cancelled status before expiry still has premium access`() {
        val status = SubscriptionStatus.Cancelled(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            cancellationDate = pastTime,
            expiryDate = futureTime
        )
        
        assertTrue(status.hasPremiumAccess(currentTime))
        assertTrue(status.isValidUntilExpiry(currentTime))
        assertEquals(SubscriptionTier.PREMIUM, status.getSubscriptionTier())
        assertEquals("已取消", status.getDisplayStatus())
    }
    
    @Test
    fun `Cancelled status after expiry has no premium access`() {
        val status = SubscriptionStatus.Cancelled(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            cancellationDate = pastTime,
            expiryDate = pastTime.plus(kotlinx.datetime.DateTimeUnit.DAY, 10)
        )
        
        assertFalse(status.hasPremiumAccess(currentTime))
        assertFalse(status.isValidUntilExpiry(currentTime))
        assertEquals(SubscriptionTier.FREE, status.getSubscriptionTier())
    }
    
    @Test
    fun `PaymentFailed status still has premium access during retry period`() {
        val status = SubscriptionStatus.PaymentFailed(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            lastSuccessfulPayment = pastTime,
            nextRetryDate = futureTime,
            retryCount = 1
        )
        
        assertTrue(status.hasPremiumAccess(currentTime))
        assertEquals(SubscriptionTier.PREMIUM, status.getSubscriptionTier())
        assertEquals("付款失敗", status.getDisplayStatus())
        assertFalse(status.hasExceededMaxRetries())
    }
    
    @Test
    fun `PaymentFailed status tracks retry count correctly`() {
        val status = SubscriptionStatus.PaymentFailed(
            subscriptionId = "sub_123",
            planId = "premium_monthly",
            lastSuccessfulPayment = pastTime,
            nextRetryDate = futureTime,
            retryCount = 3,
            maxRetries = 3
        )
        
        assertTrue(status.hasExceededMaxRetries())
    }
    
    @Test
    fun `PaymentProvider enum contains expected values`() {
        val expectedProviders = setOf(
            PaymentProvider.GOOGLE_PLAY,
            PaymentProvider.APP_STORE,
            PaymentProvider.STRIPE,
            PaymentProvider.UNKNOWN
        )
        
        assertEquals(expectedProviders, PaymentProvider.values().toSet())
    }
    
    @Test
    fun `CancellationReason enum contains expected values`() {
        val expectedReasons = setOf(
            CancellationReason.USER_CANCELLED,
            CancellationReason.PAYMENT_FAILED,
            CancellationReason.REFUND_REQUESTED,
            CancellationReason.POLICY_VIOLATION,
            CancellationReason.OTHER
        )
        
        assertEquals(expectedReasons, CancellationReason.values().toSet())
    }
    
    @Test
    fun `SubscriptionTier enum contains expected values`() {
        val expectedTiers = setOf(
            SubscriptionTier.FREE,
            SubscriptionTier.PREMIUM
        )
        
        assertEquals(expectedTiers, SubscriptionTier.values().toSet())
    }
    
    @Test
    fun `Premium status with different payment providers`() {
        val providers = PaymentProvider.values()
        
        providers.forEach { provider ->
            val status = SubscriptionStatus.Premium(
                subscriptionId = "sub_${provider.name}",
                planId = "premium_monthly",
                startDate = pastTime,
                expiryDate = futureTime,
                paymentProvider = provider
            )
            
            assertEquals(provider, status.paymentProvider)
            assertTrue(status.hasPremiumAccess(currentTime))
        }
    }
}