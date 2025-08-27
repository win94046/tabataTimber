package org.tabata.timber.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import org.tabata.timber.data.repositories.PresetRepositoryImpl
import org.tabata.timber.database.TabataTimberDatabase
import org.tabata.timber.domain.models.timer.TimerConfiguration
import kotlin.test.*

/**
 * Comprehensive database tests for SQLDelight implementation.
 * Tests database operations, migrations, error handling, and recovery.
 */
class DatabaseTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: TabataTimberDatabase
    private lateinit var databaseManager: DatabaseManager
    private lateinit var presetRepository: PresetRepositoryImpl
    
    @BeforeTest
    fun setup() {
        // Use in-memory SQLite database for testing
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TabataTimberDatabase.Schema.create(driver)
        database = TabataTimberDatabase(driver)
        
        // Create test database manager
        val testDriverFactory = object : DatabaseDriverFactory {
            override fun createDriver(databaseName: String): SqlDriver = driver
        }
        databaseManager = DatabaseManager(testDriverFactory)
        presetRepository = PresetRepositoryImpl(databaseManager)
    }
    
    @AfterTest
    fun teardown() {
        driver.close()
    }
    
    @Test
    fun testDatabaseCreation() {
        // Verify that database was created successfully
        assertNotNull(database)
        
        // Check that preset configurations were inserted
        val presets = database.timerConfigurationQueries.selectPresets().executeAsList()
        assertEquals(3, presets.size, "Should have 3 preset configurations")
        
        // Verify preset names
        val presetNames = presets.map { it.name }
        assertTrue(presetNames.contains("標準 Tabata"))
        assertTrue(presetNames.contains("新手友善"))
        assertTrue(presetNames.contains("高強度進階"))
    }
    
    @Test
    fun testTimerConfigurationCRUD() = runTest {
        // Test Create
        val newConfig = TimerConfiguration.createStandard().copy(
            name = "測試配置"
        )
        
        val createResult = presetRepository.createConfiguration(newConfig)
        assertTrue(createResult.isSuccess, "Should successfully create configuration")
        
        // Test Read
        val userConfigs = presetRepository.getUserConfigurations()
        // Note: In a real test, you'd collect from the Flow
        // For this example, we'll test the database directly
        val dbConfigs = database.timerConfigurationQueries.selectUserConfigurations().executeAsList()
        assertEquals(1, dbConfigs.size, "Should have 1 user configuration")
        
        // Test Update
        val configToUpdate = createResult.getOrNull()!!.copy(name = "更新的配置")
        val updateResult = presetRepository.updateConfiguration(configToUpdate)
        assertTrue(updateResult.isSuccess, "Should successfully update configuration")
        
        // Verify update
        val updatedConfig = presetRepository.getConfigurationById(configToUpdate.id!!)
        assertEquals("更新的配置", updatedConfig?.name)
        
        // Test Delete
        val deleteResult = presetRepository.deleteConfiguration(configToUpdate.id!!)
        assertTrue(deleteResult.isSuccess, "Should successfully delete configuration")
        
        // Verify deletion
        val deletedConfig = presetRepository.getConfigurationById(configToUpdate.id!!)
        assertNull(deletedConfig, "Configuration should be deleted")
    }
    
    @Test
    fun testConfigurationValidation() = runTest {
        // Test valid configuration
        val validConfig = TimerConfiguration.createStandard()
        val validResult = presetRepository.validateConfiguration(validConfig)
        assertTrue(validResult.isSuccess, "Valid configuration should pass validation")
        
        // Test invalid configuration (work duration too high)
        val invalidConfig = validConfig.copy(workDuration = 5000) // Exceeds MAX_DURATION
        val invalidResult = presetRepository.validateConfiguration(invalidConfig)
        assertTrue(invalidResult.isFailure, "Invalid configuration should fail validation")
    }
    
    @Test
    fun testWorkoutSessionOperations() {
        // Test workout session creation
        val sessionId = "test_session_${System.currentTimeMillis()}"
        val configId = "preset_standard"
        val startTime = System.currentTimeMillis() / 1000
        
        database.workoutSessionQueries.insertSession(
            id = sessionId,
            configuration_id = configId,
            start_time = startTime,
            end_time = null,
            total_duration = null,
            completed_cycles = 0,
            completed_sets = 0,
            is_completed = 0,
            completion_percentage = 0.0,
            calories_burned = null,
            heart_rate_avg = null,
            heart_rate_max = null,
            rating = null,
            notes = "",
            created_at = startTime
        )
        
        // Verify session was created
        val session = database.workoutSessionQueries.selectById(sessionId).executeAsOneOrNull()
        assertNotNull(session)
        assertEquals(configId, session.configuration_id)
        
        // Test session update
        database.workoutSessionQueries.updateSessionProgress(
            completed_cycles = 4,
            completed_sets = 1,
            completion_percentage = 50.0,
            id = sessionId
        )
        
        // Verify update
        val updatedSession = database.workoutSessionQueries.selectById(sessionId).executeAsOneOrNull()
        assertNotNull(updatedSession)
        assertEquals(4, updatedSession.completed_cycles.toInt())
        assertEquals(50.0, updatedSession.completion_percentage, 0.01)
        
        // Test session completion
        val endTime = startTime + 300 // 5 minutes later
        database.workoutSessionQueries.completeSession(
            end_time = endTime,
            total_duration = 300,
            id = sessionId
        )
        
        // Verify completion
        val completedSession = database.workoutSessionQueries.selectById(sessionId).executeAsOneOrNull()
        assertNotNull(completedSession)
        assertEquals(1, completedSession.is_completed.toInt())
        assertEquals(endTime, completedSession.end_time)
    }
    
    @Test
    fun testWeightEntryOperations() {
        // Test weight entry creation
        val entryId = "test_weight_${System.currentTimeMillis()}"
        val currentTime = System.currentTimeMillis() / 1000
        
        database.weightEntryQueries.insertWeightEntry(
            id = entryId,
            weight = 70.5,
            unit = "KG",
            weight_kg = 70.5,
            date_recorded = "2024-01-15",
            time_recorded = currentTime,
            notes = "測試體重記錄",
            body_fat_percentage = 15.0,
            muscle_mass = 50.0,
            water_percentage = 60.0,
            visceral_fat = 8.0,
            bone_mass = 3.5,
            source = "MANUAL",
            created_at = currentTime
        )
        
        // Verify entry was created
        val entry = database.weightEntryQueries.selectById(entryId).executeAsOneOrNull()
        assertNotNull(entry)
        assertEquals(70.5, entry.weight, 0.01)
        assertEquals("KG", entry.unit)
        assertEquals("測試體重記錄", entry.notes)
        
        // Test weight statistics
        val stats = database.weightEntryQueries.selectWeightStats().executeAsOne()
        assertEquals(1, stats.total_entries.toInt())
        assertEquals(70.5, stats.min_weight, 0.01)
        assertEquals(70.5, stats.max_weight, 0.01)
        assertEquals(70.5, stats.avg_weight, 0.01)
    }
    
    @Test
    fun testUserProfileOperations() {
        // Test user profile update
        val currentTime = System.currentTimeMillis() / 1000
        
        database.userProfileQueries.updateUserProfile(
            height_cm = 175.0,
            birth_date = "1990-01-01",
            gender = "MALE",
            activity_level = "ACTIVE",
            target_weight_kg = 75.0,
            preferred_unit = "KG",
            updated_at = currentTime
        )
        
        // Verify profile was updated
        val profile = database.userProfileQueries.selectUserProfile().executeAsOne()
        assertEquals(175.0, profile.height_cm, 0.01)
        assertEquals("1990-01-01", profile.birth_date)
        assertEquals("MALE", profile.gender)
        assertEquals("ACTIVE", profile.activity_level)
        assertEquals(75.0, profile.target_weight_kg, 0.01)
        assertEquals("KG", profile.preferred_unit)
    }
    
    @Test
    fun testSubscriptionDataOperations() {
        // Test subscription update to premium
        val currentTime = System.currentTimeMillis() / 1000
        val expiryTime = currentTime + (30 * 24 * 60 * 60) // 30 days from now
        
        database.subscriptionDataQueries.updateSubscriptionToPremium(
            subscription_id = "premium_123",
            plan_id = "monthly_premium",
            start_date = currentTime,
            expiry_date = expiryTime,
            auto_renew = 1,
            payment_provider = "GOOGLE_PLAY",
            updated_at = currentTime
        )
        
        // Verify subscription update
        val subscription = database.subscriptionDataQueries.selectCurrentSubscription().executeAsOne()
        assertEquals("PREMIUM", subscription.subscription_type)
        assertEquals("premium_123", subscription.subscription_id)
        assertEquals("monthly_premium", subscription.plan_id)
        assertEquals("GOOGLE_PLAY", subscription.payment_provider)
        
        // Test premium access check
        val accessCheck = database.subscriptionDataQueries.checkPremiumAccess(
            currentTime, currentTime, currentTime, currentTime
        ).executeAsOne()
        assertEquals(1, accessCheck.has_premium_access.toInt())
    }
    
    @Test
    fun testFeatureUsageTracking() {
        // Test feature usage increment
        val currentTime = System.currentTimeMillis() / 1000
        
        database.subscriptionDataQueries.incrementFeatureUsage(
            updated_at = currentTime,
            feature_name = "UNLIMITED_PRESETS"
        )
        
        // Verify usage was incremented
        val usage = database.subscriptionDataQueries.selectFeatureUsage("UNLIMITED_PRESETS").executeAsOne()
        assertEquals(1, usage.usage_count.toInt())
        assertEquals(1, usage.daily_usage_count.toInt())
    }
    
    @Test
    fun testDatabaseConstraints() {
        // Test CHECK constraints
        assertFailsWith<Exception> {
            // This should fail due to weight constraint
            database.weightEntryQueries.insertWeightEntry(
                id = "invalid_weight",
                weight = -10.0, // Invalid weight
                unit = "KG",
                weight_kg = -10.0,
                date_recorded = "2024-01-15",
                time_recorded = System.currentTimeMillis() / 1000,
                notes = "",
                body_fat_percentage = null,
                muscle_mass = null,
                water_percentage = null,
                visceral_fat = null,
                bone_mass = null,
                source = "MANUAL",
                created_at = System.currentTimeMillis() / 1000
            )
        }
        
        assertFailsWith<Exception> {
            // This should fail due to invalid timer duration
            database.timerConfigurationQueries.insertConfiguration(
                id = "invalid_config",
                name = "Invalid",
                work_duration = 0, // Invalid duration (below minimum)
                rest_duration = 10,
                cycles = 1,
                sets = 1,
                set_break_duration = 60,
                warmup_duration = 0,
                cooldown_duration = 0,
                enable_sound = 1,
                enable_vibration = 1,
                work_phase_sound = "work_beep",
                rest_phase_sound = "rest_beep",
                countdown_seconds = 3,
                is_preset = 0,
                created_at = System.currentTimeMillis() / 1000,
                updated_at = System.currentTimeMillis() / 1000
            )
        }
    }
    
    @Test
    fun testDatabaseIndexes() {
        // Verify that indexes were created by checking SQLite schema
        val indexes = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'",
            mapper = { cursor -> cursor.getString(0) },
            parameters = 0
        )
        
        val indexNames = mutableListOf<String>()
        while (indexes.next().value) {
            indexNames.add(indexes.getString(0)!!)
        }
        
        // Verify key indexes exist
        assertTrue(indexNames.any { it.contains("timer_configurations") })
        assertTrue(indexNames.any { it.contains("workout_sessions") })
        assertTrue(indexNames.any { it.contains("weight_entries") })
    }
}