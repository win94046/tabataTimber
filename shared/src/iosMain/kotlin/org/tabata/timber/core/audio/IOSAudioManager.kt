package org.tabata.timber.core.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.AVFAudio.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of AudioManager using AVAudioPlayer
 * Optimized for iOS audio session management and background playback
 */
class IOSAudioManager : AudioManager {
    
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
    
    private var audioSession: AVAudioSession? = null
    private var currentPlayer: AVAudioPlayer? = null
    private val audioPlayers = mutableMapOf<SoundType, AVAudioPlayer>()
    private var currentPriority = 0
    
    private val soundFileMap = mapOf(
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
                if (_isMuted.value || _audioSettings.value.soundProfile == AudioSettings.SoundProfile.SILENT) {
                    return@withContext Result.success(Unit)
                }
                
                val player = audioPlayers[soundType] ?: return@withContext Result.failure(
                    IllegalArgumentException("Audio player not loaded for sound type: $soundType")
                )
                
                // Check priority
                if (currentPlayer?.isPlaying == true && priority <= currentPriority) {
                    return@withContext Result.success(Unit)
                }
                
                // Stop current player if needed
                currentPlayer?.stop()
                
                _playbackState.value = AudioPlaybackState.PLAYING
                val adjustedVolume = (volume * _volume.value).coerceIn(0.0f, 1.0f)
                player.volume = adjustedVolume
                
                val success = player.play()
                
                if (success) {
                    currentPlayer = player
                    currentPriority = priority
                    Result.success(Unit)
                } else {
                    _playbackState.value = AudioPlaybackState.ERROR
                    Result.failure(RuntimeException("Failed to play sound: $soundType"))
                }
                
            } catch (e: Exception) {
                _playbackState.value = AudioPlaybackState.ERROR
                Result.failure(e)
            }
        }
    }
    
    override suspend fun stopSound(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                currentPlayer?.stop()
                currentPlayer = null
                currentPriority = 0
                _playbackState.value = AudioPlaybackState.IDLE
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun setVolume(volume: Float): Result<Unit> {
        _volume.value = volume.coerceIn(0.0f, 1.0f)
        audioPlayers.values.forEach { player ->
            player.volume = _volume.value
        }
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
                
                // Update all players volume
                audioPlayers.values.forEach { player ->
                    player.volume = _volume.value
                }
                
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun initialize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Setup audio session
                audioSession = AVAudioSession.sharedInstance()
                audioSession?.let { session ->
                    var error: NSError? = null
                    session.setCategory(AVAudioSessionCategoryPlayback, error = &error)
                    if (error != null) {
                        return@withContext Result.failure(Exception("Failed to set audio session category: ${error.localizedDescription}"))
                    }
                    
                    session.setActive(true, error = &error)
                    if (error != null) {
                        return@withContext Result.failure(Exception("Failed to activate audio session: ${error.localizedDescription}"))
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
                // Stop and release all players
                audioPlayers.values.forEach { player ->
                    player.stop()
                }
                audioPlayers.clear()
                currentPlayer = null
                currentPriority = 0
                
                // Deactivate audio session
                audioSession?.let { session ->
                    var error: NSError? = null
                    session.setActive(false, error = &error)
                }
                audioSession = null
                
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
                val resources = mutableListOf<AudioResource>()
                var loadSuccessCount = 0
                
                soundFileMap.forEach { (soundType, fileName) ->
                    val bundle = NSBundle.mainBundle
                    val soundPath = bundle.pathForResource(fileName.substringBefore("."), ofType = fileName.substringAfter("."))
                    
                    if (soundPath != null) {
                        val soundUrl = NSURL.fileURLWithPath(soundPath)
                        var error: NSError? = null
                        
                        val player = AVAudioPlayer(contentsOfURL = soundUrl, error = &error)
                        
                        if (error == null && player != null) {
                            player.prepareToPlay()
                            player.volume = _volume.value
                            audioPlayers[soundType] = player
                            loadSuccessCount++
                            
                            resources.add(
                                AudioResource(
                                    soundType = soundType,
                                    fileName = fileName,
                                    duration = (player.duration * 1000).toLong(),
                                    isLoaded = true
                                )
                            )
                        } else {
                            resources.add(
                                AudioResource(
                                    soundType = soundType,
                                    fileName = fileName,
                                    isLoaded = false
                                )
                            )
                        }
                    }
                }
                
                _audioResources.value = resources
                
                if (loadSuccessCount == soundFileMap.size) {
                    _playbackState.value = AudioPlaybackState.IDLE
                    Result.success(Unit)
                } else {
                    _playbackState.value = AudioPlaybackState.ERROR
                    Result.failure(Exception("Failed to load $loadSuccessCount/${soundFileMap.size} sounds"))
                }
            } catch (e: Exception) {
                _playbackState.value = AudioPlaybackState.ERROR
                Result.failure(e)
            }
        }
    }
    
    override suspend fun preloadSound(soundType: SoundType): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = soundFileMap[soundType] ?: return@withContext Result.failure(
                    IllegalArgumentException("Unknown sound type: $soundType")
                )
                
                val bundle = NSBundle.mainBundle
                val soundPath = bundle.pathForResource(fileName.substringBefore("."), ofType = fileName.substringAfter("."))
                
                if (soundPath != null) {
                    val soundUrl = NSURL.fileURLWithPath(soundPath)
                    var error: NSError? = null
                    
                    val player = AVAudioPlayer(contentsOfURL = soundUrl, error = &error)
                    
                    if (error == null && player != null) {
                        player.prepareToPlay()
                        player.volume = _volume.value
                        audioPlayers[soundType] = player
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to load sound $soundType: ${error?.localizedDescription}"))
                    }
                } else {
                    Result.failure(Exception("Sound file not found: $fileName"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun isSoundReady(soundType: SoundType): Boolean {
        return audioPlayers[soundType] != null
    }
    
    override fun getAudioResource(soundType: SoundType): AudioResource? {
        return _audioResources.value.find { it.soundType == soundType }
    }
    
    override suspend fun testAudio(): Result<Unit> {
        return playSound(SoundType.COUNTDOWN, 0.5f, 100)
    }
}