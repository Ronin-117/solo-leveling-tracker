package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val routineType: String,
    val completedExercisesCount: Int = 0,
    val totalExercisesCount: Int = 0,
    val week: Int = 1
)
