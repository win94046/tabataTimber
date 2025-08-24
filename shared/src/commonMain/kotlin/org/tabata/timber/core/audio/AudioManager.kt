package org.tabata.timber.core.audio

/**
 * Audio management interface for TTS, sound effects, and music ducking.
 * Platform-specific implementations handle iOS and Android audio systems.
 */
interface AudioManager {
    /**
     * Play text-to-speech with specified language
     * @param text Text to speak
     * @param language Language code (e.g., "en-US")
     */
    suspend fun playTTS(text: String, language: String)
    
    /**
     * Play a sound effect
     * @param soundType Type of sound to play
     */
    suspend fun playSound(soundType: SoundType)
    
    /**
     * Enable music ducking (lower music volume during audio prompts)
     */
    suspend fun enableMusicDucking()
    
    /**
     * Disable music ducking (restore normal music volume)
     */
    suspend fun disableMusicDucking()
    
    /**
     * Set audio volume
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float)
    
    /**
     * Get list of supported TTS languages
     */
    suspend fun getSupportedLanguages(): List<String>
    
    /**
     * Validate if a language is supported and get fallback if needed
     * @param language Language code to validate
     */
    suspend fun validateLanguageSupport(language: String): LanguageSupport
}

/**
 * Types of sound effects available
 */
enum class SoundType {
    PHASE_START,
    PHASE_END,
    COUNTDOWN_3,
    COUNTDOWN_1,
    HALFWAY_POINT
}

/**
 * Language support information
 */
data class LanguageSupport(
    val isSupported: Boolean,
    val fallbackLanguage: String? = null,
    val quality: TTSQuality
)

/**
 * TTS quality levels
 */
enum class TTSQuality {
    HIGH,
    MEDIUM,
    LOW,
    FALLBACK
}