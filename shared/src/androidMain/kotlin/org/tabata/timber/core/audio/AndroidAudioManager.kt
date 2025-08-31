package org.tabata.timber.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager as AndroidSystemAudioManager
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
// Resources will be referenced by resource identifier string

/**
 * Android implementation of AudioManager using SoundPool for better performance
 * Optimized for short sound clips with low latency playback
 */
class AndroidAudioManager(
    private val context: Context
) : AudioManager {
    
    private val _volume = MutableStateFlow(1.0f)
    private val _isMuted = MutableStateFlow(false)
    private val _isAudioAvailable = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(AudioPlaybackState.IDLE)
    private val _audioSettings = MutableStateFlow(AudioSettings())
    private val _audioResources = MutableStateFlow<List<AudioResource>>(emptyList())
    
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    override val isAudioAvailable: StateFlow<Boolean> = _isAudioAvailable.asStateFlow()
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()
    override val audioSettings: StateFlow<AudioSettings> = _audioSettings.asStateFlow()
    override val audioResources: StateFlow<List<AudioResource>> = _audioResources.asStateFlow()
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private val loadedSounds = mutableSetOf<SoundType>()
    private var currentStreamId: Int? = null
    private var currentPriority = 0
    
    private val soundResourceMap = mapOf(
        SoundType.START_WORK to "start_work.wav",
        SoundType.START_REST to "start_rest.wav",
        SoundType.COUNTDOWN to "countdown.wav",
        SoundType.SESSION_END to "session_end.wav",
        SoundType.HALFWAY to "halfway.wav",
        SoundType.WARNING to "warning.wav"
    )
    
    override suspend fun playSound(soundType: SoundType, priority: Int): Result<Unit> {
        return playSound(soundType, _volume.value, priority)
    }
    
    override suspend fun playSound(soundType: SoundType, volume: Float, priority: Int): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                val pool = soundPool ?: return@withContext Result.failure(
                    IllegalStateException("SoundPool not initialized")
                )
                
                if (_isMuted.value || _audioSettings.value.soundProfile == AudioSettings.SoundProfile.SILENT) {
                    return@withContext Result.success(Unit)
                }
                
                val soundId = soundMap[soundType] ?: return@withContext Result.failure(
                    IllegalArgumentException("Sound not loaded: $soundType")
                )
                
                // Check if we should interrupt current sound
                if (currentStreamId != null && priority <= currentPriority) {
                    return@withContext Result.success(Unit) // Lower priority, don't interrupt
                }
                
                // Stop current sound if playing
                currentStreamId?.let { pool.stop(it) }
                
                _playbackState.value = AudioPlaybackState.PLAYING
                val adjustedVolume = (volume * _volume.value).coerceIn(0.0f, 1.0f)
                
                val streamId = pool.play(
                    soundId,
                    adjustedVolume,
                    adjustedVolume,
                    priority,
                    0,
                    1.0f
                )
                
                if (streamId == 0) {
                    _playbackState.value = AudioPlaybackState.ERROR
                    return@withContext Result.failure(
                        RuntimeException("Failed to play sound: $soundType")
                    )
                }
                
                currentStreamId = streamId
                currentPriority = priority
                
                Result.success(Unit)
            } catch (e: Exception) {
                _playbackState.value = AudioPlaybackState.ERROR
                Result.failure(e)
            }
        }
    }
    
    override suspend fun stopSound(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                currentStreamId?.let { streamId ->
                    soundPool?.stop(streamId)
                    currentStreamId = null
                    currentPriority = 0
                }
                _playbackState.value = AudioPlaybackState.IDLE
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        _volume.value = volume.coerceIn(0.0f, 1.0f)
        return Result.success(Unit)
    }
    
    override suspend fun setMuted(muted: Boolean): Result<Unit> {
        _isMuted.value = muted
        if (muted) {
            stopSound()
        }
        return Result.success(Unit)
    }
    
    override suspend fun toggleMute(): Result<Unit> {
        return setMuted(!_isMuted.value)
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
        return withContext(Dispatchers.IO) {
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                soundPool = SoundPool.Builder()
                    .setMaxStreams(3)
                    .setAudioAttributes(audioAttributes)
                    .build()
                
                soundPool?.setOnLoadCompleteListener { _, soundId, status ->
                    if (status == 0) { // Success
                        val soundType = soundMap.entries.find { it.value == soundId }?.key
                        soundType?.let { loadedSounds.add(it) }
                        
                        if (loadedSounds.size == soundResourceMap.size) {
                            _playbackState.value = AudioPlaybackState.IDLE
                        }
                    }
                }
                
                _isAudioAvailable.value = true
                _playbackState.value = AudioPlaybackState.LOADING
                
                Result.success(Unit)
            } catch (e: Exception) {
                _isAudioAvailable.value = false
                _playbackState.value = AudioPlaybackState.ERROR
                Result.failure(e)
            }
        }
    }
    
    override suspend fun release(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                soundPool?.release()
                soundPool = null
                soundMap.clear()
                loadedSounds.clear()
                currentStreamId = null
                currentPriority = 0
                
                _isAudioAvailable.value = false
                _playbackState.value = AudioPlaybackState.IDLE
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun preloadSounds(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pool = soundPool ?: return@withContext Result.failure(
                    IllegalStateException("SoundPool not initialized")
                )
                
                val resources = mutableListOf<AudioResource>()
                
                soundResourceMap.forEach { (soundType, fileName) ->
                    // For now, we'll use a placeholder implementation
                    // In a real implementation, you'd load from assets or raw resources
                    // val soundId = pool.load(context, getResourceId(fileName), 1)
                    // soundMap[soundType] = soundId
                    
                    resources.add(
                        AudioResource(
                            soundType = soundType,
                            fileName = fileName,
                            isLoaded = true // Placeholder - mark as loaded
                        )
                    )
                }
                
                _audioResources.value = resources
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun preloadSound(soundType: SoundType): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pool = soundPool ?: return@withContext Result.failure(
                    IllegalStateException("SoundPool not initialized")
                )
                
                val fileName = soundResourceMap[soundType] ?: return@withContext Result.failure(
                    IllegalArgumentException("Unknown sound type: $soundType")
                )
                
                // Placeholder implementation
                // val soundId = pool.load(context, getResourceId(fileName), 1)
                // soundMap[soundType] = soundId
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun isSoundReady(soundType: SoundType): Boolean {
        return loadedSounds.contains(soundType)
    }
    
    override fun getAudioResource(soundType: SoundType): AudioResource? {
        return _audioResources.value.find { it.soundType == soundType }
    }
    
    override suspend fun testAudio(): Result<Unit> {
        return playSound(SoundType.COUNTDOWN, 0.5f, 100)
    }
}