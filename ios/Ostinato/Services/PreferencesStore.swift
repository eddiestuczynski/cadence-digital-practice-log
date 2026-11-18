import Foundation
import Observation

/// Persistent user preferences backed by `UserDefaults`.
/// Mirrors the Android DataStore keys so the behaviour is identical.
///
/// Each observed setting is exposed as a computed property backed by a private
/// stored value. That keeps `@Observable` tracking intact while still writing
/// through to `UserDefaults` on every mutation (property observers on observed
/// stored properties are not reliably supported by the Observation macro).
@MainActor
@Observable
final class PreferencesStore {

    static let defaultInstruments = [
        "Piano", "Violin", "Guitar", "Cello", "Voice",
        "Flute", "Saxophone", "Clarinet", "Drums", "Trumpet"
    ]

    static let builtInFocusTags = [
        "Technique", "Dynamics", "Sight-Reading", "Intonation",
        "Rhythm", "Articulation", "Memorization", "Slow Practice", "Phrasing"
    ]

    private enum Keys {
        static let themeMode = "theme_mode"
        static let preferredDarkVariant = "preferred_dark_variant"
        static let keepScreenAwake = "keep_screen_awake"
        static let weeklyGoalMinutes = "weekly_goal_minutes"
        static let defaultInstrument = "default_instrument"
        static let globalInstrumentFilter = "global_instrument_filter"
        static let customInstruments = "custom_instruments_csv"
        static let customFocusTags = "custom_focus_tags_csv"
        static let initialSeedCompleted = "initial_seed_completed"
        static let timerIsRunning = "timer_is_running"
        static let timerStartEpochMs = "timer_start_epoch_ms"
        static let timerAccumulatedSec = "timer_accumulated_sec"
    }

    struct TimerSnapshot {
        var isRunning: Bool
        var startEpochMs: Int64
        var accumulatedSeconds: Int
    }

    private let defaults: UserDefaults

    private var storedThemeMode: ThemeMode
    private var storedPreferredDarkVariant: DarkVariant
    private var storedKeepScreenAwake: Bool
    private var storedWeeklyGoalMinutes: Int
    private var storedGlobalInstrumentFilter: String
    private var storedCustomInstruments: [String]
    private var storedCustomFocusTags: [String]
    private var storedInitialSeedCompleted: Bool

    var themeMode: ThemeMode {
        get { storedThemeMode }
        set {
            storedThemeMode = newValue
            defaults.set(newValue.rawValue, forKey: Keys.themeMode)
        }
    }

    var preferredDarkVariant: DarkVariant {
        get { storedPreferredDarkVariant }
        set {
            storedPreferredDarkVariant = newValue
            defaults.set(newValue.rawValue, forKey: Keys.preferredDarkVariant)
        }
    }

    var keepScreenAwake: Bool {
        get { storedKeepScreenAwake }
        set {
            storedKeepScreenAwake = newValue
            defaults.set(newValue, forKey: Keys.keepScreenAwake)
        }
    }

    var weeklyGoalMinutes: Int {
        get { storedWeeklyGoalMinutes }
        set {
            storedWeeklyGoalMinutes = newValue
            defaults.set(newValue, forKey: Keys.weeklyGoalMinutes)
        }
    }

    var globalInstrumentFilter: String {
        get { storedGlobalInstrumentFilter }
        set {
            storedGlobalInstrumentFilter = newValue
            defaults.set(newValue, forKey: Keys.globalInstrumentFilter)
        }
    }

    var customInstruments: [String] {
        get { storedCustomInstruments }
        set {
            storedCustomInstruments = newValue
            defaults.set(newValue.joined(separator: "|"), forKey: Keys.customInstruments)
        }
    }

    var customFocusTags: [String] {
        get { storedCustomFocusTags }
        set {
            storedCustomFocusTags = newValue
            defaults.set(newValue.joined(separator: "|"), forKey: Keys.customFocusTags)
        }
    }

    var initialSeedCompleted: Bool {
        get { storedInitialSeedCompleted }
        set {
            storedInitialSeedCompleted = newValue
            defaults.set(newValue, forKey: Keys.initialSeedCompleted)
        }
    }

    private(set) var timerIsRunning: Bool
    private(set) var timerStartEpochMs: Int64
    private(set) var timerAccumulatedSeconds: Int

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        let storedTheme = defaults.string(forKey: Keys.themeMode) ?? ThemeMode.DAY.rawValue
        if storedTheme == "NIGHT" {
            storedThemeMode = .NIGHT_STAGE
        } else {
            storedThemeMode = ThemeMode(rawValue: storedTheme) ?? .DAY
        }

        storedPreferredDarkVariant = DarkVariant(
            rawValue: defaults.string(forKey: Keys.preferredDarkVariant) ?? ""
        ) ?? .STAGE_AMBER

        storedKeepScreenAwake = defaults.object(forKey: Keys.keepScreenAwake) as? Bool ?? true
        storedWeeklyGoalMinutes = defaults.object(forKey: Keys.weeklyGoalMinutes) as? Int ?? 180
        storedGlobalInstrumentFilter = defaults.string(forKey: Keys.globalInstrumentFilter) ?? "All"

        storedCustomInstruments = Self.parseList(defaults.string(forKey: Keys.customInstruments))
        storedCustomFocusTags = Self.parseList(defaults.string(forKey: Keys.customFocusTags))
        storedInitialSeedCompleted = defaults.object(forKey: Keys.initialSeedCompleted) as? Bool ?? false

        timerIsRunning = defaults.object(forKey: Keys.timerIsRunning) as? Bool ?? false
        timerStartEpochMs = (defaults.object(forKey: Keys.timerStartEpochMs) as? NSNumber)?.int64Value ?? 0
        timerAccumulatedSeconds = defaults.object(forKey: Keys.timerAccumulatedSec) as? Int ?? 0
    }

    var allInstruments: [String] {
        deduplicated(Self.defaultInstruments + customInstruments)
    }

    var allFocusTags: [String] {
        deduplicated(Self.builtInFocusTags + customFocusTags)
    }

    var timerSnapshot: TimerSnapshot {
        TimerSnapshot(
            isRunning: timerIsRunning,
            startEpochMs: timerStartEpochMs,
            accumulatedSeconds: timerAccumulatedSeconds
        )
    }

    // MARK: - Theme

    func setThemeMode(_ mode: ThemeMode) {
        themeMode = mode
        switch mode {
        case .NIGHT_STAGE:
            preferredDarkVariant = .STAGE_AMBER
        case .NIGHT_OLED:
            preferredDarkVariant = .OLED_BLACK
        case .DAY, .SYSTEM:
            break
        }
    }

    func setPreferredDarkVariant(_ variant: DarkVariant) {
        preferredDarkVariant = variant
        if themeMode == .NIGHT_STAGE || themeMode == .NIGHT_OLED {
            themeMode = variant == .OLED_BLACK ? .NIGHT_OLED : .NIGHT_STAGE
        }
    }

    func cycleVisualMode() {
        switch themeMode {
        case .DAY: themeMode = .NIGHT_STAGE
        case .NIGHT_STAGE: themeMode = .NIGHT_OLED
        case .NIGHT_OLED: themeMode = .SYSTEM
        case .SYSTEM: themeMode = .DAY
        }
    }

    // MARK: - Settings

    func setWeeklyGoalMinutes(_ minutes: Int) {
        weeklyGoalMinutes = min(max(minutes, 15), 2400)
    }

    func addCustomInstrument(_ instrument: String) {
        let clean = instrument.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "|", with: "")
        guard !clean.isEmpty else { return }
        let known = Self.defaultInstruments + customInstruments
        guard !known.contains(where: { $0.caseInsensitiveCompare(clean) == .orderedSame }) else { return }
        customInstruments.append(clean)
    }

    func removeCustomInstrument(_ instrument: String) {
        customInstruments.removeAll { $0.caseInsensitiveCompare(instrument) == .orderedSame }
        if globalInstrumentFilter.caseInsensitiveCompare(instrument) == .orderedSame {
            globalInstrumentFilter = "All"
        }
    }

    func addCustomFocusTag(_ tag: String) {
        let clean = tag.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "|", with: "")
            .replacingOccurrences(of: ",", with: "")
        guard !clean.isEmpty else { return }
        let known = Self.builtInFocusTags + customFocusTags
        guard !known.contains(where: { $0.caseInsensitiveCompare(clean) == .orderedSame }) else { return }
        customFocusTags.append(clean)
    }

    func removeCustomFocusTag(_ tag: String) {
        customFocusTags.removeAll { $0.caseInsensitiveCompare(tag) == .orderedSame }
    }

    // MARK: - Stopwatch persistence

    func saveTimerState(isRunning: Bool, startEpochMs: Int64, accumulatedSec: Int) {
        timerIsRunning = isRunning
        timerStartEpochMs = startEpochMs
        timerAccumulatedSeconds = max(accumulatedSec, 0)
        defaults.set(isRunning, forKey: Keys.timerIsRunning)
        defaults.set(startEpochMs, forKey: Keys.timerStartEpochMs)
        defaults.set(timerAccumulatedSeconds, forKey: Keys.timerAccumulatedSec)
    }

    func markInitialSeedCompleted() {
        initialSeedCompleted = true
    }

    func restoreFromBackup(weeklyGoalMinutes: Int?, customInstruments: [String], customTags: [String]) {
        if let weeklyGoalMinutes {
            setWeeklyGoalMinutes(weeklyGoalMinutes)
        }
        self.customInstruments = Self.deduplicated(
            customInstruments.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        )
        self.customFocusTags = Self.deduplicated(
            customTags.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        )
    }

    // MARK: - Helpers

    private static func parseList(_ raw: String?) -> [String] {
        guard let raw, !raw.isEmpty else { return [] }
        return raw.split(separator: "|")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    private static func deduplicated(_ values: [String]) -> [String] {
        var seen = Set<String>()
        var output: [String] = []
        for value in values {
            let key = value.lowercased()
            if seen.insert(key).inserted {
                output.append(value)
            }
        }
        return output
    }

    private func deduplicated(_ values: [String]) -> [String] {
        Self.deduplicated(values)
    }
}
