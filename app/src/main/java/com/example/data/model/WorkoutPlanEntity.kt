package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "morning" or "evening"
    val description: String,
    val baseRounds: Int = 3,
    val exercisesRaw: String // Newline-separated exercises for easy customization
)

data class ExerciseConfig(
    val name: String,
    val baseReps: Int,
    val repUnit: String, // "reps" or "sec"
    val baseSets: Int,
    val isFinisher: Boolean = false
)

fun WorkoutPlanEntity.getExercisesList(): List<ExerciseConfig> {
    if (exercisesRaw.isBlank()) return emptyList()
    return exercisesRaw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
        val parts = line.split("|")
        ExerciseConfig(
            name = parts.getOrNull(0) ?: "Exercise",
            baseReps = parts.getOrNull(1)?.toIntOrNull() ?: 10,
            repUnit = parts.getOrNull(2) ?: "reps",
            baseSets = parts.getOrNull(3)?.toIntOrNull() ?: 3,
            isFinisher = parts.getOrNull(4)?.trim()?.toBoolean() ?: false
        )
    }
}

fun List<ExerciseConfig>.toExercisesRaw(): String {
    return joinToString("\n") { config ->
        "${config.name}|${config.baseReps}|${config.repUnit}|${config.baseSets}|${config.isFinisher}"
    }
}

