package org.tabata.timber.core.audio

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.tabata.timber.core.timer.TimerEngine
import org.tabata.timber.domain.models.timer.TimerConfiguration

/**
 * Example showing how to use the audio system with timer integration
 * This demonstrates the complete workflow of setting up and using the audio system
 */
class AudioSystemExample(
    private val timerAudioIntegration: TimerAudioIntegration,
    private val audioEventHandler: AudioEventHandler
) {
    
    /**
     * Complete example of using the audio system
     */
    suspend fun runExample(): Result<Unit> {
        return try {
            // 1. Configure audio settings
            val customSettings = AudioSettings(
                volume = 0.8f,
                isMuted = false,
                enableStartWorkSound = true,
                enableStartRestSound = true,
                enableCountdownSound = true,
                enableSessionEndSound = true,
                enableHalfwaySound = true,
                enableWarningSound = true,
                countdownStartSeconds = 3,
                warningBeforeSeconds = 5,
                soundProfile = AudioSettings.SoundProfile.DEFAULT
            )
            
            println("🔧 Configuring audio settings...")
            timerAudioIntegration.updateAudioSettings(customSettings).getOrThrow()
            
            // 2. Start the audio integration
            println("🎵 Starting audio integration...")
            timerAudioIntegration.start().getOrThrow()
            
            // 3. Demonstrate manual audio events
            println("🔊 Testing individual sounds...")
            
            // Play start work sound
            val workEvent = AudioEvent.PhaseStarted(
                phaseType = org.tabata.timber.domain.models.timer.PhaseType.WORK,
                phaseNumber = 1,
                cycleNumber = 1,
                setNumber = 1
            )
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(1000)
            
            // Play countdown sound
            val countdownEvent = AudioEvent.CountdownTick(
                secondsRemaining = 3,
                nextPhase = org.tabata.timber.domain.models.timer.PhaseType.REST
            )
            audioEventHandler.handleEvent(countdownEvent).getOrThrow()
            delay(1000)
            
            // Play warning sound
            val warningEvent = AudioEvent.PhaseWarning(
                currentPhase = org.tabata.timber.domain.models.timer.PhaseType.WORK,
                nextPhase = org.tabata.timber.domain.models.timer.PhaseType.REST,
                secondsUntilTransition = 5
            )
            audioEventHandler.handleEvent(warningEvent).getOrThrow()
            delay(1000)
            
            // Play session end sound
            val sessionEndEvent = AudioEvent.SessionCompleted(
                totalDurationMs = 1200000,
                cyclesCompleted = 8,
                setsCompleted = 4
            )
            audioEventHandler.handleEvent(sessionEndEvent).getOrThrow()
            delay(1000)
            
            // 4. Test muting functionality
            println("🔇 Testing mute functionality...")
            val mutedSettings = customSettings.copy(isMuted = true)
            timerAudioIntegration.updateAudioSettings(mutedSettings).getOrThrow()
            
            // This should not produce sound
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(500)
            
            // Unmute
            timerAudioIntegration.updateAudioSettings(customSettings).getOrThrow()
            
            // 5. Test different sound profiles
            println("🏠 Testing HOME sound profile...")
            val homeSettings = customSettings.copy(soundProfile = AudioSettings.SoundProfile.HOME)
            timerAudioIntegration.updateAudioSettings(homeSettings).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(1000)
            
            println("🏋️ Testing GYM sound profile...")
            val gymSettings = customSettings.copy(soundProfile = AudioSettings.SoundProfile.GYM)
            timerAudioIntegration.updateAudioSettings(gymSettings).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(1000)
            
            println("🤫 Testing SILENT sound profile...")
            val silentSettings = customSettings.copy(soundProfile = AudioSettings.SoundProfile.SILENT)
            timerAudioIntegration.updateAudioSettings(silentSettings).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow() // Should be silent
            delay(500)
            
            // 6. Test volume control
            println("🔉 Testing volume control...")
            val quietSettings = customSettings.copy(volume = 0.3f)
            timerAudioIntegration.updateAudioSettings(quietSettings).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(1000)
            
            val loudSettings = customSettings.copy(volume = 1.0f)
            timerAudioIntegration.updateAudioSettings(loudSettings).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow()
            delay(1000)
            
            // 7. Test selective sound disabling
            println("⚙️ Testing selective sound control...")
            audioEventHandler.setSoundEnabled(SoundType.START_WORK, false).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow() // Should be silent
            delay(500)
            
            audioEventHandler.setSoundEnabled(SoundType.START_WORK, true).getOrThrow()
            audioEventHandler.handleEvent(workEvent).getOrThrow() // Should play
            delay(1000)
            
            // 8. Display current settings
            println("📊 Current audio settings:")
            val currentSettings = timerAudioIntegration.getCurrentAudioSettings()
            displaySettings(currentSettings)
            
            // 9. Stop the integration
            println("⏹️ Stopping audio integration...")
            timerAudioIntegration.stop().getOrThrow()
            
            println("✅ Audio system example completed successfully!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Audio system example failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun displaySettings(settings: AudioSettings) {
        println("""
            | Volume: ${settings.volume}
            | Muted: ${settings.isMuted}
            | Sound Profile: ${settings.soundProfile}
            | Start Work Sound: ${settings.enableStartWorkSound}
            | Start Rest Sound: ${settings.enableStartRestSound}
            | Countdown Sound: ${settings.enableCountdownSound}
            | Session End Sound: ${settings.enableSessionEndSound}
            | Halfway Sound: ${settings.enableHalfwaySound}
            | Warning Sound: ${settings.enableWarningSound}
            | Countdown Start: ${settings.countdownStartSeconds}s
            | Warning Before: ${settings.warningBeforeSeconds}s
        """.trimMargin())
    }
    
    /**
     * Simulates a complete timer session with audio integration
     * This would typically be handled automatically by the TimerAudioIntegration
     */
    suspend fun simulateTimerSession(): Result<Unit> {
        return try {
            println("🏃‍♂️ Simulating a complete timer session with audio...")
            
            timerAudioIntegration.start().getOrThrow()
            
            // Simulate session phases
            val phases = listOf(
                org.tabata.timber.domain.models.timer.PhaseType.WARMUP to "Warmup",
                org.tabata.timber.domain.models.timer.PhaseType.WORK to "Work",
                org.tabata.timber.domain.models.timer.PhaseType.REST to "Rest",
                org.tabata.timber.domain.models.timer.PhaseType.WORK to "Work",
                org.tabata.timber.domain.models.timer.PhaseType.REST to "Rest",
                org.tabata.timber.domain.models.timer.PhaseType.COOLDOWN to "Cooldown"
            )
            
            for ((index, phaseInfo) in phases.withIndex()) {
                val (phaseType, phaseName) = phaseInfo
                println("⏱️ Starting $phaseName phase...")
                
                val phaseEvent = AudioEvent.PhaseStarted(
                    phaseType = phaseType,
                    phaseNumber = index + 1,
                    cycleNumber = 1,
                    setNumber = 1
                )
                audioEventHandler.handleEvent(phaseEvent).getOrThrow()
                
                // Simulate phase duration
                delay(2000)
                
                // Simulate countdown at the end of work/rest phases
                if (phaseType in listOf(
                    org.tabata.timber.domain.models.timer.PhaseType.WORK,
                    org.tabata.timber.domain.models.timer.PhaseType.REST
                )) {
                    println("⏰ 3... 2... 1...")
                    for (countdown in 3 downTo 1) {
                        val countdownEvent = AudioEvent.CountdownTick(
                            secondsRemaining = countdown,
                            nextPhase = if (index + 1 < phases.size) phases[index + 1].first else phaseType
                        )
                        audioEventHandler.handleEvent(countdownEvent).getOrThrow()
                        delay(1000)
                    }
                }
            }
            
            // Session completed
            println("🎉 Session completed!")
            val completionEvent = AudioEvent.SessionCompleted(
                totalDurationMs = 300000, // 5 minutes
                cyclesCompleted = 2,
                setsCompleted = 1
            )
            audioEventHandler.handleEvent(completionEvent).getOrThrow()
            
            delay(2000)
            timerAudioIntegration.stop().getOrThrow()
            
            println("✅ Timer session simulation completed!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            println("❌ Timer session simulation failed: ${e.message}")
            Result.failure(e)
        }
    }
}