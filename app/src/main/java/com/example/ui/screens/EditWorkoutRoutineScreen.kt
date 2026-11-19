package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseConfig
import com.example.data.model.WorkoutPlanEntity
import com.example.data.model.getExercisesList
import com.example.data.model.toExercisesRaw
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutRoutineScreen(
    plans: List<WorkoutPlanEntity>,
    initialPlanId: String? = null,
    onSavePlan: (WorkoutPlanEntity) -> Unit,
    onResetPlan: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onClose()
    }

    var selectedPlanId by remember(plans, initialPlanId) {
        mutableStateOf(initialPlanId ?: plans.firstOrNull()?.id ?: "morning_cardio")
    }

    val currentPlan = plans.find { it.id == selectedPlanId } ?: plans.firstOrNull()

    // Local mutable state for editing current routine
    var batchRounds by remember(currentPlan) {
        mutableIntStateOf(currentPlan?.baseRounds ?: 3)
    }

    var editableExercises by remember(currentPlan) {
        mutableStateOf(currentPlan?.getExercisesList() ?: emptyList())
    }

    // Number edit dialog state
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var editDialogValue by remember { mutableStateOf("") }
    var editDialogType by remember { mutableStateOf("reps") } // "reps" or "sets"

    // Add new exercise dialog state
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseReps by remember { mutableStateOf("12") }
    var newExerciseUnit by remember { mutableStateOf("reps") }
    var newExerciseSets by remember { mutableIntStateOf(3) }
    var newExerciseIsFinisher by remember { mutableStateOf(false) }

    if (currentPlan == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("No workout plan loaded", color = TextWhite, fontFamily = FontFamily.Monospace)
        }
        return
    }

    Scaffold(
        containerColor = PitchBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("button_back_dashboard")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "BACK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, NeonCyan), shape = RoundedCornerShape(2.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EDIT PROTOCOL",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SELECT ROUTINE BATCH:",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal selector for plans
                val planOrder = listOf(
                    "morning_cardio" to "Morning Cardio",
                    "morning_core" to "Morning Core",
                    "evening_push" to "Push Day",
                    "evening_legs" to "Legs Day",
                    "evening_pull" to "Pull Day",
                    "sunday_walk" to "Sunday Walk"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    planOrder.forEach { (planId, label) ->
                        val isSelected = selectedPlanId == planId
                        Box(
                            modifier = Modifier
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else BorderGray
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else PitchBlack)
                                .clickable {
                                    selectedPlanId = planId
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("select_plan_$planId")
                        ) {
                            Text(
                                text = label.uppercase(),
                                color = if (isSelected) NeonCyan else TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
            }
        },
        bottomBar = {
            // Save and Reset controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, NeonCyan))
                    .navigationBarsPadding(),
                color = PitchBlack
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetPlan(currentPlan.id)
                            val defaultPlan = com.example.data.repository.WorkoutRepository(
                                com.example.data.local.WorkoutDatabase.getDatabase(context).workoutDao()
                            ).getDefaultWorkoutPlans().find { it.id == currentPlan.id }
                            if (defaultPlan != null) {
                                editableExercises = defaultPlan.getExercisesList()
                                batchRounds = defaultPlan.baseRounds
                            }
                            Toast.makeText(context, "ROUTINE RESET TO DEFAULT", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("button_reset_plan"),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "RESET DEFAULT",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val updatedPlan = currentPlan.copy(
                                baseRounds = batchRounds,
                                exercisesRaw = editableExercises.toExercisesRaw()
                            )
                            onSavePlan(updatedPlan)
                            Toast.makeText(context, "CHANGES SAVED TO DATABASE", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("button_save_plan"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = PitchBlack
                        ),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp),
                            tint = PitchBlack
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "SAVE PROTOCOL",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
        ) {
            // Plan Title card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray)
                        .background(DarkGrey.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentPlan.name.uppercase(),
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentPlan.description,
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 1. Batch Set Count Control ("change the set count of that batch")
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan)
                        .background(DarkGrey.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BATCH CIRCUIT SETS (ROUNDS)",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Total circuit cycles executed during this workout",
                                color = TextGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Stepper for batch rounds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (batchRounds > 1) batchRounds -= 1
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("button_decrement_batch_sets"),
                                border = BorderStroke(1.dp, if (batchRounds > 1) NeonCyan else BorderGray),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (batchRounds > 1) NeonCyan else BorderGray
                                ),
                                enabled = batchRounds > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Sets", modifier = Modifier.size(16.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .border(1.dp, BorderGray)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$batchRounds SETS",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (batchRounds < 10) batchRounds += 1
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("button_increment_batch_sets"),
                                border = BorderStroke(1.dp, if (batchRounds < 10) NeonCyan else BorderGray),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (batchRounds < 10) NeonCyan else BorderGray
                                ),
                                enabled = batchRounds < 10
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Sets", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Section Header: Exercises in this batch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXERCISE REPS & TIMERS (${editableExercises.size} MOVEMENTS):",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TAP +/- TO ADJUST",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 2. Rep Count of each exercise in that batch (important)
            itemsIndexed(editableExercises) { index, exercise ->
                val isSec = exercise.repUnit == "sec"
                val stepSize = if (isSec) 5 else 1

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_card_$index"),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkGrey.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Title & Tags Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${exercise.name.uppercase()}",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val tagText = when {
                                exercise.isFinisher -> "FINISHER"
                                exercise.baseSets == 1 -> "WARMUP"
                                else -> "CIRCUIT"
                            }
                            val tagColor = when {
                                exercise.isFinisher -> Color.Red
                                exercise.baseSets == 1 -> LightCyan
                                else -> NeonCyan
                            }

                            Box(
                                modifier = Modifier
                                    .border(1.dp, tagColor.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tagText,
                                    color = tagColor,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    editableExercises = editableExercises.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(24.dp).testTag("button_delete_exercise_$index")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Exercise",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Rep Count / Timer Stepper (Important)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSec) Icons.Default.Timer else Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isSec) "DURATION" else "REPETITIONS",
                                        color = TextWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isSec) "Seconds per set" else "Reps per set",
                                        color = TextGray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Stepper Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // -5 for seconds or quick -5 for reps if high
                                if (isSec || exercise.baseReps > 10) {
                                    OutlinedButton(
                                        onClick = {
                                            val decrement = if (isSec) 15 else 5
                                            val newReps = (exercise.baseReps - decrement).coerceAtLeast(1)
                                            editableExercises = editableExercises.toMutableList().also {
                                                it[index] = exercise.copy(baseReps = newReps)
                                            }
                                        },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(36.dp),
                                        border = BorderStroke(1.dp, BorderGray),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = TextGray
                                        )
                                    ) {
                                        Text(if (isSec) "-15" else "-5", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // Decrement button (-)
                                OutlinedButton(
                                    onClick = {
                                        val minVal = if (isSec) 5 else 1
                                        if (exercise.baseReps > minVal) {
                                            editableExercises = editableExercises.toMutableList().also {
                                                it[index] = exercise.copy(baseReps = (exercise.baseReps - stepSize).coerceAtLeast(minVal))
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("button_dec_rep_$index"),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = NeonCyan
                                    )
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(14.dp))
                                }

                                // Interactive display box (can be tapped to type value)
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, NeonCyan)
                                        .background(PitchBlack)
                                        .clickable {
                                            editingExerciseIndex = index
                                            editDialogType = "reps"
                                            editDialogValue = exercise.baseReps.toString()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${exercise.baseReps} ${if (isSec) "SEC" else "REPS"}",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Increment button (+)
                                OutlinedButton(
                                    onClick = {
                                        editableExercises = editableExercises.toMutableList().also {
                                            it[index] = exercise.copy(baseReps = exercise.baseReps + stepSize)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("button_inc_rep_$index"),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = NeonCyan
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(14.dp))
                                }

                                // +5 for seconds or quick +5 for reps
                                if (isSec || exercise.baseReps >= 5) {
                                    OutlinedButton(
                                        onClick = {
                                            val increment = if (isSec) 15 else 5
                                            editableExercises = editableExercises.toMutableList().also {
                                                it[index] = exercise.copy(baseReps = exercise.baseReps + increment)
                                            }
                                        },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .width(36.dp),
                                        border = BorderStroke(1.dp, BorderGray),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = TextGray
                                        )
                                    ) {
                                        Text(if (isSec) "+15" else "+5", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // Exercise Sets Stepper (For warmups or finishers)
                        if (exercise.isFinisher || exercise.baseSets > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (exercise.isFinisher) "FINISHER SETS:" else "EXERCISE SETS:",
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (exercise.baseSets > 1) {
                                                editableExercises = editableExercises.toMutableList().also {
                                                    it[index] = exercise.copy(baseSets = exercise.baseSets - 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp),
                                        border = BorderStroke(1.dp, if (exercise.baseSets > 1) BorderCyan else BorderGray),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = BorderCyan
                                        ),
                                        enabled = exercise.baseSets > 1
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Dec Set", modifier = Modifier.size(12.dp))
                                    }

                                    Text(
                                        text = "${exercise.baseSets} SETS",
                                        color = TextWhite,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (exercise.baseSets < 10) {
                                                editableExercises = editableExercises.toMutableList().also {
                                                    it[index] = exercise.copy(baseSets = exercise.baseSets + 1)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp),
                                        border = BorderStroke(1.dp, if (exercise.baseSets < 10) BorderCyan else BorderGray),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = BorderCyan
                                        ),
                                        enabled = exercise.baseSets < 10
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Inc Set", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        newExerciseName = ""
                        newExerciseReps = "12"
                        newExerciseUnit = "reps"
                        newExerciseSets = 3
                        newExerciseIsFinisher = false
                        showAddExerciseDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("button_add_exercise"),
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeonCyan.copy(alpha = 0.05f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "+ ADD EXERCISE TO ROUTINE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Direct input dialog if user taps value
    editingExerciseIndex?.let { idx ->
        val ex = editableExercises.getOrNull(idx)
        if (ex != null) {
            AlertDialog(
                onDismissRequest = { editingExerciseIndex = null },
                containerColor = PitchBlack,
                tonalElevation = 6.dp,
                modifier = Modifier.border(1.dp, NeonCyan),
                title = {
                    Text(
                        text = "SET EXACT ${if (ex.repUnit == "sec") "SECONDS" else "REPS"}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter target count for ${ex.name}:",
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editDialogValue,
                            onValueChange = { editDialogValue = it.filter { ch -> ch.isDigit() } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = NeonCyan
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intVal = editDialogValue.toIntOrNull()
                            if (intVal != null && intVal > 0) {
                                editableExercises = editableExercises.toMutableList().also {
                                    it[idx] = ex.copy(baseReps = intVal)
                                }
                            }
                            editingExerciseIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = PitchBlack
                        ),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("APPLY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingExerciseIndex = null }) {
                        Text("CANCEL", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            )
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            containerColor = PitchBlack,
            tonalElevation = 6.dp,
            modifier = Modifier.border(1.dp, NeonCyan),
            title = {
                Text(
                    text = "ADD NEW EXERCISE",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exercise Name:", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Reverse crunch", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Target Count:", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = newExerciseReps,
                                onValueChange = { newExerciseReps = it.filter { ch -> ch.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = BorderGray,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    cursorColor = NeonCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text("Unit:", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { newExerciseUnit = "reps" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (newExerciseUnit == "reps") NeonCyan else DarkGrey,
                                        contentColor = if (newExerciseUnit == "reps") PitchBlack else TextWhite
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("REPS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { newExerciseUnit = "sec" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (newExerciseUnit == "sec") NeonCyan else DarkGrey,
                                        contentColor = if (newExerciseUnit == "sec") PitchBlack else TextWhite
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("SEC", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Base Sets: $newExerciseSets", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { newExerciseSets = (newExerciseSets - 1).coerceAtLeast(1) },
                                modifier = Modifier.size(30.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", color = NeonCyan, fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick = { newExerciseSets = (newExerciseSets + 1).coerceAtMost(10) },
                                modifier = Modifier.size(30.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", color = NeonCyan, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = newExerciseReps.toIntOrNull() ?: 12
                        val name = newExerciseName.trim().ifEmpty { "Custom Exercise" }
                        editableExercises = editableExercises + ExerciseConfig(
                            name = name,
                            baseReps = count,
                            repUnit = newExerciseUnit,
                            baseSets = newExerciseSets,
                            isFinisher = newExerciseIsFinisher
                        )
                        showAddExerciseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("ADD EXERCISE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("CANCEL", color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        )
    }
}
