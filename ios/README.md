# Cadence for iOS & iPadOS

A native **SwiftUI** port of the Cadence musician practice log. The app targets
**iOS / iPadOS 17.0+** and is built as a single universal app (iPhone + iPad).

This directory is the complete Xcode project. It has no external dependencies —
everything ships with the Apple SDKs.

---

## Requirements

| Tool | Version |
| --- | --- |
| Xcode | 16.0 or newer (project uses `objectVersion = 77`) |
| Swift | 5 language mode |
| Deployment target | iOS / iPadOS 17.0 |
| Devices | Universal — iPhone and iPad |

## Open & run

1. Open `ios/Cadence.xcodeproj` in Xcode.
2. Select the **Cadence** scheme and an iPhone or iPad simulator.
3. Press **Run** (`⌘R`).

The project uses a *file-system synchronized group*, so any `.swift` file added
under `ios/Cadence/` is picked up automatically — no manual project edits.

### Build from the command line

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

Continuous integration runs the same build on every push that touches `ios/**`
via `.github/workflows/ios.yml`.

---

## Architecture

| Layer | Implementation |
| --- | --- |
| UI | SwiftUI, adaptive `NavigationSplitView` (regular width) / `TabView` (compact) |
| Persistence | SwiftData models mirroring the Android Room schema (scalar `Int64` ids) |
| Preferences | `UserDefaults` via an `@Observable` `PreferencesStore` |
| Audio | `AVAudioEngine` + synthesized PCM click buffers |
| State | `@Observable` store, view model and preference singletons injected through the environment |

Key source folders:

```
ios/Cadence/
├── CadenceApp.swift          # App entry, SwiftData container, first-launch seed
├── Models/                   # SwiftData entities + enums
├── Services/                 # Store, preferences, analytics, backup, metronome, demo data
├── ViewModels/               # StudioViewModel (timer, queue, tempo, save/log)
├── Theme/                    # Palettes and reusable components
└── Views/                    # Studio, Journal, Repertoire, Analytics, Settings, Music Stand
```

## Feature parity with Android

- **Practice Studio** — timestamp-anchored stopwatch, single-piece and queue
  modes, routine templates, retroactive logging, quick-note templates.
- **Metronome** — drift-compensated `AVAudioEngine` click, subdivisions
  (1×/2×/3×/4×/swing/2&4), per-beat accent → normal → mute, visual pulse when
  muted, tempo marking labels.
- **Music Stand mode** — full-screen large-type clock, silent target progress,
  beat circles, tempo controls, pinned goals/score notes, screen wake lock.
- **Repertoire** — pieces, movements, categories, status cycling, best vs. last
  BPM, tempo-mastery progress, per-piece notes and next-session goals.
- **Journal** — search, category/instrument filters, inline editing, deletion,
  log-past sessions.
- **Analytics** — weekly goal + Monday–Sunday streak, monthly heatmap with day
  drill-down, category/instrument/top-piece breakdown, last-7-days bars, tempo
  progression chart, "rusty repertoire" alerts.
- **Settings & data** — four visual modes (Day, Stage Amber, OLED Pit, System),
  custom instruments and focus tags, teacher Markdown reports, CSV export, and
  **JSON backup/restore that is byte-compatible with the Android app (schema v2)**.

## Data compatibility

JSON backups exported from the Android app can be imported on iOS and vice
versa. The schema keeps the same numeric ids and field names, so users can move
their practice history between platforms freely.

## License

Released under the same terms as the Android app. See the repository root
`README.md` for details.
