package org.tabata.timber.core.audio

/**
 * Audio settings and preferences
 */
data class AudioSettings(
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val enableStartWorkSound: Boolean = true,
    val enableStartRestSound: Boolean = true,
    val enableCountdownSound: Boolean = true,
    val enableSessionEndSound: Boolean = true,
    val enableHalfwaySound: Boolean = false,
    val enableWarningSound: Boolean = true,
    val countdownStartSeconds: Int = 3,
    val warningBeforeSeconds: Int = 5,
    val soundProfile: SoundProfile = SoundProfile.DEFAULT
) {
    /**
     * Sound profiles for different workout environments
     */
    enum class SoundProfile {
        DEFAULT,    // Standard sounds for general use
        GYM,        // Louder, more aggressive sounds for gym environment
        HOME,       // Softer sounds for home workouts
        OUTDOOR,    // High-pitched sounds that cut through ambient noise
        SILENT      // Visual-only indicators
    }
    
    /**
     * Validates audio settings
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (volume < 0.0f || volume > 1.0f) {
            errors.add("Volume must be between 0.0 and 1.0")
        }
        
        if (countdownStartSeconds < 1 || countdownStartSeconds > 10) {
            errors.add("Countdown start seconds must be between 1 and 10")
        }
        
        if (warningBeforeSeconds < 1 || warningBeforeSeconds > 30) {
            errors.add("Warning before seconds must be between 1 and 30")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
}