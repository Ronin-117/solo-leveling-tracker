package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progression_states")
data class ProgressionStateEntity(
    @PrimaryKey val id: Int = 1, // Single global row
    val repsModifier: Int = 0,
    val setsModifier: Int = 0,
    val restDurationSeconds: Int = 60,
    val isHarderVariation: Boolean = false,
    val cardioPhase: Int = 1, // 1 for Weeks 1-2, 3 for Weeks 3+
    val currentWorkoutWeek: Int = 1
)
