package org.tabata.timber.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles audio events and coordinates sound playback based on settings
 */
interface AudioEventHandler {
    
    /**
     * Current audio settings
     */
    val settings: StateFlow<AudioSettings>
    
    /**
     * Stream of audio events being processed
     */
    val audioEvents: Flow<AudioEvent>
    
    /**
     * Processes an audio event and triggers appropriate sound playback
     */
    suspend fun handleEvent(event: AudioEvent): Result<Unit>
    
    /**
     * Updates audio settings
     */
    suspend fun updateSettings(settings: AudioSettings): Result<Unit>
    
    /**
     * Enables/disables specific sound type
     */
    suspend fun setSoundEnabled(soundType: SoundType, enabled: Boolean): Result<Unit>
    
    /**
     * Gets current enabled status for a sound type
     */
    fun isSoundEnabled(soundType: SoundType): Boolean
    
    /**
     * Starts processing audio events
     */
    suspend fun start(): Result<Unit>
    
    /**
     * Stops processing audio events
     */
    suspend fun stop(): Result<Unit>
}

/**
 * Default implementation of AudioEventHandler
 */
class DefaultAudioEventHandler(
    private val audioManager: AudioManager,
    initialSettings: AudioSettings = AudioSettings()
) : AudioEventHandler {
    
    private val _settings = kotlinx.coroutines.flow.MutableStateFlow(initialSettings)
    override val settings: StateFlow<AudioSettings> = _settings
    
    private val _audioEvents = kotlinx.coroutines.flow.MutableSharedFlow<AudioEvent>()
    override val audioEvents: Flow<AudioEvent> = _audioEvents
    
    private var isProcessing = false
    
    override suspend fun handleEvent(event: AudioEvent): Result<Unit> {
        return try {
            _audioEvents.emit(event)
            
            if (!isProcessing || !audioManager.isAudioAvailable.value) {
                return Result.success(Unit)
            }
            
            val currentSettings = _settings.value
            if (currentSettings.isMuted || currentSettings.soundProfile == AudioSettings.SoundProfile.SILENT) {
                return Result.success(Unit)
            }
            
            val soundType = event.getSoundType() ?: return Result.success(Unit)
            
            if (!shouldPlaySound(soundType, currentSettings)) {
                return Result.success(Unit)
            }
            
            audioManager.playSound(soundType)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to handle audio event: ${e.message}", e))
        }
    }
    
    override suspend fun updateSettings(settings: AudioSettings): Result<Unit> {
        return when (val validation = settings.validate()) {
            is AudioSettings.ValidationResult.Invalid -> {
                Result.failure(IllegalArgumentException("Invalid audio settings: ${validation.errors.joinToString()}"))
            }
            is AudioSettings.ValidationResult.Valid -> {
                _settings.value = settings
                audioManager.setVolume(settings.volume)
                if (settings.isMuted) {
                    audioManager.toggleMute()
                }
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun setSoundEnabled(soundType: SoundType, enabled: Boolean): Result<Unit> {
        val currentSettings = _settings.value
        val updatedSettings = when (soundType) {
            SoundType.START_WORK -> currentSettings.copy(enableStartWorkSound = enabled)
            SoundType.START_REST -> currentSettings.copy(enableStartRestSound = enabled)
            SoundType.COUNTDOWN -> currentSettings.copy(enableCountdownSound = enabled)
            SoundType.SESSION_END -> currentSettings.copy(enableSessionEndSound = enabled)
            SoundType.HALFWAY -> currentSettings.copy(enableHalfwaySound = enabled)
            SoundType.WARNING -> currentSettings.copy(enableWarningSound = enabled)
        }
        
        return updateSettings(updatedSettings)
    }
    
    override fun isSoundEnabled(soundType: SoundType): Boolean {
        val settings = _settings.value
        return when (soundType) {
            SoundType.START_WORK -> settings.enableStartWorkSound
            SoundType.START_REST -> settings.enableStartRestSound
            SoundType.COUNTDOWN -> settings.enableCountdownSound
            SoundType.SESSION_END -> settings.enableSessionEndSound
            SoundType.HALFWAY -> settings.enableHalfwaySound
            SoundType.WARNING -> settings.enableWarningSound
        }
    }
    
    override suspend fun start(): Result<Unit> {
        return try {
            audioManager.initialize().getOrThrow()
            audioManager.preloadSounds().getOrThrow()
            isProcessing = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to start audio event handler: ${e.message}", e))
        }
    }
    
    override suspend fun stop(): Result<Unit> {
        return try {
            isProcessing = false
            audioManager.stopSound()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to stop audio event handler: ${e.message}", e))
        }
    }
    
    private fun shouldPlaySound(soundType: SoundType, settings: AudioSettings): Boolean {
        return when (soundType) {
            SoundType.START_WORK -> settings.enableStartWorkSound
            SoundType.START_REST -> settings.enableStartRestSound
            SoundType.COUNTDOWN -> settings.enableCountdownSound
            SoundType.SESSION_END -> settings.enableSessionEndSound
            SoundType.HALFWAY -> settings.enableHalfwaySound
            SoundType.WARNING -> settings.enableWarningSound
        }
    }
}