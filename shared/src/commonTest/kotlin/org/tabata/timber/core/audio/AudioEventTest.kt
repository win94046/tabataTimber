package org.tabata.timber.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.tabata.timber.domain.models.timer.PhaseType

class AudioEventTest {
    
    @Test
    fun testPhaseStartedEvent() {
        val event = AudioEvent.PhaseStarted(
            phaseType = PhaseType.WORK,
            phaseNumber = 1,
            cycleNumber = 2,
            setNumber = 3
        )
        
        assertEquals(PhaseType.WORK, event.phaseType)
        assertEquals(1, event.phaseNumber)
        assertEquals(2, event.cycleNumber)
        assertEquals(3, event.setNumber)
        assertEquals(SoundType.START_WORK, event.getSoundType())
        assertEquals(8, event.getPriority())
    }
    
    @Test
    fun testCountdownTickEvent() {
        val event = AudioEvent.CountdownTick(
            secondsRemaining = 3,
            nextPhase = PhaseType.REST
        )
        
        assertEquals(3, event.secondsRemaining)
        assertEquals(PhaseType.REST, event.nextPhase)
        assertEquals(SoundType.COUNTDOWN, event.getSoundType())
        assertEquals(7, event.getPriority())
    }
    
    @Test
    fun testPhaseHalfwayEvent() {
        val event = AudioEvent.PhaseHalfway(
            currentPhase = PhaseType.WORK,
            remainingSeconds = 10
        )
        
        assertEquals(PhaseType.WORK, event.currentPhase)
        assertEquals(10, event.remainingSeconds)
        assertEquals(SoundType.HALFWAY, event.getSoundType())
        assertEquals(3, event.getPriority())
    }
    
    @Test
    fun testPhaseWarningEvent() {
        val event = AudioEvent.PhaseWarning(
            currentPhase = PhaseType.WORK,
            nextPhase = PhaseType.REST,
            secondsUntilTransition = 5
        )
        
        assertEquals(PhaseType.WORK, event.currentPhase)
        assertEquals(PhaseType.REST, event.nextPhase)
        assertEquals(5, event.secondsUntilTransition)
        assertEquals(SoundType.WARNING, event.getSoundType())
        assertEquals(6, event.getPriority())
    }
    
    @Test
    fun testSessionCompletedEvent() {
        val event = AudioEvent.SessionCompleted(
            totalDurationMs = 1200000,
            cyclesCompleted = 8,
            setsCompleted = 3
        )
        
        assertEquals(1200000, event.totalDurationMs)
        assertEquals(8, event.cyclesCompleted)
        assertEquals(3, event.setsCompleted)
        assertEquals(SoundType.SESSION_END, event.getSoundType())
        assertEquals(10, event.getPriority())
    }
    
    @Test
    fun testSetCompletedEvent() {
        val event = AudioEvent.SetCompleted(
            setNumber = 2,
            totalSets = 4
        )
        
        assertEquals(2, event.setNumber)
        assertEquals(4, event.totalSets)
        assertEquals(SoundType.SESSION_END, event.getSoundType())
        assertEquals(5, event.getPriority())
    }
    
    @Test
    fun testCycleCompletedEvent() {
        val event = AudioEvent.CycleCompleted(
            cycleNumber = 4,
            totalCycles = 8,
            setNumber = 2
        )
        
        assertEquals(4, event.cycleNumber)
        assertEquals(8, event.totalCycles)
        assertEquals(2, event.setNumber)
        assertEquals(SoundType.HALFWAY, event.getSoundType())
        assertEquals(4, event.getPriority())
    }
    
    @Test
    fun testAudioErrorEvent() {
        val event = AudioEvent.AudioError(
            errorMessage = "Failed to load sound",
            soundType = SoundType.START_WORK
        )
        
        assertEquals("Failed to load sound", event.errorMessage)
        assertEquals(SoundType.START_WORK, event.soundType)
        assertNull(event.getSoundType())
        assertEquals(9, event.getPriority())
    }
    
    @Test
    fun testAudioAvailabilityChangedEvent() {
        val event = AudioEvent.AudioAvailabilityChanged(
            isAvailable = false
        )
        
        assertEquals(false, event.isAvailable)
        assertNull(event.getSoundType())
        assertEquals(1, event.getPriority())
    }
    
    @Test
    fun testPhaseTypeToSoundTypeMapping() {
        // Test different phase types
        val workEvent = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        assertEquals(SoundType.START_WORK, workEvent.getSoundType())
        
        val restEvent = AudioEvent.PhaseStarted(PhaseType.REST, 1, 1, 1)
        assertEquals(SoundType.START_REST, restEvent.getSoundType())
        
        val warmupEvent = AudioEvent.PhaseStarted(PhaseType.WARMUP, 1, 1, 1)
        assertEquals(SoundType.START_WORK, warmupEvent.getSoundType())
        
        val cooldownEvent = AudioEvent.PhaseStarted(PhaseType.COOLDOWN, 1, 1, 1)
        assertEquals(SoundType.START_REST, cooldownEvent.getSoundType())
        
        val setBreakEvent = AudioEvent.PhaseStarted(PhaseType.SET_BREAK, 1, 1, 1)
        assertEquals(SoundType.START_REST, setBreakEvent.getSoundType())
        
        val preparationEvent = AudioEvent.PhaseStarted(PhaseType.PREPARATION, 1, 1, 1)
        assertEquals(SoundType.COUNTDOWN, preparationEvent.getSoundType())
    }
    
    @Test
    fun testEventPriorityOrdering() {
        val sessionCompleted = AudioEvent.SessionCompleted(0, 0, 0)
        val audioError = AudioEvent.AudioError("error")
        val phaseStarted = AudioEvent.PhaseStarted(PhaseType.WORK, 1, 1, 1)
        val countdown = AudioEvent.CountdownTick(3, PhaseType.REST)
        val warning = AudioEvent.PhaseWarning(PhaseType.WORK, PhaseType.REST, 5)
        val setCompleted = AudioEvent.SetCompleted(1, 4)
        val cycleCompleted = AudioEvent.CycleCompleted(1, 8, 1)
        val halfway = AudioEvent.PhaseHalfway(PhaseType.WORK, 10)
        val availability = AudioEvent.AudioAvailabilityChanged(true)
        
        // Verify priority ordering
        assertTrue(sessionCompleted.getPriority() > audioError.getPriority())
        assertTrue(audioError.getPriority() > phaseStarted.getPriority())
        assertTrue(phaseStarted.getPriority() > countdown.getPriority())
        assertTrue(countdown.getPriority() > warning.getPriority())
        assertTrue(warning.getPriority() > setCompleted.getPriority())
        assertTrue(setCompleted.getPriority() > cycleCompleted.getPriority())
        assertTrue(cycleCompleted.getPriority() > halfway.getPriority())
        assertTrue(halfway.getPriority() > availability.getPriority())
    }
}