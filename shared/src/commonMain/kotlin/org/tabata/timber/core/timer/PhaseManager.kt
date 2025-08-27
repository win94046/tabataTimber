package org.tabata.timber.core.timer

import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration

/**
 * Manages phase transitions and calculations for Tabata timer sessions
 * Calculates current phase based on elapsed time and configuration
 */
class PhaseManager {
    
    /**
     * Current phase calculation result
     */
    data class PhaseInfo(
        val phase: PhaseType,
        val remainingTimeMs: Long,
        val totalPhaseTimeMs: Long,
        val currentCycle: Int,
        val currentSet: Int,
        val progressPercent: Double,
        val isLastPhaseOfSession: Boolean
    )
    
    /**
     * Calculates current phase info based on elapsed time and configuration
     * 
     * @param elapsedMs Total elapsed time since session start
     * @param config Timer configuration
     * @return Current phase information
     */
    fun calculateCurrentPhase(elapsedMs: Long, config: TimerConfiguration): PhaseInfo {
        var remainingTime = elapsedMs
        
        // 1. Check if in warmup phase
        if (config.warmupDuration > 0 && remainingTime < config.warmupDuration * 1000L) {
            val phaseTimeMs = config.warmupDuration * 1000L
            val remaining = phaseTimeMs - remainingTime
            return PhaseInfo(
                phase = PhaseType.WARMUP,
                remainingTimeMs = remaining,
                totalPhaseTimeMs = phaseTimeMs,
                currentCycle = 0,
                currentSet = 0,
                progressPercent = (remainingTime.toDouble() / phaseTimeMs) * 100,
                isLastPhaseOfSession = false
            )
        }
        
        // Move past warmup
        remainingTime -= config.warmupDuration * 1000L
        
        // 2. Calculate work/rest cycles
        val cycleDurationMs = (config.workDuration + config.restDuration) * 1000L
        val setDurationMs = cycleDurationMs * config.cycles
        val setWithBreakDurationMs = setDurationMs + config.setBreakDuration * 1000L
        
        // Calculate current set
        var currentSet = 1
        var timeInCurrentSet = remainingTime
        
        // Find which set we're in
        while (currentSet < config.sets && timeInCurrentSet >= setWithBreakDurationMs) {
            timeInCurrentSet -= setWithBreakDurationMs
            currentSet++
        }
        
        // If we've completed all sets, check cooldown
        if (currentSet > config.sets || timeInCurrentSet >= setDurationMs + config.setBreakDuration * 1000L) {
            // We're in cooldown or finished
            val cooldownStartTime = (config.sets * setWithBreakDurationMs - config.setBreakDuration * 1000L)
            val cooldownElapsed = remainingTime - cooldownStartTime
            
            if (config.cooldownDuration > 0 && cooldownElapsed < config.cooldownDuration * 1000L) {
                val phaseTimeMs = config.cooldownDuration * 1000L
                val remaining = phaseTimeMs - cooldownElapsed
                return PhaseInfo(
                    phase = PhaseType.COOLDOWN,
                    remainingTimeMs = remaining,
                    totalPhaseTimeMs = phaseTimeMs,
                    currentCycle = config.cycles,
                    currentSet = config.sets,
                    progressPercent = (cooldownElapsed.toDouble() / phaseTimeMs) * 100,
                    isLastPhaseOfSession = true
                )
            } else {
                // Session is complete
                return PhaseInfo(
                    phase = PhaseType.COOLDOWN,
                    remainingTimeMs = 0L,
                    totalPhaseTimeMs = config.cooldownDuration * 1000L,
                    currentCycle = config.cycles,
                    currentSet = config.sets,
                    progressPercent = 100.0,
                    isLastPhaseOfSession = true
                )
            }
        }
        
        // 3. Check if in set break (between sets)
        if (timeInCurrentSet >= setDurationMs) {
            val setBreakElapsed = timeInCurrentSet - setDurationMs
            val phaseTimeMs = config.setBreakDuration * 1000L
            val remaining = phaseTimeMs - setBreakElapsed
            
            return PhaseInfo(
                phase = PhaseType.SET_BREAK,
                remainingTimeMs = remaining,
                totalPhaseTimeMs = phaseTimeMs,
                currentCycle = config.cycles,
                currentSet = currentSet,
                progressPercent = (setBreakElapsed.toDouble() / phaseTimeMs) * 100,
                isLastPhaseOfSession = currentSet == config.sets
            )
        }
        
        // 4. We're in a work/rest cycle
        val cycleNumber = (timeInCurrentSet / cycleDurationMs).toInt() + 1
        val timeInCycle = timeInCurrentSet % cycleDurationMs
        
        val workDurationMs = config.workDuration * 1000L
        
        return if (timeInCycle < workDurationMs) {
            // In work phase
            val remaining = workDurationMs - timeInCycle
            PhaseInfo(
                phase = PhaseType.WORK,
                remainingTimeMs = remaining,
                totalPhaseTimeMs = workDurationMs,
                currentCycle = cycleNumber,
                currentSet = currentSet,
                progressPercent = (timeInCycle.toDouble() / workDurationMs) * 100,
                isLastPhaseOfSession = false
            )
        } else {
            // In rest phase
            val restElapsed = timeInCycle - workDurationMs
            val restDurationMs = config.restDuration * 1000L
            val remaining = restDurationMs - restElapsed
            
            PhaseInfo(
                phase = PhaseType.REST,
                remainingTimeMs = remaining,
                totalPhaseTimeMs = restDurationMs,
                currentCycle = cycleNumber,
                currentSet = currentSet,
                progressPercent = (restElapsed.toDouble() / restDurationMs) * 100,
                isLastPhaseOfSession = false
            )
        }
    }
    
    /**
     * Calculates the time to skip to the next phase
     * 
     * @param currentPhaseInfo Current phase information
     * @return Time in milliseconds to advance to reach next phase
     */
    fun calculateSkipToNextMs(currentPhaseInfo: PhaseInfo): Long {
        return currentPhaseInfo.remainingTimeMs
    }
    
    /**
     * Calculates total session duration from configuration
     */
    fun calculateTotalDurationMs(config: TimerConfiguration): Long {
        val workRestMs = (config.workDuration + config.restDuration) * 1000L * config.cycles * config.sets
        val setBreaksMs = config.setBreakDuration * 1000L * (config.sets - 1).coerceAtLeast(0)
        val warmupMs = config.warmupDuration * 1000L
        val cooldownMs = config.cooldownDuration * 1000L
        
        return warmupMs + workRestMs + setBreaksMs + cooldownMs
    }
    
    /**
     * Validates if a skip operation is allowed for current phase
     */
    fun canSkipPhase(currentPhase: PhaseType): Boolean {
        return currentPhase != PhaseType.COOLDOWN // Don't allow skipping cooldown
    }
}