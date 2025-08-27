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
     * Plays a specific sound type
     */
    suspend fun playSound(soundType: SoundType): Result<Unit>
    
    /**
     * Stops any currently playing sound
     */
    suspend fun stopSound(): Result<Unit>
    
    /**
     * Sets the volume level (0.0 to 1.0)
     */
    suspend fun setVolume(volume: Float): Result<Unit>
    
    /**
     * Toggles mute state
     */
    suspend fun toggleMute(): Result<Unit>
    
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
}