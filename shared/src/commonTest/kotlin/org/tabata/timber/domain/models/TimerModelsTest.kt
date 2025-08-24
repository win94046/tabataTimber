package org.tabata.timber.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TimerConfigurationTest {

    @Test
    fun `create valid configuration should succeed`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test Workout",
            warmupDuration = 30.seconds,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = 2.minutes,
            cooldownDuration = 1.minutes,
            rounds = 8,
            sets = 3,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Success)
        val config = (result as ValidationResult.Success).value
        assertEquals("Test Workout", config.name)
        assertEquals(30.seconds, config.warmupDuration)
        assertEquals(45.seconds, config.workDuration)
        assertEquals(15.seconds, config.restDuration)
        assertEquals(2.minutes, config.setBreakDuration)
        assertEquals(1.minutes, config.cooldownDuration)
        assertEquals(8, config.rounds)
        assertEquals(3, config.sets)
    }

    @Test
    fun `create configuration with blank name should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "",
            warmupDuration = 30.seconds,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Name cannot be blank"))
    }

    @Test
    fun `create configuration with too long name should fail`() {
        val audioSettings = AudioSettings()
        val longName = "a".repeat(51)
        val result = TimerConfiguration.create(
            name = longName,
            warmupDuration = 30.seconds,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Name cannot exceed 50 characters"))
    }

    @Test
    fun `create configuration with invalid work duration should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = Duration.ZERO,
            workDuration = Duration.ZERO, // Invalid: work duration must be at least 1 second
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Work duration must be between 1 second and 1 hour"))
    }

    @Test
    fun `create configuration with too long duration should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = 2.hours, // Invalid: exceeds 1 hour limit
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Warmup duration must be between 0 and 1 hour"))
    }

    @Test
    fun `create configuration with invalid rounds should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = Duration.ZERO,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 0, // Invalid: rounds must be at least 1
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Rounds must be between 1 and 100"))
    }

    @Test
    fun `create configuration with too many rounds should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = Duration.ZERO,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 101, // Invalid: exceeds maximum of 100
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Rounds must be between 1 and 100"))
    }

    @Test
    fun `create configuration with invalid sets should fail`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = Duration.ZERO,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 0, // Invalid: sets must be at least 1
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Sets must be between 1 and 20"))
    }

    @Test
    fun `create tabata configuration should succeed`() {
        val result = TimerConfiguration.createTabata()

        assertTrue(result is ValidationResult.Success)
        val config = (result as ValidationResult.Success).value
        assertEquals("Tabata", config.name)
        assertEquals(Duration.ZERO, config.warmupDuration)
        assertEquals(20.seconds, config.workDuration)
        assertEquals(10.seconds, config.restDuration)
        assertEquals(Duration.ZERO, config.setBreakDuration)
        assertEquals(Duration.ZERO, config.cooldownDuration)
        assertEquals(8, config.rounds)
        assertEquals(1, config.sets)
    }

    @Test
    fun `calculate total duration should be correct`() {
        val audioSettings = AudioSettings()
        val result = TimerConfiguration.create(
            name = "Test",
            warmupDuration = 1.minutes,
            workDuration = 30.seconds,
            restDuration = 10.seconds,
            setBreakDuration = 2.minutes,
            cooldownDuration = 1.minutes,
            rounds = 2,
            sets = 2,
            audioSettings = audioSettings
        )

        assertTrue(result is ValidationResult.Success)
        val config = (result as ValidationResult.Success).value
        
        // Expected: 1min warmup + (30s work + 10s rest) * 2 rounds * 2 sets + 2min set break + 1min cooldown
        // = 1min + 80s * 2 + 2min + 1min = 1min + 160s + 2min + 1min = 6min 40s
        val expected = 1.minutes + (30.seconds + 10.seconds) * 2 * 2 + 2.minutes + 1.minutes
        assertEquals(expected, config.calculateTotalDuration())
    }

    @Test
    fun `validate should return same result as create`() {
        val audioSettings = AudioSettings()
        val config = TimerConfiguration(
            name = "",
            warmupDuration = Duration.ZERO,
            workDuration = Duration.ZERO,
            restDuration = Duration.ZERO,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 0,
            sets = 0,
            audioSettings = audioSettings
        )

        val result = config.validate()
        assertTrue(result is ValidationResult.Error)
    }
}

class TimerStateTest {

    @Test
    fun `initial state from configuration with warmup should start with warmup`() {
        val audioSettings = AudioSettings()
        val configResult = TimerConfiguration.create(
            name = "Test",
            warmupDuration = 30.seconds,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(configResult is ValidationResult.Success)
        val config = (configResult as ValidationResult.Success).value
        val state = TimerState.initial(config)

        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(PhaseType.WARMUP, state.currentPhase)
        assertEquals(30.seconds, state.phaseTimeRemaining)
        assertEquals(config.calculateTotalDuration(), state.totalTimeRemaining)
        assertEquals(1, state.currentRound)
        assertEquals(8, state.totalRounds)
        assertEquals(1, state.currentSet)
        assertEquals(1, state.totalSets)
    }

    @Test
    fun `initial state from configuration without warmup should start with work`() {
        val audioSettings = AudioSettings()
        val configResult = TimerConfiguration.create(
            name = "Test",
            warmupDuration = Duration.ZERO,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            setBreakDuration = Duration.ZERO,
            cooldownDuration = Duration.ZERO,
            rounds = 8,
            sets = 1,
            audioSettings = audioSettings
        )

        assertTrue(configResult is ValidationResult.Success)
        val config = (configResult as ValidationResult.Success).value
        val state = TimerState.initial(config)

        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(PhaseType.WORK, state.currentPhase)
        assertEquals(45.seconds, state.phaseTimeRemaining)
    }

    @Test
    fun `status check methods should work correctly`() {
        val audioSettings = AudioSettings()
        val configResult = TimerConfiguration.createTabata()
        assertTrue(configResult is ValidationResult.Success)
        val config = (configResult as ValidationResult.Success).value

        val idleState = TimerState.initial(config)
        assertTrue(idleState.isIdle())
        assertFalse(idleState.isRunning())
        assertFalse(idleState.isPaused())
        assertFalse(idleState.isCompleted())

        val runningState = idleState.copy(status = TimerStatus.RUNNING)
        assertFalse(runningState.isIdle())
        assertTrue(runningState.isRunning())
        assertFalse(runningState.isPaused())
        assertFalse(runningState.isCompleted())

        val pausedState = idleState.copy(status = TimerStatus.PAUSED)
        assertFalse(pausedState.isIdle())
        assertFalse(pausedState.isRunning())
        assertTrue(pausedState.isPaused())
        assertFalse(pausedState.isCompleted())

        val completedState = idleState.copy(status = TimerStatus.COMPLETED)
        assertFalse(completedState.isIdle())
        assertFalse(completedState.isRunning())
        assertFalse(completedState.isPaused())
        assertTrue(completedState.isCompleted())
    }

    @Test
    fun `get progress should calculate correctly`() {
        val totalDuration = 10.minutes
        val state = TimerState(
            status = TimerStatus.RUNNING,
            currentPhase = PhaseType.WORK,
            phaseTimeRemaining = 30.seconds,
            totalTimeRemaining = 5.minutes,
            currentRound = 1,
            totalRounds = 8,
            currentSet = 1,
            totalSets = 1
        )

        val progress = state.getProgress(totalDuration)
        assertEquals(0.5f, progress) // 50% complete (5 minutes remaining out of 10)
    }

    @Test
    fun `get progress with zero total duration should return 1`() {
        val state = TimerState(
            status = TimerStatus.COMPLETED,
            currentPhase = PhaseType.WORK,
            phaseTimeRemaining = Duration.ZERO,
            totalTimeRemaining = Duration.ZERO,
            currentRound = 8,
            totalRounds = 8,
            currentSet = 1,
            totalSets = 1
        )

        val progress = state.getProgress(Duration.ZERO)
        assertEquals(1.0f, progress)
    }
}

class PhaseTypeTest {

    @Test
    fun `phase display names should be correct`() {
        assertEquals("Warm Up", PhaseType.WARMUP.displayName)
        assertEquals("Work", PhaseType.WORK.displayName)
        assertEquals("Rest", PhaseType.REST.displayName)
        assertEquals("Set Break", PhaseType.SET_BREAK.displayName)
        assertEquals("Cool Down", PhaseType.COOLDOWN.displayName)
    }

    @Test
    fun `phase colors should be defined`() {
        assertNotNull(PhaseType.WARMUP.colorHex)
        assertNotNull(PhaseType.WORK.colorHex)
        assertNotNull(PhaseType.REST.colorHex)
        assertNotNull(PhaseType.SET_BREAK.colorHex)
        assertNotNull(PhaseType.COOLDOWN.colorHex)
    }

    @Test
    fun `get next phase from warmup should be work`() {
        val nextPhase = PhaseType.WARMUP.getNextPhase(1, 8, 1, 1, false, false)
        assertEquals(PhaseType.WORK, nextPhase)
    }

    @Test
    fun `get next phase from work mid-round should be rest`() {
        val nextPhase = PhaseType.WORK.getNextPhase(1, 8, 1, 1, false, false)
        assertEquals(PhaseType.REST, nextPhase)
    }

    @Test
    fun `get next phase from work end of rounds with set break should be set break`() {
        val nextPhase = PhaseType.WORK.getNextPhase(8, 8, 1, 2, true, false)
        assertEquals(PhaseType.SET_BREAK, nextPhase)
    }

    @Test
    fun `get next phase from work end of workout with cooldown should be cooldown`() {
        val nextPhase = PhaseType.WORK.getNextPhase(8, 8, 1, 1, false, true)
        assertEquals(PhaseType.COOLDOWN, nextPhase)
    }

    @Test
    fun `get next phase from work end of workout should be null`() {
        val nextPhase = PhaseType.WORK.getNextPhase(8, 8, 1, 1, false, false)
        assertEquals(null, nextPhase)
    }

    @Test
    fun `get next phase from rest should be work`() {
        val nextPhase = PhaseType.REST.getNextPhase(1, 8, 1, 1, false, false)
        assertEquals(PhaseType.WORK, nextPhase)
    }

    @Test
    fun `get next phase from set break should be work`() {
        val nextPhase = PhaseType.SET_BREAK.getNextPhase(1, 8, 2, 2, false, false)
        assertEquals(PhaseType.WORK, nextPhase)
    }

    @Test
    fun `get next phase from cooldown should be null`() {
        val nextPhase = PhaseType.COOLDOWN.getNextPhase(8, 8, 1, 1, false, false)
        assertEquals(null, nextPhase)
    }

    @Test
    fun `requires countdown should be true for all phases except cooldown`() {
        assertTrue(PhaseType.WARMUP.requiresCountdown())
        assertTrue(PhaseType.WORK.requiresCountdown())
        assertTrue(PhaseType.REST.requiresCountdown())
        assertTrue(PhaseType.SET_BREAK.requiresCountdown())
        assertFalse(PhaseType.COOLDOWN.requiresCountdown())
    }
}

class AudioSettingsTest {

    @Test
    fun `create valid audio settings should succeed`() {
        val result = AudioSettings.create(
            ttsEnabled = true,
            soundEffectsEnabled = true,
            vibrationEnabled = false,
            language = "en-US",
            volume = 0.8f,
            musicDuckingEnabled = true
        )

        assertTrue(result is ValidationResult.Success)
        val settings = (result as ValidationResult.Success).value
        assertTrue(settings.ttsEnabled)
        assertTrue(settings.soundEffectsEnabled)
        assertFalse(settings.vibrationEnabled)
        assertEquals("en-US", settings.language)
        assertEquals(0.8f, settings.volume)
        assertTrue(settings.musicDuckingEnabled)
    }

    @Test
    fun `create audio settings with invalid volume should fail`() {
        val result = AudioSettings.create(volume = 1.5f) // Invalid: exceeds maximum

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Volume must be between 0.0 and 1.0"))
    }

    @Test
    fun `create audio settings with negative volume should fail`() {
        val result = AudioSettings.create(volume = -0.1f) // Invalid: below minimum

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Volume must be between 0.0 and 1.0"))
    }

    @Test
    fun `create audio settings with blank language should fail`() {
        val result = AudioSettings.create(language = "")

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Language cannot be blank"))
    }

    @Test
    fun `is language supported should work correctly`() {
        val englishSettings = AudioSettings(language = "en-US")
        assertTrue(englishSettings.isLanguageSupported())

        val unsupportedSettings = AudioSettings(language = "zh-CN")
        assertFalse(unsupportedSettings.isLanguageSupported())
    }

    @Test
    fun `get fallback language should work correctly`() {
        val englishSettings = AudioSettings(language = "en-AU")
        assertEquals("en-US", englishSettings.getFallbackLanguage())

        val spanishSettings = AudioSettings(language = "es-MX")
        assertEquals("es-ES", spanishSettings.getFallbackLanguage())

        val frenchSettings = AudioSettings(language = "fr-CA")
        assertEquals("fr-FR", frenchSettings.getFallbackLanguage())

        val unknownSettings = AudioSettings(language = "zh-CN")
        assertEquals("en-US", unknownSettings.getFallbackLanguage())
    }

    @Test
    fun `validate should return same result as create`() {
        val settings = AudioSettings(volume = 1.5f)
        val result = settings.validate()
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `default audio settings should be valid`() {
        val settings = AudioSettings()
        val result = settings.validate()
        assertTrue(result is ValidationResult.Success)
    }
}

// ============================================================================
// SUBSCRIPTION AND FEATURE ACCESS TESTS
// ============================================================================

class SubscriptionInfoTest {

    @Test
    fun `create free subscription should succeed`() {
        val subscription = SubscriptionInfo.free()
        
        assertEquals(SubscriptionStatus.FREE, subscription.status)
        assertEquals(null, subscription.startDate)
        assertEquals(null, subscription.endDate)
        assertEquals(null, subscription.trialEndDate)
        assertEquals(null, subscription.productId)
        assertFalse(subscription.isAutoRenewing)
        assertFalse(subscription.isActive())
        assertFalse(subscription.isInTrial())
    }

    @Test
    fun `create trial subscription should succeed`() {
        val now = Clock.System.now()
        val trialEnd = now + 7.days
        val result = SubscriptionInfo.trial(trialEnd)

        assertTrue(result is ValidationResult.Success)
        val subscription = (result as ValidationResult.Success).value
        assertEquals(SubscriptionStatus.TRIAL, subscription.status)
        assertEquals(trialEnd, subscription.trialEndDate)
        assertTrue(subscription.isActive())
        assertTrue(subscription.isInTrial())
    }

    @Test
    fun `create trial subscription with past date should fail`() {
        val now = Clock.System.now()
        val pastDate = now - 1.days
        val result = SubscriptionInfo.trial(pastDate)

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Trial end date must be in the future"))
    }

    @Test
    fun `create active subscription should succeed`() {
        val now = Clock.System.now()
        val startDate = now - 1.days
        val endDate = now + 30.days
        val result = SubscriptionInfo.active(startDate, endDate, "premium_monthly")

        assertTrue(result is ValidationResult.Success)
        val subscription = (result as ValidationResult.Success).value
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
        assertEquals(startDate, subscription.startDate)
        assertEquals(endDate, subscription.endDate)
        assertEquals("premium_monthly", subscription.productId)
        assertTrue(subscription.isActive())
        assertFalse(subscription.isInTrial())
    }

    @Test
    fun `create active subscription with invalid dates should fail`() {
        val now = Clock.System.now()
        val startDate = now + 1.days
        val endDate = now - 1.days // End before start
        val result = SubscriptionInfo.active(startDate, endDate, "premium_monthly")

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Start date must be before end date"))
    }

    @Test
    fun `create active subscription with blank product id should fail`() {
        val now = Clock.System.now()
        val startDate = now - 1.days
        val endDate = now + 30.days
        val result = SubscriptionInfo.active(startDate, endDate, "")

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Product ID cannot be blank"))
    }

    @Test
    fun `get effective end date should work correctly`() {
        val now = Clock.System.now()
        
        // Trial subscription
        val trialEnd = now + 7.days
        val trialResult = SubscriptionInfo.trial(trialEnd)
        assertTrue(trialResult is ValidationResult.Success)
        val trialSub = (trialResult as ValidationResult.Success).value
        assertEquals(trialEnd, trialSub.getEffectiveEndDate())

        // Active subscription
        val activeEnd = now + 30.days
        val activeResult = SubscriptionInfo.active(now - 1.days, activeEnd, "premium")
        assertTrue(activeResult is ValidationResult.Success)
        val activeSub = (activeResult as ValidationResult.Success).value
        assertEquals(activeEnd, activeSub.getEffectiveEndDate())

        // Free subscription
        val freeSub = SubscriptionInfo.free()
        assertEquals(null, freeSub.getEffectiveEndDate())
    }

    @Test
    fun `get remaining time should work correctly`() {
        val now = Clock.System.now()
        val endDate = now + 7.days
        val result = SubscriptionInfo.active(now - 1.days, endDate, "premium")
        
        assertTrue(result is ValidationResult.Success)
        val subscription = (result as ValidationResult.Success).value
        val remainingTime = subscription.getRemainingTime()
        
        assertNotNull(remainingTime)
        assertTrue(remainingTime > 6.days)
        assertTrue(remainingTime <= 7.days)
    }

    @Test
    fun `needs renewal soon should work correctly`() {
        val now = Clock.System.now()
        
        // Subscription ending in 3 days (should need renewal)
        val soonEnd = now + 3.days
        val soonResult = SubscriptionInfo.active(now - 1.days, soonEnd, "premium")
        assertTrue(soonResult is ValidationResult.Success)
        val soonSub = (soonResult as ValidationResult.Success).value
        assertTrue(soonSub.needsRenewalSoon())

        // Subscription ending in 30 days (should not need renewal)
        val laterEnd = now + 30.days
        val laterResult = SubscriptionInfo.active(now - 1.days, laterEnd, "premium")
        assertTrue(laterResult is ValidationResult.Success)
        val laterSub = (laterResult as ValidationResult.Success).value
        assertFalse(laterSub.needsRenewalSoon())
    }

    @Test
    fun `update status should expire old subscriptions`() {
        val now = Clock.System.now()
        val pastDate = now - 1.days
        
        // Create an "active" subscription that should be expired
        val expiredSub = SubscriptionInfo(
            status = SubscriptionStatus.ACTIVE,
            startDate = now - 30.days,
            endDate = pastDate,
            trialEndDate = null,
            productId = "premium",
            isAutoRenewing = true
        )

        val updated = expiredSub.updateStatus()
        assertEquals(SubscriptionStatus.EXPIRED, updated.status)
        assertFalse(updated.isActive())
    }

    @Test
    fun `validate should catch invalid configurations`() {
        // Trial without trial end date
        val invalidTrial = SubscriptionInfo(
            status = SubscriptionStatus.TRIAL,
            startDate = null,
            endDate = null,
            trialEndDate = null,
            productId = null
        )
        val trialResult = invalidTrial.validate()
        assertTrue(trialResult is ValidationResult.Error)

        // Active without required fields
        val invalidActive = SubscriptionInfo(
            status = SubscriptionStatus.ACTIVE,
            startDate = null,
            endDate = null,
            trialEndDate = null,
            productId = null
        )
        val activeResult = invalidActive.validate()
        assertTrue(activeResult is ValidationResult.Error)
    }
}

class PremiumFeatureTest {

    @Test
    fun `premium features should have correct properties`() {
        assertEquals("Unlimited Timers", PremiumFeature.UNLIMITED_TIMERS.displayName)
        assertEquals(3, PremiumFeature.UNLIMITED_TIMERS.freeLimit)
        assertTrue(PremiumFeature.UNLIMITED_TIMERS.hasFreeLimit())
        assertEquals(3, PremiumFeature.UNLIMITED_TIMERS.getFreeLimit())

        assertEquals("Custom Sounds", PremiumFeature.CUSTOM_SOUNDS.displayName)
        assertEquals(null, PremiumFeature.CUSTOM_SOUNDS.freeLimit)
        assertFalse(PremiumFeature.CUSTOM_SOUNDS.hasFreeLimit())
        assertEquals(0, PremiumFeature.CUSTOM_SOUNDS.getFreeLimit())
    }

    @Test
    fun `all premium features should have display names`() {
        PremiumFeature.values().forEach { feature ->
            assertTrue(feature.displayName.isNotBlank())
            assertTrue(feature.description.isNotBlank())
        }
    }
}

class FeatureLimitStatusTest {

    @Test
    fun `feature limit status accessibility should work correctly`() {
        assertTrue(FeatureLimitStatus.Available.isAccessible())
        assertFalse(FeatureLimitStatus.RequiresSubscription.isAccessible())
        assertFalse(FeatureLimitStatus.TrialExpired.isAccessible())
        assertFalse(FeatureLimitStatus.LimitReached(3, 3).isAccessible())
        assertFalse(FeatureLimitStatus.TemporarilyUnavailable("Test").isAccessible())
    }

    @Test
    fun `feature limit status messages should be meaningful`() {
        assertEquals("Feature is available", FeatureLimitStatus.Available.getMessage())
        assertEquals("This feature requires a premium subscription", FeatureLimitStatus.RequiresSubscription.getMessage())
        assertEquals("Your trial has expired. Upgrade to continue using this feature", FeatureLimitStatus.TrialExpired.getMessage())
        assertEquals("You've reached the limit of 3. Upgrade for unlimited access", FeatureLimitStatus.LimitReached(3, 3).getMessage())
        assertEquals("Custom reason", FeatureLimitStatus.TemporarilyUnavailable("Custom reason").getMessage())
    }
}

class BasicFeatureGateManagerTest {

    @Test
    fun `free user should have limited access to unlimited timers`() {
        val manager = BasicFeatureGateManager()
        
        // Should allow up to 3 timers
        val status1 = manager.checkFeatureAccess(PremiumFeature.UNLIMITED_TIMERS, 0)
        assertTrue(status1.isAccessible())

        val status2 = manager.checkFeatureAccess(PremiumFeature.UNLIMITED_TIMERS, 2)
        assertTrue(status2.isAccessible())

        val status3 = manager.checkFeatureAccess(PremiumFeature.UNLIMITED_TIMERS, 3)
        assertFalse(status3.isAccessible())
        assertTrue(status3 is FeatureLimitStatus.LimitReached)
    }

    @Test
    fun `free user should not have access to premium features`() {
        val manager = BasicFeatureGateManager()
        
        val customSoundsStatus = manager.checkFeatureAccess(PremiumFeature.CUSTOM_SOUNDS)
        assertFalse(customSoundsStatus.isAccessible())
        assertTrue(customSoundsStatus is FeatureLimitStatus.RequiresSubscription)

        val cloudSyncStatus = manager.checkFeatureAccess(PremiumFeature.CLOUD_SYNC)
        assertFalse(cloudSyncStatus.isAccessible())
        assertTrue(cloudSyncStatus is FeatureLimitStatus.RequiresSubscription)
    }

    @Test
    fun `premium user should have access to all features`() {
        val now = Clock.System.now()
        val activeSubResult = SubscriptionInfo.active(
            now - 1.days, 
            now + 30.days, 
            "premium_monthly"
        )
        assertTrue(activeSubResult is ValidationResult.Success)
        val activeSub = (activeSubResult as ValidationResult.Success).value

        val manager = BasicFeatureGateManager(activeSub)
        
        assertTrue(manager.hasPremiumAccess())
        
        // Should have access to all features
        PremiumFeature.values().forEach { feature ->
            val status = manager.checkFeatureAccess(feature, 100) // High usage
            assertTrue(status.isAccessible(), "Feature ${feature.displayName} should be accessible")
        }
    }

    @Test
    fun `trial user should have access to all features`() {
        val now = Clock.System.now()
        val trialSubResult = SubscriptionInfo.trial(now + 7.days)
        assertTrue(trialSubResult is ValidationResult.Success)
        val trialSub = (trialSubResult as ValidationResult.Success).value

        val manager = BasicFeatureGateManager(trialSub)
        
        assertTrue(manager.hasPremiumAccess())
        
        // Should have access to all features during trial
        PremiumFeature.values().forEach { feature ->
            val status = manager.checkFeatureAccess(feature)
            assertTrue(status.isAccessible(), "Feature ${feature.displayName} should be accessible during trial")
        }
    }

    @Test
    fun `expired trial user should see trial expired message`() {
        val now = Clock.System.now()
        val expiredTrialSub = SubscriptionInfo(
            status = SubscriptionStatus.EXPIRED,
            startDate = now - 14.days,
            endDate = null,
            trialEndDate = now - 7.days,
            productId = "trial"
        )

        val manager = BasicFeatureGateManager(expiredTrialSub)
        
        assertFalse(manager.hasPremiumAccess())
        
        val status = manager.checkFeatureAccess(PremiumFeature.CUSTOM_SOUNDS)
        assertFalse(status.isAccessible())
        assertTrue(status is FeatureLimitStatus.TrialExpired)
    }

    @Test
    fun `can create timer should respect limits`() {
        val manager = BasicFeatureGateManager()
        
        assertTrue(manager.canCreateTimer(0).isAccessible())
        assertTrue(manager.canCreateTimer(2).isAccessible())
        assertFalse(manager.canCreateTimer(3).isAccessible())
        
        assertEquals(3, manager.getMaxTimersAllowed())
    }

    @Test
    fun `premium user should have unlimited timers`() {
        val now = Clock.System.now()
        val activeSubResult = SubscriptionInfo.active(
            now - 1.days, 
            now + 30.days, 
            "premium_monthly"
        )
        assertTrue(activeSubResult is ValidationResult.Success)
        val activeSub = (activeSubResult as ValidationResult.Success).value

        val manager = BasicFeatureGateManager(activeSub)
        
        assertTrue(manager.canCreateTimer(1000).isAccessible())
        assertEquals(Int.MAX_VALUE, manager.getMaxTimersAllowed())
    }

    @Test
    fun `should show ads for free users`() {
        val freeManager = BasicFeatureGateManager()
        assertTrue(freeManager.shouldShowAds())

        val now = Clock.System.now()
        val activeSubResult = SubscriptionInfo.active(
            now - 1.days, 
            now + 30.days, 
            "premium_monthly"
        )
        assertTrue(activeSubResult is ValidationResult.Success)
        val activeSub = (activeSubResult as ValidationResult.Success).value

        val premiumManager = BasicFeatureGateManager(activeSub)
        assertFalse(premiumManager.shouldShowAds())
    }

    @Test
    fun `get usage limits should return correct limits`() {
        val manager = BasicFeatureGateManager()
        val limits = manager.getUsageLimits()
        
        assertEquals(3, limits[PremiumFeature.UNLIMITED_TIMERS])
        assertFalse(limits.containsKey(PremiumFeature.CUSTOM_SOUNDS))
    }

    @Test
    fun `update subscription info should work correctly`() {
        val manager = BasicFeatureGateManager()
        
        // Initially free
        assertFalse(manager.hasPremiumAccess())
        
        // Update to premium
        val now = Clock.System.now()
        val activeSubResult = SubscriptionInfo.active(
            now - 1.days, 
            now + 30.days, 
            "premium_monthly"
        )
        assertTrue(activeSubResult is ValidationResult.Success)
        val activeSub = (activeSubResult as ValidationResult.Success).value

        manager.updateSubscriptionInfo(activeSub)
        assertTrue(manager.hasPremiumAccess())
    }
}

// ============================================================================
// WEIGHT TRACKING TESTS
// ============================================================================

class BMICategoryTest {

    @Test
    fun `BMI categories should have correct ranges`() {
        assertEquals(BMICategory.UNDERWEIGHT, BMICategory.fromBMI(15.0))
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(22.0))
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(27.0))
        assertEquals(BMICategory.OBESE_CLASS_I, BMICategory.fromBMI(32.0))
        assertEquals(BMICategory.OBESE_CLASS_II, BMICategory.fromBMI(37.0))
        assertEquals(BMICategory.OBESE_CLASS_III, BMICategory.fromBMI(45.0))
    }

    @Test
    fun `BMI boundary values should be classified correctly`() {
        assertEquals(BMICategory.UNDERWEIGHT, BMICategory.fromBMI(18.4))
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(18.5))
        assertEquals(BMICategory.NORMAL, BMICategory.fromBMI(24.9))
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(25.0))
        assertEquals(BMICategory.OVERWEIGHT, BMICategory.fromBMI(29.9))
        assertEquals(BMICategory.OBESE_CLASS_I, BMICategory.fromBMI(30.0))
    }

    @Test
    fun `only normal BMI category should be considered healthy`() {
        assertTrue(BMICategory.NORMAL.isHealthy())
        assertFalse(BMICategory.UNDERWEIGHT.isHealthy())
        assertFalse(BMICategory.OVERWEIGHT.isHealthy())
        assertFalse(BMICategory.OBESE_CLASS_I.isHealthy())
    }

    @Test
    fun `all BMI categories should have health recommendations`() {
        BMICategory.values().forEach { category ->
            assertTrue(category.getHealthRecommendation().isNotBlank())
        }
    }

    @Test
    fun `get all ranges should return correct format`() {
        val ranges = BMICategory.getAllRanges()
        assertEquals(BMICategory.values().size, ranges.size)
        
        val normalRange = ranges.find { it.first == BMICategory.NORMAL }
        assertNotNull(normalRange)
        assertTrue(normalRange!!.second.contains("18.5"))
        assertTrue(normalRange.second.contains("25.0"))
    }
}

class WeightEntryTest {

    @Test
    fun `create valid weight entry should succeed`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = 1.75,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Success)
        val entry = (result as ValidationResult.Success).value
        assertEquals("test-1", entry.id)
        assertEquals(70.0, entry.weight)
        assertEquals(1.75, entry.height)
        assertEquals(WeightDataSource.MANUAL, entry.source)
    }

    @Test
    fun `create weight entry with invalid weight should fail`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 5.0, // Too low
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Weight must be between"))
    }

    @Test
    fun `create weight entry with invalid height should fail`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = 0.3, // Too short
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Height must be between"))
    }

    @Test
    fun `create weight entry with future date should fail`() {
        val future = Clock.System.now() + 1.hours
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            recordedAt = future
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Recorded time cannot be in the future"))
    }

    @Test
    fun `create weight entry with blank id should fail`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "",
            weight = 70.0,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("ID cannot be blank"))
    }

    @Test
    fun `create weight entry with too long notes should fail`() {
        val now = Clock.System.now()
        val longNotes = "a".repeat(501)
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            recordedAt = now - 1.hours,
            notes = longNotes
        )

        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("Notes cannot exceed"))
    }

    @Test
    fun `BMI calculation should be correct`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = 1.75,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Success)
        val entry = (result as ValidationResult.Success).value
        val bmi = entry.calculateBMI()
        
        assertNotNull(bmi)
        // BMI = 70 / (1.75^2) = 22.86
        assertEquals(22.86, bmi!!, 0.01)
    }

    @Test
    fun `BMI calculation without height should return null`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = null,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Success)
        val entry = (result as ValidationResult.Success).value
        assertEquals(null, entry.calculateBMI())
    }

    @Test
    fun `BMI category should be determined correctly`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = 1.75,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Success)
        val entry = (result as ValidationResult.Success).value
        assertEquals(BMICategory.NORMAL, entry.getBMICategory())
    }

    @Test
    fun `unit conversions should be accurate`() {
        // Test pounds to kg
        assertEquals(45.36, WeightEntry.poundsToKg(100.0), 0.01)
        
        // Test kg to pounds
        assertEquals(220.46, WeightEntry.kgToPounds(100.0), 0.01)
        
        // Test feet/inches to meters
        assertEquals(1.8288, WeightEntry.feetInchesToMeters(6, 0.0), 0.001)
        
        // Test meters to feet/inches
        val (feet, inches) = WeightEntry.metersToFeetInches(1.75)
        assertEquals(5, feet)
        assertEquals(8.9, inches, 0.1)
    }

    @Test
    fun `is complete should check for height presence`() {
        val now = Clock.System.now()
        
        val completeResult = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            height = 1.75,
            recordedAt = now - 1.hours
        )
        assertTrue(completeResult is ValidationResult.Success)
        assertTrue((completeResult as ValidationResult.Success).value.isComplete())

        val incompleteResult = WeightEntry.create(
            id = "test-2",
            weight = 70.0,
            height = null,
            recordedAt = now - 1.hours
        )
        assertTrue(incompleteResult is ValidationResult.Success)
        assertFalse((incompleteResult as ValidationResult.Success).value.isComplete())
    }

    @Test
    fun `get weight in pounds should convert correctly`() {
        val now = Clock.System.now()
        val result = WeightEntry.create(
            id = "test-1",
            weight = 70.0,
            recordedAt = now - 1.hours
        )

        assertTrue(result is ValidationResult.Success)
        val entry = (result as ValidationResult.Success).value
        assertEquals(154.32, entry.getWeightInPounds(), 0.01)
    }
}

class WeightStatsTest {

    private fun createTestEntry(id: String, weight: Double, height: Double? = null, daysAgo: Int = 0): WeightEntry {
        val recordedAt = Clock.System.now() - daysAgo.days
        val result = WeightEntry.create(id, weight, height, recordedAt)
        return (result as ValidationResult.Success).value
    }

    @Test
    fun `from entries should create stats correctly`() {
        val entries = listOf(
            createTestEntry("1", 75.0, 1.75, 30),
            createTestEntry("2", 73.0, null, 15),
            createTestEntry("3", 71.0, null, 0)
        )

        val stats = WeightStats.fromEntries(entries)
        assertEquals(3, stats.entries.size)
        assertEquals(1.75, stats.currentHeight) // Should use height from first entry with height
    }

    @Test
    fun `get current weight should return most recent entry`() {
        val entries = listOf(
            createTestEntry("1", 75.0, daysAgo = 30),
            createTestEntry("2", 73.0, daysAgo = 15),
            createTestEntry("3", 71.0, daysAgo = 0)
        )

        val stats = WeightStats.fromEntries(entries)
        val current = stats.getCurrentWeight()
        
        assertNotNull(current)
        assertEquals(71.0, current!!.weight)
        assertEquals("3", current.id)
    }

    @Test
    fun `get current BMI should calculate correctly`() {
        val entries = listOf(
            createTestEntry("1", 70.0, 1.75, 0)
        )

        val stats = WeightStats.fromEntries(entries, currentHeight = 1.75)
        val bmi = stats.getCurrentBMI()
        
        assertNotNull(bmi)
        assertEquals(22.86, bmi!!, 0.01)
    }

    @Test
    fun `get weight change should calculate difference from previous entry`() {
        val entries = listOf(
            createTestEntry("1", 75.0, daysAgo = 15),
            createTestEntry("2", 73.0, daysAgo = 0)
        )

        val stats = WeightStats.fromEntries(entries)
        val change = stats.getWeightChange()
        
        assertNotNull(change)
        assertEquals(-2.0, change!!, 0.01)
    }

    @Test
    fun `get weight change with single entry should return null`() {
        val entries = listOf(createTestEntry("1", 75.0))
        val stats = WeightStats.fromEntries(entries)
        
        assertEquals(null, stats.getWeightChange())
    }

    @Test
    fun `get weight trend should detect trends correctly`() {
        // Increasing trend
        val increasingEntries = listOf(
            createTestEntry("1", 70.0, daysAgo = 20),
            createTestEntry("2", 72.0, daysAgo = 10),
            createTestEntry("3", 74.0, daysAgo = 0)
        )
        val increasingStats = WeightStats.fromEntries(increasingEntries)
        assertEquals(WeightTrend.INCREASING, increasingStats.getWeightTrend())

        // Decreasing trend
        val decreasingEntries = listOf(
            createTestEntry("1", 80.0, daysAgo = 20),
            createTestEntry("2", 78.0, daysAgo = 10),
            createTestEntry("3", 76.0, daysAgo = 0)
        )
        val decreasingStats = WeightStats.fromEntries(decreasingEntries)
        assertEquals(WeightTrend.DECREASING, decreasingStats.getWeightTrend())

        // Stable trend
        val stableEntries = listOf(
            createTestEntry("1", 75.0, daysAgo = 20),
            createTestEntry("2", 75.2, daysAgo = 10),
            createTestEntry("3", 74.8, daysAgo = 0)
        )
        val stableStats = WeightStats.fromEntries(stableEntries)
        assertEquals(WeightTrend.STABLE, stableStats.getWeightTrend())
    }

    @Test
    fun `get average weight should calculate correctly`() {
        val entries = listOf(
            createTestEntry("1", 70.0, daysAgo = 20),
            createTestEntry("2", 72.0, daysAgo = 10),
            createTestEntry("3", 74.0, daysAgo = 0)
        )

        val stats = WeightStats.fromEntries(entries)
        val average = stats.getAverageWeight(30)
        
        assertNotNull(average)
        assertEquals(72.0, average!!, 0.01)
    }

    @Test
    fun `get highest and lowest weight should work correctly`() {
        val entries = listOf(
            createTestEntry("1", 70.0),
            createTestEntry("2", 75.0),
            createTestEntry("3", 68.0)
        )

        val stats = WeightStats.fromEntries(entries)
        
        assertEquals(75.0, stats.getHighestWeight()?.weight)
        assertEquals(68.0, stats.getLowestWeight()?.weight)
    }

    @Test
    fun `get progress to target should calculate correctly`() {
        val entries = listOf(
            createTestEntry("1", 80.0, daysAgo = 30), // Starting weight
            createTestEntry("2", 75.0, daysAgo = 0)   // Current weight
        )

        val stats = WeightStats.fromEntries(entries)
        val progress = stats.getProgressToTarget(70.0) // Target weight
        
        assertNotNull(progress)
        assertEquals(80.0, progress!!.startingWeight)
        assertEquals(75.0, progress.currentWeight)
        assertEquals(70.0, progress.targetWeight)
        assertEquals(50.0, progress.progressPercentage, 0.01) // 50% progress
        assertEquals(-5.0, progress.remainingWeight) // Need to lose 5kg more
    }

    @Test
    fun `get summary should include all statistics`() {
        val entries = listOf(
            createTestEntry("1", 80.0, 1.75, 30),
            createTestEntry("2", 75.0, null, 0)
        )

        val stats = WeightStats.fromEntries(entries)
        val summary = stats.getSummary()
        
        assertEquals(2, summary.totalEntries)
        assertEquals(75.0, summary.currentWeight)
        assertNotNull(summary.currentBMI)
        assertEquals(BMICategory.NORMAL, summary.currentBMICategory)
        assertEquals(-5.0, summary.weightChange)
        assertEquals(WeightTrend.DECREASING, summary.trend)
        assertNotNull(summary.firstRecordDate)
        assertNotNull(summary.lastRecordDate)
    }
}

class WeightTrendTest {

    @Test
    fun `weight trends should have correct properties`() {
        assertEquals("Increasing", WeightTrend.INCREASING.displayName)
        assertEquals("Decreasing", WeightTrend.DECREASING.displayName)
        assertEquals("Stable", WeightTrend.STABLE.displayName)
        
        assertTrue(WeightTrend.INCREASING.getDescription().contains("upward"))
        assertTrue(WeightTrend.DECREASING.getDescription().contains("downward"))
        assertTrue(WeightTrend.STABLE.getDescription().contains("stable"))
    }
}

class WeightProgressTest {

    @Test
    fun `is target reached should work correctly`() {
        val reachedProgress = WeightProgress(
            startingWeight = 80.0,
            currentWeight = 70.05, // Within 100g of target
            targetWeight = 70.0,
            progressPercentage = 100.0,
            remainingWeight = -0.05
        )
        assertTrue(reachedProgress.isTargetReached())

        val notReachedProgress = WeightProgress(
            startingWeight = 80.0,
            currentWeight = 75.0,
            targetWeight = 70.0,
            progressPercentage = 50.0,
            remainingWeight = -5.0
        )
        assertFalse(notReachedProgress.isTargetReached())
    }

    @Test
    fun `get progress description should be meaningful`() {
        val loseWeightProgress = WeightProgress(
            startingWeight = 80.0,
            currentWeight = 75.0,
            targetWeight = 70.0,
            progressPercentage = 50.0,
            remainingWeight = -5.0
        )
        assertTrue(loseWeightProgress.getProgressDescription().contains("lose"))

        val gainWeightProgress = WeightProgress(
            startingWeight = 60.0,
            currentWeight = 65.0,
            targetWeight = 70.0,
            progressPercentage = 50.0,
            remainingWeight = 5.0
        )
        assertTrue(gainWeightProgress.getProgressDescription().contains("gain"))

        val reachedProgress = WeightProgress(
            startingWeight = 80.0,
            currentWeight = 70.0,
            targetWeight = 70.0,
            progressPercentage = 100.0,
            remainingWeight = 0.0
        )
        assertTrue(reachedProgress.getProgressDescription().contains("reached"))
    }
}