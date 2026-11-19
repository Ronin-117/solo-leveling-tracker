package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProgressionStateEntity
import com.example.data.model.WorkoutLogEntity
import com.example.ui.WorkoutViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Custom dashed border drawing for Geometric Balance theme
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    dashLength: Float = 15f,
    gapLength: Float = 10f
) = this.drawBehind {
    val strokeWidthPx = width.toPx()
    val stroke = Stroke(
        width = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
    )
    drawRect(
        color = color,
        style = stroke
    )
}

@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeUser by viewModel.activeUser.collectAsState()
    val progression by viewModel.progressionState.collectAsState()
    val logs by viewModel.workoutLogs.collectAsState()
    val plans by viewModel.workoutPlans.collectAsState()
    
    val pausedWorkoutId by viewModel.pausedWorkoutId.collectAsState()
    val pausedStepIndex by viewModel.pausedStepIndex.collectAsState()
    val pausedStepCompletedStates by viewModel.pausedStepCompletedStates.collectAsState()
    val pausedStepCurrentReps by viewModel.pausedStepCurrentReps.collectAsState()

    var currentModuleTab by rememberSaveable { mutableStateOf("workout") } // "workout", "diet", "history"
    var activeTrackingWorkout by rememberSaveable { mutableStateOf<String?>(null) } // active routine name to track
    var editingPlanId by rememberSaveable { mutableStateOf<String?>(null) }

    val handleStartWorkout = { workoutName: String ->
        activeTrackingWorkout = workoutName
    }
    
    val activePlan = remember(activeTrackingWorkout, plans) {
        plans.find { it.name.equals(activeTrackingWorkout, ignoreCase = true) || it.id.equals(activeTrackingWorkout, ignoreCase = true) }
            ?: com.example.data.model.WorkoutPlanEntity(
                id = "custom",
                name = activeTrackingWorkout ?: "Workout",
                category = "morning",
                description = "Custom protocol execution",
                exercisesRaw = "Jumping Jacks|50|reps|1|false"
            )
    }
    
    // Day of week detection
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    
    val dayName = when (dayOfWeek) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Today"
    }

    if (editingPlanId != null) {
        EditWorkoutRoutineScreen(
            plans = plans,
            initialPlanId = editingPlanId,
            onSavePlan = { updatedPlan ->
                viewModel.updateWorkoutPlan(updatedPlan)
            },
            onResetPlan = { planId ->
                viewModel.resetWorkoutPlanToDefault(planId)
            },
            onClose = {
                editingPlanId = null
            }
        )
    } else if (activeTrackingWorkout != null) {
        val isResuming = pausedWorkoutId != null && (
            pausedWorkoutId.equals(activeTrackingWorkout, ignoreCase = true) ||
            pausedWorkoutId.equals(activePlan.id, ignoreCase = true) ||
            pausedWorkoutId.equals(activePlan.name, ignoreCase = true)
        )
        ImmersiveWorkoutPlayer(
            plan = activePlan,
            progression = progression ?: ProgressionStateEntity(),
            initialStepIdx = if (isResuming) pausedStepIndex else 0,
            initialCompletedStates = if (isResuming) pausedStepCompletedStates else emptyMap(),
            initialCurrentReps = if (isResuming) pausedStepCurrentReps else emptyMap(),
            onComplete = { completed, total ->
                viewModel.logWorkout(activeTrackingWorkout!!, completed, total)
                viewModel.clearWorkoutProgress()
                Toast.makeText(context, "WORKOUT SECURED AND LOGGED", Toast.LENGTH_LONG).show()
                activeTrackingWorkout = null
            },
            onClose = {
                activeTrackingWorkout = null
            },
            onPause = { stepIdx, completed, reps ->
                viewModel.saveWorkoutProgress(activeTrackingWorkout!!, stepIdx, completed, reps)
                Toast.makeText(context, "PROGRESS SAVED — RESUME ANYTIME", Toast.LENGTH_SHORT).show()
                activeTrackingWorkout = null
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            bottomBar = {
            // Modularity check: Workout, Diet, and History Modules Bottom Bar
            // Geometric Balance 4-column layout mirroring layout mockup: W_OUT, DIET, DATA, AUTH
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, NeonCyan))
                    .navigationBarsPadding(),
                color = PitchBlack
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 1: W_OUT
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentModuleTab = "workout"
                                activeTrackingWorkout = null
                            }
                            .background(if (currentModuleTab == "workout") NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                            .border(width = 1.dp, color = if (currentModuleTab == "workout") NeonCyan else Color.Transparent)
                            .padding(vertical = 12.dp)
                            .testTag("nav_workout")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, if (currentModuleTab == "workout") NeonCyan else TextGray)
                        ) {
                            if (currentModuleTab == "workout") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .background(NeonCyan)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WORKOUT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (currentModuleTab == "workout") NeonCyan else TextGray
                        )
                    }

                    // Item 2: DIET (Locked)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                Toast.makeText(context, "DIET SYSTEM LOCKED - V2 PROTOCOL REQUIRED", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp)
                            .testTag("nav_diet")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, TextGray.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "DIET",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextGray.copy(alpha = 0.4f)
                        )
                    }

                    // Item 3: DATA (Workout logs)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentModuleTab = "history"
                                activeTrackingWorkout = null
                            }
                            .background(if (currentModuleTab == "history") NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                            .border(width = 1.dp, color = if (currentModuleTab == "history") NeonCyan else Color.Transparent)
                            .padding(vertical = 12.dp)
                            .testTag("nav_history")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, if (currentModuleTab == "history") NeonCyan else TextGray)
                        ) {
                            if (currentModuleTab == "history") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .background(NeonCyan)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "HISTORY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (currentModuleTab == "history") NeonCyan else TextGray
                        )
                    }

                    // Item 4: DISCONNECT / AUTH
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.logout()
                                onLogout()
                                Toast.makeText(context, "DISCONNECTED FROM SYSTEM CORE", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp)
                            .testTag("nav_auth")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, TextGray)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LOGOUT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTrackingWorkout == null && currentModuleTab == "workout") {
                WorkoutActionButton(
                    dayName = dayName,
                    onClick = { workoutName ->
                        handleStartWorkout(workoutName)
                    }
                )
            }
        },
        containerColor = PitchBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Title enclosed in thin border: System.Core
                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, NeonCyan), shape = RoundedCornerShape(2.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "WORKOUT LOG",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Right Section: Disconnect & Round ID Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "User: ${activeUser?.username ?: "Guest"}",
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    // ID Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(BorderStroke(1.dp, NeonCyan), shape = androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (activeUser?.username?.take(2)?.uppercase() ?: "ID"),
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderGray)
                    .padding(bottom = 12.dp)
            )

            // Dynamic Content Screens depending on Module Tab & Active Workout Detail
            when (currentModuleTab) {
                "workout" -> {
                    WorkoutTabScreen(
                        progression = progression ?: ProgressionStateEntity(),
                        dayName = dayName,
                        isWeekend = isWeekend,
                        logs = logs,
                        pausedWorkoutId = pausedWorkoutId,
                        onDiscardPausedWorkout = {
                            viewModel.clearWorkoutProgress()
                            Toast.makeText(context, "PAUSED WORKOUT DISCARDED", Toast.LENGTH_SHORT).show()
                        },
                        onStartWorkout = handleStartWorkout,
                        onEditRoutine = { planId -> editingPlanId = planId },
                        onProgressionSelected = { type ->
                            viewModel.applyProgression(type)
                            Toast.makeText(context, "PLAN PROTOCOLS ADVANCED", Toast.LENGTH_SHORT).show()
                        },
                        onResetProgression = {
                            viewModel.resetProgression()
                            Toast.makeText(context, "PLAN REVERTED TO ORIGIN", Toast.LENGTH_SHORT).show()
                        },
                        onSetCustomProgression = { week, reps, sets, rest, harder, cardio ->
                            viewModel.setExactProgression(week, reps, sets, rest, harder, cardio)
                            Toast.makeText(context, "PROGRESSION UPDATED TO WEEK $week", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                "history" -> {
                    HistoryTabScreen(
                        logs = logs,
                        onClearAll = {
                            viewModel.clearAllLogs()
                            Toast.makeText(context, "HISTORY STORAGE SECURED AND WIPED", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun WorkoutTabScreen(
    progression: ProgressionStateEntity,
    dayName: String,
    isWeekend: Boolean,
    logs: List<com.example.data.model.WorkoutLogEntity>,
    pausedWorkoutId: String? = null,
    onDiscardPausedWorkout: () -> Unit = {},
    onStartWorkout: (String) -> Unit,
    onEditRoutine: (String) -> Unit,
    onProgressionSelected: (String) -> Unit,
    onResetProgression: () -> Unit,
    onSetCustomProgression: (Int, Int, Int, Int, Boolean, Int) -> Unit = { _, _, _, _, _, _ -> }
) {
    var showProgressionHelp by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Paused Workout in Progress Banner
        if (pausedWorkoutId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(2.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WORKOUT IN PROGRESS",
                                    color = Color(0xFFFFB300),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = pausedWorkoutId.uppercase(),
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartWorkout(pausedWorkoutId) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = PitchBlack
                                ),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RESUME WORKOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            OutlinedButton(
                                onClick = onDiscardPausedWorkout,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(36.dp),
                                border = BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text("DISCARD", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Workout Heatmap showing athlete activity
        item {
            WorkoutHeatmap(logs = logs)
        }

        // Progression suggestion panel (Weekend Triggered or Manual override) - Styled with custom dashed border
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBorder(
                        width = if (isWeekend) 2.dp else 1.dp,
                        color = if (isWeekend) NeonCyan else MutedCyan
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = "Progression icon",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWeekend) "WEEKEND PROGRESSION" else "PROGRESSION LEVEL",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = { showProgressionHelp = !showProgressionHelp }) {
                        Icon(
                            imageVector = if (showProgressionHelp) Icons.Default.ExpandLess else Icons.Default.Info,
                            contentDescription = "Help Info",
                            tint = NeonCyan
                        )
                    }
                }

                if (isWeekend) {
                    Text(
                        text = "Weekend recommendation: Update your level by choosing one progression option below.",
                        color = LightCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                } else {
                    Text(
                        text = "Your training settings are listed below. You can apply a progression option anytime to update your plan.",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                if (showProgressionHelp) {
                    Text(
                        text = "Rules:\n• Try ONE of these every weekend\n• Never increase sets and reps simultaneously\n• Ensure rest cycles stay active\n• Progression updates all exercise sheets instantly.",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .border(1.dp, BorderGray)
                            .padding(8.dp)
                    )
                }

                // Current modifier list
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Reps: +${progression.repsModifier}", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Sets: +${progression.setsModifier}", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Rest: ${progression.restDurationSeconds}s", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Cardio: ${if (progression.cardioPhase == 1) "Phase 1" else "Phase 2"}", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Difficulty: ${if (progression.isHarderVariation) "Advanced" else "Base"}", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Week: ${progression.currentWorkoutWeek}", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progression selection items (never fill colors, just outlines)
                Text(
                    "CHOOSE AN UPGRADE:",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onProgressionSelected("reps") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_rep"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("+1 Rep", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onProgressionSelected("sets") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_set"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("+1 Set", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onProgressionSelected("rest") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_rest"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("-5s Rest", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onProgressionSelected("harder") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_harder"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Harder Var", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onProgressionSelected("cardio") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_cardio"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Cardio Phase 2", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onProgressionSelected("advance_week") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("button_prog_week"),
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Next Week", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        border = BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("button_open_restore_dialog")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Set Level", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SET LEVEL / WEEK", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onResetProgression
                    ) {
                        Text(
                            "Reset to origin",
                            color = Color.Red,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }

        // Active day indicator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Schedule Tracker",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Today is $dayName",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Today Icon",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Section Title: Morning Routine (20-25 min) with Edit Protocols button
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Morning Routine (Daily)",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedButton(
                        onClick = { onEditRoutine("morning_cardio") },
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("button_customize_protocols"),
                        border = BorderStroke(1.dp, NeonCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NeonCyan.copy(alpha = 0.08f),
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Protocols",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "EDIT PROTOCOLS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Cards for morning routines
                val cardioLog = getTitleCompletedLog("Morning warm-up & Cardio", logs)
                MorningRoutineCard(
                    title = "Morning warm-up & Cardio",
                    duration = "15-17 min",
                    isCompleted = cardioLog != null && cardioLog.completedExercisesCount >= cardioLog.totalExercisesCount && cardioLog.totalExercisesCount > 0,
                    completedCount = cardioLog?.completedExercisesCount ?: 0,
                    totalCount = cardioLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Morning warm-up & Cardio") },
                    onEdit = { onEditRoutine("morning_cardio") }
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                val coreLog = getTitleCompletedLog("Morning Core", logs)
                MorningRoutineCard(
                    title = "Morning Core",
                    duration = "5-7 min",
                    isCompleted = coreLog != null && coreLog.completedExercisesCount >= coreLog.totalExercisesCount && coreLog.totalExercisesCount > 0,
                    completedCount = coreLog?.completedExercisesCount ?: 0,
                    totalCount = coreLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Morning Core") },
                    onEdit = { onEditRoutine("morning_core") }
                )
            }
        }

        // Section Title: Evening Strength (30 min)
        item {
            Column {
                Text(
                    text = "Evening Routine",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Highlight Push days: Monday / Thursday
                val highlightPush = dayName == "Monday" || dayName == "Thursday"
                val pushLog = getTitleCompletedLog("Push Day (Chest, Shoulders, Triceps)", logs)
                EveningRoutineCard(
                    title = "Push Day (Chest, Shoulders, Triceps)",
                    schedule = "Mon / Thu",
                    isRecommended = highlightPush,
                    isCompleted = pushLog != null && pushLog.completedExercisesCount >= pushLog.totalExercisesCount && pushLog.totalExercisesCount > 0,
                    completedCount = pushLog?.completedExercisesCount ?: 0,
                    totalCount = pushLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Evening Push") },
                    onEdit = { onEditRoutine("evening_push") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Leg days: Tuesday / Friday
                val highlightLegs = dayName == "Tuesday" || dayName == "Friday"
                val legsLog = getTitleCompletedLog("Legs Day (Quads, Hamstrings, Calves)", logs)
                EveningRoutineCard(
                    title = "Legs Day (Quads, Hamstrings, Calves)",
                    schedule = "Tue / Fri",
                    isRecommended = highlightLegs,
                    isCompleted = legsLog != null && legsLog.completedExercisesCount >= legsLog.totalExercisesCount && legsLog.totalExercisesCount > 0,
                    completedCount = legsLog?.completedExercisesCount ?: 0,
                    totalCount = legsLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Evening Legs") },
                    onEdit = { onEditRoutine("evening_legs") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Pull days: Wednesday / Saturday
                val highlightPull = dayName == "Wednesday" || dayName == "Saturday"
                val pullLog = getTitleCompletedLog("Pull Day (Core & Pull Protocol)", logs)
                EveningRoutineCard(
                    title = "Pull Day (Core & Pull Protocol)",
                    schedule = "Wed / Sat",
                    isRecommended = highlightPull,
                    isCompleted = pullLog != null && pullLog.completedExercisesCount >= pullLog.totalExercisesCount && pullLog.totalExercisesCount > 0,
                    completedCount = pullLog?.completedExercisesCount ?: 0,
                    totalCount = pullLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Evening Pull") },
                    onEdit = { onEditRoutine("evening_pull") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Walk days: Sunday
                val highlightWalk = dayName == "Sunday"
                val walkLog = getTitleCompletedLog("Sunday Stretch & Mindful Walk", logs)
                EveningRoutineCard(
                    title = "Sunday Stretch & Mindful Walk",
                    schedule = "Sun",
                    isRecommended = highlightWalk,
                    isCompleted = walkLog != null && walkLog.completedExercisesCount >= walkLog.totalExercisesCount && walkLog.totalExercisesCount > 0,
                    completedCount = walkLog?.completedExercisesCount ?: 0,
                    totalCount = walkLog?.totalExercisesCount ?: 0,
                    onStart = { onStartWorkout("Sunday Stretch & Walk") },
                    onEdit = { onEditRoutine("sunday_walk") }
                )
            }
        }
    }

    if (showRestoreDialog) {
        var selectedWeek by remember { mutableIntStateOf(progression.currentWorkoutWeek) }
        var selectedRepsMod by remember { mutableIntStateOf(progression.repsModifier) }
        var selectedSetsMod by remember { mutableIntStateOf(progression.setsModifier) }
        var selectedRest by remember { mutableIntStateOf(progression.restDurationSeconds) }
        var isHarder by remember { mutableStateOf(progression.isHarderVariation) }
        var cardioPhase by remember { mutableIntStateOf(progression.cardioPhase) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = PitchBlack,
            modifier = Modifier.border(1.dp, NeonCyan),
            title = {
                Text(
                    "SET / RESTORE PROGRESSION",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Restore or customize your current training level and week instantly:",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Week Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Week:", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (selectedWeek > 1) selectedWeek-- },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("-", color = NeonCyan, fontSize = 14.sp)
                            }
                            Text(
                                "  Week $selectedWeek  ",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedButton(
                                onClick = { selectedWeek++ },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("+", color = NeonCyan, fontSize = 14.sp)
                            }
                        }
                    }

                    // Reps Modifier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reps Bonus:", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (selectedRepsMod > 0) selectedRepsMod-- },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("-", color = NeonCyan, fontSize = 14.sp)
                            }
                            Text(
                                "  +$selectedRepsMod  ",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedButton(
                                onClick = { selectedRepsMod++ },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("+", color = NeonCyan, fontSize = 14.sp)
                            }
                        }
                    }

                    // Sets Modifier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sets Bonus:", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (selectedSetsMod > 0) selectedSetsMod-- },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("-", color = NeonCyan, fontSize = 14.sp)
                            }
                            Text(
                                "  +$selectedSetsMod  ",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedButton(
                                onClick = { selectedSetsMod++ },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("+", color = NeonCyan, fontSize = 14.sp)
                            }
                        }
                    }

                    // Rest duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rest Time:", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (selectedRest > 15) selectedRest -= 5 },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
                            ) {
                                Text("-", color = NeonCyan, fontSize = 14.sp)
                            }
                            Text(
                                "  ${selectedRest}s  ",
                                color = Color.Yellow,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedButton(
                                onClick = { selectedRest += 5 },
                                modifier = Modifier.size(28.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NeonCyan)
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
                        onSetCustomProgression(selectedWeek, selectedRepsMod, selectedSetsMod, selectedRest, isHarder, cardioPhase)
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("APPLY LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("CANCEL", color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun MorningRoutineCard(
    title: String,
    duration: String,
    isCompleted: Boolean = false,
    completedCount: Int = 0,
    totalCount: Int = 0,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    val isPartiallyDone = completedCount > 0 && !isCompleted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCompleted) NeonCyan.copy(alpha = 0.3f) else if (isPartiallyDone) Color(0xFFFFB300).copy(alpha = 0.5f) else BorderGray
            )
            .clickable { onStart() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                color = if (isCompleted) TextGray else if (isPartiallyDone) Color(0xFFFFB300) else TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isCompleted || isPartiallyDone) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "DONE",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isPartiallyDone) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "DONE ($completedCount/$totalCount)",
                            color = Color(0xFFFFB300),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Duration: $duration • Daily",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("button_edit_${title.take(8).replace(" ", "_")}")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Reps & Sets",
                    tint = NeonCyan.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onStart) {
                Icon(
                    if (isCompleted || isPartiallyDone) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    contentDescription = "Start Workout",
                    tint = if (isCompleted) NeonCyan else if (isPartiallyDone) Color(0xFFFFB300) else NeonCyan
                )
            }
        }
    }
}

@Composable
fun EveningRoutineCard(
    title: String,
    schedule: String,
    isRecommended: Boolean,
    isCompleted: Boolean = false,
    completedCount: Int = 0,
    totalCount: Int = 0,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    val isPartiallyDone = completedCount > 0 && !isCompleted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = if (isRecommended && !isCompleted) 1.5.dp else 1.dp,
                    color = if (isCompleted) NeonCyan.copy(alpha = 0.3f) else if (isPartiallyDone) Color(0xFFFFB300).copy(alpha = 0.5f) else if (isRecommended) NeonCyan else BorderGray
                )
            )
            .clickable { onStart() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                color = if (isCompleted) TextGray else if (isPartiallyDone) Color(0xFFFFB300) else if (isRecommended) NeonCyan else TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isCompleted || isPartiallyDone) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecommended && !isCompleted && !isPartiallyDone) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "TODAY",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "DONE",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isPartiallyDone) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(2.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "DONE ($completedCount/$totalCount)",
                            color = Color(0xFFFFB300),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Schedule: $schedule • Strength Plan",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("button_edit_${title.take(8).replace(" ", "_")}")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Reps & Sets",
                    tint = NeonCyan.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onStart) {
                Icon(
                    if (isCompleted || isPartiallyDone) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                    contentDescription = "Start strength routine",
                    tint = if (isCompleted) NeonCyan else if (isPartiallyDone) Color(0xFFFFB300) else if (isRecommended) NeonCyan else TextGray
                )
            }
        }
    }
}

@Composable
fun WorkoutTrackerPane(
    routineName: String,
    progression: ProgressionStateEntity,
    onComplete: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    // Generate exercises based on routineName and state modifier
    val rawList = when (routineName) {
        "Morning warm-up & Cardio" -> {
            val cardioMoves = if (progression.cardioPhase == 1) {
                listOf(
                    "Jog in place (Steady pace) — 30 sec",
                    "High Knees (Active effort) — 20 sec",
                    "Rest and catch breath — ${progression.restDurationSeconds} sec"
                )
            } else {
                val modText = if (progression.isHarderVariation) "Max Pace " else ""
                listOf(
                    "${modText}Mountain Climbers (Core tight) — 30 sec",
                    "${modText}Burpees (High impact) — 20 sec",
                    "${modText}Skater hops (Side to side) — 30 sec",
                    "Active rest interval — 30 sec"
                )
            }
            
            val totalRounds = 5 + progression.setsModifier
            val list = mutableListOf<String>()
            // 5 min warm up
            list.add("Jumping Jacks — ${50 + 5 * progression.repsModifier} reps")
            list.add("Arm circles (Mobility focus) — 1 min")
            list.add("Hip circles (Mobility focus) — 1 min")
            list.add("Leg swings (Dynamic stretch) — 1 min")
            list.add("Neck mobility circles — 1 min")
            
            // Cardio rounds
            list.add("--- CARDIO SYSTEM INTERVALS (${totalRounds} ROUNDS) ---")
            for (i in 1..totalRounds) {
                list.add("Round $i of ${totalRounds}")
                list.addAll(cardioMoves)
            }
            // Stretch finish
            list.add("Cooldown static stretching — 2 min")
            list
        }
        "Morning Core" -> {
            val list = mutableListOf<String>()
            val totalRounds = 3 + progression.setsModifier
            list.add("--- CORE PROTOCOL: ${totalRounds} ROUNDS ---")
            for (i in 1..totalRounds) {
                list.add("Round $i of ${totalRounds}")
                list.add("Plank — ${45 + 5 * progression.repsModifier} sec")
                list.add("Bicycle crunches — ${20 + progression.repsModifier} reps")
                list.add("Side plank — ${30 + 5 * progression.repsModifier} sec each side")
                list.add("Superman Raise — ${12 + progression.repsModifier} reps")
            }
            list.add("Cooldown stretch (Cobra/Child's pose) — 2 min")
            list
        }
        "Evening Push" -> {
            val sets = 3 + progression.setsModifier
            val list = mutableListOf<String>()
            val v = progression.isHarderVariation
            
            list.add(if (v) "Decline Push-ups — $sets sets of ${8 + progression.repsModifier} to ${12 + progression.repsModifier} reps" else "Regular Push-ups — $sets sets of ${8 + progression.repsModifier} to ${12 + progression.repsModifier} reps")
            list.add(if (v) "Archer Push-ups — $sets sets of ${10 + progression.repsModifier} reps" else "Incline Push-ups (using a chair) — $sets sets of ${12 + progression.repsModifier} reps")
            list.add(if (v) "Elevated Pike Push-ups — $sets sets of ${8 + progression.repsModifier} reps" else "Pike Push-ups — $sets sets of ${8 + progression.repsModifier} reps")
            list.add(if (v) "Diamond Push-ups (decline/incline) — $sets sets of ${6 + progression.repsModifier} reps" else "Diamond Push-ups — $sets sets of ${6 + progression.repsModifier} reps")
            list.add(if (v) "Weighted Chair Dips — $sets sets of ${10 + progression.repsModifier} reps" else "Chair Dips — $sets sets of ${10 + progression.repsModifier} reps")
            list.add("--- FINISHER CHALLENGE ---")
            list.add("Plank Hold Finisher — 2 sets of ${45 + 5 * progression.repsModifier} sec")
            list
        }
        "Evening Legs" -> {
            val sets = 3 + progression.setsModifier
            val list = mutableListOf<String>()
            val v = progression.isHarderVariation
            
            list.add(if (v) "Pistol / Jump Squats — $sets sets of ${10 + progression.repsModifier} reps" else "Standard Squats — $sets sets of ${15 + progression.repsModifier} reps")
            list.add(if (v) "Jumping Lunges — $sets sets of ${10 + progression.repsModifier} reps each" else "Reverse Lunges — $sets sets of ${10 + progression.repsModifier} reps each")
            list.add(if (v) "Single Leg Glute Bridge — $sets sets of ${12 + progression.repsModifier} reps each" else "Standard Glute Bridge — $sets sets of ${15 + progression.repsModifier} reps")
            list.add(if (v) "Single Leg Wall Sit — $sets sets of ${30 + progression.repsModifier} sec" else "Wall Sit Hold — $sets sets of ${45 + 5 * progression.repsModifier} sec")
            list.add(if (v) "Single Leg Calf Raises — $sets sets of ${15 + progression.repsModifier} reps each" else "Calf Raises — $sets sets of ${20 + progression.repsModifier} reps")
            list.add("--- FINISHER CHALLENGE ---")
            list.add("Jump Squats Plyo Burst — 2 sets of ${10 + progression.repsModifier} reps")
            list
        }
        "Evening Pull" -> {
            val list = mutableListOf<String>()
            val totalRounds = 3 + progression.setsModifier
            list.add("--- PULL & CORE PROTOCOL: ${totalRounds} ROUNDS ---")
            for (i in 1..totalRounds) {
                list.add("Round $i of ${totalRounds}")
                list.add("Reverse crunch — ${12 + progression.repsModifier} reps")
                list.add("Dead bug — ${10 + progression.repsModifier} each side")
                list.add("Plank — ${45 + 5 * progression.repsModifier} sec")
                list.add("Side plank — ${30 + 5 * progression.repsModifier} sec each side")
                list.add("Mountain climbers — ${30 + 5 * progression.repsModifier} total")
            }
            list.add("Cooldown stretch (Cobra/Child's pose) — 2 min")
            list
        }
        else -> { // Sunday Walk
            listOf(
                "Mindful Outdoor Walk — 20 to 30 min",
                "Deep Full Body Cooldown Static Stretch — 10 min"
            )
        }
    }

    // Interactive tracking state
    var checkedState by remember(routineName) { mutableStateOf(List(rawList.size) { false }) }
    var timerSeconds by remember { mutableStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Active stopwatch logic
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, NeonCyan))
            .padding(16.dp)
    ) {
        // Tracker Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE SESSION TRACKER",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = routineName.uppercase(),
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Abort Tracking", tint = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timer Panel (strictly outlined, black background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "CHRONOMETER PROTOCOL",
                    color = TextGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                val minStr = (timerSeconds / 60).toString().padStart(2, '0')
                val secStr = (timerSeconds % 60).toString().padStart(2, '0')
                Text(
                    "$minStr:$secStr",
                    color = NeonCyan,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row {
                OutlinedButton(
                    onClick = { timerRunning = !timerRunning },
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(36.dp).testTag("timer_toggle")
                ) {
                    Text(
                        if (timerRunning) "PAUSE" else "START",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = {
                        timerRunning = false
                        timerSeconds = 0
                    },
                    border = BorderStroke(1.dp, BorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = TextWhite),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.height(36.dp).testTag("timer_reset")
                ) {
                    Text("RESET", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CHECKLIST PROTOCOLS (TAP TO MARK COMPLETED):",
            color = NeonCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Lazy list of tasks
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rawList.size) { index ->
                val text = rawList[index]
                val isHeader = text.startsWith("---") && text.endsWith("---")
                val isRound = text.startsWith("Round ")
                
                if (isHeader) {
                    Text(
                        text = text,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else if (isRound) {
                    Text(
                        text = text,
                        color = LightCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textDecoration = TextDecoration.Underline
                    )
                } else {
                    val isChecked = checkedState[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isChecked) NeonCyan else BorderGray)
                            .clickable {
                                checkedState = checkedState
                                    .toMutableList()
                                    .also { it[index] = !isChecked }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Task checkbox",
                            tint = if (isChecked) NeonCyan else TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            color = if (isChecked) NeonCyan else TextWhite,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Complete Workout Action Button (Outlined only, no fills)
        val completedCount = checkedState.count { it }
        val checkableCount = rawList.count { !it.startsWith("---") && !it.startsWith("Round ") }
        
        OutlinedButton(
            onClick = {
                timerRunning = false
                onComplete(completedCount, checkableCount)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_complete_workout"),
            border = BorderStroke(1.dp, NeonCyan),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = NeonCyan),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "LOG SESSION STATUS [COMPLETED $completedCount/$checkableCount]",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun HistoryTabScreen(
    logs: List<WorkoutLogEntity>,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HistoryToggleOff,
                    contentDescription = "Logs",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Workout History",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            if (logs.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text(
                        "Clear History",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, BorderGray)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = "Empty History",
                    tint = TextGray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No workouts logged yet",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Complete workouts to save history logs here.",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.routineType.uppercase(),
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val dateStr = sdf.format(Date(log.timestamp))
                            Text(
                                text = dateStr,
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Week: ${log.week} • Exercises: ${log.completedExercisesCount}/${log.totalExercisesCount}",
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            val isFull = log.completedExercisesCount >= log.totalExercisesCount && log.totalExercisesCount > 0
                            val isPartial = log.completedExercisesCount > 0 && !isFull
                            val badgeColor = when {
                                isFull -> Color(0xFF00FF00) // Green
                                isPartial -> Color(0xFFFFB300) // Amber/Yellow
                                else -> Color(0xFFFF5252) // Red
                            }
                            val badgeLabel = when {
                                isFull -> "100% COMPLETE"
                                isPartial -> "DONE (${log.completedExercisesCount}/${log.totalExercisesCount})"
                                else -> "INCOMPLETE"
                            }

                            Box(
                                modifier = Modifier
                                    .border(1.dp, badgeColor, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Today's Routine completion mapping and checker
fun getTitleCompletedLog(title: String, logs: List<com.example.data.model.WorkoutLogEntity>): com.example.data.model.WorkoutLogEntity? {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = dateFormat.format(Date())
    val mappedRoutines = when (title) {
        "Morning warm-up & Cardio" -> listOf("Morning warm-up & Cardio")
        "Morning Core" -> listOf("Morning Core")
        "Push Day (Chest, Shoulders, Triceps)" -> listOf("Evening Push", "Push Day (Chest, Shoulders, Triceps)")
        "Pull Day (Core & Pull Protocol)" -> listOf("Evening Pull", "Pull Day (Core & Pull Protocol)", "Pull Day (Back, Rear Delts, Biceps)")
        "Pull Day (Back, Rear Delts, Biceps)" -> listOf("Evening Pull", "Pull Day (Back, Rear Delts, Biceps)", "Pull Day (Core & Pull Protocol)")
        "Sunday Stretch & Mindful Walk" -> listOf("Sunday Stretch & Walk", "Sunday Stretch & Mindful Walk")
        else -> listOf(title)
    }
    return logs.firstOrNull { log ->
        dateFormat.format(Date(log.timestamp)) == todayStr &&
        mappedRoutines.any { it.equals(log.routineType, ignoreCase = true) }
    }
}

fun isTitleCompletedToday(title: String, logs: List<com.example.data.model.WorkoutLogEntity>): Boolean {
    val log = getTitleCompletedLog(title, logs) ?: return false
    return log.completedExercisesCount >= log.totalExercisesCount && log.totalExercisesCount > 0
}

// GitHub-like Activity Heatmap (Last 35 Days)
@Composable
fun WorkoutHeatmap(logs: List<com.example.data.model.WorkoutLogEntity>, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    val logsByDate = remember(logs) {
        logs.groupBy { dateFormat.format(Date(it.timestamp)) }
    }
    
    val daysList = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -34) // 5 weeks (35 days)
        for (i in 0..34) {
            list.add(dateFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .padding(12.dp)
    ) {
        Text(
            text = "Workout Activity (Last 35 days)",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Weekday symbols
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        text = label,
                        color = TextGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.height(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // 5 Columns representing weeks
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (weekIndex in 0 until 5) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (dayIndex in 0 until 7) {
                            val dateIdx = weekIndex * 7 + dayIndex
                            val dateStr = daysList.getOrNull(dateIdx) ?: ""
                            val dayLogs = logsByDate[dateStr] ?: emptyList()
                            
                            val cellColor = if (dayLogs.isEmpty()) {
                                Color(0xFF3B1515) // Deep Red for empty
                            } else {
                                val totalCompleted = dayLogs.sumOf { it.completedExercisesCount }
                                val totalExercises = dayLogs.sumOf { it.totalExercisesCount }
                                val percentage = if (totalExercises > 0) {
                                    totalCompleted.toFloat() / totalExercises
                                } else 0f
                                
                                when {
                                    percentage >= 1f -> Color(0xFF00FF00) // Green
                                    percentage > 0.66f -> Color(0xFFFFFF00) // Intensified Yellow
                                    percentage > 0.33f -> Color(0xFFAC9420) // Medium Yellow
                                    else -> Color(0xFF6B5B15) // Light Yellow
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(cellColor)
                                    .border(0.5.dp, PitchBlack)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Heatmap Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Activity Intensity Key",
                color = TextGray,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("None", color = TextGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF3B1515)))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF6B5B15)))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFAC9420)))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFFF00)))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00FF00)))
                Text("Done", color = TextGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// Workout Action Button (Floating) with active time zones and pulse animation
@Composable
fun WorkoutActionButton(
    dayName: String,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    val isMorningTime = currentHour in 4..10 // 4 AM - 11 AM
    val isEveningTime = currentHour in 16..20 // 4 PM - 9 PM
    
    val targetWorkout = when {
        isMorningTime -> "Morning warm-up & Cardio"
        isEveningTime -> {
            when (dayName) {
                "Monday", "Thursday" -> "Evening Push"
                "Tuesday", "Friday" -> "Evening Legs"
                "Wednesday", "Saturday" -> "Evening Pull"
                "Sunday" -> "Sunday Stretch & Walk"
                else -> "Evening Push"
            }
        }
        else -> null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "FABPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FABPulse"
    )

    if (targetWorkout != null) {
        FloatingActionButton(
            onClick = { onClick(targetWorkout) },
            containerColor = PitchBlack,
            modifier = Modifier
                .scale(pulseScale)
                .border(BorderStroke(1.5.dp, NeonCyan), shape = RoundedCornerShape(4.dp))
                .testTag("fab_start_workout"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Workout",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Start Workout",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        FloatingActionButton(
            onClick = {
                Toast.makeText(
                    context, 
                    "Resting now. Workouts are scheduled from 4-11 AM and 4-9 PM.", 
                    Toast.LENGTH_LONG
                ).show()
            },
            containerColor = PitchBlack,
            modifier = Modifier
                .scale(pulseScale)
                .border(BorderStroke(1.dp, BorderGray), shape = RoundedCornerShape(4.dp))
                .testTag("fab_rest_workout"),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Rest",
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resting",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

