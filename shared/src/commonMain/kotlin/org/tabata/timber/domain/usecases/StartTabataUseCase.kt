package org.tabata.timber.domain.usecases

import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.repositories.TimerRepository
import kotlinx.datetime.Clock

/**
 * Use case for starting a new Tabata workout session
 */
class StartTabataUseCase(
    private val timerRepository: TimerRepository
) {
    
    suspend operator fun invoke(config: TabataConfig): Result<WorkoutSession> {
        return try {
            val session = WorkoutSession(
                id = generateSessionId(),
                config = config,
                startTime = Clock.System.now(),
                currentState = TimerState.IDLE,
                remainingTime = if (config.warmupDuration > 0) config.warmupDuration else config.workDuration
            )
            
            // Save the session
            timerRepository.saveWorkoutSession(session).fold(
                onSuccess = { 
                    Result.success(session) 
                },
                onFailure = { error -> 
                    Result.failure(error) 
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateSessionId(): String {
        return "session_${Clock.System.now().epochSeconds}_${(0..999).random()}"
    }
}