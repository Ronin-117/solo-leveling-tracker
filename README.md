# Solo Leveling

A minimalist Android workout tracker built with Jetpack Compose, designed around a high-contrast pitch-black and cyan monospace interface. Built for structured weekly bodyweight training, progressive overload tracking, and distraction-free execution.

---

## Overview

Solo Leveling organizes training into a weekly split with morning conditioning and targeted evening sessions. Every routine is stored locally via Room and synchronized with Firebase when signed in, letting you customize exercises, track weekly progression modifiers, and resume interrupted workouts without losing progress.

---

## Features

### Weekly Training Schedule
- **Morning Protocols:** Daily warm-up & cardio (mobility drills, jog/high-knees intervals) and core conditioning.
- **Evening Split:**
  - **Monday / Thursday:** Evening Push (Chest, Shoulders, Triceps)
  - **Tuesday / Friday:** Evening Legs (Quads, Hamstrings, Calves)
  - **Wednesday / Saturday:** Evening Pull & Core
  - **Sunday:** Stretch & Walk active recovery
- **Today Detection:** Automatically flags and prioritizes the current day's scheduled routines on the dashboard.

### Interactive Workout Player
- **Gesture-Driven Tracking:** Drag up or down anywhere on the screen to adjust rep counts or add/subtract 5 seconds on timed holds. Double-tap anywhere to mark a set complete or skip an active rest interval.
- **Timed & Bilateral Exercises:** Built-in countdown timer for duration-based movements. Exercises marked as `each side` (e.g., Side Plank) include a 5-second side-switch countdown before auto-starting Side 2, plus interactive `SIDE 1: LEFT` / `SIDE 2: RIGHT` selector pills.
- **Rest Timers & Feedback:** Automated between-set rest countdowns with audio tone and haptic vibration cues (`VIBRATE` permission), instant skip controls, and middle-screen navigation arrows.
- **Auto-Save & Resume:** Exiting mid-session via the close button or system back button automatically saves the active step, completed sets, and current rep counts so you can pick up right where you left off.

### Routine Customization & Progression
- **Routine Editor:** Edit any workout plan—add or remove exercises, toggle between reps and timed seconds, batch-update total rounds, or restore any plan to its default template.
- **Progressive Overload Engine:** Increment rep modifiers, set counts, rest duration reductions, harder exercise variations, or cardio phases week over week, or configure exact progression values manually.
- **History & Activity Heatmap:** Review logged sessions alongside a visual training consistency heatmap.

### Architecture & Sync
- **MVVM + Clean Data Layer:** `WorkoutViewModel` exposes reactive `StateFlow` streams backed by `WorkoutRepository`.
- **Offline-First Storage:** Local persistence powered by Room (`WorkoutDatabase`) for plans, progression state, user sessions, and workout logs.
- **Cloud Sync:** Optional Firebase Authentication and Cloud Firestore synchronization via `FirebaseSyncManager`, with automatic local fallback.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language | Kotlin |
| UI Framework | Jetpack Compose, Material 3 |
| Concurrency | Kotlin Coroutines & Flow |
| Local Database | Room (with KSP) |
| Backend / Sync | Firebase Auth, Cloud Firestore |
| Build & Config | Gradle (Kotlin DSL), Version Catalog, Secrets Gradle Plugin |
| Testing | JUnit 4, Robolectric, Roborazzi |

---

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── WorkoutDao.kt
│   │   └── WorkoutDatabase.kt
│   ├── model/
│   │   ├── ProgressionStateEntity.kt
│   │   ├── UserEntity.kt
│   │   ├── WorkoutLogEntity.kt
│   │   ├── WorkoutPlanEntity.kt
│   │   └── WorkoutStep.kt
│   └── repository/
│       ├── FirebaseSyncManager.kt
│       └── WorkoutRepository.kt
└── ui/
    ├── WorkoutViewModel.kt
    ├── screens/
    │   ├── DashboardScreen.kt
    │   ├── EditWorkoutRoutineScreen.kt
    │   ├── ImmersiveWorkoutPlayer.kt
    │   └── LoginScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Getting Started

### Prerequisites
- Android Studio (Ladybug or newer recommended)
- JDK 11 or higher
- Android SDK 36 (`minSdk 24`)

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/solo-leveling-android.git
   cd solo-leveling-android
   ```
2. (Optional) Configure Firebase:
   - Replace `app/google-services.json` with your own Firebase project configuration if you want to connect your own Auth/Firestore instance.
3. (Optional) Configure environment variables:
   - Copy `.env.example` to `.env` if needed for local Secrets Gradle Plugin resolution.
4. Open in Android Studio and sync Gradle, or build and test from the command line:
   ```bash
   gradle assembleDebug
   gradle :app:testDebugUnitTest
   ```

### Releases
Every push to `main` automatically compiles an installable APK via GitHub Actions and publishes it under the repository's **Releases** page.

