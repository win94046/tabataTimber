package org.tabata.timber.domain.repositories

import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TabataConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing timer-related data operations
 */
interface TimerRepository {
    
    /**
     * Saves a workout session to persistent storage
     */
    suspend fun saveWorkoutSession(session: WorkoutSession): Result<Unit>
    
    /**
     * Gets a workout session by ID
     */
    suspend fun getWorkoutSession(id: String): Result<WorkoutSession?>
    
    /**
     * Gets all workout sessions, ordered by start time (most recent first)
     */
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>
    
    /**
     * Deletes a workout session by ID
     */
    suspend fun deleteWorkoutSession(id: String): Result<Unit>
    
    /**
     * Saves a custom Tabata configuration
     */
    suspend fun saveTabataConfig(config: TabataConfig, name: String): Result<Unit>
    
    /**
     * Gets all saved Tabata configurations
     */
    fun getSavedConfigs(): Flow<List<Pair<String, TabataConfig>>>
    
    /**
     * Deletes a saved configuration by name
     */
    suspend fun deleteConfig(name: String): Result<Unit>
}