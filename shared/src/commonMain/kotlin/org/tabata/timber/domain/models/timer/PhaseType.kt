package org.tabata.timber.domain.models.timer

/**
 * Represents the different phases of a timer workout session
 */
enum class PhaseType {
    /**
     * Initial preparation phase before starting the workout
     */
    PREPARATION,
    
    /**
     * Initial warmup phase before the main workout
     */
    WARMUP,
    
    /**
     * High-intensity work period
     */
    WORK,
    
    /**
     * Low-intensity rest period between work intervals
     */
    REST,
    
    /**
     * Extended break between sets
     */
    SET_BREAK,
    
    /**
     * Final cooldown phase after the main workout
     */
    COOLDOWN;
    
    /**
     * Returns a human-readable name for the phase
     */
    fun getDisplayName(): String = when (this) {
        PREPARATION -> "Preparation"
        WARMUP -> "Warmup"
        WORK -> "Work"
        REST -> "Rest"
        SET_BREAK -> "Set Break"
        COOLDOWN -> "Cooldown"
    }
    
    /**
     * Returns true if this phase is considered an active/intense phase
     */
    fun isActivePhase(): Boolean = when (this) {
        WORK -> true
        PREPARATION, WARMUP, REST, SET_BREAK, COOLDOWN -> false
    }
    
    /**
     * Returns the recommended color for UI display
     */
    fun getRecommendedColor(): String = when (this) {
        PREPARATION -> "#9E9E9E"  // Grey
        WARMUP -> "#FFA726"      // Orange
        WORK -> "#EF5350"        // Red
        REST -> "#66BB6A"        // Green
        SET_BREAK -> "#42A5F5"   // Blue
        COOLDOWN -> "#AB47BC"    // Purple
    }
}