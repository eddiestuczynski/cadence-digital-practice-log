# Cadence — Musician Practice Log for Android & iOS

**Cadence** is an offline-first practice log for instrumentalists and vocalists, built to structure practice routines, track tempo mastery, maintain connected session notes, and read comfortably on any music stand or stage.

It ships as two native apps that share the same data model, feature set and backup format:

- **Android** — Kotlin + Jetpack Compose (Material 3), Room + DataStore.
- **iOS / iPadOS** — SwiftUI + SwiftData, targeting iOS 17+, universal for iPhone and iPad.

See [`ios/README.md`](ios/README.md) for the iOS build and architecture notes.

---

## Key Features

### 1. High-Contrast Day & Night Visual Modes + Music Stand Focus View
- **Day Mode (Sheet Music Paper)**: Warm, non-glare ivory (`#FDFBF6`) with deep engraving ink (`#1A1614`) for bright practice rooms.
- **Night Mode (Stage Amber)**: Low-eye-strain warm slate (`#101318`) with warm stand-lamp amber accents (`#F4B860`).
- **Night Mode (OLED Pit Black)**: True `#000000` pitch-black surfaces for zero light bleed in dark orchestra pits and stages.
- **System Auto**: Automatically follows Android system light/dark mode with your preferred dark variant.
- **Distraction-Free Music Stand Focus Mode**: Full-screen practice overlay with oversized stopwatch numerals, giant BPM controls, interactive beat indicators, visual screen-edge pulse, and pinned score/session notes readable from 50–80 cm away, plus automatic screen wake-lock while practicing.

### 2. Hybrid Practice Studio & Saveable Routine Templates
- **Quick Single-Piece Mode & Multi-Item Session Queues**: Log single practice blocks immediately or load ordered routine templates (e.g., *Warmup → Etude → Repertoire*) with silent target-duration progress bars.
- **Timestamp-Anchored Count-Up Stopwatch**: Survives app-switching (such as checking a PDF score reader) and screen rotation without losing elapsed seconds.
- **Retroactive Logging ("Log Past Session")**: Record practice sessions completed away from your phone with custom dates, times, durations, BPMs, and notes.

### 3. Precision Synthesized Metronome & Smart Tempo Tracking
- **Low-Latency `AudioTrack` PCM Engine**: Synthesizes crisp percussive clicks with zero external audio dependencies.
- **Subdivisions & Groove Presets**: Quarter (`1x`), Eighth (`2x`), Triplet (`3x`), Sixteenth (`4x`), Swing Eighths, and a one-tap **2 & 4 Swing** preset.
- **Per-Beat Accent & Mute Customization**: Tap any beat circle to cycle **Loud Accent → Normal → Silent Beat**, paired with a visual border pulse that stays active even when muted for loud acoustic/amplified instruments.
- **Dual Best vs. Last BPM Tracking**: Automatically records both **Best Achieved BPM** and **Last Practiced BPM** per piece and movement—so slow-practice cleanup sessions never overwrite your peak tempo—while keeping Mastery Status (`Learning` / `Polishing` / `Mastered`) under manual musician control.

### 4. Connected Practice Notes & Repertoire Hierarchy
- **Session-to-Session Continuity**: Selecting a piece or movement in the Studio automatically pins your **Next Session Goal**, **Standing Score Notes**, and **Previous Session Summary**.
- **Rich Repertoire Metadata**: Organize items by category (*Repertoire Piece*, *Etude / Study*, *Warmup / Scale / Technique*), custom instruments, Key Signature, Time Signature (auto-configured in the metronome), and optional **Movements / Sub-Sections**.

### 5. Injury-Safe Weekly Streaks, Analytics & Portability
- **Weekly Consistency Streak**: Tracks consecutive Monday–Sunday weeks meeting your weekly minutes target so rest days never break your streak.
- **Interactive Monthly Practice Heatmap**: Tap any calendar day to inspect exact sessions, tempos, measures, and notes.
- **Time Breakdowns & "Rusty Repertoire" Alerts**: View practice distribution by category, instrument, and top pieces, plus automatic reminders for active pieces not practiced in 5+ days.
- **Teacher Reports & Full JSON Backup**: Share formatted 7-day / 30-day Markdown reports or CSV spreadsheets via the Android Share Sheet, and export/restore your complete offline database as JSON.

---

## Tech Stack

### Android
- **UI**: Kotlin 2.0, Jetpack Compose, Material 3, Custom Canvas Charts
- **Architecture**: MVVM (`StateFlow`, Coroutines, Repository pattern)
- **Persistence**: Room SQLite (`v2` schema with relational entities) & Jetpack DataStore Preferences
- **Audio**: Native Android `AudioTrack` static PCM synthesis
- **Testing**: JUnit 4 unit tests covering Mon–Sun streak math and JSON backup round-trip serialization

### iOS / iPadOS
- **UI**: SwiftUI, adaptive `NavigationSplitView` / `TabView`, Swift Charts–style custom drawing
- **Architecture**: `@Observable` store + view model, environment injection
- **Persistence**: SwiftData (schema mirrors Room with scalar `Int64` ids) & `UserDefaults` preferences
- **Audio**: `AVAudioEngine` with synthesized PCM click buffers
- **Build**: Xcode 16+ project, iOS 17.0 deployment target, universal iPhone/iPad
- **CI**: GitHub Actions builds the app for the iOS Simulator on every `ios/**` change

---

## Getting Started

### Android — install pre-built APK
Download **`CadencePracticeLog.apk`** from the **Releases** page on this repository and install it on any Android 8.0+ (`minSdk 26`) device.

### Android — build from source
1. Clone this repository and open it in **Android Studio Ladybug (or newer)** with **JDK 17**.
2. Run unit tests and assemble the debug APK from the terminal:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
3. The compiled APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

### iOS / iPadOS — build from source
1. Open `ios/Cadence.xcodeproj` in **Xcode 16 or newer**.
2. Select the **Cadence** scheme and an iPhone or iPad simulator (iOS 17.0+).
3. Press **Run** (`⌘R`), or build headlessly:
   ```bash
   cd ios
   xcodebuild \
     -project Cadence.xcodeproj \
     -scheme Cadence \
     -sdk iphonesimulator \
     -destination 'generic/platform=iOS Simulator' \
     CODE_SIGNING_ALLOWED=NO \
     build
   ```

The iOS project has no external package dependencies and is built automatically
by the `iOS Build` GitHub Actions workflow on every change under `ios/**`.

---

## Cross-platform backups

JSON backups are schema-versioned and byte-compatible between Android and iOS
(schema `v2`), so practice history, repertoire, routines and preferences can be
moved between platforms with the in-app export/import controls.
