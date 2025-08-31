package org.tabata.timber.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.tabata.timber.core.timer.MockTimerEngine
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration

class TimerAudioIntegrationTest {
    
    private fun createTestConfiguration() = TimerConfiguration(
        warmupDuration = 5,
        workDuration = 20,
        restDuration = 10,
        cooldownDuration = 5,
        cycles = 8,
        sets = 4,
        setBreakDuration = 60
    )
    
    @Test
    fun testStartAndStop() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        val startResult = integration.start()
        assertTrue(startResult.isSuccess)
        assertTrue(mockAudioManager.initializeCalled)
        assertTrue(mockAudioManager.preloadSoundsCalled)
        
        val stopResult = integration.stop()
        assertTrue(stopResult.isSuccess)
    }
    
    @Test
    fun testStartFailure() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        mockAudioManager.shouldFailInitialize = true
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        val result = integration.start()
        assertTrue(result.isFailure)
    }
    
    @Test
    fun testPhaseChangeHandling() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        
        // Simulate phase change from warmup to work
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify work phase sound was played
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.START_WORK })
    }
    
    @Test
    fun testStateChangeHandling() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        
        // Simulate state change to finished
        mockTimerEngine.simulateStateChange(TimerState.FINISHED)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify session end sound was played
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.SESSION_END })
    }
    
    @Test
    fun testCountdownHandling() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        
        // Simulate running state
        mockTimerEngine.simulateStateChange(TimerState.RUNNING)
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        
        // Simulate countdown (3 seconds remaining)
        mockTimerEngine.simulateRemainingTime(3)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify countdown sound was played
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.COUNTDOWN })
    }
    
    @Test
    fun testWarningHandling() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        
        // Simulate running state
        mockTimerEngine.simulateStateChange(TimerState.RUNNING)
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        
        // Simulate warning (5 seconds remaining - default warning time)
        mockTimerEngine.simulateRemainingTime(5)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify warning sound was played
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.WARNING })
    }
    
    @Test
    fun testHalfwayHandling() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        // Enable halfway sounds
        val settingsWithHalfway = AudioSettings(enableHalfwaySound = true)
        integration.updateAudioSettings(settingsWithHalfway)
        
        integration.start()
        
        // Set configuration for proper phase duration calculation
        mockTimerEngine.simulateConfiguration(createTestConfiguration())
        
        // Simulate running state and work phase
        mockTimerEngine.simulateStateChange(TimerState.RUNNING)
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        
        // Simulate halfway point (10 seconds remaining for 20-second work phase)
        mockTimerEngine.simulateRemainingTime(10)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify halfway sound was played
        assertTrue(mockAudioManager.playedSounds.any { it.first == SoundType.HALFWAY })
    }
    
    @Test
    fun testPausedStateStopsTimingEvents() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        
        // Simulate paused state
        mockTimerEngine.simulateStateChange(TimerState.PAUSED)
        
        // Simulate countdown time - should not trigger sound when paused
        mockTimerEngine.simulateRemainingTime(3)
        
        // Allow some time for event processing
        kotlinx.coroutines.delay(100)
        
        // Verify no countdown sound was played
        assertFalse(mockAudioManager.playedSounds.any { it.first == SoundType.COUNTDOWN })
    }
    
    @Test
    fun testUpdateAudioSettings() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        val newSettings = AudioSettings(
            volume = 0.8f,
            enableStartWorkSound = false,
            soundProfile = AudioSettings.SoundProfile.HOME
        )
        
        val result = integration.updateAudioSettings(newSettings)
        assertTrue(result.isSuccess)
        
        val currentSettings = integration.getCurrentAudioSettings()
        assertEquals(0.8f, currentSettings.volume)
        assertFalse(currentSettings.enableStartWorkSound)
        assertEquals(AudioSettings.SoundProfile.HOME, currentSettings.soundProfile)
    }
    
    @Test
    fun testGetCurrentAudioSettings() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        val settings = integration.getCurrentAudioSettings()
        assertEquals(AudioSettings(), settings) // Should return default settings initially
    }
    
    @Test
    fun testMultiplePhaseTransitions() = runTest {
        val mockTimerEngine = MockTimerEngine()
        val mockAudioManager = MockAudioManager()
        val mockEventHandler = DefaultAudioEventHandler(mockAudioManager)
        val integration = TimerAudioIntegration(mockTimerEngine, mockEventHandler)
        
        integration.start()
        mockAudioManager.playedSounds.clear() // Clear initialization sounds
        
        // Simulate sequence: warmup -> work -> rest -> work
        mockTimerEngine.simulatePhaseChange(PhaseType.WARMUP)
        kotlinx.coroutines.delay(50)
        
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        kotlinx.coroutines.delay(50)
        
        mockTimerEngine.simulatePhaseChange(PhaseType.REST)
        kotlinx.coroutines.delay(50)
        
        mockTimerEngine.simulatePhaseChange(PhaseType.WORK)
        kotlinx.coroutines.delay(50)
        
        // Verify all phase sounds were played
        val workSounds = mockAudioManager.playedSounds.count { it.first == SoundType.START_WORK }
        val restSounds = mockAudioManager.playedSounds.count { it.first == SoundType.START_REST }
        
        assertEquals(3, workSounds) // warmup + 2 work phases
        assertEquals(1, restSounds) // 1 rest phase
    }
}