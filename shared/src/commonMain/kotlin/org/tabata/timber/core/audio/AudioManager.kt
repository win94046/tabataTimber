package org.tabata.timber.core.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Sound types for different timer events
 */
enum class SoundType {
    START_WORK,     // Sound when work period starts
    START_REST,     // Sound when rest period starts
    COUNTDOWN,      // Countdown beep sound (3-2-1)
    SESSION_END,    // Sound when entire session completes
    HALFWAY,        // Sound at halfway point of a period
    WARNING         // Warning sound before period transition
}

/**
 * Audio playback state
 */
enum class AudioPlaybackState {
    IDLE,           // No sound playing
    PLAYING,        // Currently playing sound
    LOADING,        // Loading/preparing sound
    ERROR           // Error state
}

/**
 * Audio resource information
 */
data class AudioResource(
    val soundType: SoundType,
    val fileName: String,
    val duration: Long = 0,
    val isLoaded: Boolean = false
)

/**
 * Core interface for managing audio playback during Tabata sessions
 * This requires platform-specific implementations
 */
interface AudioManager {
    
    /**
     * Current volume level (0.0 to 1.0)
     */
    val volume: StateFlow<Float>
    
    /**
     * Whether audio is currently muted
     */
    val isMuted: StateFlow<Boolean>
    
    /**
     * Whether audio is currently available/initialized
     */
    val isAudioAvailable: StateFlow<Boolean>
    
    /**
     * Current audio playback state
     */
    val playbackState: StateFlow<AudioPlaybackState>
    
    /**
     * Current audio settings
     */
    val audioSettings: StateFlow<AudioSettings>
    
    /**
     * List of available audio resources
     */
    val audioResources: StateFlow<List<AudioResource>>
    
    /**
     * Plays a specific sound type
     * @param soundType The type of sound to play
     * @param priority Higher priority sounds can interrupt lower priority ones
     */
    suspend fun playSound(soundType: SoundType, priority: Int = 0): Result<Unit>
    
    /**
     * Plays a sound with custom volume override
     */
    suspend fun playSound(soundType: SoundType, volume: Float, priority: Int = 0): Result<Unit>
    
    /**
     * Stops any currently playing sound
     */
    suspend fun stopSound(): Result<Unit>
    
    /**
     * Sets the volume level (0.0 to 1.0)
     */
    suspend fun setVolume(volume: Float): Result<Unit>
    
    /**
     * Sets mute state explicitly
     */
    suspend fun setMuted(muted: Boolean): Result<Unit>
    
    /**
     * Toggles mute state
     */
    suspend fun toggleMute(): Result<Unit>
    
    /**
     * Updates audio settings
     */
    suspend fun updateSettings(settings: AudioSettings): Result<Unit>
    
    /**
     * Initializes the audio system
     * Should be called before using any audio functions
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Releases audio resources
     * Should be called when no longer needed
     */
    suspend fun release(): Result<Unit>
    
    /**
     * Preloads all sound files for better performance
     */
    suspend fun preloadSounds(): Result<Unit>
    
    /**
     * Preloads a specific sound type
     */
    suspend fun preloadSound(soundType: SoundType): Result<Unit>
    
    /**
     * Checks if a sound is ready to play
     */
    fun isSoundReady(soundType: SoundType): Boolean
    
    /**
     * Gets audio resource information for a sound type
     */
    fun getAudioResource(soundType: SoundType): AudioResource?
    
    /**
     * Tests audio system by playing a test sound
     */
    suspend fun testAudio(): Result<Unit>
}