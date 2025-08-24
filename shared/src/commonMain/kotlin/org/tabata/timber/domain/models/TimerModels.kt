package org.tabata.timber.domain.models

import kotlin.time.Duration

/**
 * Timer configuration for a workout session
 */
data class TimerConfiguration(
    val name: String,
    val warmupDuration: Duration,
    val workDuration: Duration,
    val restDuration: Duration,
    val setBreakDuration: Duration,
    val cooldownDuration: Duration,
    val rounds: Int,
    val sets: Int,
    val audioSettings: AudioSettings
)

/**
 * Current timer state
 */
data class TimerState(
    val status: TimerStatus,
    val currentPhase: PhaseType,
    val phaseTimeRemaining: Duration,
    val totalTimeRemaining: Duration,
    val currentRound: Int,
    val totalRounds: Int,
    val currentSet: Int,
    val totalSets: Int
)

/**
 * Timer status enumeration
 */
enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

/**
 * Workout phase types
 */
enum class PhaseType {
    WARMUP,
    WORK,
    REST,
    SET_BREAK,
    COOLDOWN
}

/**
 * Audio settings for timer sessions
 */
data class AudioSettings(
    val ttsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val language: String = "en-US",
    val volume: Float = 1.0f,
    val musicDuckingEnabled: Boolean = true
)