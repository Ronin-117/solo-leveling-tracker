package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.WorkoutLogEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    val isFirebaseAvailable: Boolean
        get() = try {
            val app = FirebaseApp.getInstance()
            app != null
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    // Register user on Firebase or fallback to local
    suspend fun registerFirebaseUser(email: String, passwordPlain: String): Boolean {
        val firebaseAuth = auth ?: return false
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, passwordPlain).await()
            Log.d(TAG, "Firebase user registration successful: $email")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase registration failed: ${e.message}")
            false
        }
    }

    // Login user on Firebase
    suspend fun loginFirebaseUser(email: String, passwordPlain: String): Boolean {
        val firebaseAuth = auth ?: return false
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, passwordPlain).await()
            Log.d(TAG, "Firebase user login successful: $email")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase login failed: ${e.message}")
            false
        }
    }

    // Logout from Firebase
    fun logoutFirebase() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase logout failed: ${e.message}")
        }
    }

    // Sync a log to Firestore
    suspend fun syncLogToFirestore(log: WorkoutLogEntity) {
        val db = firestore ?: return
        val currentUser = auth?.currentUser ?: return
        try {
            val logMap = hashMapOf(
                "userId" to currentUser.uid,
                "timestamp" to log.timestamp,
                "routineType" to log.routineType,
                "completedExercisesCount" to log.completedExercisesCount,
                "totalExercisesCount" to log.totalExercisesCount,
                "week" to log.week
            )
            db.collection("users")
                .document(currentUser.uid)
                .collection("workout_logs")
                .add(logMap)
                .await()
            Log.d(TAG, "Log synced to Firestore successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync failed: ${e.message}")
        }
    }

    // Fetch logs from Firestore
    suspend fun fetchLogsFromFirestore(): List<WorkoutLogEntity> {
        val db = firestore ?: return emptyList()
        val currentUser = auth?.currentUser ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(currentUser.uid)
                .collection("workout_logs")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val routineType = doc.getString("routineType") ?: ""
                val completed = doc.getLong("completedExercisesCount")?.toInt() ?: 0
                val total = doc.getLong("totalExercisesCount")?.toInt() ?: 0
                val week = doc.getLong("week")?.toInt() ?: 1
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                WorkoutLogEntity(
                    id = 0,
                    routineType = routineType,
                    completedExercisesCount = completed,
                    totalExercisesCount = total,
                    week = week,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore fetch failed: ${e.message}")
            emptyList()
        }
    }
}
