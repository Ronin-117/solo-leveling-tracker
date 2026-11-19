package com.example.data.local

import androidx.room.*
import com.example.data.model.ProgressionStateEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkoutLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // User Queries
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    // Progression Queries
    @Query("SELECT * FROM progression_states WHERE id = 1 LIMIT 1")
    fun getProgressionStateFlow(): Flow<ProgressionStateEntity?>

    @Query("SELECT * FROM progression_states WHERE id = 1 LIMIT 1")
    suspend fun getProgressionState(): ProgressionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressionState(state: ProgressionStateEntity)

    // Workout Log Queries
    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkoutLogsFlow(): Flow<List<WorkoutLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Query("DELETE FROM workout_logs")
    suspend fun clearAllLogs()

    // Workout Plan Queries
    @Query("SELECT * FROM workout_plans")
    fun getAllWorkoutPlansFlow(): Flow<List<com.example.data.model.WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans")
    suspend fun getAllWorkoutPlans(): List<com.example.data.model.WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plans WHERE id = :planId LIMIT 1")
    suspend fun getWorkoutPlanById(planId: String): com.example.data.model.WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: com.example.data.model.WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlans(plans: List<com.example.data.model.WorkoutPlanEntity>)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deleteWorkoutPlanById(planId: String)
}
