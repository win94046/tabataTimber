package org.tabata.timber.domain.models.timer

import org.tabata.timber.domain.models.TabataConfig

/**
 * Enhanced timer configuration with comprehensive validation and additional features
 */
data class TimerConfiguration(
    val workDuration: Int,              // Work period duration in seconds
    val restDuration: Int,              // Rest period duration in seconds
    val cycles: Int,                    // Number of work/rest cycles per set
    val sets: Int,                      // Number of sets
    val setBreakDuration: Int,          // Break duration between sets in seconds
    val warmupDuration: Int = 0,        // Optional warmup duration in seconds
    val cooldownDuration: Int = 0,      // Optional cooldown duration in seconds
    val enableSound: Boolean = true,    // Enable audio cues
    val enableVibration: Boolean = true, // Enable vibration cues
    val workPhaseSound: String = "work_beep",    // Sound for work phase
    val restPhaseSound: String = "rest_beep",    // Sound for rest phase
    val countdownSeconds: Int = 3       // Countdown before starting each phase
) {
    
    /**
     * Validation result for timer configuration
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
    
    /**
     * Validates the timer configuration
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Duration validations
        if (workDuration < MIN_DURATION || workDuration > MAX_DURATION) {
            errors.add("工作時間必須在 ${MIN_DURATION} 到 ${MAX_DURATION} 秒之間")
        }
        
        if (restDuration < MIN_DURATION || restDuration > MAX_DURATION) {
            errors.add("休息時間必須在 ${MIN_DURATION} 到 ${MAX_DURATION} 秒之間")
        }
        
        if (setBreakDuration < MIN_DURATION || setBreakDuration > MAX_SET_BREAK_DURATION) {
            errors.add("組間休息時間必須在 ${MIN_DURATION} 到 ${MAX_SET_BREAK_DURATION} 秒之間")
        }
        
        if (warmupDuration < 0 || warmupDuration > MAX_WARMUP_COOLDOWN_DURATION) {
            errors.add("熱身時間必須在 0 到 ${MAX_WARMUP_COOLDOWN_DURATION} 秒之間")
        }
        
        if (cooldownDuration < 0 || cooldownDuration > MAX_WARMUP_COOLDOWN_DURATION) {
            errors.add("緩和時間必須在 0 到 ${MAX_WARMUP_COOLDOWN_DURATION} 秒之間")
        }
        
        // Count validations
        if (cycles < MIN_CYCLES || cycles > MAX_CYCLES) {
            errors.add("循環次數必須在 ${MIN_CYCLES} 到 ${MAX_CYCLES} 之間")
        }
        
        if (sets < MIN_SETS || sets > MAX_SETS) {
            errors.add("組數必須在 ${MIN_SETS} 到 ${MAX_SETS} 之間")
        }
        
        if (countdownSeconds < 0 || countdownSeconds > MAX_COUNTDOWN_SECONDS) {
            errors.add("倒數時間必須在 0 到 ${MAX_COUNTDOWN_SECONDS} 秒之間")
        }
        
        // Total duration validation
        val totalDuration = getTotalDuration()
        if (totalDuration > MAX_TOTAL_DURATION) {
            errors.add("總訓練時間不能超過 ${MAX_TOTAL_DURATION / 60} 分鐘")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Calculates the total workout duration in seconds
     */
    fun getTotalDuration(): Int {
        val cyclesDuration = (workDuration + restDuration) * cycles * sets
        val setBreaksDuration = setBreakDuration * maxOf(0, sets - 1)
        return warmupDuration + cyclesDuration + setBreaksDuration + cooldownDuration
    }
    
    /**
     * Calculates the duration of a single set (all cycles within one set)
     */
    fun getSingleSetDuration(): Int {
        return (workDuration + restDuration) * cycles
    }
    
    /**
     * Calculates total work time across all sets
     */
    fun getTotalWorkTime(): Int {
        return workDuration * cycles * sets
    }
    
    /**
     * Calculates total rest time across all sets
     */
    fun getTotalRestTime(): Int {
        return restDuration * cycles * sets
    }
    
    /**
     * Returns the work-to-rest ratio
     */
    fun getWorkToRestRatio(): Double {
        return if (restDuration > 0) workDuration.toDouble() / restDuration else Double.POSITIVE_INFINITY
    }
    
    /**
     * Converts to the legacy TabataConfig format for backward compatibility
     */
    fun toTabataConfig(): TabataConfig {
        return TabataConfig(
            workDuration = workDuration,
            restDuration = restDuration,
            cycles = cycles,
            sets = sets,
            setBreakDuration = setBreakDuration,
            warmupDuration = warmupDuration,
            cooldownDuration = cooldownDuration
        )
    }
    
    /**
     * Creates a copy with modified durations while maintaining validation
     */
    fun withModifiedDurations(
        newWorkDuration: Int? = null,
        newRestDuration: Int? = null,
        newSetBreakDuration: Int? = null
    ): TimerConfiguration {
        return copy(
            workDuration = newWorkDuration ?: workDuration,
            restDuration = newRestDuration ?: restDuration,
            setBreakDuration = newSetBreakDuration ?: setBreakDuration
        )
    }
    
    companion object {
        // Validation constants
        const val MIN_DURATION = 1                    // 1 second
        const val MAX_DURATION = 3600                 // 1 hour
        const val MAX_SET_BREAK_DURATION = 1800       // 30 minutes
        const val MAX_WARMUP_COOLDOWN_DURATION = 1800 // 30 minutes
        const val MIN_CYCLES = 1
        const val MAX_CYCLES = 50
        const val MIN_SETS = 1
        const val MAX_SETS = 10
        const val MAX_COUNTDOWN_SECONDS = 10
        const val MAX_TOTAL_DURATION = 7200            // 2 hours
        
        /**
         * Creates a standard Tabata configuration (20s work, 10s rest, 8 cycles, 1 set)
         */
        fun createStandard(): TimerConfiguration {
            return TimerConfiguration(
                workDuration = 20,
                restDuration = 10,
                cycles = 8,
                sets = 1,
                setBreakDuration = 60,
                warmupDuration = 0,
                cooldownDuration = 0
            )
        }
        
        /**
         * Creates a beginner-friendly configuration
         */
        fun createBeginner(): TimerConfiguration {
            return TimerConfiguration(
                workDuration = 15,
                restDuration = 15,
                cycles = 4,
                sets = 1,
                setBreakDuration = 60,
                warmupDuration = 30,
                cooldownDuration = 30
            )
        }
        
        /**
         * Creates an advanced high-intensity configuration
         */
        fun createAdvanced(): TimerConfiguration {
            return TimerConfiguration(
                workDuration = 30,
                restDuration = 10,
                cycles = 8,
                sets = 3,
                setBreakDuration = 120,
                warmupDuration = 60,
                cooldownDuration = 60
            )
        }
        
        /**
         * Creates a configuration from legacy TabataConfig
         */
        fun fromTabataConfig(config: TabataConfig): TimerConfiguration {
            return TimerConfiguration(
                workDuration = config.workDuration,
                restDuration = config.restDuration,
                cycles = config.cycles,
                sets = config.sets,
                setBreakDuration = config.setBreakDuration,
                warmupDuration = config.warmupDuration,
                cooldownDuration = config.cooldownDuration
            )
        }
    }
}