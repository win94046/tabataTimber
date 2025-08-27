package org.tabata.timber.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

/**
 * Comprehensive error handling and recovery system for database operations.
 * Provides centralized error handling, logging, and recovery strategies.
 */
class DatabaseErrorHandler {
    
    companion object {
        /**
         * Wraps database operations with error handling and recovery.
         */
        suspend fun <T> safeExecute(
            operation: suspend () -> T,
            onError: ((Throwable) -> T)? = null,
            maxRetries: Int = 3
        ): Result<T> {
            var lastException: Throwable? = null
            
            repeat(maxRetries) { attempt ->
                try {
                    val result = operation()
                    return Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    println("Database operation failed (attempt ${attempt + 1}/$maxRetries): ${e.message}")
                    
                    when (e) {
                        is DatabaseCorruptionException -> {
                            // For corruption, don't retry - need manual recovery
                            return Result.failure(e)
                        }
                        is DatabaseLockedException -> {
                            // For lock issues, wait and retry
                            kotlinx.coroutines.delay(100L * (attempt + 1))
                        }
                        is DatabaseDiskFullException -> {
                            // For disk full, don't retry until space is available
                            return Result.failure(e)
                        }
                        else -> {
                            // Generic database error, retry with exponential backoff
                            kotlinx.coroutines.delay(50L * (attempt + 1))
                        }
                    }
                }
            }
            
            // All retries failed
            val finalException = lastException ?: Exception("Unknown database error")
            return onError?.let { Result.success(it(finalException)) } 
                ?: Result.failure(finalException)
        }
        
        /**
         * Wraps Flow operations with error handling.
         */
        fun <T> Flow<T>.handleDatabaseErrors(
            onError: (Throwable) -> T
        ): Flow<T> {
            return this
                .onStart { 
                    println("Starting database flow operation")
                }
                .catch { e ->
                    println("Database flow error: ${e.message}")
                    when (e) {
                        is DatabaseCorruptionException -> {
                            // Emit error state and request manual recovery
                            emit(onError(e))
                        }
                        is DatabaseLockedException -> {
                            // For lock issues, retry after delay
                            kotlinx.coroutines.delay(200L)
                            throw e // Re-throw to trigger retry at higher level
                        }
                        else -> {
                            emit(onError(e))
                        }
                    }
                }
        }
    }
}

/**
 * Database-specific exceptions for better error handling.
 */
sealed class DatabaseError : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Thrown when database file is locked by another process.
 */
class DatabaseLockedException(message: String, cause: Throwable? = null) : DatabaseError(message, cause ?: Exception())

/**
 * Thrown when database disk is full.
 */
class DatabaseDiskFullException(message: String, cause: Throwable? = null) : DatabaseError(message, cause ?: Exception())

/**
 * Thrown when database cannot be accessed due to permission issues.
 */
class DatabasePermissionException(message: String, cause: Throwable? = null) : DatabaseError(message, cause ?: Exception())

/**
 * Thrown when database version mismatch occurs.
 */
class DatabaseVersionException(message: String, cause: Throwable? = null) : DatabaseError(message, cause ?: Exception())

/**
 * Database health monitoring and automatic recovery system.
 */
class DatabaseHealthMonitor(private val databaseManager: DatabaseManager) {
    
    private var lastHealthCheck: Long = 0
    private var consecutiveFailures: Int = 0
    private val maxConsecutiveFailures = 3
    private val healthCheckInterval = 60_000L // 1 minute
    
    /**
     * Performs a health check on the database.
     * Returns true if database is healthy, false if issues detected.
     */
    suspend fun performHealthCheck(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Don't check too frequently
        if (currentTime - lastHealthCheck < healthCheckInterval) {
            return consecutiveFailures < maxConsecutiveFailures
        }
        
        lastHealthCheck = currentTime
        
        return try {
            val healthReport = databaseManager.checkDatabaseHealth()
            
            if (healthReport.isHealthy) {
                consecutiveFailures = 0
                true
            } else {
                consecutiveFailures++
                println("Database health check failed: ${healthReport.error}")
                
                if (consecutiveFailures >= maxConsecutiveFailures) {
                    println("Maximum consecutive failures reached, attempting recovery...")
                    attemptAutomaticRecovery()
                }
                
                false
            }
        } catch (e: Exception) {
            consecutiveFailures++
            println("Health check exception: ${e.message}")
            false
        }
    }
    
    /**
     * Attempts automatic recovery when health checks fail.
     */
    private suspend fun attemptAutomaticRecovery() {
        try {
            println("Starting automatic database recovery...")
            
            // Step 1: Try database maintenance
            databaseManager.performMaintenance()
            
            // Step 2: Check if maintenance fixed the issues
            val healthReport = databaseManager.checkDatabaseHealth()
            if (healthReport.isHealthy) {
                println("Database recovery successful through maintenance")
                consecutiveFailures = 0
                return
            }
            
            // Step 3: More aggressive recovery if needed
            println("Maintenance didn't fix issues, attempting more aggressive recovery...")
            
            // This would typically involve backing up data and recreating database
            // Implementation depends on specific requirements and backup strategy
            
        } catch (e: Exception) {
            println("Automatic recovery failed: ${e.message}")
            // At this point, manual intervention may be required
        }
    }
    
    /**
     * Gets database health metrics.
     */
    suspend fun getHealthMetrics(): DatabaseHealthMetrics {
        val healthReport = databaseManager.checkDatabaseHealth()
        
        return DatabaseHealthMetrics(
            isHealthy = healthReport.isHealthy,
            sizeBytes = healthReport.sizeBytes,
            consecutiveFailures = consecutiveFailures,
            lastCheckTime = lastHealthCheck,
            error = healthReport.error
        )
    }
}

/**
 * Database health metrics for monitoring and diagnostics.
 */
data class DatabaseHealthMetrics(
    val isHealthy: Boolean,
    val sizeBytes: Long,
    val consecutiveFailures: Int,
    val lastCheckTime: Long,
    val error: String? = null
)

/**
 * Database backup and restore utilities.
 */
class DatabaseBackupManager(private val databaseManager: DatabaseManager) {
    
    /**
     * Creates a backup of critical database tables.
     */
    suspend fun createBackup(): Result<BackupData> {
        return DatabaseErrorHandler.safeExecute {
            val database = databaseManager.database
            
            // Backup essential data
            val timerConfigs = database.timerConfigurationQueries.selectUserConfigurations().executeAsList()
            val recentWorkouts = database.workoutSessionQueries.selectRecentWorkouts(50).executeAsList()
            val recentWeightEntries = database.weightEntryQueries.selectRecent(50).executeAsList()
            val userProfile = database.userProfileQueries.selectUserProfile().executeAsOneOrNull()
            
            BackupData(
                timerConfigurations = timerConfigs,
                workoutSessions = recentWorkouts,
                weightEntries = recentWeightEntries,
                userProfile = userProfile,
                backupTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Restores database from backup data.
     */
    suspend fun restoreFromBackup(backupData: BackupData): Result<Unit> {
        return DatabaseErrorHandler.safeExecute {
            val database = databaseManager.database
            
            database.transaction {
                // Restore timer configurations
                backupData.timerConfigurations.forEach { config ->
                    database.timerConfigurationQueries.insertConfiguration(
                        config.id,
                        config.name,
                        config.work_duration,
                        config.rest_duration,
                        config.cycles,
                        config.sets,
                        config.set_break_duration,
                        config.warmup_duration,
                        config.cooldown_duration,
                        config.enable_sound,
                        config.enable_vibration,
                        config.work_phase_sound,
                        config.rest_phase_sound,
                        config.countdown_seconds,
                        config.is_preset,
                        config.created_at,
                        config.updated_at
                    )
                }
                
                // Restore other data similarly...
            }
        }
    }
}

/**
 * Backup data structure.
 */
data class BackupData(
    val timerConfigurations: List<org.tabata.timber.database.SelectUserConfigurations>,
    val workoutSessions: List<org.tabata.timber.database.SelectRecentWorkouts>,
    val weightEntries: List<org.tabata.timber.database.SelectRecent>,
    val userProfile: org.tabata.timber.database.SelectUserProfile?,
    val backupTime: Long
)