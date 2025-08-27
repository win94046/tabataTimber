package org.tabata.timber.data.repositories

import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.repositories.TimerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of TimerRepository
 * In a real app, this would typically use a database like SQLDelight
 */
class TimerRepositoryImpl : TimerRepository {
    
    private val mutex = Mutex()
    private val _workoutSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val _savedConfigs = MutableStateFlow<List<Pair<String, TabataConfig>>>(
        listOf("Standard Tabata" to TabataConfig.standard())
    )
    
    override suspend fun saveWorkoutSession(session: WorkoutSession): Result<Unit> {
        return try {
            mutex.withLock {
                val currentSessions = _workoutSessions.value.toMutableList()
                val existingIndex = currentSessions.indexOfFirst { it.id == session.id }
                
                if (existingIndex != -1) {
                    currentSessions[existingIndex] = session
                } else {
                    currentSessions.add(session)
                }
                
                // Sort by start time (most recent first)
                currentSessions.sortByDescending { it.startTime.epochSeconds }
                _workoutSessions.value = currentSessions
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getWorkoutSession(id: String): Result<WorkoutSession?> {
        return try {
            val session = _workoutSessions.value.find { it.id == id }
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> {
        return _workoutSessions.asStateFlow()
    }
    
    override suspend fun deleteWorkoutSession(id: String): Result<Unit> {
        return try {
            mutex.withLock {
                val currentSessions = _workoutSessions.value.toMutableList()
                currentSessions.removeAll { it.id == id }
                _workoutSessions.value = currentSessions
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveTabataConfig(config: TabataConfig, name: String): Result<Unit> {
        return try {
            mutex.withLock {
                val currentConfigs = _savedConfigs.value.toMutableList()
                val existingIndex = currentConfigs.indexOfFirst { it.first == name }
                
                if (existingIndex != -1) {
                    currentConfigs[existingIndex] = name to config
                } else {
                    currentConfigs.add(name to config)
                }
                
                _savedConfigs.value = currentConfigs
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSavedConfigs(): Flow<List<Pair<String, TabataConfig>>> {
        return _savedConfigs.asStateFlow()
    }
    
    override suspend fun deleteConfig(name: String): Result<Unit> {
        return try {
            mutex.withLock {
                val currentConfigs = _savedConfigs.value.toMutableList()
                currentConfigs.removeAll { it.first == name }
                _savedConfigs.value = currentConfigs
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}