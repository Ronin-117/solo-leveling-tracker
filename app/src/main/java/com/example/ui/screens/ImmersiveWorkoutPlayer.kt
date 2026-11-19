package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProgressionStateEntity
import com.example.data.model.WorkoutPlanEntity
import com.example.data.model.WorkoutStep
import com.example.data.model.getExercisesList
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ImmersiveWorkoutPlayer(
    plan: WorkoutPlanEntity,
    progression: ProgressionStateEntity,
    onComplete: (Int, Int) -> Unit,
    onClose: () -> Unit,
    onPause: (Int, Map<Int, Boolean>, Map<Int, Int>) -> Unit,
    initialStepIdx: Int = 0,
    initialCompletedStates: Map<Int, Boolean> = emptyMap(),
    initialCurrentReps: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape orientation during workout and restore on exit
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                activity?.requestedOrientation = originalOrientation
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Generate workout steps on start
    val steps = remember(plan, progression) {
        val exercises = plan.getExercisesList()
        val generated = mutableListOf<WorkoutStep>()
        
        val baseRounds = plan.baseRounds
        val totalCircuitSets = (baseRounds + progression.setsModifier).coerceAtLeast(1)

        val warmups = exercises.filter { it.baseSets == 1 && !it.isFinisher && !it.name.contains("cooldown", ignoreCase = true) && !it.name.contains("stretch", ignoreCase = true) }
        val circuits = exercises.filter { it.baseSets > 1 && !it.isFinisher && !it.name.contains("cooldown", ignoreCase = true) && !it.name.contains("stretch", ignoreCase = true) }
        val singleSetCircuits = exercises.filter { it.baseSets == 1 && !it.isFinisher && !it.name.contains("cooldown", ignoreCase = true) && !it.name.contains("stretch", ignoreCase = true) && warmups.none { w -> w.name == it.name } }
        val effectiveCircuits = if (circuits.isNotEmpty()) circuits else singleSetCircuits
        
        val effectiveCircuitSets = when (plan.id) {
            "morning_core" -> 3 + progression.setsModifier
            "evening_pull" -> 3 + progression.setsModifier
            "morning_cardio" -> 5 + progression.setsModifier
            else -> totalCircuitSets
        }.coerceAtLeast(1)

        val finishers = exercises.filter { it.isFinisher }
        val cooldowns = exercises.filter { it.name.contains("cooldown", ignoreCase = true) || it.name.contains("stretch", ignoreCase = true) }

        // Phase 1: Warmup items
        warmups.forEach { ex ->
            val target = (ex.baseReps + progression.repsModifier).coerceAtLeast(1)
            generated.add(
                WorkoutStep(
                    name = ex.name,
                    targetReps = target,
                    repUnit = ex.repUnit,
                    currentSetLabel = "WARMUP (SET 1/1)"
                )
            )
            generated.add(
                WorkoutStep(
                    name = "WARMUP REST RECOVERY",
                    targetReps = 0,
                    repUnit = "sec",
                    isRest = true,
                    restDuration = 10,
                    currentSetLabel = "TRANSITION"
                )
            )
        }

        // Phase 2: Main Circuit Sets
        for (round in 1..effectiveCircuitSets) {
            effectiveCircuits.forEachIndexed { exIdx, ex ->
                val target = (ex.baseReps + progression.repsModifier).coerceAtLeast(1)
                generated.add(
                    WorkoutStep(
                        name = ex.name,
                        targetReps = target,
                        repUnit = ex.repUnit,
                        currentSetLabel = "ROUND $round/$effectiveCircuitSets"
                    )
                )

                val isLastExerciseOfRound = exIdx == effectiveCircuits.size - 1
                val isFinalRound = round == effectiveCircuitSets

                val restDuration = when {
                    isLastExerciseOfRound && !isFinalRound -> 45
                    isLastExerciseOfRound && isFinalRound -> 30
                    else -> 15
                }

                generated.add(
                    WorkoutStep(
                        name = if (isLastExerciseOfRound) "ROUND $round COMPLETED - RECOVERY" else "INTER-EXERCISE RECOVERY",
                        targetReps = 0,
                        repUnit = "sec",
                        isRest = true,
                        restDuration = restDuration,
                        currentSetLabel = "REST INTERVAL"
                    )
                )
            }
        }

        // Phase 3: Finishers
        finishers.forEach { fin ->
            val finisherSets = fin.baseSets.coerceAtLeast(1)
            for (fSet in 1..finisherSets) {
                generated.add(
                    WorkoutStep(
                        name = fin.name,
                        targetReps = fin.baseReps,
                        repUnit = fin.repUnit,
                        currentSetLabel = "FINISHER BURN ($fSet/$finisherSets)"
                    )
                )
                generated.add(
                    WorkoutStep(
                        name = "FINISHER REST",
                        targetReps = 0,
                        repUnit = "sec",
                        isRest = true,
                        restDuration = 25,
                        currentSetLabel = "REST"
                    )
                )
            }
        }

        // Phase 4: Cooldown
        cooldowns.forEach { cool ->
            generated.add(
                WorkoutStep(
                    name = cool.name,
                    targetReps = cool.baseReps,
                    repUnit = cool.repUnit,
                    currentSetLabel = "COOLDOWN & RECOVERY"
                )
            )
        }

        // Restore any initial state
        initialCompletedStates.forEach { (idx, completed) ->
            if (idx in generated.indices) {
                generated[idx].isCompleted = completed
            }
        }
        initialCurrentReps.forEach { (idx, reps) ->
            if (idx in generated.indices) {
                generated[idx].currentReps = reps
            }
        }

        generated
    }

    val totalSteps = steps.size
    val checkableStepsCount = steps.count { !it.isRest }

    var currentStepIdx by remember(steps) {
        mutableIntStateOf(initialStepIdx.coerceIn(0, (totalSteps - 1).coerceAtLeast(0)))
    }

    val currentStep = steps.getOrNull(currentStepIdx)

    // Interactive tracking state
    var activeRepsState by remember(currentStepIdx) { 
        mutableIntStateOf(currentStep?.currentReps ?: 0) 
    }
    
    var activeCompletedState by remember(currentStepIdx) {
        mutableStateOf(currentStep?.isCompleted ?: false)
    }

    // Auto rest countdown timer
    var restCountdown by remember(currentStepIdx) {
        mutableIntStateOf(if (currentStep?.isRest == true) currentStep.restDuration else 0)
    }

    // --- Timed Exercise Detection & State ---
    val isTimedExercise = currentStep != null && !currentStep.isRest && currentStep.repUnit.equals("sec", ignoreCase = true)
    val isEachSide = remember(currentStep?.name) {
        currentStep?.name?.contains("each side", ignoreCase = true) == true
    }

    var currentSide by remember(currentStepIdx) { mutableIntStateOf(1) } // 1: First side (Left), 2: Second side (Right)
    var switchSideCountdown by remember(currentStepIdx) { mutableIntStateOf(0) } // 5..1 during 5s countdown
    var isTimerActive by remember(currentStepIdx) { mutableStateOf(false) }
    var timerSecondsRemaining by remember(currentStepIdx, currentStep?.targetReps) {
        mutableIntStateOf(currentStep?.targetReps ?: 0)
    }

    // Auto-save progress and exit cleanly without popup dialogs
    val handleExitAndSave = {
        val completedStates = steps.mapIndexed { index, s -> index to s.isCompleted }.toMap()
        val repsStates = steps.mapIndexed { index, s -> index to s.currentReps }.toMap()
        onPause(currentStepIdx, completedStates, repsStates)
    }

    // Centralized step advancement
    val advanceToNext = {
        playAudioFeedback(context)
        if (currentStepIdx < totalSteps - 1) {
            currentStepIdx++
        } else {
            val completedCheckable = steps.count { !it.isRest && it.isCompleted }
            onComplete(completedCheckable, checkableStepsCount)
        }
    }

    // Instant rest skip
    val skipRest = {
        if (currentStep?.isRest == true) {
            restCountdown = 0
            currentStep.isCompleted = true
            advanceToNext()
        }
    }

    // Manual side selection toggle
    val selectSide = { side: Int ->
        if (isEachSide && (side == 1 || side == 2)) {
            switchSideCountdown = 0
            currentSide = side
            timerSecondsRemaining = currentStep?.targetReps ?: 0
            activeRepsState = 0
            isTimerActive = true
        }
    }

    // Intercept hardware Android back button to auto-save progress and exit
    BackHandler {
        handleExitAndSave()
    }

    // Rest interval auto-countdown (continuous loop keyed on currentStepIdx)
    LaunchedEffect(currentStepIdx) {
        if (currentStep?.isRest == true) {
            while (isActive && restCountdown > 0) {
                delay(1000)
                restCountdown--
            }
            if (isActive && restCountdown <= 0 && currentStep.isRest) {
                currentStep.isCompleted = true
                advanceToNext()
            }
        }
    }

    // Switch sides 5-second countdown
    LaunchedEffect(currentStepIdx, switchSideCountdown > 0) {
        if (switchSideCountdown > 0) {
            while (isActive && switchSideCountdown > 0) {
                delay(1000)
                switchSideCountdown--
            }
            if (isActive && switchSideCountdown == 0 && currentStep != null && !activeCompletedState) {
                // Auto transition to Side 2 and start timer
                currentSide = 2
                timerSecondsRemaining = currentStep.targetReps
                playAudioFeedback(context)
                isTimerActive = true
            }
        }
    }

    // Timed exercise interactive countdown
    LaunchedEffect(currentStepIdx, isTimerActive, switchSideCountdown > 0) {
        if (isTimedExercise && isTimerActive && !activeCompletedState && switchSideCountdown == 0) {
            while (isActive && isTimerActive && timerSecondsRemaining > 0 && switchSideCountdown == 0) {
                delay(1000)
                timerSecondsRemaining--
                val target = currentStep?.targetReps ?: 0
                activeRepsState = (target - timerSecondsRemaining).coerceAtLeast(0)
            }
            if (isActive && isTimerActive && timerSecondsRemaining <= 0 && switchSideCountdown == 0) {
                playAudioFeedback(context)
                if (isEachSide && currentSide == 1) {
                    // Side 1 completed: Trigger 5-second switch sides transition
                    isTimerActive = false
                    switchSideCountdown = 5
                } else {
                    // Timed exercise fully completed
                    isTimerActive = false
                    activeCompletedState = true
                }
            }
        }
    }

    // Update steps list values when UI states change
    LaunchedEffect(activeRepsState) {
        currentStep?.currentReps = activeRepsState
        if (currentStep != null && !currentStep.isRest && !isTimedExercise && activeRepsState >= currentStep.targetReps && currentStep.targetReps > 0 && !activeCompletedState) {
            activeCompletedState = true
        }
    }

    LaunchedEffect(activeCompletedState) {
        currentStep?.isCompleted = activeCompletedState
        if (activeCompletedState) {
            currentStep?.currentReps = currentStep?.targetReps ?: 0
            activeRepsState = currentStep?.targetReps ?: 0
            
            // Auto move on to next exercise smoothly
            delay(1200)
            if (activeCompletedState && currentStep?.isCompleted == true) {
                advanceToNext()
            }
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        if (currentStep == null) {
            // Fallback empty view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No workout steps found", color = Color.Red, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("Exit")
                }
            }
        } else {
            var lastTapTime by remember(currentStepIdx) { mutableLongStateOf(0L) }

            // 1. FULL-SCREEN GESTURE DETECTOR & WORKOUT VIEW
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentStepIdx, isTimedExercise) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                            val downTime = System.currentTimeMillis()
                            val startPos = down.position
                            var isDragging = false
                            var verticalAccumulator = 0f

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!pointer.pressed) {
                                    // Pointer released (UP event)
                                    val upTime = System.currentTimeMillis()
                                    val duration = upTime - downTime
                                    val dx = kotlin.math.abs(pointer.position.x - startPos.x)
                                    val dy = kotlin.math.abs(pointer.position.y - startPos.y)

                                    if (!isDragging && dx < 28f && dy < 28f && duration < 400) {
                                        // Tap detected - only trigger double-tap if not consumed by a clickable button
                                        if (!pointer.isConsumed) {
                                            if (upTime - lastTapTime < 380) {
                                                // Double tap detected!
                                                lastTapTime = 0L
                                                if (currentStep.isRest) {
                                                    skipRest()
                                                } else {
                                                    isTimerActive = false
                                                    switchSideCountdown = 0
                                                    activeCompletedState = true
                                                }
                                            } else {
                                                lastTapTime = upTime
                                            }
                                        }
                                    }
                                    break
                                }

                                // Check vertical drag movement
                                val currentY = pointer.position.y
                                val deltaY = currentY - pointer.previousPosition.y
                                val totalDeltaY = currentY - startPos.y

                                if (!isDragging && kotlin.math.abs(totalDeltaY) > 14f) {
                                    isDragging = true
                                    verticalAccumulator = 0f
                                }

                                if (isDragging) {
                                    verticalAccumulator += deltaY
                                    val threshold = 18f
                                    if (isTimedExercise) {
                                        if (verticalAccumulator <= -threshold) {
                                            timerSecondsRemaining = (timerSecondsRemaining + 5).coerceAtLeast(1)
                                            verticalAccumulator = 0f
                                            pointer.consume()
                                        } else if (verticalAccumulator >= threshold) {
                                            timerSecondsRemaining = (timerSecondsRemaining - 5).coerceAtLeast(1)
                                            verticalAccumulator = 0f
                                            pointer.consume()
                                        }
                                    } else if (!currentStep.isRest) {
                                        val target = currentStep.targetReps.coerceAtLeast(1)
                                        if (verticalAccumulator <= -threshold) {
                                            activeRepsState = (activeRepsState + 1).coerceAtMost(target)
                                            verticalAccumulator = 0f
                                            pointer.consume()
                                        } else if (verticalAccumulator >= threshold) {
                                            activeRepsState = (activeRepsState - 1).coerceAtLeast(0)
                                            verticalAccumulator = 0f
                                            pointer.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // MAIN CONTENT UI (Flanked cleanly by the side arrows)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 68.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // TOP BAR: Progress, Exit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exit Button (Auto-saves progress directly without dialogs)
                    OutlinedButton(
                        onClick = {
                            handleExitAndSave()
                        },
                        border = BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("button_exit_immersive")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Workout", modifier = Modifier.size(14.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quit", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    // Workout Title Header
                    Text(
                        text = plan.name.uppercase(),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    // Progress bar / Label
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Step: ${currentStepIdx + 1}/$totalSteps",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressFactor = (currentStepIdx + 1).toFloat() / totalSteps
                        LinearProgressIndicator(
                            progress = { progressFactor },
                            modifier = Modifier
                                .width(160.dp)
                                .height(3.dp),
                            color = NeonCyan,
                            trackColor = Color(0xFF1E1E1E)
                        )
                    }
                }

                // CENTER BODY: Exercise Name & Interactive Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Set Category Badge
                    Text(
                        text = currentStep.currentSetLabel.uppercase(),
                        color = if (currentStep.isRest) Color.Yellow else NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Exercise Name - Strikethrough if completed
                    Text(
                        text = currentStep.name.uppercase(),
                        color = if (activeCompletedState) NeonCyan.copy(alpha = 0.5f) else TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        textDecoration = if (activeCompletedState) TextDecoration.LineThrough else TextDecoration.None,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Center Interactive Display
                    if (currentStep.isRest) {
                        // Rest Recovery Countdown
                        val progressPercent = restCountdown.toFloat() / currentStep.restDuration.coerceAtLeast(1)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Rest: ${restCountdown}s",
                                color = Color.Yellow,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                progress = { progressPercent },
                                color = Color.Yellow,
                                trackColor = Color(0xFF333300),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    skipRest()
                                },
                                border = BorderStroke(1.dp, Color.Yellow),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("button_skip_rest")
                            ) {
                                Text(
                                    "SKIP REST",
                                    color = Color.Yellow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isTimedExercise) {
                        // --- DEDICATED TIMER WINDOW ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        if (isTimerActive) NeonCyan else NeonCyan.copy(alpha = 0.4f)
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("timer_window"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1017)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Side Status Badges if Each Side (Interactive Toggles)
                                if (isEachSide) {
                                    Row(
                                        modifier = Modifier.padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (currentSide == 1) NeonCyan.copy(alpha = 0.25f) else Color(0xFF141923),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.5.dp, if (currentSide == 1) NeonCyan else TextGray.copy(alpha = 0.3f)),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    selectSide(1)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .testTag("button_side_1")
                                        ) {
                                            Text(
                                                text = if (currentSide == 2) "SIDE 1: LEFT ✓" else "SIDE 1: LEFT",
                                                color = if (currentSide == 1) NeonCyan else TextGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (currentSide == 2) NeonCyan.copy(alpha = 0.25f) else Color(0xFF141923),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.5.dp, if (currentSide == 2) NeonCyan else TextGray.copy(alpha = 0.3f)),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    selectSide(2)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .testTag("button_side_2")
                                        ) {
                                            Text(
                                                text = "SIDE 2: RIGHT",
                                                color = if (currentSide == 2) NeonCyan else TextGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                if (switchSideCountdown > 0) {
                                    // 5-second switch sides transition screen with clear countdown & manual skip
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "⚡ SWITCH SIDES ⚡",
                                            color = Color.Yellow,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "SIDE 2 (RIGHT) IN ${switchSideCountdown}s",
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${switchSideCountdown}s",
                                            color = Color.Yellow,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { selectSide(2) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonCyan,
                                                contentColor = PitchBlack
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("button_start_side_2_now")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Side 2", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "START SIDE 2 NOW",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Countdown Timer Clock Display
                                    val targetDuration = currentStep.targetReps.coerceAtLeast(1)
                                    val fraction = 1f - (timerSecondsRemaining.toFloat() / targetDuration.toFloat())
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        // Quick -5s button
                                        OutlinedButton(
                                            onClick = {
                                                timerSecondsRemaining = (timerSecondsRemaining - 5).coerceAtLeast(1)
                                            },
                                            modifier = Modifier.size(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            border = BorderStroke(1.dp, BorderStroke(1.dp, TextGray.copy(alpha = 0.4f)).brush),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("-5", fontSize = 9.sp, color = TextGray, fontFamily = FontFamily.Monospace)
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        val minutes = timerSecondsRemaining / 60
                                        val seconds = timerSecondsRemaining % 60
                                        val timeDisplay = String.format("%02d:%02d", minutes, seconds)
                                        
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = timeDisplay,
                                                fontSize = 38.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (timerSecondsRemaining <= 5 && isTimerActive) Color(0xFFFF5252) else if (isTimerActive) NeonCyan else TextWhite,
                                                letterSpacing = 2.sp
                                            )
                                            Text(
                                                text = if (isTimerActive) "COUNTING DOWN..." else if (timerSecondsRemaining == 0) "FINISHED" else "PAUSED",
                                                color = if (isTimerActive) NeonCyan else TextGray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Quick +5s button
                                        OutlinedButton(
                                            onClick = {
                                                timerSecondsRemaining += 5
                                            },
                                            modifier = Modifier.size(28.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            border = BorderStroke(1.dp, BorderStroke(1.dp, TextGray.copy(alpha = 0.4f)).brush),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("+5", fontSize = 9.sp, color = TextGray, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(4.dp),
                                        color = if (isTimerActive) NeonCyan else TextGray,
                                        trackColor = Color(0xFF18202C)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Timer Controls Row: RESET, START/PAUSE, COMPLETE
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Reset Button
                                        OutlinedButton(
                                            onClick = {
                                                isTimerActive = false
                                                switchSideCountdown = 0
                                                timerSecondsRemaining = currentStep.targetReps
                                                currentSide = 1
                                            },
                                            border = BorderStroke(1.dp, TextGray.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("button_timer_reset")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", modifier = Modifier.size(14.dp), tint = TextGray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("RESET", color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }

                                        // Primary START / PAUSE Button
                                        Button(
                                            onClick = {
                                                if (timerSecondsRemaining <= 0) {
                                                    timerSecondsRemaining = currentStep.targetReps
                                                }
                                                isTimerActive = !isTimerActive
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isTimerActive) Color(0xFFFFB300) else NeonCyan,
                                                contentColor = PitchBlack
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("button_timer_toggle")
                                        ) {
                                            Icon(
                                                if (isTimerActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isTimerActive) "Pause" else "Start",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isTimerActive) "PAUSE TIMER" else "START TIMER",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Complete Now Button
                                        OutlinedButton(
                                            onClick = {
                                                isTimerActive = false
                                                switchSideCountdown = 0
                                                activeCompletedState = true
                                            },
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("button_timer_complete_now")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(14.dp), tint = NeonCyan)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DONE", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                            }
                        }
                    } else {
                        // Standard Reps Tracker Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Manual Decrement Button
                            OutlinedButton(
                                onClick = {
                                    activeRepsState = (activeRepsState - 1).coerceAtLeast(0)
                                },
                                border = BorderStroke(1.dp, NeonCyan),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("button_rep_decrement"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Current",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$activeRepsState",
                                    color = if (activeCompletedState) NeonCyan else TextWhite,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Text(
                                text = " / ",
                                color = TextGray,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Target",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentStep.targetReps} ${currentStep.repUnit.uppercase()}",
                                    color = NeonCyan,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Manual Increment Button
                            OutlinedButton(
                                onClick = {
                                    activeRepsState = (activeRepsState + 1).coerceAtMost(currentStep.targetReps)
                                },
                                border = BorderStroke(1.dp, NeonCyan),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("button_rep_increment"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Help overlay guide
                    Spacer(modifier = Modifier.height(8.dp))
                    if (activeCompletedState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "COMPLETED!",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = if (currentStep.isRest) {
                                "Rest timer active • Double-tap anywhere to skip • Use side arrows to navigate"
                            } else if (isTimedExercise) {
                                "Timed exercise • Drag up/down anywhere to adjust timer • Double-tap anywhere to complete"
                            } else {
                                "Drag up/down anywhere to adjust reps • Double-tap anywhere to complete"
                            },
                            color = TextGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // BOTTOM NAV BAR: Centered Mark Complete button (Side prev/next removed in favor of middle arrows)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentStep.isRest) {
                        OutlinedButton(
                            onClick = { activeCompletedState = !activeCompletedState },
                            border = BorderStroke(1.dp, if (activeCompletedState) NeonCyan else TextGray.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("button_mark_completed")
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Mark Complete",
                                tint = if (activeCompletedState) NeonCyan else TextGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (activeCompletedState) "COMPLETED" else "MARK COMPLETE",
                                color = if (activeCompletedState) NeonCyan else TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. MIDDLE-HEIGHT NAVIGATION ARROWS (Placed on top so they are always clickable and never obstructed)
        IconButton(
            onClick = {
                if (currentStepIdx > 0) {
                    currentStepIdx--
                }
            },
            enabled = currentStepIdx > 0,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(48.dp)
                .background(
                    if (currentStepIdx > 0) Color(0xFF141923) else Color(0xFF0C1017),
                    RoundedCornerShape(24.dp)
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (currentStepIdx > 0) NeonCyan.copy(alpha = 0.8f) else TextGray.copy(alpha = 0.2f)
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("button_prev_side_arrow")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous exercise",
                tint = if (currentStepIdx > 0) NeonCyan else TextGray.copy(alpha = 0.3f),
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(
            onClick = {
                if (currentStep?.isRest == true) {
                    currentStep.isCompleted = true
                }
                advanceToNext()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(48.dp)
                .background(Color(0xFF141923), RoundedCornerShape(24.dp))
                .border(
                    BorderStroke(1.5.dp, NeonCyan),
                    RoundedCornerShape(24.dp)
                )
                .testTag("button_next_side_arrow")
        ) {
            Icon(
                if (currentStepIdx < totalSteps - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                contentDescription = if (currentStepIdx < totalSteps - 1) "Next exercise" else "Complete workout",
                tint = NeonCyan,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
}

private fun playAudioFeedback(context: Context) {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
    } catch (_: Exception) {}
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        }
    } catch (_: Exception) {}
}
