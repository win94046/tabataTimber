package org.tabata.timber.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabataConfigTest {
    
    @Test
    fun `standard config should have correct values`() {
        val config = TabataConfig.standard()
        
        assertEquals(20, config.workDuration)
        assertEquals(10, config.restDuration)
        assertEquals(8, config.cycles)
        assertEquals(1, config.sets)
        assertEquals(60, config.setBreakDuration)
    }
    
    @Test
    fun `total duration calculation should be correct`() {
        val config = TabataConfig(
            workDuration = 20,
            restDuration = 10,
            cycles = 8,
            sets = 2,
            setBreakDuration = 60,
            warmupDuration = 30,
            cooldownDuration = 30
        )
        
        // (20+10) * 8 * 2 + 60 * (2-1) + 30 + 30
        // 30 * 16 + 60 + 60 = 480 + 120 = 600
        val expectedTotal = 600
        assertEquals(expectedTotal, config.getTotalDuration())
    }
}