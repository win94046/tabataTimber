package org.tabata.timber.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import org.tabata.timber.database.TabataTimberDatabase

/**
 * iOS implementation of DatabaseDriverFactory.
 * Uses NativeSqliteDriver for SQLite operations on iOS platform.
 */
actual class DatabaseDriverFactory {
    
    /**
     * Creates an iOS-specific SQLite driver.
     * @param databaseName The name of the database file
     * @return NativeSqliteDriver instance configured with migration support and iOS optimizations
     */
    actual fun createDriver(databaseName: String): SqlDriver {
        return NativeSqliteDriver(
            schema = TabataTimberDatabase.Schema,
            name = databaseName,
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(
                        foreignKeyConstraints = true,
                        busyTimeout = 30000,  // 30 seconds timeout for busy database
                        basePath = getIOSDatabasePath()
                    )
                )
            }
        )
    }
    
    /**
     * Gets the appropriate database path for iOS.
     * Uses the app's Documents directory for database storage.
     */
    private fun getIOSDatabasePath(): String? {
        // Return null to use default path, or specify a custom path if needed
        // On iOS, SQLite databases are typically stored in the Documents directory
        return null
    }
}

/**
 * iOS-specific database configuration and utilities.
 */
object IOSDatabaseConfig {
    
    /**
     * Default database name for iOS.
     */
    const val DEFAULT_DATABASE_NAME = "tabata_timber.db"
    
    /**
     * Creates a DatabaseManager instance configured for iOS.
     * @param databaseName Optional custom database name
     * @return Configured DatabaseManager instance
     */
    fun createDatabaseManager(databaseName: String = DEFAULT_DATABASE_NAME): DatabaseManager {
        val factory = DatabaseDriverFactory()
        return DatabaseManager(factory)
    }
    
    /**
     * Configures SQLite performance settings optimized for iOS.
     * These settings are tuned for iOS device characteristics and usage patterns.
     */
    fun optimizeForIOS(driver: SqlDriver) {
        try {
            // Enable WAL mode for better concurrency and crash safety
            driver.execute(null, "PRAGMA journal_mode = WAL", 0)
            
            // Set cache size appropriate for mobile devices (2MB)
            driver.execute(null, "PRAGMA cache_size = -2000", 0)
            
            // Use memory for temporary storage to reduce disk I/O
            driver.execute(null, "PRAGMA temp_store = MEMORY", 0)
            
            // Set synchronous mode for balance between performance and data safety
            driver.execute(null, "PRAGMA synchronous = NORMAL", 0)
            
            // Enable query planning optimization
            driver.execute(null, "PRAGMA optimize", 0)
            
            // Set a reasonable timeout for database locks
            driver.execute(null, "PRAGMA busy_timeout = 30000", 0)
            
            // Enable automatic index creation for better query performance
            driver.execute(null, "PRAGMA automatic_index = ON", 0)
            
        } catch (e: Exception) {
            println("Failed to apply iOS optimizations: ${e.message}")
        }
    }
    
    /**
     * Enables iOS-specific security features.
     * Configures database encryption and secure storage if needed.
     */
    fun enableIOSSecurity(driver: SqlDriver) {
        try {
            // Enable secure deletion of data
            driver.execute(null, "PRAGMA secure_delete = ON", 0)
            
            // Note: For full database encryption on iOS, consider using SQLCipher
            // This would require additional dependencies and configuration
            
        } catch (e: Exception) {
            println("Failed to enable iOS security features: ${e.message}")
        }
    }
}

/**
 * iOS-specific database migration helper.
 * Handles platform-specific migration scenarios including iOS app updates and device migrations.
 */
class IOSDatabaseMigrationHelper {
    
    companion object {
        /**
         * Handles iOS-specific migration scenarios.
         * This includes migrations during iOS app updates, device transfers, and iCloud sync.
         */
        fun handleIOSMigration(driver: SqlDriver, fromVersion: Int, toVersion: Int) {
            try {
                when {
                    fromVersion < 1 -> {
                        // First time installation
                        println("iOS: First time database creation")
                        initializeIOSDefaults(driver)
                    }
                    fromVersion == 1 && toVersion >= 2 -> {
                        // Migration from version 1 to 2+
                        migrateFromV1ToV2(driver)
                    }
                    fromVersion == 2 && toVersion >= 3 -> {
                        // Migration from version 2 to 3+
                        migrateFromV2ToV3(driver)
                    }
                }
                
                // Apply iOS-specific optimizations after migration
                IOSDatabaseConfig.optimizeForIOS(driver)
                IOSDatabaseConfig.enableIOSSecurity(driver)
                
            } catch (e: Exception) {
                throw DatabaseMigrationException(
                    "iOS migration failed from version $fromVersion to $toVersion: ${e.message}",
                    MigrationStrategy.RECREATE,
                    e
                )
            }
        }
        
        /**
         * Initializes iOS-specific default settings during first app launch.
         */
        private fun initializeIOSDefaults(driver: SqlDriver) {
            try {
                // Set iOS-specific default preferences
                // For example, iOS users might prefer imperial units by default in certain regions
                val isUSRegion = isUSRegion()
                val defaultUnit = if (isUSRegion) "LB" else "KG"
                
                driver.execute(
                    null,
                    "UPDATE user_profile SET preferred_unit = ? WHERE id = 1",
                    1
                ) {
                    bindString(0, defaultUnit)
                }
                
                // Enable iOS-specific features by default
                driver.execute(
                    null,
                    "INSERT OR IGNORE INTO feature_usage (feature_name, created_at, updated_at) VALUES (?, ?, ?)",
                    3
                ) {
                    bindString(0, "HEALTHKIT_INTEGRATION")
                    bindLong(1, System.currentTimeMillis() / 1000)
                    bindLong(2, System.currentTimeMillis() / 1000)
                }
                
            } catch (e: Exception) {
                println("Failed to initialize iOS defaults: ${e.message}")
            }
        }
        
        private fun migrateFromV1ToV2(driver: SqlDriver) {
            // iOS-specific migration logic for v1 to v2
            driver.execute(
                null,
                "UPDATE timer_configurations SET template_category = 'CUSTOM' WHERE template_category IS NULL",
                0
            )
        }
        
        private fun migrateFromV2ToV3(driver: SqlDriver) {
            // iOS-specific migration logic for v2 to v3
            // Initialize heart rate zones with iOS HealthKit integration consideration
            driver.execute(
                null,
                "INSERT OR IGNORE INTO heart_rate_zones (id, auto_calculate, created_at, updated_at) VALUES (1, 1, ?, ?)",
                2
            ) {
                bindLong(0, System.currentTimeMillis() / 1000)
                bindLong(1, System.currentTimeMillis() / 1000)
            }
        }
        
        /**
         * Determines if the device is in a US region for unit preferences.
         * This is a simplified implementation; in practice, you'd use iOS locale APIs.
         */
        private fun isUSRegion(): Boolean {
            // Simplified implementation - in real iOS app, use NSLocale
            return false  // Default to metric
        }
    }
}

/**
 * iOS-specific backup and restore utilities.
 * Handles iCloud backup integration and device transfer scenarios.
 */
object IOSBackupManager {
    
    /**
     * Prepares database for iOS backup to iCloud.
     * Ensures sensitive data is properly handled during backup.
     */
    fun prepareForBackup(driver: SqlDriver) {
        try {
            // Perform any necessary cleanup before backup
            driver.execute(null, "VACUUM", 0)
            
            // Note: In a real implementation, you might want to encrypt
            // or exclude certain sensitive data from iCloud backups
            
        } catch (e: Exception) {
            println("Failed to prepare database for iOS backup: ${e.message}")
        }
    }
    
    /**
     * Handles database restoration from iOS backup.
     * Validates and repairs data after restore from iCloud backup.
     */
    fun handleRestoreFromBackup(driver: SqlDriver): Boolean {
        return try {
            // Verify database integrity after restore
            val integrityCheck = driver.executeQuery(
                null,
                "PRAGMA integrity_check",
                mapper = { cursor -> cursor.getString(0) },
                parameters = 0
            )
            
            val isIntact = integrityCheck.value == "ok"
            
            if (isIntact) {
                // Apply any necessary post-restore fixes
                driver.execute(null, "PRAGMA optimize", 0)
                println("Database successfully restored from iOS backup")
            } else {
                println("Database corruption detected after restore from backup")
            }
            
            isIntact
        } catch (e: Exception) {
            println("Failed to validate database after restore: ${e.message}")
            false
        }
    }
}