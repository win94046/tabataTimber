package org.tabata.timber.data.database

// TODO: Re-enable when SQLDelight plugin is fixed
// This file is temporarily commented out due to SQLDelight plugin issues

/*
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.tabata.timber.database.TabataTimberDatabase

/**
 * Android implementation of DatabaseDriverFactory.
 * Uses AndroidSqliteDriver for SQLite operations on Android platform.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    
    /**
     * Creates an Android-specific SQLite driver.
     * @param databaseName The name of the database file
     * @return AndroidSqliteDriver instance configured with migration support
     */
    actual fun createDriver(databaseName: String): SqlDriver {
        return AndroidSqliteDriver(
            schema = TabataTimberDatabase.Schema,
            context = context,
            name = databaseName,
            callback = AndroidSqliteDriver.Callback(
                schema = TabataTimberDatabase.Schema,
                // Migration callbacks can be added here if needed
                // For example, to handle data migration between versions:
                // AfterVersion(2) { driver ->
                //     // Migrate data after version 2 upgrade
                //     driver.execute(null, "UPDATE timer_configurations SET difficulty_level = 1 WHERE difficulty_level IS NULL", 0)
                // }
            )
        )
    }
}

/**
 * Android-specific database configuration and utilities.
 */
object AndroidDatabaseConfig {
    
    /**
     * Default database name for Android.
     */
    const val DEFAULT_DATABASE_NAME = "tabata_timber.db"
    
    /**
     * Creates a DatabaseManager instance configured for Android.
     * @param context Android application context
     * @param databaseName Optional custom database name
     * @return Configured DatabaseManager instance
     */
    fun createDatabaseManager(
        context: Context,
        databaseName: String = DEFAULT_DATABASE_NAME
    ): DatabaseManager {
        val factory = DatabaseDriverFactory(context.applicationContext)
        return DatabaseManager(factory)
    }
    
    /**
     * Enables foreign key constraints for Android SQLite.
     * This should be called during database initialization.
     */
    fun enableForeignKeys(driver: SqlDriver) {
        try {
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        } catch (e: Exception) {
            println("Failed to enable foreign keys: ${e.message}")
        }
    }
    
    /**
     * Configures SQLite performance settings for Android.
     * These optimizations can improve database performance on mobile devices.
     */
    fun optimizeForMobile(driver: SqlDriver) {
        try {
            // Enable WAL mode for better concurrency
            driver.execute(null, "PRAGMA journal_mode = WAL", 0)
            
            // Set reasonable cache size (negative value = KB, positive = pages)
            driver.execute(null, "PRAGMA cache_size = -2000", 0)  // 2MB cache
            
            // Optimize for mobile storage
            driver.execute(null, "PRAGMA temp_store = MEMORY", 0)
            
            // Set synchronous mode for balance between performance and safety
            driver.execute(null, "PRAGMA synchronous = NORMAL", 0)
            
            // Enable automatic indexing
            driver.execute(null, "PRAGMA automatic_index = ON", 0)
            
        } catch (e: Exception) {
            println("Failed to apply mobile optimizations: ${e.message}")
        }
    }
}

/**
 * Android-specific database migration helper.
 * Handles platform-specific migration scenarios.
 */
class AndroidDatabaseMigrationHelper {
    
    companion object {
        /**
         * Handles Android-specific migration scenarios.
         * For example, migrating from older app versions or handling device upgrades.
         */
        fun handleAndroidMigration(context: Context, driver: SqlDriver, fromVersion: Int, toVersion: Int) {
            try {
                when {
                    fromVersion < 1 -> {
                        // First time installation, no migration needed
                        println("First time database creation")
                    }
                    fromVersion == 1 && toVersion >= 2 -> {
                        // Migration from version 1 to 2+
                        migrateFromV1ToV2(driver)
                    }
                    fromVersion == 2 && toVersion >= 3 -> {
                        // Migration from version 2 to 3+
                        migrateFromV2ToV3(driver)
                    }
                    // Add more migration cases as needed
                }
            } catch (e: Exception) {
                throw DatabaseMigrationException(
                    "Android migration failed from version $fromVersion to $toVersion: ${e.message}",
                    MigrationStrategy.RECREATE,
                    e
                )
            }
        }
        
        private fun migrateFromV1ToV2(driver: SqlDriver) {
            // Example: Add default values for new columns added in v2
            driver.execute(
                null,
                "UPDATE timer_configurations SET template_category = 'CUSTOM' WHERE template_category IS NULL",
                0
            )
        }
        
        private fun migrateFromV2ToV3(driver: SqlDriver) {
            // Example: Initialize heart rate zones for existing users
            driver.execute(
                null,
                "INSERT OR IGNORE INTO heart_rate_zones (id, created_at, updated_at) VALUES (1, ?, ?)",
                2
            ) {
                bindLong(0, System.currentTimeMillis() / 1000)
                bindLong(1, System.currentTimeMillis() / 1000)
            }
        }
    }
}
*/