package org.tabata.timber.domain.models

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue

/**
 * Validation result for timer configuration
 */
sealed class ValidationResult<out T> {
    data class Success<T>(val value: T) : ValidationResult<T>()
    data class Error(val message: String) : ValidationResult<Nothing>()
}

/**
 * Timer configuration for a workout session
 */
data class TimerConfiguration(
    val name: String,
    val warmupDuration: Duration,
    val workDuration: Duration,
    val restDuration: Duration,
    val setBreakDuration: Duration,
    val cooldownDuration: Duration,
    val rounds: Int,
    val sets: Int,
    val audioSettings: AudioSettings
) {
    companion object {
        private val MIN_WORK_DURATION = 1.seconds
        private val MAX_DURATION = 1.hours
        private val MIN_DURATION = Duration.ZERO
        private const val MIN_ROUNDS = 1
        private const val MAX_ROUNDS = 100
        private const val MIN_SETS = 1
        private const val MAX_SETS = 20
        private const val MAX_NAME_LENGTH = 50

        /**
         * Creates a validated TimerConfiguration
         */
        fun create(
            name: String,
            warmupDuration: Duration,
            workDuration: Duration,
            restDuration: Duration,
            setBreakDuration: Duration,
            cooldownDuration: Duration,
            rounds: Int,
            sets: Int,
            audioSettings: AudioSettings
        ): ValidationResult<TimerConfiguration> {
            val validationErrors = mutableListOf<String>()

            // Validate name
            if (name.isBlank()) {
                validationErrors.add("Name cannot be blank")
            }
            if (name.length > MAX_NAME_LENGTH) {
                validationErrors.add("Name cannot exceed $MAX_NAME_LENGTH characters")
            }

            // Validate durations
            if (warmupDuration < MIN_DURATION || warmupDuration > MAX_DURATION) {
                validationErrors.add("Warmup duration must be between 0 and 1 hour")
            }
            if (workDuration < MIN_WORK_DURATION || workDuration > MAX_DURATION) {
                validationErrors.add("Work duration must be between 1 second and 1 hour")
            }
            if (restDuration < MIN_DURATION || restDuration > MAX_DURATION) {
                validationErrors.add("Rest duration must be between 0 and 1 hour")
            }
            if (setBreakDuration < MIN_DURATION || setBreakDuration > MAX_DURATION) {
                validationErrors.add("Set break duration must be between 0 and 1 hour")
            }
            if (cooldownDuration < MIN_DURATION || cooldownDuration > MAX_DURATION) {
                validationErrors.add("Cooldown duration must be between 0 and 1 hour")
            }

            // Validate counts
            if (rounds < MIN_ROUNDS || rounds > MAX_ROUNDS) {
                validationErrors.add("Rounds must be between $MIN_ROUNDS and $MAX_ROUNDS")
            }
            if (sets < MIN_SETS || sets > MAX_SETS) {
                validationErrors.add("Sets must be between $MIN_SETS and $MAX_SETS")
            }

            return if (validationErrors.isEmpty()) {
                ValidationResult.Success(
                    TimerConfiguration(
                        name = name.trim(),
                        warmupDuration = warmupDuration,
                        workDuration = workDuration,
                        restDuration = restDuration,
                        setBreakDuration = setBreakDuration,
                        cooldownDuration = cooldownDuration,
                        rounds = rounds,
                        sets = sets,
                        audioSettings = audioSettings
                    )
                )
            } else {
                ValidationResult.Error(validationErrors.joinToString("; "))
            }
        }

        /**
         * Creates a standard Tabata configuration (20s work / 10s rest × 8 rounds)
         */
        fun createTabata(
            name: String = "Tabata",
            warmupDuration: Duration = Duration.ZERO,
            cooldownDuration: Duration = Duration.ZERO,
            audioSettings: AudioSettings = AudioSettings()
        ): ValidationResult<TimerConfiguration> {
            return create(
                name = name,
                warmupDuration = warmupDuration,
                workDuration = 20.seconds,
                restDuration = 10.seconds,
                setBreakDuration = Duration.ZERO,
                cooldownDuration = cooldownDuration,
                rounds = 8,
                sets = 1,
                audioSettings = audioSettings
            )
        }
    }

    /**
     * Calculates the total duration of the workout
     */
    fun calculateTotalDuration(): Duration {
        val workoutDuration = (workDuration + restDuration) * rounds * sets
        val setBreaksDuration = if (sets > 1) setBreakDuration * (sets - 1) else Duration.ZERO
        return warmupDuration + workoutDuration + setBreaksDuration + cooldownDuration
    }

    /**
     * Validates the current configuration
     */
    fun validate(): ValidationResult<TimerConfiguration> {
        return create(
            name, warmupDuration, workDuration, restDuration,
            setBreakDuration, cooldownDuration, rounds, sets, audioSettings
        )
    }
}

/**
 * Current timer state
 */
data class TimerState(
    val status: TimerStatus,
    val currentPhase: PhaseType,
    val phaseTimeRemaining: Duration,
    val totalTimeRemaining: Duration,
    val currentRound: Int,
    val totalRounds: Int,
    val currentSet: Int,
    val totalSets: Int
) {
    companion object {
        /**
         * Creates an initial timer state from configuration
         */
        fun initial(configuration: TimerConfiguration): TimerState {
            val startingPhase = if (configuration.warmupDuration > Duration.ZERO) {
                PhaseType.WARMUP
            } else {
                PhaseType.WORK
            }
            
            val phaseTimeRemaining = when (startingPhase) {
                PhaseType.WARMUP -> configuration.warmupDuration
                PhaseType.WORK -> configuration.workDuration
                else -> Duration.ZERO
            }

            return TimerState(
                status = TimerStatus.IDLE,
                currentPhase = startingPhase,
                phaseTimeRemaining = phaseTimeRemaining,
                totalTimeRemaining = configuration.calculateTotalDuration(),
                currentRound = 1,
                totalRounds = configuration.rounds,
                currentSet = 1,
                totalSets = configuration.sets
            )
        }
    }

    /**
     * Checks if the timer is in a running state
     */
    fun isRunning(): Boolean = status == TimerStatus.RUNNING

    /**
     * Checks if the timer is paused
     */
    fun isPaused(): Boolean = status == TimerStatus.PAUSED

    /**
     * Checks if the timer is completed
     */
    fun isCompleted(): Boolean = status == TimerStatus.COMPLETED

    /**
     * Checks if the timer is idle
     */
    fun isIdle(): Boolean = status == TimerStatus.IDLE

    /**
     * Gets the progress percentage (0.0 to 1.0)
     */
    fun getProgress(totalDuration: Duration): Float {
        if (totalDuration <= Duration.ZERO) return 1.0f
        val elapsed = totalDuration - totalTimeRemaining
        return (elapsed / totalDuration).toFloat().coerceIn(0.0f, 1.0f)
    }
}

/**
 * Timer status enumeration
 */
enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

/**
 * Workout phase types
 */
enum class PhaseType(val displayName: String, val colorHex: String) {
    WARMUP("Warm Up", "#FFA726"),      // Orange
    WORK("Work", "#EF5350"),           // Red
    REST("Rest", "#42A5F5"),           // Blue
    SET_BREAK("Set Break", "#AB47BC"),  // Purple
    COOLDOWN("Cool Down", "#66BB6A");   // Green

    /**
     * Gets the next phase in the workout sequence
     */
    fun getNextPhase(
        currentRound: Int,
        totalRounds: Int,
        currentSet: Int,
        totalSets: Int,
        hasSetBreak: Boolean,
        hasCooldown: Boolean
    ): PhaseType? {
        return when (this) {
            WARMUP -> WORK
            WORK -> {
                if (currentRound < totalRounds) {
                    REST
                } else if (currentSet < totalSets && hasSetBreak) {
                    SET_BREAK
                } else if (hasCooldown) {
                    COOLDOWN
                } else {
                    null // Workout complete
                }
            }
            REST -> WORK
            SET_BREAK -> WORK
            COOLDOWN -> null // Workout complete
        }
    }

    /**
     * Checks if this phase requires a countdown
     */
    fun requiresCountdown(): Boolean = this != COOLDOWN
}

/**
 * Audio settings for timer sessions
 */
data class AudioSettings(
    val ttsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val language: String = "en-US",
    val volume: Float = 1.0f,
    val musicDuckingEnabled: Boolean = true
) {
    companion object {
        private const val MIN_VOLUME = 0.0f
        private const val MAX_VOLUME = 1.0f
        private val SUPPORTED_LANGUAGES = setOf(
            "en-US", "en-GB", "en", "es-ES", "es", "fr-FR", "fr", 
            "de-DE", "de", "it-IT", "it", "pt-BR", "pt", "ja-JP", "ja"
        )

        /**
         * Creates validated AudioSettings
         */
        fun create(
            ttsEnabled: Boolean = true,
            soundEffectsEnabled: Boolean = true,
            vibrationEnabled: Boolean = true,
            language: String = "en-US",
            volume: Float = 1.0f,
            musicDuckingEnabled: Boolean = true
        ): ValidationResult<AudioSettings> {
            val validationErrors = mutableListOf<String>()

            if (volume < MIN_VOLUME || volume > MAX_VOLUME) {
                validationErrors.add("Volume must be between $MIN_VOLUME and $MAX_VOLUME")
            }

            if (language.isBlank()) {
                validationErrors.add("Language cannot be blank")
            }

            return if (validationErrors.isEmpty()) {
                ValidationResult.Success(
                    AudioSettings(
                        ttsEnabled = ttsEnabled,
                        soundEffectsEnabled = soundEffectsEnabled,
                        vibrationEnabled = vibrationEnabled,
                        language = language,
                        volume = volume.coerceIn(MIN_VOLUME, MAX_VOLUME),
                        musicDuckingEnabled = musicDuckingEnabled
                    )
                )
            } else {
                ValidationResult.Error(validationErrors.joinToString("; "))
            }
        }
    }

    /**
     * Checks if the language is in the supported list
     */
    fun isLanguageSupported(): Boolean = language in SUPPORTED_LANGUAGES

    /**
     * Gets a fallback language if current language is not supported
     */
    fun getFallbackLanguage(): String {
        return when {
            language.startsWith("en") -> "en-US"
            language.startsWith("es") -> "es-ES"
            language.startsWith("fr") -> "fr-FR"
            language.startsWith("de") -> "de-DE"
            language.startsWith("it") -> "it-IT"
            language.startsWith("pt") -> "pt-BR"
            language.startsWith("ja") -> "ja-JP"
            else -> "en-US"
        }
    }

    /**
     * Validates the current audio settings
     */
    fun validate(): ValidationResult<AudioSettings> {
        return create(ttsEnabled, soundEffectsEnabled, vibrationEnabled, language, volume, musicDuckingEnabled)
    }
}

// ============================================================================
// SUBSCRIPTION AND FEATURE ACCESS MODELS
// ============================================================================

/**
 * Subscription status enumeration
 */
enum class SubscriptionStatus {
    FREE,           // Free user with no subscription
    TRIAL,          // Active trial period
    ACTIVE,         // Active paid subscription
    EXPIRED,        // Subscription has expired
    CANCELLED,      // Subscription cancelled but may still be active until period end
    GRACE_PERIOD,   // Payment failed but still in grace period
    PAUSED          // Subscription paused (supported on some platforms)
}

/**
 * Premium features available in the app
 */
enum class PremiumFeature(
    val displayName: String,
    val description: String,
    val freeLimit: Int? = null
) {
    UNLIMITED_TIMERS("Unlimited Timers", "Create unlimited custom timer configurations", freeLimit = 3),
    CUSTOM_SOUNDS("Custom Sounds", "Upload and use custom audio files for timer alerts"),
    ADVANCED_ANALYTICS("Advanced Analytics", "Detailed workout statistics and progress tracking"),
    CLOUD_SYNC("Cloud Sync", "Sync your data across all devices"),
    AD_FREE("Ad-Free Experience", "Remove all advertisements"),
    WEIGHT_TRACKING("Weight Tracking", "Track your weight and BMI over time"),
    CUSTOM_THEMES("Custom Themes", "Personalize the app with custom color themes");

    /**
     * Checks if this feature has a free usage limit
     */
    fun hasFreeLimit(): Boolean = freeLimit != null

    /**
     * Gets the free usage limit for this feature
     */
    fun getFreeLimit(): Int = freeLimit ?: 0
}

/**
 * Feature access status
 */
sealed class FeatureLimitStatus {
    object Available : FeatureLimitStatus()
    object RequiresSubscription : FeatureLimitStatus()
    object TrialExpired : FeatureLimitStatus()
    data class LimitReached(val limit: Int, val current: Int) : FeatureLimitStatus()
    data class TemporarilyUnavailable(val reason: String) : FeatureLimitStatus()

    /**
     * Checks if the feature is accessible
     */
    fun isAccessible(): Boolean = this is Available

    /**
     * Gets a user-friendly message for the status
     */
    fun getMessage(): String {
        return when (this) {
            is Available -> "Feature is available"
            is RequiresSubscription -> "This feature requires a premium subscription"
            is TrialExpired -> "Your trial has expired. Upgrade to continue using this feature"
            is LimitReached -> "You've reached the limit of $limit. Upgrade for unlimited access"
            is TemporarilyUnavailable -> reason
        }
    }
}

/**
 * Subscription information
 */
data class SubscriptionInfo(
    val status: SubscriptionStatus,
    val startDate: Instant?,
    val endDate: Instant?,
    val trialEndDate: Instant?,
    val productId: String?,
    val isAutoRenewing: Boolean = false,
    val lastVerified: Instant = Clock.System.now()
) {
    companion object {
        /**
         * Creates a free user subscription info
         */
        fun free(): SubscriptionInfo {
            return SubscriptionInfo(
                status = SubscriptionStatus.FREE,
                startDate = null,
                endDate = null,
                trialEndDate = null,
                productId = null,
                isAutoRenewing = false
            )
        }

        /**
         * Creates a trial subscription info
         */
        fun trial(trialEndDate: Instant): ValidationResult<SubscriptionInfo> {
            val now = Clock.System.now()
            if (trialEndDate <= now) {
                return ValidationResult.Error("Trial end date must be in the future")
            }

            return ValidationResult.Success(
                SubscriptionInfo(
                    status = SubscriptionStatus.TRIAL,
                    startDate = now,
                    endDate = null,
                    trialEndDate = trialEndDate,
                    productId = "trial",
                    isAutoRenewing = false
                )
            )
        }

        /**
         * Creates an active subscription info
         */
        fun active(
            startDate: Instant,
            endDate: Instant,
            productId: String,
            isAutoRenewing: Boolean = true
        ): ValidationResult<SubscriptionInfo> {
            val validationErrors = mutableListOf<String>()

            if (startDate >= endDate) {
                validationErrors.add("Start date must be before end date")
            }
            if (productId.isBlank()) {
                validationErrors.add("Product ID cannot be blank")
            }

            return if (validationErrors.isEmpty()) {
                ValidationResult.Success(
                    SubscriptionInfo(
                        status = SubscriptionStatus.ACTIVE,
                        startDate = startDate,
                        endDate = endDate,
                        trialEndDate = null,
                        productId = productId,
                        isAutoRenewing = isAutoRenewing
                    )
                )
            } else {
                ValidationResult.Error(validationErrors.joinToString("; "))
            }
        }
    }

    /**
     * Checks if the subscription is currently active
     */
    fun isActive(): Boolean {
        val now = Clock.System.now()
        return when (status) {
            SubscriptionStatus.ACTIVE -> endDate?.let { it > now } ?: false
            SubscriptionStatus.TRIAL -> trialEndDate?.let { it > now } ?: false
            SubscriptionStatus.GRACE_PERIOD -> endDate?.let { it > now } ?: false
            else -> false
        }
    }

    /**
     * Checks if the subscription is in trial period
     */
    fun isInTrial(): Boolean {
        return status == SubscriptionStatus.TRIAL && 
               trialEndDate?.let { it > Clock.System.now() } ?: false
    }

    /**
     * Gets the effective end date (trial end or subscription end)
     */
    fun getEffectiveEndDate(): Instant? {
        return when (status) {
            SubscriptionStatus.TRIAL -> trialEndDate
            SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD -> endDate
            else -> null
        }
    }

    /**
     * Gets the remaining time for the subscription
     */
    fun getRemainingTime(): Duration? {
        val effectiveEndDate = getEffectiveEndDate() ?: return null
        val now = Clock.System.now()
        return if (effectiveEndDate > now) {
            effectiveEndDate - now
        } else {
            Duration.ZERO
        }
    }

    /**
     * Checks if the subscription needs renewal soon (within 7 days)
     */
    fun needsRenewalSoon(): Boolean {
        val effectiveEndDate = getEffectiveEndDate() ?: return false
        val now = Clock.System.now()
        val sevenDaysFromNow = now + 7.days
        return effectiveEndDate <= sevenDaysFromNow && effectiveEndDate > now
    }

    /**
     * Updates the subscription status based on current time
     */
    fun updateStatus(): SubscriptionInfo {
        val now = Clock.System.now()
        val newStatus = when (status) {
            SubscriptionStatus.TRIAL -> {
                if (trialEndDate?.let { it <= now } == true) {
                    SubscriptionStatus.EXPIRED
                } else {
                    status
                }
            }
            SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD -> {
                if (endDate?.let { it <= now } == true) {
                    SubscriptionStatus.EXPIRED
                } else {
                    status
                }
            }
            else -> status
        }

        return if (newStatus != status) {
            copy(status = newStatus, lastVerified = now)
        } else {
            copy(lastVerified = now)
        }
    }

    /**
     * Validates the subscription info
     */
    fun validate(): ValidationResult<SubscriptionInfo> {
        val validationErrors = mutableListOf<String>()

        when (status) {
            SubscriptionStatus.TRIAL -> {
                if (trialEndDate == null) {
                    validationErrors.add("Trial subscription must have trial end date")
                }
            }
            SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD -> {
                if (startDate == null || endDate == null) {
                    validationErrors.add("Active subscription must have start and end dates")
                }
                if (productId.isNullOrBlank()) {
                    validationErrors.add("Active subscription must have product ID")
                }
                if (startDate != null && endDate != null && startDate >= endDate) {
                    validationErrors.add("Start date must be before end date")
                }
            }
            else -> {
                // FREE, EXPIRED, CANCELLED, PAUSED don't require additional validation
            }
        }

        return if (validationErrors.isEmpty()) {
            ValidationResult.Success(this)
        } else {
            ValidationResult.Error(validationErrors.joinToString("; "))
        }
    }
}

/**
 * Feature gate manager interface
 */
interface FeatureGateManager {
    /**
     * Checks if a feature is accessible for the current subscription
     */
    fun checkFeatureAccess(feature: PremiumFeature, currentUsage: Int = 0): FeatureLimitStatus

    /**
     * Gets the subscription info
     */
    fun getSubscriptionInfo(): SubscriptionInfo

    /**
     * Updates the subscription info
     */
    fun updateSubscriptionInfo(subscriptionInfo: SubscriptionInfo)

    /**
     * Checks if any premium features are available
     */
    fun hasPremiumAccess(): Boolean
}

/**
 * Basic implementation of FeatureGateManager
 */
class BasicFeatureGateManager(
    private var subscriptionInfo: SubscriptionInfo = SubscriptionInfo.free()
) : FeatureGateManager {

    override fun checkFeatureAccess(feature: PremiumFeature, currentUsage: Int): FeatureLimitStatus {
        // Update subscription status first
        subscriptionInfo = subscriptionInfo.updateStatus()

        // Check if user has premium access
        if (subscriptionInfo.isActive()) {
            return FeatureLimitStatus.Available
        }

        // Handle free users with limits
        if (feature.hasFreeLimit()) {
            val limit = feature.getFreeLimit()
            return if (currentUsage < limit) {
                FeatureLimitStatus.Available
            } else {
                FeatureLimitStatus.LimitReached(limit, currentUsage)
            }
        }

        // Feature requires subscription
        return when (subscriptionInfo.status) {
            SubscriptionStatus.FREE -> FeatureLimitStatus.RequiresSubscription
            SubscriptionStatus.EXPIRED -> {
                if (subscriptionInfo.trialEndDate != null) {
                    FeatureLimitStatus.TrialExpired
                } else {
                    FeatureLimitStatus.RequiresSubscription
                }
            }
            SubscriptionStatus.CANCELLED -> FeatureLimitStatus.RequiresSubscription
            SubscriptionStatus.PAUSED -> FeatureLimitStatus.TemporarilyUnavailable("Subscription is paused")
            else -> FeatureLimitStatus.RequiresSubscription
        }
    }

    override fun getSubscriptionInfo(): SubscriptionInfo {
        return subscriptionInfo.updateStatus()
    }

    override fun updateSubscriptionInfo(subscriptionInfo: SubscriptionInfo) {
        this.subscriptionInfo = subscriptionInfo
    }

    override fun hasPremiumAccess(): Boolean {
        return subscriptionInfo.updateStatus().isActive()
    }

    /**
     * Gets usage limits for free users
     */
    fun getUsageLimits(): Map<PremiumFeature, Int> {
        return PremiumFeature.values()
            .filter { it.hasFreeLimit() }
            .associateWith { it.getFreeLimit() }
    }

    /**
     * Checks if user can create more timers
     */
    fun canCreateTimer(currentTimerCount: Int): FeatureLimitStatus {
        return checkFeatureAccess(PremiumFeature.UNLIMITED_TIMERS, currentTimerCount)
    }

    /**
     * Gets the maximum number of timers allowed
     */
    fun getMaxTimersAllowed(): Int {
        return if (hasPremiumAccess()) {
            Int.MAX_VALUE
        } else {
            PremiumFeature.UNLIMITED_TIMERS.getFreeLimit()
        }
    }

    /**
     * Checks if ads should be shown
     */
    fun shouldShowAds(): Boolean {
        return checkFeatureAccess(PremiumFeature.AD_FREE) !is FeatureLimitStatus.Available
    }
}

// ============================================================================
// WEIGHT TRACKING DATA MODELS
// ============================================================================

/**
 * BMI categories based on WHO standards
 */
enum class BMICategory(
    val displayName: String,
    val description: String,
    val colorHex: String,
    val minBMI: Double,
    val maxBMI: Double?
) {
    UNDERWEIGHT("Underweight", "Below normal weight", "#3498DB", 0.0, 18.5),
    NORMAL("Normal Weight", "Healthy weight range", "#2ECC71", 18.5, 25.0),
    OVERWEIGHT("Overweight", "Above normal weight", "#F39C12", 25.0, 30.0),
    OBESE_CLASS_I("Obese Class I", "Moderately obese", "#E67E22", 30.0, 35.0),
    OBESE_CLASS_II("Obese Class II", "Severely obese", "#D35400", 35.0, 40.0),
    OBESE_CLASS_III("Obese Class III", "Very severely obese", "#C0392B", 40.0, null);

    companion object {
        /**
         * Gets BMI category from BMI value
         */
        fun fromBMI(bmi: Double): BMICategory {
            return values().find { category ->
                bmi >= category.minBMI && (category.maxBMI == null || bmi < category.maxBMI)
            } ?: OBESE_CLASS_III
        }

        /**
         * Gets all categories with their ranges
         */
        fun getAllRanges(): List<Pair<BMICategory, String>> {
            return values().map { category ->
                val range = if (category.maxBMI != null) {
                    "${category.minBMI} - ${category.maxBMI}"
                } else {
                    "${category.minBMI}+"
                }
                category to range
            }
        }
    }

    /**
     * Checks if this category is considered healthy
     */
    fun isHealthy(): Boolean = this == NORMAL

    /**
     * Gets health recommendation for this category
     */
    fun getHealthRecommendation(): String {
        return when (this) {
            UNDERWEIGHT -> "Consider consulting a healthcare provider about healthy weight gain strategies"
            NORMAL -> "Maintain your current healthy weight through balanced diet and regular exercise"
            OVERWEIGHT -> "Consider lifestyle changes to achieve a healthier weight"
            OBESE_CLASS_I -> "Consult a healthcare provider about weight management strategies"
            OBESE_CLASS_II -> "Seek medical advice for comprehensive weight management"
            OBESE_CLASS_III -> "Consult a healthcare provider immediately for medical weight management"
        }
    }
}

/**
 * Data source for weight measurements
 */
enum class WeightDataSource(val displayName: String) {
    MANUAL("Manual Entry"),
    SMART_SCALE("Smart Scale"),
    FITNESS_APP("Fitness App"),
    HEALTH_KIT("Health Kit"),
    IMPORTED("Imported Data")
}

/**
 * Weight entry record
 */
data class WeightEntry(
    val id: String,
    val weight: Double, // in kilograms
    val height: Double?, // in meters, optional as height doesn't change frequently
    val recordedAt: Instant,
    val notes: String? = null,
    val source: WeightDataSource = WeightDataSource.MANUAL,
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        private const val MIN_WEIGHT = 10.0 // kg
        private const val MAX_WEIGHT = 1000.0 // kg
        private const val MIN_HEIGHT = 0.5 // meters
        private const val MAX_HEIGHT = 3.0 // meters
        private const val MAX_NOTES_LENGTH = 500

        /**
         * Creates a validated WeightEntry
         */
        fun create(
            id: String,
            weight: Double,
            height: Double? = null,
            recordedAt: Instant,
            notes: String? = null,
            source: WeightDataSource = WeightDataSource.MANUAL
        ): ValidationResult<WeightEntry> {
            val validationErrors = mutableListOf<String>()

            // Validate ID
            if (id.isBlank()) {
                validationErrors.add("ID cannot be blank")
            }

            // Validate weight
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                validationErrors.add("Weight must be between $MIN_WEIGHT and $MAX_WEIGHT kg")
            }

            // Validate height if provided
            height?.let { h ->
                if (h < MIN_HEIGHT || h > MAX_HEIGHT) {
                    validationErrors.add("Height must be between $MIN_HEIGHT and $MAX_HEIGHT meters")
                }
            }

            // Validate recorded time
            val now = Clock.System.now()
            if (recordedAt > now) {
                validationErrors.add("Recorded time cannot be in the future")
            }

            // Validate notes length
            notes?.let { n ->
                if (n.length > MAX_NOTES_LENGTH) {
                    validationErrors.add("Notes cannot exceed $MAX_NOTES_LENGTH characters")
                }
            }

            return if (validationErrors.isEmpty()) {
                ValidationResult.Success(
                    WeightEntry(
                        id = id.trim(),
                        weight = weight,
                        height = height,
                        recordedAt = recordedAt,
                        notes = notes?.trim()?.takeIf { it.isNotBlank() },
                        source = source
                    )
                )
            } else {
                ValidationResult.Error(validationErrors.joinToString("; "))
            }
        }

        /**
         * Converts weight from pounds to kilograms
         */
        fun poundsToKg(pounds: Double): Double = pounds * 0.453592

        /**
         * Converts weight from kilograms to pounds
         */
        fun kgToPounds(kg: Double): Double = kg / 0.453592

        /**
         * Converts height from feet and inches to meters
         */
        fun feetInchesToMeters(feet: Int, inches: Double): Double {
            return (feet * 12 + inches) * 0.0254
        }

        /**
         * Converts height from meters to feet and inches
         */
        fun metersToFeetInches(meters: Double): Pair<Int, Double> {
            val totalInches = meters / 0.0254
            val feet = (totalInches / 12).toInt()
            val inches = totalInches % 12
            return feet to inches
        }
    }

    /**
     * Calculates BMI if height is available
     */
    fun calculateBMI(): Double? {
        return height?.let { h ->
            if (h > 0) weight / (h * h) else null
        }
    }

    /**
     * Gets BMI category if BMI can be calculated
     */
    fun getBMICategory(): BMICategory? {
        return calculateBMI()?.let { BMICategory.fromBMI(it) }
    }

    /**
     * Checks if this entry has complete data (weight and height)
     */
    fun isComplete(): Boolean = height != null

    /**
     * Gets weight in pounds
     */
    fun getWeightInPounds(): Double = kgToPounds(weight)

    /**
     * Gets height in feet and inches
     */
    fun getHeightInFeetInches(): Pair<Int, Double>? {
        return height?.let { metersToFeetInches(it) }
    }

    /**
     * Validates the current entry
     */
    fun validate(): ValidationResult<WeightEntry> {
        return create(id, weight, height, recordedAt, notes, source)
    }
}

/**
 * Weight statistics and trends
 */
data class WeightStats(
    val entries: List<WeightEntry>,
    val currentHeight: Double? = null // User's current height for BMI calculations
) {
    companion object {
        /**
         * Creates WeightStats from a list of entries
         */
        fun fromEntries(
            entries: List<WeightEntry>,
            currentHeight: Double? = null
        ): WeightStats {
            return WeightStats(
                entries = entries.sortedBy { it.recordedAt },
                currentHeight = currentHeight ?: entries.lastOrNull { it.height != null }?.height
            )
        }
    }

    /**
     * Gets the most recent weight entry
     */
    fun getCurrentWeight(): WeightEntry? = entries.maxByOrNull { it.recordedAt }

    /**
     * Gets the current BMI
     */
    fun getCurrentBMI(): Double? {
        val currentWeight = getCurrentWeight()?.weight ?: return null
        val height = currentHeight ?: return null
        return if (height > 0) currentWeight / (height * height) else null
    }

    /**
     * Gets the current BMI category
     */
    fun getCurrentBMICategory(): BMICategory? {
        return getCurrentBMI()?.let { BMICategory.fromBMI(it) }
    }

    /**
     * Gets weight change from the previous entry
     */
    fun getWeightChange(): Double? {
        if (entries.size < 2) return null
        val current = getCurrentWeight()?.weight ?: return null
        val previous = entries.dropLast(1).lastOrNull()?.weight ?: return null
        return current - previous
    }

    /**
     * Gets weight trend over time
     */
    fun getWeightTrend(days: Int = 30): WeightTrend {
        val cutoffDate = Clock.System.now() - days.days
        val recentEntries = entries.filter { it.recordedAt >= cutoffDate }
        
        if (recentEntries.size < 2) return WeightTrend.STABLE

        val firstWeight = recentEntries.first().weight
        val lastWeight = recentEntries.last().weight
        val change = lastWeight - firstWeight
        val changePercentage = (change / firstWeight) * 100

        return when {
            changePercentage > 2.0 -> WeightTrend.INCREASING
            changePercentage < -2.0 -> WeightTrend.DECREASING
            else -> WeightTrend.STABLE
        }
    }

    /**
     * Gets average weight over a period
     */
    fun getAverageWeight(days: Int = 30): Double? {
        val cutoffDate = Clock.System.now() - days.days
        val recentEntries = entries.filter { it.recordedAt >= cutoffDate }
        
        return if (recentEntries.isNotEmpty()) {
            recentEntries.map { it.weight }.average()
        } else null
    }

    /**
     * Gets the highest weight recorded
     */
    fun getHighestWeight(): WeightEntry? = entries.maxByOrNull { it.weight }

    /**
     * Gets the lowest weight recorded
     */
    fun getLowestWeight(): WeightEntry? = entries.minByOrNull { it.weight }

    /**
     * Gets weight entries within a date range
     */
    fun getEntriesInRange(startDate: Instant, endDate: Instant): List<WeightEntry> {
        return entries.filter { it.recordedAt >= startDate && it.recordedAt <= endDate }
    }

    /**
     * Gets BMI history for entries with height data
     */
    fun getBMIHistory(): List<Pair<Instant, Double>> {
        return entries.mapNotNull { entry ->
            entry.calculateBMI()?.let { bmi ->
                entry.recordedAt to bmi
            }
        }
    }

    /**
     * Calculates progress towards a target weight
     */
    fun getProgressToTarget(targetWeight: Double): WeightProgress? {
        val currentWeight = getCurrentWeight()?.weight ?: return null
        val startingWeight = entries.firstOrNull()?.weight ?: return null
        
        val totalChange = targetWeight - startingWeight
        val currentChange = currentWeight - startingWeight
        
        val progressPercentage = if (totalChange != 0.0) {
            (currentChange / totalChange * 100).coerceIn(0.0, 100.0)
        } else 0.0

        return WeightProgress(
            startingWeight = startingWeight,
            currentWeight = currentWeight,
            targetWeight = targetWeight,
            progressPercentage = progressPercentage,
            remainingWeight = targetWeight - currentWeight
        )
    }

    /**
     * Gets summary statistics
     */
    fun getSummary(): WeightSummary {
        val current = getCurrentWeight()
        val currentBMI = getCurrentBMI()
        val trend = getWeightTrend()
        val change = getWeightChange()
        
        return WeightSummary(
            totalEntries = entries.size,
            currentWeight = current?.weight,
            currentBMI = currentBMI,
            currentBMICategory = getCurrentBMICategory(),
            weightChange = change,
            trend = trend,
            averageWeight30Days = getAverageWeight(30),
            highestWeight = getHighestWeight()?.weight,
            lowestWeight = getLowestWeight()?.weight,
            firstRecordDate = entries.firstOrNull()?.recordedAt,
            lastRecordDate = entries.lastOrNull()?.recordedAt
        )
    }
}

/**
 * Weight trend enumeration
 */
enum class WeightTrend(val displayName: String, val colorHex: String) {
    INCREASING("Increasing", "#E74C3C"),
    DECREASING("Decreasing", "#2ECC71"),
    STABLE("Stable", "#95A5A6");

    /**
     * Gets trend description
     */
    fun getDescription(): String {
        return when (this) {
            INCREASING -> "Your weight has been trending upward"
            DECREASING -> "Your weight has been trending downward"
            STABLE -> "Your weight has been relatively stable"
        }
    }
}

/**
 * Weight progress towards a target
 */
data class WeightProgress(
    val startingWeight: Double,
    val currentWeight: Double,
    val targetWeight: Double,
    val progressPercentage: Double,
    val remainingWeight: Double
) {
    /**
     * Checks if the target has been reached
     */
    fun isTargetReached(): Boolean = remainingWeight.absoluteValue < 0.1 // Within 100g

    /**
     * Gets progress description
     */
    fun getProgressDescription(): String {
        return when {
            isTargetReached() -> "Target weight reached!"
            remainingWeight > 0 -> "Need to lose ${remainingWeight.absoluteValue.format(1)} kg"
            else -> "Need to gain ${remainingWeight.absoluteValue.format(1)} kg"
        }
    }
}

/**
 * Weight summary statistics
 */
data class WeightSummary(
    val totalEntries: Int,
    val currentWeight: Double?,
    val currentBMI: Double?,
    val currentBMICategory: BMICategory?,
    val weightChange: Double?,
    val trend: WeightTrend,
    val averageWeight30Days: Double?,
    val highestWeight: Double?,
    val lowestWeight: Double?,
    val firstRecordDate: Instant?,
    val lastRecordDate: Instant?
)

/**
 * Extension function to format double values
 */
private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}