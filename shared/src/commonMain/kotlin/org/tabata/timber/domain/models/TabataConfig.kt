package org.tabata.timber.domain.models

/**
 * Configuration for a Tabata workout session
 */
data class TabataConfig(
    val workDuration: Int,          // Work period duration in seconds
    val restDuration: Int,          // Rest period duration in seconds
    val cycles: Int,                // Number of work/rest cycles
    val sets: Int,                  // Number of sets (each set contains multiple cycles)
    val setBreakDuration: Int,      // Break duration between sets in seconds
    val warmupDuration: Int = 0,    // Optional warmup duration in seconds
    val cooldownDuration: Int = 0   // Optional cooldown duration in seconds
) {
    
    /**
     * Calculates the total workout duration in seconds
     */
    fun getTotalDuration(): Int {
        val cyclesDuration = (workDuration + restDuration) * cycles * sets
        val setBreaksDuration = setBreakDuration * (sets - 1)
        return warmupDuration + cyclesDuration + setBreaksDuration + cooldownDuration
    }
    
    companion object {
        /**
         * Creates a standard Tabata configuration (20s work, 10s rest, 8 cycles, 1 set)
         */
        fun standard(): TabataConfig {
            return TabataConfig(
                workDuration = 20,
                restDuration = 10,
                cycles = 8,
                sets = 1,
                setBreakDuration = 60
            )
        }
    }
}