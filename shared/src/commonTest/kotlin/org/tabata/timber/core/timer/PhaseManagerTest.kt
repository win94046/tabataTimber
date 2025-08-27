package org.tabata.timber.core.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration

class PhaseManagerTest {
    
    private val phaseManager = PhaseManager()
    
    private fun createStandardConfig(): TimerConfiguration {
        return TimerConfiguration(
            workDuration = 20,      // 20 seconds work
            restDuration = 10,      // 10 seconds rest
            cycles = 2,             // 2 cycles per set
            sets = 1,               // 1 set
            setBreakDuration = 60,  // 60 seconds set break
            warmupDuration = 10,    // 10 seconds warmup
            cooldownDuration = 30   // 30 seconds cooldown
        )
        // Total: warmup(10) + work(20) + rest(10) + work(20) + rest(10) + cooldown(30) = 100 seconds
    }
    
    @Test
    fun testWarmupPhase() {
        val config = createStandardConfig()
        
        // Test beginning of warmup
        val phase1 = phaseManager.calculateCurrentPhase(0L, config)
        assertEquals(PhaseType.WARMUP, phase1.phase)
        assertEquals(10000L, phase1.remainingTimeMs) // 10 seconds remaining
        assertEquals(10000L, phase1.totalPhaseTimeMs)
        assertEquals(0, phase1.currentCycle)
        assertEquals(0, phase1.currentSet)
        assertEquals(0.0, phase1.progressPercent)
        assertFalse(phase1.isLastPhaseOfSession)
        
        // Test middle of warmup
        val phase2 = phaseManager.calculateCurrentPhase(5000L, config)
        assertEquals(PhaseType.WARMUP, phase2.phase)
        assertEquals(5000L, phase2.remainingTimeMs)
        assertEquals(50.0, phase2.progressPercent)
        
        // Test end of warmup
        val phase3 = phaseManager.calculateCurrentPhase(9999L, config)
        assertEquals(PhaseType.WARMUP, phase3.phase)
        assertEquals(1L, phase3.remainingTimeMs)
    }
    
    @Test
    fun testWorkPhase() {
        val config = createStandardConfig()
        
        // Test first work period (after 10s warmup)
        val phase1 = phaseManager.calculateCurrentPhase(10000L, config) // Exactly after warmup
        assertEquals(PhaseType.WORK, phase1.phase)
        assertEquals(20000L, phase1.remainingTimeMs) // 20 seconds of work
        assertEquals(20000L, phase1.totalPhaseTimeMs)
        assertEquals(1, phase1.currentCycle)
        assertEquals(1, phase1.currentSet)
        assertEquals(0.0, phase1.progressPercent)
        
        // Test middle of first work period
        val phase2 = phaseManager.calculateCurrentPhase(20000L, config) // 10s into work
        assertEquals(PhaseType.WORK, phase2.phase)
        assertEquals(10000L, phase2.remainingTimeMs)
        assertEquals(50.0, phase2.progressPercent)
        
        // Test end of first work period
        val phase3 = phaseManager.calculateCurrentPhase(29999L, config)
        assertEquals(PhaseType.WORK, phase3.phase)
        assertEquals(1L, phase3.remainingTimeMs)
    }
    
    @Test
    fun testRestPhase() {
        val config = createStandardConfig()
        
        // Test first rest period (after warmup + work)
        val phase1 = phaseManager.calculateCurrentPhase(30000L, config) // 10s warmup + 20s work
        assertEquals(PhaseType.REST, phase1.phase)
        assertEquals(10000L, phase1.remainingTimeMs) // 10 seconds of rest
        assertEquals(10000L, phase1.totalPhaseTimeMs)
        assertEquals(1, phase1.currentCycle)
        assertEquals(1, phase1.currentSet)
        assertEquals(0.0, phase1.progressPercent)
        
        // Test middle of rest period
        val phase2 = phaseManager.calculateCurrentPhase(35000L, config) // 5s into rest
        assertEquals(PhaseType.REST, phase2.phase)
        assertEquals(5000L, phase2.remainingTimeMs)
        assertEquals(50.0, phase2.progressPercent)
        
        // Test end of first rest period
        val phase3 = phaseManager.calculateCurrentPhase(39999L, config)
        assertEquals(PhaseType.REST, phase3.phase)
        assertEquals(1L, phase3.remainingTimeMs)
    }
    
    @Test\n    fun testSecondCycle() {\n        val config = createStandardConfig()\n        \n        // Test second work period (after first cycle)\n        val phase1 = phaseManager.calculateCurrentPhase(40000L, config) // After first cycle\n        assertEquals(PhaseType.WORK, phase1.phase)\n        assertEquals(20000L, phase1.remainingTimeMs)\n        assertEquals(2, phase1.currentCycle)\n        assertEquals(1, phase1.currentSet)\n        \n        // Test second rest period\n        val phase2 = phaseManager.calculateCurrentPhase(60000L, config) // Second work done\n        assertEquals(PhaseType.REST, phase2.phase)\n        assertEquals(10000L, phase2.remainingTimeMs)\n        assertEquals(2, phase2.currentCycle)\n        assertEquals(1, phase1.currentSet)\n    }\n    \n    @Test\n    fun testCooldownPhase() {\n        val config = createStandardConfig()\n        \n        // Test beginning of cooldown (after all cycles)\n        val phase1 = phaseManager.calculateCurrentPhase(70000L, config) // After all work/rest\n        assertEquals(PhaseType.COOLDOWN, phase1.phase)\n        assertEquals(30000L, phase1.remainingTimeMs) // 30 seconds cooldown\n        assertEquals(30000L, phase1.totalPhaseTimeMs)\n        assertEquals(2, phase1.currentCycle)\n        assertEquals(1, phase1.currentSet)\n        assertTrue(phase1.isLastPhaseOfSession)\n        \n        // Test middle of cooldown\n        val phase2 = phaseManager.calculateCurrentPhase(85000L, config) // 15s into cooldown\n        assertEquals(PhaseType.COOLDOWN, phase2.phase)\n        assertEquals(15000L, phase2.remainingTimeMs)\n        assertEquals(50.0, phase2.progressPercent)\n        \n        // Test end of cooldown (session complete)\n        val phase3 = phaseManager.calculateCurrentPhase(100000L, config) // End of session\n        assertEquals(PhaseType.COOLDOWN, phase3.phase)\n        assertEquals(0L, phase3.remainingTimeMs)\n        assertEquals(100.0, phase3.progressPercent)\n        assertTrue(phase3.isLastPhaseOfSession)\n    }\n    \n    @Test\n    fun testMultipleSetConfiguration() {\n        val config = TimerConfiguration(\n            workDuration = 10,\n            restDuration = 5,\n            cycles = 2,  // 2 cycles per set\n            sets = 2,    // 2 sets\n            setBreakDuration = 30,\n            warmupDuration = 0,\n            cooldownDuration = 0\n        )\n        // Set 1: work(10) + rest(5) + work(10) + rest(5) = 30s\n        // Set break: 30s\n        // Set 2: work(10) + rest(5) + work(10) + rest(5) = 30s\n        // Total: 90s\n        \n        // Test first set\n        val phase1 = phaseManager.calculateCurrentPhase(0L, config)\n        assertEquals(PhaseType.WORK, phase1.phase)\n        assertEquals(1, phase1.currentSet)\n        assertEquals(1, phase1.currentCycle)\n        \n        // Test set break\n        val phase2 = phaseManager.calculateCurrentPhase(30000L, config) // After first set\n        assertEquals(PhaseType.SET_BREAK, phase2.phase)\n        assertEquals(30000L, phase2.remainingTimeMs)\n        assertEquals(2, phase2.currentCycle) // Finished cycles in set 1\n        assertEquals(1, phase2.currentSet)\n        \n        // Test second set\n        val phase3 = phaseManager.calculateCurrentPhase(60000L, config) // After set break\n        assertEquals(PhaseType.WORK, phase3.phase)\n        assertEquals(1, phase3.currentCycle) // First cycle of set 2\n        assertEquals(2, phase3.currentSet) // But we're actually in set 2 now\n    }\n    \n    @Test\n    fun testCalculateSkipToNext() {\n        val config = createStandardConfig()\n        \n        // Test skipping from middle of warmup\n        val warmupPhase = phaseManager.calculateCurrentPhase(5000L, config)\n        val skipAmount = phaseManager.calculateSkipToNextMs(warmupPhase)\n        assertEquals(5000L, skipAmount) // Remaining time in warmup\n        \n        // Test skipping from middle of work\n        val workPhase = phaseManager.calculateCurrentPhase(20000L, config)\n        val skipAmountWork = phaseManager.calculateSkipToNextMs(workPhase)\n        assertEquals(10000L, skipAmountWork) // Remaining time in work phase\n    }\n    \n    @Test\n    fun testCalculateTotalDurationMs() {\n        val config = createStandardConfig()\n        val totalDuration = phaseManager.calculateTotalDurationMs(config)\n        \n        // warmup(10) + 2*(work(20) + rest(10)) + cooldown(30) = 100 seconds\n        assertEquals(100000L, totalDuration)\n    }\n    \n    @Test\n    fun testCalculateTotalDurationMsMultipleSets() {\n        val config = TimerConfiguration(\n            workDuration = 10,\n            restDuration = 5,\n            cycles = 2,\n            sets = 3,\n            setBreakDuration = 20,\n            warmupDuration = 5,\n            cooldownDuration = 10\n        )\n        \n        val totalDuration = phaseManager.calculateTotalDurationMs(config)\n        \n        // warmup(5) + 3*[2*(work(10) + rest(5))] + 2*setBreak(20) + cooldown(10)\n        // = 5 + 3*30 + 2*20 + 10 = 5 + 90 + 40 + 10 = 145 seconds\n        assertEquals(145000L, totalDuration)\n    }\n    \n    @Test\n    fun testCanSkipPhase() {\n        assertTrue(phaseManager.canSkipPhase(PhaseType.WARMUP))\n        assertTrue(phaseManager.canSkipPhase(PhaseType.WORK))\n        assertTrue(phaseManager.canSkipPhase(PhaseType.REST))\n        assertTrue(phaseManager.canSkipPhase(PhaseType.SET_BREAK))\n        assertFalse(phaseManager.canSkipPhase(PhaseType.COOLDOWN))\n    }\n    \n    @Test\n    fun testConfigurationWithoutWarmupCooldown() {\n        val config = TimerConfiguration(\n            workDuration = 20,\n            restDuration = 10,\n            cycles = 1,\n            sets = 1,\n            setBreakDuration = 0,\n            warmupDuration = 0,\n            cooldownDuration = 0\n        )\n        \n        // Should start directly with work\n        val phase1 = phaseManager.calculateCurrentPhase(0L, config)\n        assertEquals(PhaseType.WORK, phase1.phase)\n        \n        // Should go to rest after work\n        val phase2 = phaseManager.calculateCurrentPhase(20000L, config)\n        assertEquals(PhaseType.REST, phase2.phase)\n        \n        // Should be complete after rest\n        val phase3 = phaseManager.calculateCurrentPhase(30000L, config)\n        assertEquals(PhaseType.COOLDOWN, phase3.phase) // Uses cooldown as end state\n        assertEquals(0L, phase3.remainingTimeMs)\n        assertTrue(phase3.isLastPhaseOfSession)\n    }\n    \n    @Test\n    fun testEdgeCasesAtPhaseBoundaries() {\n        val config = createStandardConfig()\n        \n        // Test exactly at phase boundaries\n        val warmupEnd = phaseManager.calculateCurrentPhase(10000L, config)\n        assertEquals(PhaseType.WORK, warmupEnd.phase) // Should transition to work\n        \n        val workEnd = phaseManager.calculateCurrentPhase(30000L, config)\n        assertEquals(PhaseType.REST, workEnd.phase) // Should transition to rest\n        \n        val restEnd = phaseManager.calculateCurrentPhase(40000L, config)\n        assertEquals(PhaseType.WORK, restEnd.phase) // Should transition to second work\n    }\n}"