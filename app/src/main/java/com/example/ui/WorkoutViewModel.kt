package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProgressionStateEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkoutLogEntity
import com.example.data.model.WorkoutPlanEntity
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded = _isSessionLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeStateIfNeeded()
            repository.activeUserFlow.collect { user ->
                _isSessionLoaded.value = true
                if (user != null) {
                    syncFirestoreLogs()
                }
            }
        }
    }

    fun syncFirestoreLogs() {
        viewModelScope.launch {
            repository.syncFirestoreLogsToLocal()
        }
    }

    val activeUser: StateFlow<UserEntity?> = repository.activeUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val progressionState: StateFlow<ProgressionStateEntity?> = repository.progressionStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val workoutLogs: StateFlow<List<WorkoutLogEntity>> = repository.allWorkoutLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val workoutPlans: StateFlow<List<WorkoutPlanEntity>> = repository.allWorkoutPlansFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun login(username: String, passwordPlain: String, onResult: (Boolean) -> Unit) {
        if (username.isBlank() || passwordPlain.isBlank()) {
            _authError.value = "Username and password cannot be empty"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val success = repository.loginUser(username, passwordPlain)
            if (success) {
                onResult(true)
            } else {
                _authError.value = "Invalid username or password"
                onResult(false)
            }
        }
    }

    fun register(username: String, passwordPlain: String, onResult: (Boolean) -> Unit) {
        if (username.isBlank() || passwordPlain.isBlank()) {
            _authError.value = "Username and password cannot be empty"
            onResult(false)
            return
        }
        if (passwordPlain.length < 4) {
            _authError.value = "Password must be at least 4 characters"
            onResult(false)
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val success = repository.registerUser(username, passwordPlain)
            if (success) {
                onResult(true)
            } else {
                _authError.value = "Username already exists"
                onResult(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun applyProgression(type: String) {
        viewModelScope.launch {
            val current = repository.getProgressionState()
            val updated = when (type) {
                "reps" -> current.copy(repsModifier = current.repsModifier + 1)
                "sets" -> current.copy(setsModifier = current.setsModifier + 1)
                "rest" -> {
                    val nextRest = if (current.restDurationSeconds > 15) current.restDurationSeconds - 5 else 15
                    current.copy(restDurationSeconds = nextRest)
                }
                "harder" -> current.copy(isHarderVariation = true)
                "cardio" -> current.copy(cardioPhase = 3) // Advance to Week 3+ (Mountain climbers, burpees, etc)
                "advance_week" -> current.copy(currentWorkoutWeek = current.currentWorkoutWeek + 1)
                else -> current
            }
            repository.updateProgressionState(updated)
        }
    }

    fun resetProgression() {
        viewModelScope.launch {
            repository.updateProgressionState(ProgressionStateEntity())
        }
    }

    fun setExactProgression(
        week: Int,
        repsModifier: Int,
        setsModifier: Int,
        restDurationSeconds: Int,
        isHarderVariation: Boolean,
        cardioPhase: Int
    ) {
        viewModelScope.launch {
            val current = repository.getProgressionState()
            val updated = current.copy(
                currentWorkoutWeek = week,
                repsModifier = repsModifier,
                setsModifier = setsModifier,
                restDurationSeconds = restDurationSeconds,
                isHarderVariation = isHarderVariation,
                cardioPhase = cardioPhase
            )
            repository.updateProgressionState(updated)
        }
    }

    fun logWorkout(routineType: String, completed: Int, total: Int) {
        viewModelScope.launch {
            repository.logWorkout(routineType, completed, total)
        }
    }

    fun updateWorkoutPlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            repository.updateWorkoutPlan(plan)
        }
    }

    fun resetWorkoutPlanToDefault(planId: String) {
        viewModelScope.launch {
            repository.resetWorkoutPlanToDefault(planId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Active workout progress tracking for pause and resume
    private val _pausedWorkoutId = MutableStateFlow<String?>(null)
    val pausedWorkoutId = _pausedWorkoutId.asStateFlow()
    
    private val _pausedStepIndex = MutableStateFlow(0)
    val pausedStepIndex = _pausedStepIndex.asStateFlow()
    
    private val _pausedStepCompletedStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val pausedStepCompletedStates = _pausedStepCompletedStates.asStateFlow()

    private val _pausedStepCurrentReps = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val pausedStepCurrentReps = _pausedStepCurrentReps.asStateFlow()

    fun saveWorkoutProgress(workoutId: String, stepIndex: Int, completedStates: Map<Int, Boolean>, currentReps: Map<Int, Int>) {
        _pausedWorkoutId.value = workoutId
        _pausedStepIndex.value = stepIndex
        _pausedStepCompletedStates.value = completedStates
        _pausedStepCurrentReps.value = currentReps
    }

    fun clearWorkoutProgress() {
        _pausedWorkoutId.value = null
        _pausedStepIndex.value = 0
        _pausedStepCompletedStates.value = emptyMap()
        _pausedStepCurrentReps.value = emptyMap()
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
                return WorkoutViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
