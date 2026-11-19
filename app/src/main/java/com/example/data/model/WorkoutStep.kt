package com.example.data.model

data class WorkoutStep(
    val id: Int = 0,
    val name: String,
    val targetReps: Int,
    var currentReps: Int = 0,
    val targetSets: Int = 1,
    val currentSetLabel: String,
    val repUnit: String = "reps", // "reps" or "sec"
    val isRest: Boolean = false,
    val restDuration: Int = 0,
    var isCompleted: Boolean = false
)
