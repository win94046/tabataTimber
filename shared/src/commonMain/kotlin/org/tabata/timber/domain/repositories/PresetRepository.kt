package org.tabata.timber.domain.repositories

import kotlinx.coroutines.flow.Flow
import org.tabata.timber.domain.models.timer.TimerConfiguration

/**
 * Repository interface for managing timer preset configurations.
 * Handles both system presets and user-created custom configurations.
 */
interface PresetRepository {
    
    /**
     * Gets all available timer configurations (presets + user configurations)
     * @return Flow of list of timer configurations, ordered by creation date
     */
    fun getAllConfigurations(): Flow<List<TimerConfiguration>>
    
    /**
     * Gets only system preset configurations
     * @return Flow of list of preset timer configurations
     */
    fun getPresetConfigurations(): Flow<List<TimerConfiguration>>
    
    /**
     * Gets only user-created custom configurations  
     * @return Flow of list of user-created timer configurations
     */
    fun getUserConfigurations(): Flow<List<TimerConfiguration>>
    
    /**
     * Gets a specific timer configuration by ID
     * @param id The configuration ID
     * @return The timer configuration if found, null otherwise
     */
    suspend fun getConfigurationById(id: String): TimerConfiguration?
    
    /**
     * Creates a new user timer configuration
     * @param configuration The timer configuration to create
     * @return Result indicating success or failure with error details
     */
    suspend fun createConfiguration(configuration: TimerConfiguration): Result<TimerConfiguration>
    
    /**
     * Updates an existing user timer configuration
     * Note: System presets cannot be updated
     * @param configuration The updated timer configuration
     * @return Result indicating success or failure with error details
     */
    suspend fun updateConfiguration(configuration: TimerConfiguration): Result<TimerConfiguration>
    
    /**
     * Deletes a user timer configuration
     * Note: System presets cannot be deleted
     * @param id The configuration ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteConfiguration(id: String): Result<Unit>
    
    /**
     * Duplicates an existing configuration (preset or user) as a new user configuration
     * @param id The ID of the configuration to duplicate
     * @param newName Optional new name for the duplicated configuration
     * @return Result with the new duplicated configuration or error
     */
    suspend fun duplicateConfiguration(id: String, newName: String? = null): Result<TimerConfiguration>
    
    /**
     * Gets the count of user-created configurations
     * Useful for enforcing limits in free vs premium tiers
     * @return The number of user configurations
     */
    suspend fun getUserConfigurationCount(): Int
    
    /**
     * Searches configurations by name or tags
     * @param query The search query
     * @return List of matching configurations
     */
    suspend fun searchConfigurations(query: String): List<TimerConfiguration>
    
    /**
     * Gets configurations filtered by category (for future template system)
     * @param category The category to filter by
     * @return Flow of configurations in the specified category
     */
    fun getConfigurationsByCategory(category: String): Flow<List<TimerConfiguration>>
    
    /**
     * Gets configurations filtered by difficulty level (for future template system)
     * @param difficulty The difficulty level (1-5)
     * @return Flow of configurations with the specified difficulty
     */
    fun getConfigurationsByDifficulty(difficulty: Int): Flow<List<TimerConfiguration>>
    
    /**
     * Validates a timer configuration before saving
     * @param configuration The configuration to validate
     * @return Result indicating validation success or failure with error details
     */
    suspend fun validateConfiguration(configuration: TimerConfiguration): Result<Unit>
    
    /**
     * Exports user configurations for backup purposes
     * @return Result with serialized configuration data or error
     */
    suspend fun exportConfigurations(): Result<String>
    
    /**
     * Imports configurations from backup data
     * @param data The serialized configuration data
     * @param replaceExisting Whether to replace existing configurations
     * @return Result indicating success with import count or error
     */
    suspend fun importConfigurations(data: String, replaceExisting: Boolean = false): Result<Int>
}

/**
 * Result class for repository operations that can succeed or fail
 */
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : RepositoryResult<Nothing>()
}

/**
 * Extension function to convert RepositoryResult to Kotlin Result
 */
fun <T> RepositoryResult<T>.toResult(): Result<T> {
    return when (this) {
        is RepositoryResult.Success -> Result.success(data)
        is RepositoryResult.Error -> Result.failure(exception)
    }
}

/**
 * Extension function to convert Kotlin Result to RepositoryResult
 */
fun <T> Result<T>.toRepositoryResult(): RepositoryResult<T> {
    return fold(
        onSuccess = { RepositoryResult.Success(it) },
        onFailure = { RepositoryResult.Error(it) }
    )
}