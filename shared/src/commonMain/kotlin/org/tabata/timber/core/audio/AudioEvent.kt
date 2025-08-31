package org.tabata.timber.core.audio

import org.tabata.timber.domain.models.timer.PhaseType

/**
 * Audio events that can be triggered during timer sessions
 */
sealed class AudioEvent {
    
    /**
     * Phase transition events
     */
    data class PhaseStarted(
        val phaseType: PhaseType,
        val phaseNumber: Int,
        val cycleNumber: Int,
        val setNumber: Int
    ) : AudioEvent()
    
    /**
     * Countdown events (3, 2, 1 before phase start)
     */
    data class CountdownTick(
        val secondsRemaining: Int,
        val nextPhase: PhaseType
    ) : AudioEvent()
    
    /**
     * Halfway point of current phase
     */
    data class PhaseHalfway(
        val currentPhase: PhaseType,
        val remainingSeconds: Int
    ) : AudioEvent()
    
    /**
     * Warning before phase transition
     */
    data class PhaseWarning(
        val currentPhase: PhaseType,
        val nextPhase: PhaseType,
        val secondsUntilTransition: Int
    ) : AudioEvent()
    
    /**
     * Session completion
     */
    data class SessionCompleted(
        val totalDurationMs: Long,
        val cyclesCompleted: Int,
        val setsCompleted: Int
    ) : AudioEvent()
    
    /**
     * Session milestone events
     */
    data class SetCompleted(
        val setNumber: Int,
        val totalSets: Int
    ) : AudioEvent()
    
    data class CycleCompleted(
        val cycleNumber: Int,
        val totalCycles: Int,
        val setNumber: Int
    ) : AudioEvent()
    
    /**
     * Error or system events
     */
    data class AudioError(
        val errorMessage: String,
        val soundType: SoundType? = null
    ) : AudioEvent()
    
    /**
     * Audio system status changes
     */
    data class AudioAvailabilityChanged(
        val isAvailable: Boolean
    ) : AudioEvent()
    
    /**
     * Maps audio events to appropriate sound types
     */
    fun getSoundType(): SoundType? = when (this) {
        is PhaseStarted -> when (phaseType) {
            PhaseType.WORK -> SoundType.START_WORK
            PhaseType.REST -> SoundType.START_REST
            PhaseType.WARMUP -> SoundType.START_WORK
            PhaseType.COOLDOWN -> SoundType.START_REST
            PhaseType.SET_BREAK -> SoundType.START_REST
            PhaseType.PREPARATION -> SoundType.COUNTDOWN
        }
        is CountdownTick -> SoundType.COUNTDOWN
        is PhaseHalfway -> SoundType.HALFWAY
        is PhaseWarning -> SoundType.WARNING
        is SessionCompleted -> SoundType.SESSION_END
        is SetCompleted -> SoundType.SESSION_END
        is CycleCompleted -> SoundType.HALFWAY
        is AudioError -> null
        is AudioAvailabilityChanged -> null
    }
    
    /**
     * Gets event priority for handling multiple simultaneous events
     */
    fun getPriority(): Int = when (this) {
        is SessionCompleted -> 10
        is AudioError -> 9
        is PhaseStarted -> 8
        is CountdownTick -> 7
        is PhaseWarning -> 6
        is SetCompleted -> 5
        is CycleCompleted -> 4
        is PhaseHalfway -> 3
        is AudioAvailabilityChanged -> 1
    }
}