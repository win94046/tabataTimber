package org.tabata.timber.core.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tabata.timber.domain.models.TabataConfig
import org.tabata.timber.domain.models.TimerState
import org.tabata.timber.domain.models.WorkoutSession
import org.tabata.timber.domain.models.timer.PhaseType
import org.tabata.timber.domain.models.timer.TimerConfiguration

/**
 * Mock implementation of TimerEngine for testing purposes
 */
class MockTimerEngine : TimerEngine {
    
    // State flows
    private val _currentSession = MutableStateFlow<WorkoutSession?>(null)
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    private val _currentPhase = MutableStateFlow(PhaseType.WARMUP)
    private val _remainingTimeMs = MutableStateFlow(0L)
    private val _remainingTime = MutableStateFlow(0)
    private val _elapsedTimeMs = MutableStateFlow(0L)
    private val _isActive = MutableStateFlow(false)
    private val _currentConfiguration = MutableStateFlow<TimerConfiguration?>(null)
    private val _currentCycle = MutableStateFlow(0)
    private val _currentSet = MutableStateFlow(0)
    private val _precisionMetrics = MutableStateFlow(TimerPrecisionMetrics.initial())
    
    // Call tracking for test verification
    var startSessionCalled = false
    var pauseSessionCalled = false
    var resumeSessionCalled = false
    var stopSessionCalled = false
    var resetTimerCalled = false
    var skipToNextCalled = false
    var skipToPreviousCalled = false
    
    // Error simulation
    private var shouldFailStartSession = false
    private var shouldFailPauseSession = false
    private var shouldFailResumeSession = false
    private var shouldFailStopSession = false
    private var shouldFailResetTimer = false
    private var shouldFailSkipToNext = false
    private var shouldFailSkipToPrevious = false
    
    // StateFlow implementations
    override val currentSession: StateFlow<WorkoutSession?> = _currentSession.asStateFlow()
    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    override val currentPhase: StateFlow<PhaseType> = _currentPhase.asStateFlow()
    override val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()
    override val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()
    override val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()
    override val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    override val currentConfiguration: StateFlow<TimerConfiguration?> = _currentConfiguration.asStateFlow()
    override val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()
    override val currentSet: StateFlow<Int> = _currentSet.asStateFlow()
    override val precisionMetrics: StateFlow<TimerPrecisionMetrics> = _precisionMetrics.asStateFlow()
    
    // Mock implementations
    override suspend fun startSession(config: TabataConfig): Result<Unit> {
        startSessionCalled = true
        return if (shouldFailStartSession) {
            Result.failure(RuntimeException("Mock start session failure"))
        } else {
            _currentConfiguration.value = TimerConfiguration.fromTabataConfig(config)
            _timerState.value = TimerState.WORK
            _isActive.value = true
            Result.success(Unit)
        }
    }
    
    override suspend fun startSession(config: TimerConfiguration): Result<Unit> {
        startSessionCalled = true
        return if (shouldFailStartSession) {
            Result.failure(RuntimeException("Mock start session failure"))
        } else {
            _currentConfiguration.value = config
            _timerState.value = TimerState.WORK
            _isActive.value = true
            Result.success(Unit)
        }
    }
    
    override suspend fun pauseSession(): Result<Unit> {
        pauseSessionCalled = true
        return if (shouldFailPauseSession) {
            Result.failure(RuntimeException("Mock pause session failure"))
        } else {
            _timerState.value = TimerState.PAUSED
            Result.success(Unit)
        }
    }
    
    override suspend fun resumeSession(): Result<Unit> {
        resumeSessionCalled = true
        return if (shouldFailResumeSession) {
            Result.failure(RuntimeException("Mock resume session failure"))
        } else {
            _timerState.value = TimerState.WORK // Assume resuming to work
            Result.success(Unit)
        }
    }
    
    override suspend fun stopSession(): Result<Unit> {
        stopSessionCalled = true
        return if (shouldFailStopSession) {
            Result.failure(RuntimeException("Mock stop session failure"))
        } else {
            _timerState.value = TimerState.IDLE
            _isActive.value = false
            _currentSession.value = null
            Result.success(Unit)
        }
    }
    
    override suspend fun resetTimer(): Result<Unit> {
        resetTimerCalled = true
        return if (shouldFailResetTimer) {
            Result.failure(RuntimeException("Mock reset timer failure"))
        } else {
            _timerState.value = TimerState.IDLE
            _isActive.value = false
            _remainingTimeMs.value = 0L
            _remainingTime.value = 0
            _elapsedTimeMs.value = 0L
            Result.success(Unit)
        }
    }
    
    override suspend fun skipToNext(): Result<Unit> {
        skipToNextCalled = true
        return if (shouldFailSkipToNext) {
            Result.failure(RuntimeException("Mock skip to next failure"))
        } else {
            // Mock phase transition\n            when (_currentPhase.value) {\n                PhaseType.WORK -> _currentPhase.value = PhaseType.REST\n                PhaseType.REST -> _currentPhase.value = PhaseType.WORK\n                PhaseType.WARMUP -> _currentPhase.value = PhaseType.WORK\n                PhaseType.SET_BREAK -> _currentPhase.value = PhaseType.WORK\n                PhaseType.COOLDOWN -> {} // No change for cooldown\n            }\n            Result.success(Unit)\n        }\n    }\n    \n    override suspend fun skipToPrevious(): Result<Unit> {\n        skipToPreviousCalled = true\n        return if (shouldFailSkipToPrevious) {\n            Result.failure(RuntimeException(\"Mock skip to previous failure\"))\n        } else {\n            // Mock going back to start of current phase\n            Result.success(Unit)\n        }\n    }\n    \n    // Helper methods for test setup\n    fun setTimerState(state: TimerState) {\n        _timerState.value = state\n    }\n    \n    fun setCurrentPhase(phase: PhaseType) {\n        _currentPhase.value = phase\n    }\n    \n    fun setIsActive(active: Boolean) {\n        _isActive.value = active\n    }\n    \n    fun setRemainingTimeMs(timeMs: Long) {\n        _remainingTimeMs.value = timeMs\n        _remainingTime.value = (timeMs / 1000L).toInt()\n    }\n    \n    fun setElapsedTimeMs(timeMs: Long) {\n        _elapsedTimeMs.value = timeMs\n    }\n    \n    fun setCurrentCycle(cycle: Int) {\n        _currentCycle.value = cycle\n    }\n    \n    fun setCurrentSet(set: Int) {\n        _currentSet.value = set\n    }\n    \n    fun setCurrentConfiguration(config: TimerConfiguration) {\n        _currentConfiguration.value = config\n    }\n    \n    fun setCurrentSession(session: WorkoutSession?) {\n        _currentSession.value = session\n    }\n    \n    fun setPrecisionMetrics(metrics: TimerPrecisionMetrics) {\n        _precisionMetrics.value = metrics\n    }\n    \n    // Helper to create a mock session\n    fun createMockSession(): WorkoutSession {\n        return WorkoutSession(\n            id = \"mock-session-123\",\n            configuration = TabataConfig(\n                workDuration = 20,\n                restDuration = 10,\n                cycles = 8,\n                sets = 1,\n                setBreakDuration = 60\n            ),\n            startTime = System.currentTimeMillis(),\n            endTime = null,\n            isCompleted = false\n        )\n    }\n    \n    // Error simulation methods\n    fun setShouldFailStartSession(shouldFail: Boolean) {\n        shouldFailStartSession = shouldFail\n    }\n    \n    fun setShouldFailPauseSession(shouldFail: Boolean) {\n        shouldFailPauseSession = shouldFail\n    }\n    \n    fun setShouldFailResumeSession(shouldFail: Boolean) {\n        shouldFailResumeSession = shouldFail\n    }\n    \n    fun setShouldFailStopSession(shouldFail: Boolean) {\n        shouldFailStopSession = shouldFail\n    }\n    \n    fun setShouldFailResetTimer(shouldFail: Boolean) {\n        shouldFailResetTimer = shouldFail\n    }\n    \n    fun setShouldFailSkipToNext(shouldFail: Boolean) {\n        shouldFailSkipToNext = shouldFail\n    }\n    \n    fun setShouldFailSkipToPrevious(shouldFail: Boolean) {\n        shouldFailSkipToPrevious = shouldFail\n    }\n    \n    // Reset all call tracking\n    fun resetCallTracking() {\n        startSessionCalled = false\n        pauseSessionCalled = false\n        resumeSessionCalled = false\n        stopSessionCalled = false\n        resetTimerCalled = false\n        skipToNextCalled = false\n        skipToPreviousCalled = false\n    }\n    \n    // Reset all error simulation\n    fun resetErrorSimulation() {\n        shouldFailStartSession = false\n        shouldFailPauseSession = false\n        shouldFailResumeSession = false\n        shouldFailStopSession = false\n        shouldFailResetTimer = false\n        shouldFailSkipToNext = false\n        shouldFailSkipToPrevious = false\n    }\n    \n    // Complete reset\n    fun reset() {\n        resetCallTracking()\n        resetErrorSimulation()\n        _timerState.value = TimerState.IDLE\n        _currentPhase.value = PhaseType.WARMUP\n        _isActive.value = false\n        _remainingTimeMs.value = 0L\n        _remainingTime.value = 0\n        _elapsedTimeMs.value = 0L\n        _currentCycle.value = 0\n        _currentSet.value = 0\n        _currentSession.value = null\n        _currentConfiguration.value = null\n        _precisionMetrics.value = TimerPrecisionMetrics.initial()\n    }\n}"