package org.tabata.timber.core.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.tabata.timber.core.timer.TimerEngine
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.timer.PhaseType

/**
 * Integrates audio system with timer engine to provide automatic sound playback
 * during timer sessions based on timer events and phase transitions
 */
class TimerAudioIntegration(
    private val timerEngine: TimerEngine,
    private val audioEventHandler: AudioEventHandler
) {
    
    private var integrationScope: CoroutineScope? = null
    private var isActive = false
    
    // Track previous states to detect transitions
    private var previousPhase: PhaseType? = null
    private var previousState: TimerState? = null
    private var previousRemainingTime = 0
    
    // Countdown and warning timers
    private var countdownJob: Job? = null
    private var warningJob: Job? = null
    
    /**
     * Starts audio integration with timer engine
     */
    suspend fun start(): Result<Unit> {
        return try {
            if (isActive) {
                return Result.success(Unit)
            }
            
            // Start audio event handler
            audioEventHandler.start().getOrThrow()
            
            // Create integration scope
            integrationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            
            // Start monitoring timer events
            startTimerMonitoring()
            
            isActive = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to start timer audio integration: ${e.message}", e))
        }
    }
    
    /**
     * Stops audio integration
     */
    suspend fun stop(): Result<Unit> {
        return try {
            if (!isActive) {
                return Result.success(Unit)
            }
            
            // Cancel all jobs
            countdownJob?.cancel()
            warningJob?.cancel()
            integrationScope?.cancel()
            
            // Stop audio event handler
            audioEventHandler.stop()
            
            isActive = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to stop timer audio integration: ${e.message}", e))
        }
    }
    
    /**
     * Updates audio settings
     */
    suspend fun updateAudioSettings(settings: AudioSettings): Result<Unit> {
        return audioEventHandler.updateSettings(settings)
    }
    
    /**
     * Gets current audio settings
     */
    fun getCurrentAudioSettings(): AudioSettings {
        return audioEventHandler.settings.value
    }
    
    private fun startTimerMonitoring() {
        val scope = integrationScope ?: return
        
        // Monitor timer state changes
        scope.launch {
            timerEngine.timerState.collect { newState ->
                handleStateChange(previousState, newState)
                previousState = newState
            }
        }
        
        // Monitor phase changes
        scope.launch {
            timerEngine.currentPhase.collect { newPhase ->
                handlePhaseChange(previousPhase, newPhase)
                previousPhase = newPhase
            }
        }
        
        // Monitor remaining time for countdown and warning sounds
        scope.launch {
            combine(
                timerEngine.remainingTime,
                timerEngine.currentPhase,
                timerEngine.timerState
            ) { remainingTime, currentPhase, timerState ->
                Triple(remainingTime, currentPhase, timerState)
            }.collect { (remainingTime, currentPhase, timerState) ->
                if (timerState == TimerState.RUNNING) {
                    handleTimingEvents(remainingTime, currentPhase)
                }
                previousRemainingTime = remainingTime
            }
        }
        
        // Monitor session completion
        scope.launch {
            timerEngine.currentSession.collect { session ->
                if (session == null && previousState == TimerState.FINISHED) {
                    val completionEvent = AudioEvent.SessionCompleted(
                        totalDurationMs = timerEngine.elapsedTimeMs.value,
                        cyclesCompleted = timerEngine.currentCycle.value,
                        setsCompleted = timerEngine.currentSet.value
                    )
                    audioEventHandler.handleEvent(completionEvent)
                }
            }
        }
    }
    
    private suspend fun handleStateChange(oldState: TimerState?, newState: TimerState) {
        when (newState) {
            TimerState.RUNNING -> {
                if (oldState == TimerState.PAUSED) {
                    // Session resumed - no specific sound needed
                } else if (oldState == TimerState.IDLE) {
                    // New session started - phase change will handle the sound
                }
            }
            TimerState.PAUSED -> {
                // Session paused - stop any ongoing audio
                cancelTimingJobs()
            }
            TimerState.FINISHED -> {
                // Session finished
                val completionEvent = AudioEvent.SessionCompleted(
                    totalDurationMs = timerEngine.elapsedTimeMs.value,
                    cyclesCompleted = timerEngine.currentCycle.value,
                    setsCompleted = timerEngine.currentSet.value
                )
                audioEventHandler.handleEvent(completionEvent)
                cancelTimingJobs()
            }
            TimerState.IDLE -> {
                // Timer reset or stopped
                cancelTimingJobs()
            }
        }
    }
    
    private suspend fun handlePhaseChange(oldPhase: PhaseType?, newPhase: PhaseType) {
        if (oldPhase == null) return // Initial state
        
        // Cancel any ongoing timing jobs
        cancelTimingJobs()
        
        // Emit phase started event
        val phaseStartedEvent = AudioEvent.PhaseStarted(
            phaseType = newPhase,
            phaseNumber = calculatePhaseNumber(newPhase),
            cycleNumber = timerEngine.currentCycle.value,
            setNumber = timerEngine.currentSet.value
        )
        audioEventHandler.handleEvent(phaseStartedEvent)
        
        // Handle set/cycle completion
        when {
            oldPhase == PhaseType.REST && newPhase == PhaseType.SET_BREAK -> {
                val cycleCompletedEvent = AudioEvent.CycleCompleted(
                    cycleNumber = timerEngine.currentCycle.value,
                    totalCycles = timerEngine.currentConfiguration.value?.cycles ?: 0,
                    setNumber = timerEngine.currentSet.value
                )
                audioEventHandler.handleEvent(cycleCompletedEvent)
            }
            oldPhase == PhaseType.SET_BREAK && newPhase == PhaseType.WORK -> {
                val setCompletedEvent = AudioEvent.SetCompleted(
                    setNumber = timerEngine.currentSet.value - 1,
                    totalSets = timerEngine.currentConfiguration.value?.sets ?: 0
                )
                audioEventHandler.handleEvent(setCompletedEvent)
            }
        }
    }
    
    private fun handleTimingEvents(remainingTime: Int, currentPhase: PhaseType) {
        val settings = audioEventHandler.settings.value
        
        // Handle countdown sounds
        if (settings.enableCountdownSound && remainingTime <= settings.countdownStartSeconds && remainingTime > 0) {
            if (previousRemainingTime > remainingTime) { // Only trigger on time decrease
                countdownJob = integrationScope?.launch {
                    val countdownEvent = AudioEvent.CountdownTick(
                        secondsRemaining = remainingTime,
                        nextPhase = getNextPhase(currentPhase)
                    )
                    audioEventHandler.handleEvent(countdownEvent)
                }
            }
        }
        
        // Handle warning sounds
        if (settings.enableWarningSound && remainingTime == settings.warningBeforeSeconds) {
            warningJob = integrationScope?.launch {
                val warningEvent = AudioEvent.PhaseWarning(
                    currentPhase = currentPhase,
                    nextPhase = getNextPhase(currentPhase),
                    secondsUntilTransition = remainingTime
                )
                audioEventHandler.handleEvent(warningEvent)
            }
        }
        
        // Handle halfway sounds
        if (settings.enableHalfwaySound) {
            val phaseDuration = getPhaseDuration(currentPhase)
            if (phaseDuration > 0 && remainingTime == phaseDuration / 2) {
                integrationScope?.launch {
                    val halfwayEvent = AudioEvent.PhaseHalfway(
                        currentPhase = currentPhase,
                        remainingSeconds = remainingTime
                    )
                    audioEventHandler.handleEvent(halfwayEvent)
                }
            }
        }
    }
    
    private fun cancelTimingJobs() {
        countdownJob?.cancel()
        warningJob?.cancel()
        countdownJob = null
        warningJob = null
    }
    
    private fun calculatePhaseNumber(phaseType: PhaseType): Int {
        val config = timerEngine.currentConfiguration.value ?: return 0
        val currentCycle = timerEngine.currentCycle.value
        val currentSet = timerEngine.currentSet.value
        
        return when (phaseType) {
            PhaseType.WARMUP -> 1
            PhaseType.WORK -> (currentSet - 1) * config.cycles + currentCycle
            PhaseType.REST -> (currentSet - 1) * config.cycles + currentCycle
            PhaseType.SET_BREAK -> currentSet - 1
            PhaseType.COOLDOWN -> 1
            PhaseType.PREPARATION -> 1
        }
    }
    
    private fun getNextPhase(currentPhase: PhaseType): PhaseType {
        val config = timerEngine.currentConfiguration.value ?: return currentPhase
        val currentCycle = timerEngine.currentCycle.value
        val currentSet = timerEngine.currentSet.value
        
        return when (currentPhase) {
            PhaseType.WARMUP -> PhaseType.WORK
            PhaseType.WORK -> if (currentCycle < config.cycles) PhaseType.REST else {
                if (currentSet < config.sets) PhaseType.SET_BREAK else PhaseType.COOLDOWN
            }
            PhaseType.REST -> if (currentCycle < config.cycles) PhaseType.WORK else {
                if (currentSet < config.sets) PhaseType.SET_BREAK else PhaseType.COOLDOWN
            }
            PhaseType.SET_BREAK -> PhaseType.WORK
            PhaseType.COOLDOWN -> PhaseType.COOLDOWN // End state
            PhaseType.PREPARATION -> PhaseType.WARMUP
        }
    }
    
    private fun getPhaseDuration(phaseType: PhaseType): Int {
        val config = timerEngine.currentConfiguration.value ?: return 0
        
        return when (phaseType) {
            PhaseType.WARMUP -> config.warmupDuration
            PhaseType.WORK -> config.workDuration
            PhaseType.REST -> config.restDuration
            PhaseType.SET_BREAK -> config.setBreakDuration
            PhaseType.COOLDOWN -> config.cooldownDuration
            PhaseType.PREPARATION -> 5 // Default preparation time
        }
    }
}