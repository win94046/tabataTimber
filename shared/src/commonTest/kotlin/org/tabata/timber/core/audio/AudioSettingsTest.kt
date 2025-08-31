package org.tabata.timber.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioSettingsTest {
    
    @Test
    fun testDefaultSettings() {
        val settings = AudioSettings()
        
        assertEquals(1.0f, settings.volume)
        assertFalse(settings.isMuted)
        assertTrue(settings.enableStartWorkSound)
        assertTrue(settings.enableStartRestSound)
        assertTrue(settings.enableCountdownSound)
        assertTrue(settings.enableSessionEndSound)
        assertFalse(settings.enableHalfwaySound)
        assertTrue(settings.enableWarningSound)
        assertEquals(3, settings.countdownStartSeconds)
        assertEquals(5, settings.warningBeforeSeconds)
        assertEquals(AudioSettings.SoundProfile.DEFAULT, settings.soundProfile)
    }
    
    @Test
    fun testValidSettings() {
        val settings = AudioSettings(
            volume = 0.5f,
            isMuted = true,
            enableStartWorkSound = false,
            countdownStartSeconds = 5,
            warningBeforeSeconds = 10,
            soundProfile = AudioSettings.SoundProfile.GYM
        )
        
        val validation = settings.validate()
        assertTrue(validation is AudioSettings.ValidationResult.Valid)
    }
    
    @Test
    fun testInvalidVolumeRange() {
        val settingsLow = AudioSettings(volume = -0.1f)
        val validationLow = settingsLow.validate()
        assertTrue(validationLow is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationLow.errors.any { it.contains("Volume must be between") })
        
        val settingsHigh = AudioSettings(volume = 1.1f)
        val validationHigh = settingsHigh.validate()
        assertTrue(validationHigh is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationHigh.errors.any { it.contains("Volume must be between") })
    }
    
    @Test
    fun testInvalidCountdownRange() {
        val settingsLow = AudioSettings(countdownStartSeconds = 0)
        val validationLow = settingsLow.validate()
        assertTrue(validationLow is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationLow.errors.any { it.contains("Countdown start seconds") })
        
        val settingsHigh = AudioSettings(countdownStartSeconds = 11)
        val validationHigh = settingsHigh.validate()
        assertTrue(validationHigh is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationHigh.errors.any { it.contains("Countdown start seconds") })
    }
    
    @Test
    fun testInvalidWarningRange() {
        val settingsLow = AudioSettings(warningBeforeSeconds = 0)
        val validationLow = settingsLow.validate()
        assertTrue(validationLow is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationLow.errors.any { it.contains("Warning before seconds") })
        
        val settingsHigh = AudioSettings(warningBeforeSeconds = 31)
        val validationHigh = settingsHigh.validate()
        assertTrue(validationHigh is AudioSettings.ValidationResult.Invalid)
        assertTrue(validationHigh.errors.any { it.contains("Warning before seconds") })
    }
    
    @Test
    fun testMultipleValidationErrors() {
        val settings = AudioSettings(
            volume = -0.5f,
            countdownStartSeconds = 0,
            warningBeforeSeconds = 50
        )
        
        val validation = settings.validate()
        assertTrue(validation is AudioSettings.ValidationResult.Invalid)
        assertEquals(3, validation.errors.size)
    }
    
    @Test
    fun testSoundProfileValues() {
        val profiles = AudioSettings.SoundProfile.values()
        
        assertEquals(5, profiles.size)
        assertTrue(profiles.contains(AudioSettings.SoundProfile.DEFAULT))
        assertTrue(profiles.contains(AudioSettings.SoundProfile.GYM))
        assertTrue(profiles.contains(AudioSettings.SoundProfile.HOME))
        assertTrue(profiles.contains(AudioSettings.SoundProfile.OUTDOOR))
        assertTrue(profiles.contains(AudioSettings.SoundProfile.SILENT))
    }
}