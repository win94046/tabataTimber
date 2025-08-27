package org.tabata.timber.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.tabata.timber.data.database.DatabaseManager
import org.tabata.timber.domain.models.timer.TimerConfiguration
import org.tabata.timber.domain.repositories.PresetRepository
import org.tabata.timber.database.TabataTimberDatabase

/**
 * SQLDelight implementation of PresetRepository.
 * Manages timer configurations using SQLDelight database operations.
 */
class PresetRepositoryImpl(
    private val databaseManager: DatabaseManager
) : PresetRepository {
    
    private val database: TabataTimberDatabase get() = databaseManager.database
    private val timerConfigQueries get() = database.timerConfigurationQueries
    
    override fun getAllConfigurations(): Flow<List<TimerConfiguration>> {
        return timerConfigQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { dbList -> dbList.map { it.toTimerConfiguration() } }
    }
    
    override fun getPresetConfigurations(): Flow<List<TimerConfiguration>> {
        return timerConfigQueries.selectPresets()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { dbList -> dbList.map { it.toTimerConfiguration() } }
    }
    
    override fun getUserConfigurations(): Flow<List<TimerConfiguration>> {
        return timerConfigQueries.selectUserConfigurations()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { dbList -> dbList.map { it.toTimerConfiguration() } }
    }
    
    override suspend fun getConfigurationById(id: String): TimerConfiguration? = withContext(Dispatchers.IO) {
        try {
            timerConfigQueries.selectById(id).executeAsOneOrNull()?.toTimerConfiguration()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun createConfiguration(configuration: TimerConfiguration): Result<TimerConfiguration> = 
        withContext(Dispatchers.IO) {
            try {
                // Validate configuration first
                val validationResult = validateConfiguration(configuration)
                if (validationResult.isFailure) {
                    return@withContext validationResult
                }
                
                val timestamp = System.currentTimeMillis() / 1000
                timerConfigQueries.insertConfiguration(
                    id = configuration.id ?: generateConfigurationId(),
                    name = configuration.name,
                    work_duration = configuration.workDuration.toLong(),
                    rest_duration = configuration.restDuration.toLong(),
                    cycles = configuration.cycles.toLong(),
                    sets = configuration.sets.toLong(),
                    set_break_duration = configuration.setBreakDuration.toLong(),
                    warmup_duration = configuration.warmupDuration.toLong(),
                    cooldown_duration = configuration.cooldownDuration.toLong(),
                    enable_sound = if (configuration.enableSound) 1L else 0L,
                    enable_vibration = if (configuration.enableVibration) 1L else 0L,
                    work_phase_sound = configuration.workPhaseSound,
                    rest_phase_sound = configuration.restPhaseSound,
                    countdown_seconds = configuration.countdownSeconds.toLong(),
                    is_preset = 0L, // User configurations are never presets
                    created_at = timestamp,
                    updated_at = timestamp
                )
                
                Result.success(configuration)
            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to create configuration: ${e.message}", e))
            }
        }
    
    override suspend fun updateConfiguration(configuration: TimerConfiguration): Result<TimerConfiguration> = 
        withContext(Dispatchers.IO) {
            try {
                // Validate configuration first
                val validationResult = validateConfiguration(configuration)
                if (validationResult.isFailure) {
                    return@withContext validationResult
                }
                
                val timestamp = System.currentTimeMillis() / 1000
                timerConfigQueries.updateConfiguration(
                    name = configuration.name,
                    work_duration = configuration.workDuration.toLong(),
                    rest_duration = configuration.restDuration.toLong(),
                    cycles = configuration.cycles.toLong(),
                    sets = configuration.sets.toLong(),
                    set_break_duration = configuration.setBreakDuration.toLong(),
                    warmup_duration = configuration.warmupDuration.toLong(),
                    cooldown_duration = configuration.cooldownDuration.toLong(),
                    enable_sound = if (configuration.enableSound) 1L else 0L,
                    enable_vibration = if (configuration.enableVibration) 1L else 0L,
                    work_phase_sound = configuration.workPhaseSound,
                    rest_phase_sound = configuration.restPhaseSound,
                    countdown_seconds = configuration.countdownSeconds.toLong(),
                    updated_at = timestamp,
                    id = configuration.id ?: return@withContext Result.failure(
                        IllegalArgumentException("Configuration ID is required for updates")
                    )
                )
                
                Result.success(configuration)
            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to update configuration: ${e.message}", e))
            }
        }
    
    override suspend fun deleteConfiguration(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            timerConfigQueries.deleteConfiguration(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to delete configuration: ${e.message}", e))
        }
    }
    
    override suspend fun duplicateConfiguration(id: String, newName: String?): Result<TimerConfiguration> = 
        withContext(Dispatchers.IO) {
            try {
                val original = getConfigurationById(id) 
                    ?: return@withContext Result.failure(IllegalArgumentException("Configuration not found"))
                
                val duplicate = original.copy(
                    id = generateConfigurationId(),
                    name = newName ?: "${original.name} (副本)"
                )
                
                createConfiguration(duplicate)
            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to duplicate configuration: ${e.message}", e))
            }
        }
    
    override suspend fun getUserConfigurationCount(): Int = withContext(Dispatchers.IO) {
        try {
            timerConfigQueries.countUserConfigurations().executeAsOne().toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    override suspend fun searchConfigurations(query: String): List<TimerConfiguration> = withContext(Dispatchers.IO) {
        try {
            // Simple search implementation - in practice you'd use FTS or more sophisticated search
            getAllConfigurations().map { configs ->
                configs.filter { config ->
                    config.name.contains(query, ignoreCase = true) ||
                    config.workPhaseSound.contains(query, ignoreCase = true) ||
                    config.restPhaseSound.contains(query, ignoreCase = true)
                }
            }.first() // Convert Flow to List for this implementation
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun getConfigurationsByCategory(category: String): Flow<List<TimerConfiguration>> {
        // Implementation would depend on future template system
        return getAllConfigurations() // Placeholder
    }
    
    override fun getConfigurationsByDifficulty(difficulty: Int): Flow<List<TimerConfiguration>> {
        // Implementation would depend on future template system
        return getAllConfigurations() // Placeholder
    }
    
    override suspend fun validateConfiguration(configuration: TimerConfiguration): Result<Unit> {
        return try {
            when (val validation = configuration.validate()) {
                is TimerConfiguration.ValidationResult.Valid -> Result.success(Unit)
                is TimerConfiguration.ValidationResult.Invalid -> 
                    Result.failure(ValidationException("配置驗證失敗: ${validation.errors.joinToString(", ")}"))
            }
        } catch (e: Exception) {
            Result.failure(ValidationException("配置驗證錯誤: ${e.message}", e))
        }
    }
    
    override suspend fun exportConfigurations(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Implementation would serialize configurations to JSON
            Result.success("[]") // Placeholder
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to export configurations: ${e.message}", e))
        }
    }
    
    override suspend fun importConfigurations(data: String, replaceExisting: Boolean): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                // Implementation would deserialize and import configurations
                Result.success(0) // Placeholder
            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to import configurations: ${e.message}", e))
            }
        }
    
    private fun generateConfigurationId(): String {
        return "config_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Extension function to convert database row to TimerConfiguration
 */
private fun org.tabata.timber.database.SelectAll.toTimerConfiguration(): TimerConfiguration {
    return TimerConfiguration(
        workDuration = work_duration.toInt(),
        restDuration = rest_duration.toInt(),
        cycles = cycles.toInt(),
        sets = sets.toInt(),
        setBreakDuration = set_break_duration.toInt(),
        warmupDuration = warmup_duration.toInt(),
        cooldownDuration = cooldown_duration.toInt(),
        enableSound = enable_sound == 1L,
        enableVibration = enable_vibration == 1L,
        workPhaseSound = work_phase_sound,
        restPhaseSound = rest_phase_sound,
        countdownSeconds = countdown_seconds.toInt()
    ).copy(id = id, name = name)
}

/**
 * Database operation exceptions
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ValidationException(message: String, cause: Throwable? = null) : Exception(message, cause)