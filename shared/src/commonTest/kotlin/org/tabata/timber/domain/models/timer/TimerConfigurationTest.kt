package org.tabata.timber.domain.models.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimerConfigurationTest {
    
    @Test
    fun `createStandard creates correct standard Tabata configuration`() {
        val config = TimerConfiguration.createStandard()
        
        assertEquals(20, config.workDuration)
        assertEquals(10, config.restDuration)
        assertEquals(8, config.cycles)
        assertEquals(1, config.sets)
        assertEquals(60, config.setBreakDuration)
        assertEquals(0, config.warmupDuration)
        assertEquals(0, config.cooldownDuration)
        assertTrue(config.enableSound)
        assertTrue(config.enableVibration)
    }
    
    @Test
    fun `createBeginner creates appropriate beginner configuration`() {
        val config = TimerConfiguration.createBeginner()
        
        assertEquals(15, config.workDuration)
        assertEquals(15, config.restDuration)
        assertEquals(4, config.cycles)
        assertEquals(1, config.sets)
        assertEquals(30, config.warmupDuration)
        assertEquals(30, config.cooldownDuration)
    }
    
    @Test
    fun `createAdvanced creates challenging configuration`() {
        val config = TimerConfiguration.createAdvanced()
        
        assertEquals(30, config.workDuration)
        assertEquals(10, config.restDuration)
        assertEquals(8, config.cycles)
        assertEquals(3, config.sets)
        assertEquals(120, config.setBreakDuration)
        assertEquals(60, config.warmupDuration)
        assertEquals(60, config.cooldownDuration)
    }
    
    @Test
    fun `validate returns Valid for correct configuration`() {
        val config = TimerConfiguration.createStandard()
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Valid)
    }
    
    @Test
    fun `validate catches work duration too short`() {
        val config = TimerConfiguration.createStandard().copy(workDuration = 0)
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("工作時間") })
    }
    
    @Test
    fun `validate catches work duration too long`() {
        val config = TimerConfiguration.createStandard().copy(workDuration = 4000)
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("工作時間") })
    }
    
    @Test
    fun `validate catches too many cycles`() {
        val config = TimerConfiguration.createStandard().copy(cycles = 100)
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("循環次數") })
    }
    
    @Test
    fun `validate catches too many sets`() {
        val config = TimerConfiguration.createStandard().copy(sets = 20)
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("組數") })
    }
    
    @Test
    fun `validate catches negative warmup duration`() {
        val config = TimerConfiguration.createStandard().copy(warmupDuration = -10)
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("熱身時間") })
    }
    
    @Test
    fun `validate catches excessive total duration`() {
        val config = TimerConfiguration(
            workDuration = 3600,
            restDuration = 3600,
            cycles = 10,
            sets = 10,
            setBreakDuration = 1800
        )
        val result = config.validate()
        
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.any { it.contains("總訓練時間") })
    }
    
    @Test
    fun `getTotalDuration calculates correctly for single set`() {
        val config = TimerConfiguration(
            workDuration = 20,
            restDuration = 10,
            cycles = 8,
            sets = 1,
            setBreakDuration = 60,
            warmupDuration = 30,
            cooldownDuration = 30
        )
        
        // (20+10)*8*1 + 60*(1-1) + 30 + 30 = 240 + 0 + 60 = 300
        assertEquals(300, config.getTotalDuration())
    }
    
    @Test
    fun `getTotalDuration calculates correctly for multiple sets`() {
        val config = TimerConfiguration(
            workDuration = 20,
            restDuration = 10,
            cycles = 8,
            sets = 3,
            setBreakDuration = 120,
            warmupDuration = 60,
            cooldownDuration = 60
        )
        
        // (20+10)*8*3 + 120*(3-1) + 60 + 60 = 720 + 240 + 120 = 1080
        assertEquals(1080, config.getTotalDuration())
    }
    
    @Test
    fun `getSingleSetDuration calculates correctly`() {
        val config = TimerConfiguration.createStandard()
        
        // (20+10)*8 = 240
        assertEquals(240, config.getSingleSetDuration())
    }
    
    @Test
    fun `getTotalWorkTime calculates correctly`() {
        val config = TimerConfiguration(
            workDuration = 30,
            restDuration = 15,
            cycles = 4,
            sets = 2,
            setBreakDuration = 60
        )
        
        // 30*4*2 = 240
        assertEquals(240, config.getTotalWorkTime())
    }
    
    @Test
    fun `getTotalRestTime calculates correctly`() {
        val config = TimerConfiguration(
            workDuration = 30,
            restDuration = 15,
            cycles = 4,
            sets = 2,
            setBreakDuration = 60
        )
        
        // 15*4*2 = 120
        assertEquals(120, config.getTotalRestTime())
    }
    
    @Test
    fun `getWorkToRestRatio calculates correctly`() {
        val config = TimerConfiguration.createStandard() // 20s work, 10s rest
        
        assertEquals(2.0, config.getWorkToRestRatio())
    }
    
    @Test
    fun `getWorkToRestRatio handles zero rest duration`() {
        val config = TimerConfiguration.createStandard().copy(restDuration = 0)
        
        assertEquals(Double.POSITIVE_INFINITY, config.getWorkToRestRatio())
    }
    
    @Test
    fun `toTabataConfig converts correctly`() {
        val timerConfig = TimerConfiguration.createStandard()
        val tabataConfig = timerConfig.toTabataConfig()
        
        assertEquals(timerConfig.workDuration, tabataConfig.workDuration)
        assertEquals(timerConfig.restDuration, tabataConfig.restDuration)
        assertEquals(timerConfig.cycles, tabataConfig.cycles)
        assertEquals(timerConfig.sets, tabataConfig.sets)
        assertEquals(timerConfig.setBreakDuration, tabataConfig.setBreakDuration)
        assertEquals(timerConfig.warmupDuration, tabataConfig.warmupDuration)
        assertEquals(timerConfig.cooldownDuration, tabataConfig.cooldownDuration)
    }
    
    @Test
    fun `fromTabataConfig converts correctly`() {
        val tabataConfig = org.tabata.timber.domain.models.TabataConfig.standard()
        val timerConfig = TimerConfiguration.fromTabataConfig(tabataConfig)
        
        assertEquals(tabataConfig.workDuration, timerConfig.workDuration)
        assertEquals(tabataConfig.restDuration, timerConfig.restDuration)
        assertEquals(tabataConfig.cycles, timerConfig.cycles)
        assertEquals(tabataConfig.sets, timerConfig.sets)
        assertEquals(tabataConfig.setBreakDuration, timerConfig.setBreakDuration)
        assertEquals(tabataConfig.warmupDuration, timerConfig.warmupDuration)
        assertEquals(tabataConfig.cooldownDuration, timerConfig.cooldownDuration)
    }
    
    @Test
    fun `withModifiedDurations preserves original values when no changes specified`() {
        val original = TimerConfiguration.createStandard()
        val modified = original.withModifiedDurations()
        
        assertEquals(original.workDuration, modified.workDuration)
        assertEquals(original.restDuration, modified.restDuration)
        assertEquals(original.setBreakDuration, modified.setBreakDuration)
    }
    
    @Test
    fun `withModifiedDurations updates specified values`() {
        val original = TimerConfiguration.createStandard()
        val modified = original.withModifiedDurations(
            newWorkDuration = 25,
            newRestDuration = 5,
            newSetBreakDuration = 90
        )
        
        assertEquals(25, modified.workDuration)
        assertEquals(5, modified.restDuration)
        assertEquals(90, modified.setBreakDuration)
        
        // Other properties should remain unchanged
        assertEquals(original.cycles, modified.cycles)
        assertEquals(original.sets, modified.sets)
        assertEquals(original.warmupDuration, modified.warmupDuration)
    }
    
    @Test
    fun `validation constants are properly defined`() {
        assertTrue(TimerConfiguration.MIN_DURATION > 0)
        assertTrue(TimerConfiguration.MAX_DURATION > TimerConfiguration.MIN_DURATION)
        assertTrue(TimerConfiguration.MIN_CYCLES > 0)
        assertTrue(TimerConfiguration.MAX_CYCLES > TimerConfiguration.MIN_CYCLES)
        assertTrue(TimerConfiguration.MIN_SETS > 0)
        assertTrue(TimerConfiguration.MAX_SETS > TimerConfiguration.MIN_SETS)
        assertTrue(TimerConfiguration.MAX_TOTAL_DURATION > 0)
    }
    
    @Test
    fun `multiple validation errors are collected`() {
        val config = TimerConfiguration(
            workDuration = 0,      // Invalid
            restDuration = 4000,   // Invalid
            cycles = 0,            // Invalid
            sets = 100,            // Invalid
            setBreakDuration = 60,
            warmupDuration = -10   // Invalid
        )
        
        val result = config.validate()
        assertTrue(result is TimerConfiguration.ValidationResult.Invalid)
        assertTrue(result.errors.size >= 5) // At least 5 validation errors
    }
}