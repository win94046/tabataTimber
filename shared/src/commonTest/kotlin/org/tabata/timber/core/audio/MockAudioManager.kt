package org.tabata.timber.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mock implementation of AudioManager for testing purposes
 */
class MockAudioManager : AudioManager {
    
    private val _volume = MutableStateFlow(1.0f)
    private val _isMuted = MutableStateFlow(false)
    private val _isAudioAvailable = MutableStateFlow(true)
    private val _playbackState = MutableStateFlow(AudioPlaybackState.IDLE)
    private val _audioSettings = MutableStateFlow(AudioSettings())
    private val _audioResources = MutableStateFlow<List<AudioResource>>(emptyList())
    
    override val volume: StateFlow<Float> = _volume
    override val isMuted: StateFlow<Boolean> = _isMuted
    override val isAudioAvailable: StateFlow<Boolean> = _isAudioAvailable
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState
    override val audioSettings: StateFlow<AudioSettings> = _audioSettings
    override val audioResources: StateFlow<List<AudioResource>> = _audioResources
    
    // Test tracking properties
    val playedSounds = mutableListOf<Pair<SoundType, Int>>() // soundType to priority
    val playedSoundsWithVolume = mutableListOf<Triple<SoundType, Float, Int>>() // soundType to volume to priority
    var initializeCalled = false
    var releaseCalled = false
    var preloadSoundsCalled = false
    var stopSoundCalled = false
    var testAudioCalled = false
    
    // Test control properties
    var shouldFailPlaySound = false
    var shouldFailInitialize = false
    var shouldFailPreload = false
    
    override suspend fun playSound(soundType: SoundType, priority: Int): Result<Unit> {
        return if (shouldFailPlaySound) {
            Result.failure(RuntimeException("Mock play sound failure"))
        } else {
            playedSounds.add(Pair(soundType, priority))
            _playbackState.value = AudioPlaybackState.PLAYING
            Result.success(Unit)
        }
    }
    
    override suspend fun playSound(soundType: SoundType, volume: Float, priority: Int): Result<Unit> {
        return if (shouldFailPlaySound) {
            Result.failure(RuntimeException("Mock play sound failure"))
        } else {
            playedSoundsWithVolume.add(Triple(soundType, volume, priority))
            _playbackState.value = AudioPlaybackState.PLAYING
            Result.success(Unit)
        }
    }
    
    override suspend fun stopSound(): Result<Unit> {
        stopSoundCalled = true
        _playbackState.value = AudioPlaybackState.IDLE
        return Result.success(Unit)
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        _volume.value = volume.coerceIn(0.0f, 1.0f)
        return Result.success(Unit)
    }
    
    override suspend fun setMuted(muted: Boolean): Result<Unit> {
        _isMuted.value = muted
        return Result.success(Unit)
    }
    
    override suspend fun toggleMute(): Result<Unit> {
        _isMuted.value = !_isMuted.value
        return Result.success(Unit)
    }
    
    override suspend fun updateSettings(settings: AudioSettings): Result<Unit> {
        return when (val validation = settings.validate()) {
            is AudioSettings.ValidationResult.Invalid -> {
                Result.failure(IllegalArgumentException("Invalid settings: ${validation.errors.joinToString()}"))
            }
            is AudioSettings.ValidationResult.Valid -> {
                _audioSettings.value = settings
                _volume.value = settings.volume
                _isMuted.value = settings.isMuted
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun initialize(): Result<Unit> {
        initializeCalled = true
        return if (shouldFailInitialize) {
            _isAudioAvailable.value = false
            Result.failure(RuntimeException("Mock initialization failure"))
        } else {
            _isAudioAvailable.value = true
            _playbackState.value = AudioPlaybackState.IDLE
            Result.success(Unit)
        }
    }
    
    override suspend fun release(): Result<Unit> {
        releaseCalled = true
        _isAudioAvailable.value = false
        _playbackState.value = AudioPlaybackState.IDLE
        playedSounds.clear()
        playedSoundsWithVolume.clear()
        return Result.success(Unit)
    }
    
    override suspend fun preloadSounds(): Result<Unit> {
        preloadSoundsCalled = true
        return if (shouldFailPreload) {
            Result.failure(RuntimeException("Mock preload failure"))
        } else {
            val resources = SoundType.values().map { soundType ->
                AudioResource(
                    soundType = soundType,
                    fileName = "${soundType.name.lowercase()}.wav",
                    duration = 1000,
                    isLoaded = true
                )
            }
            _audioResources.value = resources
            Result.success(Unit)
        }
    }
    
    override suspend fun preloadSound(soundType: SoundType): Result<Unit> {
        return if (shouldFailPreload) {
            Result.failure(RuntimeException("Mock preload failure for $soundType"))
        } else {
            val currentResources = _audioResources.value.toMutableList()
            val existingIndex = currentResources.indexOfFirst { it.soundType == soundType }
            
            val resource = AudioResource(
                soundType = soundType,
                fileName = "${soundType.name.lowercase()}.wav",
                duration = 1000,
                isLoaded = true
            )
            
            if (existingIndex >= 0) {
                currentResources[existingIndex] = resource
            } else {
                currentResources.add(resource)
            }
            
            _audioResources.value = currentResources
            Result.success(Unit)
        }
    }
    
    override fun isSoundReady(soundType: SoundType): Boolean {
        return _audioResources.value.any { it.soundType == soundType && it.isLoaded }
    }
    
    override fun getAudioResource(soundType: SoundType): AudioResource? {
        return _audioResources.value.find { it.soundType == soundType }
    }
    
    override suspend fun testAudio(): Result<Unit> {
        testAudioCalled = true
        return playSound(SoundType.COUNTDOWN, 0.5f, 100)
    }
    
    // Test helper methods
    fun reset() {
        playedSounds.clear()
        playedSoundsWithVolume.clear()
        initializeCalled = false
        releaseCalled = false
        preloadSoundsCalled = false
        stopSoundCalled = false
        testAudioCalled = false
        shouldFailPlaySound = false
        shouldFailInitialize = false
        shouldFailPreload = false
        
        _volume.value = 1.0f
        _isMuted.value = false
        _isAudioAvailable.value = true
        _playbackState.value = AudioPlaybackState.IDLE
        _audioSettings.value = AudioSettings()
        _audioResources.value = emptyList()
    }
    
    fun simulateUnavailable() {
        _isAudioAvailable.value = false
        _playbackState.value = AudioPlaybackState.ERROR
    }
    
    fun simulateLoading() {
        _playbackState.value = AudioPlaybackState.LOADING
    }
}