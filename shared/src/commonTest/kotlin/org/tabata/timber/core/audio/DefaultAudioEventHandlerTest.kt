package org.tabata.timber.core.audio

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.tabata.timber.domain.models.timer.PhaseType

class DefaultAudioEventHandlerTest {
    
    @Test
    fun testInitialState() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val settings = handler.settings.first()
        assertEquals(AudioSettings(), settings)
        assertTrue(handler.isSoundEnabled(SoundType.START_WORK))
        assertTrue(handler.isSoundEnabled(SoundType.START_REST))
        assertTrue(handler.isSoundEnabled(SoundType.COUNTDOWN))
        assertTrue(handler.isSoundEnabled(SoundType.SESSION_END))
        assertFalse(handler.isSoundEnabled(SoundType.HALFWAY))
        assertTrue(handler.isSoundEnabled(SoundType.WARNING))
    }
    
    @Test
    fun testStartAndStop() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val result = handler.start()
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.initializeCalled)
        assertTrue(mockAudioManager.preloadSoundsCalled)
        
        val stopResult = handler.stop()
        assertTrue(stopResult.isSuccess)
    }
    
    @Test
    fun testStartFailure() = runTest {
        val mockAudioManager = MockAudioManager()
        mockAudioManager.shouldFailInitialize = true
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val result = handler.start()
        assertTrue(result.isFailure)
    }
    
    @Test
    fun testHandlePhaseStartedEvent() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        handler.start()
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.START_WORK })
    }
    
    @Test
    fun testHandleEventWhenMuted() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        handler.start()
        
        // Mute the audio
        val mutedSettings = AudioSettings(isMuted = true)
        handler.updateSettings(mutedSettings)
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound should be played when muted
    }
    
    @Test
    fun testHandleEventWhenSilentProfile() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        handler.start()
        
        // Set silent profile
        val silentSettings = AudioSettings(soundProfile = AudioSettings.SoundProfile.SILENT)
        handler.updateSettings(silentSettings)
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound should be played in silent profile
    }
    
    @Test
    fun testHandleEventWhenSoundDisabled() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        handler.start()
        
        // Disable start work sound
        handler.setSoundEnabled(SoundType.START_WORK, false)
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound should be played when disabled
    }
    
    @Test
    fun testHandleEventWithNoSoundType() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        handler.start()
        
        val event = AudioEvent.AudioError("Test error")
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound for error events
    }
    
    @Test
    fun testHandleEventWhenNotProcessing() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        // Don't start the handler
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound when not processing
    }
    
    @Test
    fun testHandleEventWhenAudioUnavailable() = runTest {
        val mockAudioManager = MockAudioManager()
        mockAudioManager.simulateUnavailable()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val event = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val result = handler.handleEvent(event)
        
        assertTrue(result.isSuccess)
        assertTrue(mockAudioManager.playedSounds.isEmpty()) // No sound when audio unavailable
    }
    
    @Test
    fun testUpdateSettings() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val newSettings = AudioSettings(
            volume = 0.7f,
            isMuted = true,
            enableStartWorkSound = false,
            soundProfile = AudioSettings.SoundProfile.GYM
        )
        
        val result = handler.updateSettings(newSettings)
        assertTrue(result.isSuccess)
        
        val currentSettings = handler.settings.first()
        assertEquals(0.7f, currentSettings.volume)
        assertTrue(currentSettings.isMuted)
        assertFalse(currentSettings.enableStartWorkSound)
        assertEquals(AudioSettings.SoundProfile.GYM, currentSettings.soundProfile)
        
        // Verify audio manager was updated
        assertEquals(0.7f, mockAudioManager.volume.first())
        assertTrue(mockAudioManager.isMuted.first())
    }
    
    @Test
    fun testUpdateInvalidSettings() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        val invalidSettings = AudioSettings(volume = -0.5f)
        val result = handler.updateSettings(invalidSettings)
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun testSetSoundEnabled() = runTest {
        val mockAudioManager = MockAudioManager()
        val handler = DefaultAudioEventHandler(mockAudioManager)
        
        assertTrue(handler.isSoundEnabled(SoundType.START_WORK))
        
        val result = handler.setSoundEnabled(SoundType.START_WORK, false)
        assertTrue(result.isSuccess)
        assertFalse(handler.isSoundEnabled(SoundType.START_WORK))
        
        val enableResult = handler.setSoundEnabled(SoundType.START_WORK, true)
        assertTrue(enableResult.isSuccess)
        assertTrue(handler.isSoundEnabled(SoundType.START_WORK))
    }
}