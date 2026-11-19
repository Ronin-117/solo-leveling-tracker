package com.example.data.repository

import com.example.data.local.WorkoutDao
import com.example.data.model.ProgressionStateEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkoutLogEntity
import com.example.data.model.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    val activeUserFlow: Flow<UserEntity?> = workoutDao.getActiveUserFlow()
    val progressionStateFlow: Flow<ProgressionStateEntity?> = workoutDao.getProgressionStateFlow()
    val allWorkoutLogsFlow: Flow<List<WorkoutLogEntity>> = workoutDao.getAllWorkoutLogsFlow()
    val allWorkoutPlansFlow: Flow<List<WorkoutPlanEntity>> = workoutDao.getAllWorkoutPlansFlow()

    suspend fun initializeStateIfNeeded() {
        val existing = workoutDao.getProgressionState()
        if (existing == null) {
            workoutDao.insertProgressionState(ProgressionStateEntity())
        }
        
        // Also insert a default demo user if no users exist
        val demoUser = workoutDao.getUserByUsername("user")
        if (demoUser == null) {
            workoutDao.insertUser(UserEntity(username = "user", passwordHash = "password", isLoggedIn = false))
        }

        // Prepopulate default workout plans if empty, or ensure routines are updated to correct protocols
        val existingPlans = workoutDao.getAllWorkoutPlans()
        if (existingPlans.isEmpty()) {
            workoutDao.insertWorkoutPlans(getDefaultWorkoutPlans())
        } else {
            // Restore morning_core to original protocol if it was accidentally changed to have Reverse crunch
            val corePlan = workoutDao.getWorkoutPlanById("morning_core")
            val defaultMorningCore = getDefaultWorkoutPlans().find { it.id == "morning_core" }
            if (defaultMorningCore != null && (corePlan == null || corePlan.exercisesRaw.contains("Reverse crunch") || !corePlan.exercisesRaw.contains("Bicycle crunches"))) {
                workoutDao.insertWorkoutPlan(defaultMorningCore)
            }

            // Remove any obsolete daily evening_core routine
            workoutDao.deleteWorkoutPlanById("evening_core")

            // Ensure evening_pull (Wed / Sat) is updated with user's core protocol (WITHOUT Superman Raise)
            val pullPlan = workoutDao.getWorkoutPlanById("evening_pull")
            val defaultPull = getDefaultWorkoutPlans().find { it.id == "evening_pull" }
            if (defaultPull != null && (pullPlan == null || pullPlan.exercisesRaw.contains("Superman Raise") || !pullPlan.exercisesRaw.contains("Reverse crunch"))) {
                workoutDao.insertWorkoutPlan(defaultPull)
            }

            for (defaultPlan in getDefaultWorkoutPlans()) {
                if (workoutDao.getWorkoutPlanById(defaultPlan.id) == null) {
                    workoutDao.insertWorkoutPlan(defaultPlan)
                }
            }
        }
    }

    fun getDefaultWorkoutPlans(): List<WorkoutPlanEntity> {
        return listOf(
            WorkoutPlanEntity(
                id = "morning_cardio",
                name = "Morning warm-up & Cardio",
                category = "morning",
                description = "15-17 min daily cardio & mobility warmup",
                baseRounds = 5,
                exercisesRaw = """
                    Jumping Jacks|50|reps|1|false
                    Arm circles (Mobility focus)|60|sec|1|false
                    Hip circles (Mobility focus)|60|sec|1|false
                    Leg swings (Dynamic stretch)|60|sec|1|false
                    Neck mobility circles|60|sec|1|false
                    Jog in place (Steady pace)|30|sec|5|false
                    High Knees (Active effort)|20|sec|5|false
                    Cooldown static stretching|120|sec|1|false
                """.trimIndent()
            ),
            WorkoutPlanEntity(
                id = "morning_core",
                name = "Morning Core",
                category = "morning",
                description = "Core reinforcement protocol (3 rounds)",
                baseRounds = 3,
                exercisesRaw = """
                    Plank|45|sec|3|false
                    Bicycle crunches|20|reps|3|false
                    Side plank (30 sec each side)|30|sec|3|false
                    Superman Raise|12|reps|3|false
                    Cooldown stretch (Cobra/Child's pose)|120|sec|1|false
                """.trimIndent()
            ),
            WorkoutPlanEntity(
                id = "evening_push",
                name = "Evening Push",
                category = "evening",
                description = "Push day strength sheet (Chest, Shoulders, Triceps)",
                baseRounds = 3,
                exercisesRaw = """
                    Regular Push-ups|10|reps|3|false
                    Incline Push-ups (using a chair)|12|reps|3|false
                    Pike Push-ups|8|reps|3|false
                    Diamond Push-ups|6|reps|3|false
                    Chair Dips|10|reps|3|false
                    Plank Hold Finisher|45|sec|2|true
                """.trimIndent()
            ),
            WorkoutPlanEntity(
                id = "evening_legs",
                name = "Evening Legs",
                category = "evening",
                description = "Legs day strength sheet (Quads, Hamstrings, Calves)",
                baseRounds = 3,
                exercisesRaw = """
                    Standard Squats|15|reps|3|false
                    Reverse Lunges|10|reps|3|false
                    Standard Glute Bridge|15|reps|3|false
                    Wall Sit Hold|45|sec|3|false
                    Calf Raises|20|reps|3|false
                    Jump Squats Plyo Burst|10|reps|2|true
                """.trimIndent()
            ),
            WorkoutPlanEntity(
                id = "evening_pull",
                name = "Evening Pull",
                category = "evening",
                description = "Wednesday & Saturday Pull & Core protocol (3 rounds)",
                baseRounds = 3,
                exercisesRaw = """
                    Reverse crunch|12|reps|3|false
                    Dead bug (10 each side)|10|reps|3|false
                    Plank|45|sec|3|false
                    Side plank (30 sec each side)|30|sec|3|false
                    Mountain climbers (30 total)|30|reps|3|false
                    Cooldown stretch (Cobra/Child's pose)|120|sec|1|false
                """.trimIndent()
            ),
            WorkoutPlanEntity(
                id = "sunday_walk",
                name = "Sunday Stretch & Walk",
                category = "evening",
                description = "Sunday restorative active recovery protocol",
                baseRounds = 1,
                exercisesRaw = """
                    Mindful Outdoor Walk|1500|sec|1|false
                    Deep Full Body Cooldown Static Stretch|600|sec|1|false
                """.trimIndent()
            )
        )
    }

    suspend fun updateWorkoutPlan(plan: WorkoutPlanEntity) {
        workoutDao.insertWorkoutPlan(plan)
    }

    suspend fun resetWorkoutPlanToDefault(planId: String) {
        val defaultPlan = getDefaultWorkoutPlans().find { it.id == planId }
        if (defaultPlan != null) {
            workoutDao.insertWorkoutPlan(defaultPlan)
        }
    }

    suspend fun registerUser(username: String, passwordPlain: String): Boolean {
        // 1. Convert username to a Firebase-compatible email format if not already an email
        val firebaseEmail = if (username.contains("@") && username.contains(".")) {
            username
        } else {
            "${username.lowercase().replace(" ", "")}@workouttracker.aistudio.com"
        }

        if (FirebaseSyncManager.isFirebaseAvailable) {
            val firebaseSuccess = FirebaseSyncManager.registerFirebaseUser(firebaseEmail, passwordPlain)
            if (firebaseSuccess) {
                // Also create/log in local UserEntity
                val newUser = UserEntity(username = username, passwordHash = passwordPlain, isLoggedIn = true)
                workoutDao.logoutAllUsers()
                workoutDao.insertUser(newUser)
                try {
                    syncFirestoreLogsToLocal()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }

        // 2. Fallback to local authentication
        val existing = workoutDao.getUserByUsername(username)
        if (existing != null) return false // Username taken
        
        val newUser = UserEntity(
            username = username,
            passwordHash = passwordPlain,
            isLoggedIn = true
        )
        workoutDao.logoutAllUsers()
        workoutDao.insertUser(newUser)
        return true
    }

    suspend fun loginUser(username: String, passwordPlain: String): Boolean {
        // 1. Convert username to a Firebase-compatible email format if not already an email
        val firebaseEmail = if (username.contains("@") && username.contains(".")) {
            username
        } else {
            "${username.lowercase().replace(" ", "")}@workouttracker.aistudio.com"
        }

        if (FirebaseSyncManager.isFirebaseAvailable) {
            val firebaseSuccess = FirebaseSyncManager.loginFirebaseUser(firebaseEmail, passwordPlain)
            if (firebaseSuccess) {
                // Ensure user exists locally and log them in
                val localUser = workoutDao.getUserByUsername(username)
                workoutDao.logoutAllUsers()
                if (localUser != null) {
                    workoutDao.updateUser(localUser.copy(isLoggedIn = true))
                } else {
                    workoutDao.insertUser(UserEntity(username = username, passwordHash = passwordPlain, isLoggedIn = true))
                }
                try {
                    syncFirestoreLogsToLocal()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }

        // 2. Fallback to local database
        val user = workoutDao.getUserByUsername(username) ?: return false
        if (user.passwordHash == passwordPlain) {
            workoutDao.logoutAllUsers()
            workoutDao.updateUser(user.copy(isLoggedIn = true))
            return true
        }
        return false
    }

    suspend fun logout() {
        FirebaseSyncManager.logoutFirebase()
        workoutDao.logoutAllUsers()
    }

    suspend fun updateProgressionState(state: ProgressionStateEntity) {
        workoutDao.insertProgressionState(state)
    }

    suspend fun getProgressionState(): ProgressionStateEntity {
        return workoutDao.getProgressionState() ?: ProgressionStateEntity().also {
            workoutDao.insertProgressionState(it)
        }
    }

    suspend fun logWorkout(routineType: String, completed: Int, total: Int) {
        val state = getProgressionState()
        val log = WorkoutLogEntity(
            routineType = routineType,
            completedExercisesCount = completed,
            totalExercisesCount = total,
            week = state.currentWorkoutWeek
        )
        workoutDao.insertWorkoutLog(log)

        // Try to sync with Firestore if active and available
        if (FirebaseSyncManager.isFirebaseAvailable) {
            FirebaseSyncManager.syncLogToFirestore(log)
        }
    }

    suspend fun syncFirestoreLogsToLocal() {
        if (FirebaseSyncManager.isFirebaseAvailable && FirebaseSyncManager.auth?.currentUser != null) {
            try {
                val remoteLogs = FirebaseSyncManager.fetchLogsFromFirestore()
                val localLogs = workoutDao.getAllWorkoutLogsFlow().firstOrNull() ?: emptyList()
                val localKeySet = localLogs.map { "${it.timestamp}_${it.routineType}" }.toSet()
                
                for (log in remoteLogs) {
                    val key = "${log.timestamp}_${log.routineType}"
                    if (!localKeySet.contains(key)) {
                        workoutDao.insertWorkoutLog(log.copy(id = 0))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getWorkoutPlanById(planId: String): WorkoutPlanEntity? {
        return workoutDao.getWorkoutPlanById(planId)
    }

    suspend fun clearLogs() {
        workoutDao.clearAllLogs()
    }
}
