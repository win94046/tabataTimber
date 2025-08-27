package org.tabata.timber.domain.models.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhaseTypeTest {
    
    @Test
    fun `getDisplayName returns correct Chinese names`() {
        assertEquals("Warmup", PhaseType.WARMUP.getDisplayName())
        assertEquals("Work", PhaseType.WORK.getDisplayName())
        assertEquals("Rest", PhaseType.REST.getDisplayName())
        assertEquals("Set Break", PhaseType.SET_BREAK.getDisplayName())
        assertEquals("Cooldown", PhaseType.COOLDOWN.getDisplayName())
    }
    
    @Test
    fun `isActivePhase correctly identifies active phases`() {
        assertTrue(PhaseType.WORK.isActivePhase())
        assertFalse(PhaseType.WARMUP.isActivePhase())
        assertFalse(PhaseType.REST.isActivePhase())
        assertFalse(PhaseType.SET_BREAK.isActivePhase())
        assertFalse(PhaseType.COOLDOWN.isActivePhase())
    }
    
    @Test
    fun `getRecommendedColor returns valid hex colors`() {
        val colors = PhaseType.values().map { it.getRecommendedColor() }
        
        colors.forEach { color ->
            assertTrue(color.startsWith("#"))
            assertEquals(7, color.length) // #RRGGBB format
        }
        
        // Test specific colors
        assertEquals("#FFA726", PhaseType.WARMUP.getRecommendedColor())
        assertEquals("#EF5350", PhaseType.WORK.getRecommendedColor())
        assertEquals("#66BB6A", PhaseType.REST.getRecommendedColor())
        assertEquals("#42A5F5", PhaseType.SET_BREAK.getRecommendedColor())
        assertEquals("#AB47BC", PhaseType.COOLDOWN.getRecommendedColor())
    }
    
    @Test
    fun `all phase types have unique colors`() {
        val colors = PhaseType.values().map { it.getRecommendedColor() }
        val uniqueColors = colors.distinct()
        
        assertEquals(colors.size, uniqueColors.size)
    }
    
    @Test
    fun `enum contains all expected phases`() {
        val expectedPhases = setOf(
            PhaseType.WARMUP,
            PhaseType.WORK,
            PhaseType.REST,
            PhaseType.SET_BREAK,
            PhaseType.COOLDOWN
        )
        
        val actualPhases = PhaseType.values().toSet()
        assertEquals(expectedPhases, actualPhases)
    }
}