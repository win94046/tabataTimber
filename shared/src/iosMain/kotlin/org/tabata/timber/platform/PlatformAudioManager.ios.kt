package org.tabata.timber.platform

import org.tabata.timber.core.audio.AudioManager
import org.tabata.timber.core.audio.LanguageSupport
import org.tabata.timber.core.audio.SoundType
import org.tabata.timber.core.audio.TTSQuality

/**
 * iOS implementation of AudioManager
 * Uses AVSpeechSynthesizer and AVAudioPlayer for audio functionality
 */
actual class PlatformAudioManager : AudioManager {
    
    override suspend fun playTTS(text: String, language: String) {
        // TODO: Implement iOS AVSpeechSynthesizer functionality
    }
    
    override suspend fun playSound(soundType: SoundType) {
        // TODO: Implement iOS AVAudioPlayer sound effects
    }
    
    override suspend fun enableMusicDucking() {
        // TODO: Implement iOS AVAudioSession ducking
        // Will use AVAudioSession.setCategory with .duckOthers option
    }
    
    override suspend fun disableMusicDucking() {
        // TODO: Implement iOS AVAudioSession normal playback
    }
    
    override fun setVolume(volume: Float) {
        // TODO: Implement iOS volume control
    }
    
    override suspend fun getSupportedLanguages(): List<String> {
        // TODO: Query iOS AVSpeechSynthesizer for supported languages
        return listOf("en-US", "en-GB", "es-ES", "fr-FR", "de-DE")
    }
    
    override suspend fun validateLanguageSupport(language: String): LanguageSupport {
        // TODO: Implement iOS TTS language validation
        val supportedLanguages = getSupportedLanguages()
        return if (language in supportedLanguages) {
            LanguageSupport(isSupported = true, quality = TTSQuality.HIGH)
        } else {
            LanguageSupport(
                isSupported = false,
                fallbackLanguage = "en-US",
                quality = TTSQuality.FALLBACK
            )
        }
    }
}