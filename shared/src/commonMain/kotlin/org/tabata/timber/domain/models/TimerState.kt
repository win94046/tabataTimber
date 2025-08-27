package org.tabata.timber.domain.models

/**
 * Represents the current state of the Tabata timer
 */
enum class TimerState {
    IDLE,        // Timer is not running and ready to start
    WORK,        // Currently in a work period
    REST,        // Currently in a rest period
    PAUSED,      // Timer is paused
    FINISHED     // Workout session has completed
}