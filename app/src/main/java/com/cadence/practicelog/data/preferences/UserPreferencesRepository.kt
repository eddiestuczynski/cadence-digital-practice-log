package com.cadence.practicelog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String, val shortLabel: String) {
    DAY("Day Mode (Sheet Music Paper)", "Day"),
    NIGHT_STAGE("Night Mode (Stage Amber)", "Stage"),
    NIGHT_OLED("Night Mode (OLED Pit Black)", "OLED"),
    SYSTEM("System Auto", "Auto")
}

enum class DarkVariant(val label: String) {
    STAGE_AMBER("Stage Amber (Warm Slate)"),
    OLED_BLACK("OLED Pit (True Black #000000)")
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val DEFAULT_INSTRUMENTS = listOf(
            "Piano", "Violin", "Guitar", "Cello", "Voice",
            "Flute", "Saxophone", "Clarinet", "Drums", "Trumpet"
        )

        val BUILT_IN_FOCUS_TAGS = listOf(
            "Technique", "Dynamics", "Sight-Reading", "Intonation",
            "Rhythm", "Articulation", "Memorization", "Slow Practice", "Phrasing"
        )
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PREFERRED_DARK_VARIANT = stringPreferencesKey("preferred_dark_variant")
        val REDUCED_ACCENTS = booleanPreferencesKey("reduced_accents")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val WEEKLY_GOAL_MINUTES = intPreferencesKey("weekly_goal_minutes")
        val DEFAULT_INSTRUMENT = stringPreferencesKey("default_instrument")
        val GLOBAL_INSTRUMENT_FILTER = stringPreferencesKey("global_instrument_filter")
        val CUSTOM_INSTRUMENTS = stringPreferencesKey("custom_instruments_csv")
        val CUSTOM_FOCUS_TAGS = stringPreferencesKey("custom_focus_tags_csv")
        val INITIAL_SEED_COMPLETED = booleanPreferencesKey("initial_seed_completed")

        // Persisted active stopwatch state across app switching / process recreation
        val TIMER_IS_RUNNING = booleanPreferencesKey("timer_is_running")
        val TIMER_START_EPOCH_MS = longPreferencesKey("timer_start_epoch_ms")
        val TIMER_ACCUMULATED_SEC = intPreferencesKey("timer_accumulated_sec")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.DAY.name
        // Migrate legacy "NIGHT" value cleanly to NIGHT_STAGE
        if (raw == "NIGHT") ThemeMode.NIGHT_STAGE
        else runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.DAY)
    }

    val preferredDarkVariantFlow: Flow<DarkVariant> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PREFERRED_DARK_VARIANT] ?: DarkVariant.STAGE_AMBER.name
        runCatching { DarkVariant.valueOf(raw) }.getOrDefault(DarkVariant.STAGE_AMBER)
    }

    /**
     * Calm palette by default: neutral ink plus one accent per theme.
     * Users can opt into the original multi-accent palettes from Settings.
     */
    val reducedAccentsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REDUCED_ACCENTS] ?: true
    }

    val keepScreenAwakeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.KEEP_SCREEN_AWAKE] ?: true
    }

    val weeklyGoalMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEEKLY_GOAL_MINUTES] ?: 180
    }

    val defaultInstrumentFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_INSTRUMENT] ?: "Piano"
    }

    val globalInstrumentFilterFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GLOBAL_INSTRUMENT_FILTER] ?: "All"
    }

    val customInstrumentsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        parseCsv(prefs[Keys.CUSTOM_INSTRUMENTS])
    }

    val allInstrumentsFlow: Flow<List<String>> = customInstrumentsFlow.map { custom ->
        (DEFAULT_INSTRUMENTS + custom).distinctBy { it.lowercase() }
    }

    val customFocusTagsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        parseCsv(prefs[Keys.CUSTOM_FOCUS_TAGS])
    }

    val allFocusTagsFlow: Flow<List<String>> = customFocusTagsFlow.map { custom ->
        (BUILT_IN_FOCUS_TAGS + custom).distinctBy { it.lowercase() }
    }

    val initialSeedCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.INITIAL_SEED_COMPLETED] ?: false
    }

    data class PersistedTimerSnapshot(
        val isRunning: Boolean,
        val startEpochMs: Long,
        val accumulatedSeconds: Int
    )

    val persistedTimerFlow: Flow<PersistedTimerSnapshot> = context.dataStore.data.map { prefs ->
        PersistedTimerSnapshot(
            isRunning = prefs[Keys.TIMER_IS_RUNNING] ?: false,
            startEpochMs = prefs[Keys.TIMER_START_EPOCH_MS] ?: 0L,
            accumulatedSeconds = prefs[Keys.TIMER_ACCUMULATED_SEC] ?: 0
        )
    }

    suspend fun shouldRunInitialSeed(): Boolean {
        return !initialSeedCompletedFlow.first()
    }

    suspend fun markInitialSeedCompleted() {
        context.dataStore.edit { prefs ->
            prefs[Keys.INITIAL_SEED_COMPLETED] = true
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
            if (mode == ThemeMode.NIGHT_STAGE) {
                prefs[Keys.PREFERRED_DARK_VARIANT] = DarkVariant.STAGE_AMBER.name
            } else if (mode == ThemeMode.NIGHT_OLED) {
                prefs[Keys.PREFERRED_DARK_VARIANT] = DarkVariant.OLED_BLACK.name
            }
        }
    }

    suspend fun setPreferredDarkVariant(variant: DarkVariant) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_DARK_VARIANT] = variant.name
            val currentMode = prefs[Keys.THEME_MODE]
            if (currentMode == ThemeMode.NIGHT_STAGE.name || currentMode == ThemeMode.NIGHT_OLED.name) {
                prefs[Keys.THEME_MODE] = if (variant == DarkVariant.OLED_BLACK) {
                    ThemeMode.NIGHT_OLED.name
                } else {
                    ThemeMode.NIGHT_STAGE.name
                }
            }
        }
    }

    suspend fun cycleVisualMode() {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.DAY.name
            val current = runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.DAY)
            val next = when (current) {
                ThemeMode.DAY -> ThemeMode.NIGHT_STAGE
                ThemeMode.NIGHT_STAGE -> ThemeMode.NIGHT_OLED
                ThemeMode.NIGHT_OLED -> ThemeMode.SYSTEM
                ThemeMode.SYSTEM -> ThemeMode.DAY
            }
            prefs[Keys.THEME_MODE] = next.name
        }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEEP_SCREEN_AWAKE] = enabled
        }
    }

    suspend fun setReducedAccents(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REDUCED_ACCENTS] = enabled
        }
    }

    suspend fun setWeeklyGoalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEEKLY_GOAL_MINUTES] = minutes.coerceIn(15, 2400)
        }
    }

    suspend fun setDefaultInstrument(instrument: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_INSTRUMENT] = instrument.trim()
        }
    }

    suspend fun setGlobalInstrumentFilter(filter: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_INSTRUMENT_FILTER] = filter
        }
    }

    suspend fun addCustomInstrument(instrument: String) {
        val clean = instrument.trim().replace("|", "")
        if (clean.isEmpty()) return
        context.dataStore.edit { prefs ->
            val existing = parseCsv(prefs[Keys.CUSTOM_INSTRUMENTS]).toMutableList()
            val allKnown = DEFAULT_INSTRUMENTS + existing
            if (allKnown.none { it.equals(clean, ignoreCase = true) }) {
                existing.add(clean)
                prefs[Keys.CUSTOM_INSTRUMENTS] = existing.joinToString("|")
            }
        }
    }

    suspend fun removeCustomInstrument(instrument: String) {
        context.dataStore.edit { prefs ->
            val updated = parseCsv(prefs[Keys.CUSTOM_INSTRUMENTS])
                .filterNot { it.equals(instrument, ignoreCase = true) }
            prefs[Keys.CUSTOM_INSTRUMENTS] = updated.joinToString("|")
            if (prefs[Keys.GLOBAL_INSTRUMENT_FILTER].equals(instrument, ignoreCase = true)) {
                prefs[Keys.GLOBAL_INSTRUMENT_FILTER] = "All"
            }
        }
    }

    suspend fun addCustomFocusTag(tag: String) {
        val clean = tag.trim().replace("|", "").replace(",", "")
        if (clean.isEmpty()) return
        context.dataStore.edit { prefs ->
            val existing = parseCsv(prefs[Keys.CUSTOM_FOCUS_TAGS]).toMutableList()
            val allKnown = BUILT_IN_FOCUS_TAGS + existing
            if (allKnown.none { it.equals(clean, ignoreCase = true) }) {
                existing.add(clean)
                prefs[Keys.CUSTOM_FOCUS_TAGS] = existing.joinToString("|")
            }
        }
    }

    suspend fun removeCustomFocusTag(tag: String) {
        context.dataStore.edit { prefs ->
            val updated = parseCsv(prefs[Keys.CUSTOM_FOCUS_TAGS])
                .filterNot { it.equals(tag, ignoreCase = true) }
            prefs[Keys.CUSTOM_FOCUS_TAGS] = updated.joinToString("|")
        }
    }

    suspend fun saveTimerState(isRunning: Boolean, startEpochMs: Long, accumulatedSec: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMER_IS_RUNNING] = isRunning
            prefs[Keys.TIMER_START_EPOCH_MS] = startEpochMs
            prefs[Keys.TIMER_ACCUMULATED_SEC] = accumulatedSec.coerceAtLeast(0)
        }
    }

    suspend fun restorePreferencesFromBackup(
        weeklyGoalMinutes: Int?,
        customInstruments: List<String>,
        customTags: List<String>
    ) {
        context.dataStore.edit { prefs ->
            if (weeklyGoalMinutes != null) {
                prefs[Keys.WEEKLY_GOAL_MINUTES] = weeklyGoalMinutes.coerceIn(15, 2400)
            }
            prefs[Keys.CUSTOM_INSTRUMENTS] = customInstruments
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("|")
            prefs[Keys.CUSTOM_FOCUS_TAGS] = customTags
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString("|")
        }
    }

    private fun parseCsv(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
