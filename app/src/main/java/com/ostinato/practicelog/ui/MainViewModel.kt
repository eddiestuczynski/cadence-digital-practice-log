package com.ostinato.practicelog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ostinato.practicelog.data.local.PieceCategory
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceStatus
import com.ostinato.practicelog.data.local.PieceWithSections
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.data.local.RoutineTemplateEntity
import com.ostinato.practicelog.data.local.RoutineTemplateItemEntity
import com.ostinato.practicelog.data.local.RoutineWithItems
import com.ostinato.practicelog.data.preferences.DarkVariant
import com.ostinato.practicelog.data.preferences.ThemeMode
import com.ostinato.practicelog.data.preferences.UserPreferencesRepository
import com.ostinato.practicelog.data.repository.PracticeRepository
import com.ostinato.practicelog.util.AnalyticsCalculator
import com.ostinato.practicelog.util.BackupAndReportManager
import com.ostinato.practicelog.util.BeatAccentLevel
import com.ostinato.practicelog.util.MetronomeEngine
import com.ostinato.practicelog.util.SubdivisionMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveQueueItem(
    val uid: String = UUID.randomUUID().toString(),
    val pieceId: Long? = null,
    val sectionId: Long? = null,
    val title: String,
    val sectionName: String = "",
    val composer: String = "",
    val instrument: String = "Piano",
    val category: PieceCategory = PieceCategory.TECHNIQUE,
    val targetMinutes: Int = 10,
    val elapsedSeconds: Int = 0,
    val practicedBpm: Int = 92,
    val targetBpm: Int = 120,
    val timeSignatureBeats: Int = 4,
    val timeSignatureNoteValue: Int = 4,
    val measuresPracticed: String = "",
    val selectedFocusTags: Set<String> = setOf("Technique"),
    val sessionNotes: String = "",
    val nextSessionGoal: String = "",
    val moodRating: Int = 4,
    val notesPrompt: String = "",
    val isLogged: Boolean = false
)

data class PracticeStudioState(
    val isQueueMode: Boolean = false,
    val activeRoutineName: String = "",
    val queueItems: List<ActiveQueueItem> = emptyList(),
    val activeQueueIndex: Int = 0,
    val selectedPieceId: Long? = null,
    val selectedSectionId: Long? = null,
    val customPieceTitle: String = "",
    val sectionName: String = "",
    val customComposer: String = "",
    val selectedInstrument: String = "Piano",
    val selectedCategory: PieceCategory = PieceCategory.REPERTOIRE,
    val timerRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val targetDurationMinutes: Int = 0,
    val metronomeBpm: Int = 92,
    val targetBpm: Int = 120,
    val beatsPerMeasure: Int = 4,
    val noteValuePerBeat: Int = 4,
    val beatPattern: List<BeatAccentLevel> = MetronomeEngine.defaultBeatPattern(4),
    val subdivisionMode: SubdivisionMode = SubdivisionMode.QUARTER,
    val metronomeMuted: Boolean = false,
    val measuresPracticed: String = "",
    val selectedFocusTags: Set<String> = setOf("Technique", "Dynamics"),
    val sessionNotes: String = "",
    val nextSessionGoal: String = "",
    val moodRating: Int = 4,
    val isMusicStandModeOpen: Boolean = false,
    val lastSavedBanner: String? = null
)

class MainViewModel(
    private val repository: PracticeRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val metronomeEngine = MetronomeEngine(viewModelScope)

    // --- Preferences Flows ---
    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DAY)

    val preferredDarkVariant: StateFlow<DarkVariant> = preferences.preferredDarkVariantFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkVariant.STAGE_AMBER)

    val reducedAccents: StateFlow<Boolean> = preferences.reducedAccentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val keepScreenAwake: StateFlow<Boolean> = preferences.keepScreenAwakeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val weeklyGoalMinutes: StateFlow<Int> = preferences.weeklyGoalMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 180)

    val globalInstrumentFilter: StateFlow<String> = preferences.globalInstrumentFilterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "All")

    val allInstruments: StateFlow<List<String>> = preferences.allInstrumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_INSTRUMENTS)

    val customInstruments: StateFlow<List<String>> = preferences.customInstrumentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFocusTags: StateFlow<List<String>> = preferences.allFocusTagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.BUILT_IN_FOCUS_TAGS)

    val customFocusTags: StateFlow<List<String>> = preferences.customFocusTagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Room Database Flows ---
    val piecesWithSections: StateFlow<List<PieceWithSections>> = repository.allPiecesWithSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pieces: StateFlow<List<RepertoirePieceEntity>> = repository.allPieces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<PracticeSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineWithItems>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Studio & Metronome Flows ---
    private val _studioState = MutableStateFlow(PracticeStudioState())
    val studioState: StateFlow<PracticeStudioState> = _studioState.asStateFlow()

    val isMetronomePlaying: StateFlow<Boolean> = metronomeEngine.isPlaying
    val currentMetronomeBeat: StateFlow<Int> = metronomeEngine.currentBeat
    val beatPulseId: StateFlow<Long> = metronomeEngine.beatPulseId

    private var timerJob: Job? = null
    private var timerAnchorEpochMs: Long = 0L
    private var accumulatedSecondsBeforeRun: Int = 0
    private val tapTimestamps = ArrayDeque<Long>()

    init {
        // Restore persisted stopwatch state if user switched apps (e.g., PDF score reader)
        viewModelScope.launch {
            val savedTimer = preferences.persistedTimerFlow.first()
            if (savedTimer.isRunning && savedTimer.startEpochMs > 0L) {
                accumulatedSecondsBeforeRun = savedTimer.accumulatedSeconds
                timerAnchorEpochMs = savedTimer.startEpochMs
                val nowElapsed = accumulatedSecondsBeforeRun +
                    ((System.currentTimeMillis() - timerAnchorEpochMs) / 1000L).toInt().coerceAtLeast(0)
                _studioState.update { it.copy(elapsedSeconds = nowElapsed) }
                resumeAnchoredTimerLoop()
            } else if (savedTimer.accumulatedSeconds > 0) {
                accumulatedSecondsBeforeRun = savedTimer.accumulatedSeconds
                _studioState.update { it.copy(elapsedSeconds = savedTimer.accumulatedSeconds) }
            }
        }

        // Select initial piece when database loads if Studio hasn't picked anything yet
        viewModelScope.launch {
            repository.allPiecesWithSections.collect { list ->
                val st = _studioState.value
                if (!st.isQueueMode && st.selectedPieceId == null && st.customPieceTitle.isEmpty() && list.isNotEmpty()) {
                    val firstItem = list.first()
                    selectPieceForPractice(firstItem.piece, firstItem.sortedSections.firstOrNull())
                }
            }
        }
    }

    // --- Visual Mode & Settings ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setPreferredDarkVariant(variant: DarkVariant) {
        viewModelScope.launch { preferences.setPreferredDarkVariant(variant) }
    }

    fun cycleVisualMode() {
        viewModelScope.launch { preferences.cycleVisualMode() }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepScreenAwake(enabled) }
    }

    fun setReducedAccents(enabled: Boolean) {
        viewModelScope.launch { preferences.setReducedAccents(enabled) }
    }

    fun updateWeeklyGoalMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setWeeklyGoalMinutes(minutes) }
    }

    fun setGlobalInstrumentFilter(instrument: String) {
        viewModelScope.launch { preferences.setGlobalInstrumentFilter(instrument) }
    }

    fun addCustomInstrument(instrument: String) {
        viewModelScope.launch { preferences.addCustomInstrument(instrument) }
    }

    fun removeCustomInstrument(instrument: String) {
        viewModelScope.launch { preferences.removeCustomInstrument(instrument) }
    }

    fun addCustomFocusTag(tag: String) {
        viewModelScope.launch { preferences.addCustomFocusTag(tag) }
    }

    fun removeCustomFocusTag(tag: String) {
        viewModelScope.launch { preferences.removeCustomFocusTag(tag) }
    }

    fun setMusicStandModeOpen(open: Boolean) {
        _studioState.update { it.copy(isMusicStandModeOpen = open) }
    }

    // --- Timestamp-Anchored Count-Up Stopwatch ---

    fun toggleTimer() {
        if (_studioState.value.timerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_studioState.value.timerRunning) return
        accumulatedSecondsBeforeRun = _studioState.value.elapsedSeconds
        timerAnchorEpochMs = System.currentTimeMillis()
        viewModelScope.launch {
            preferences.saveTimerState(
                isRunning = true,
                startEpochMs = timerAnchorEpochMs,
                accumulatedSec = accumulatedSecondsBeforeRun
            )
        }
        resumeAnchoredTimerLoop()
    }

    private fun resumeAnchoredTimerLoop() {
        _studioState.update { it.copy(timerRunning = true, lastSavedBanner = null) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _studioState.value.timerRunning) {
                val deltaSec = ((System.currentTimeMillis() - timerAnchorEpochMs) / 1000L).toInt().coerceAtLeast(0)
                val totalSec = accumulatedSecondsBeforeRun + deltaSec
                _studioState.update { state ->
                    val updatedQueue = if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                        state.queueItems.mapIndexed { idx, item ->
                            if (idx == state.activeQueueIndex) item.copy(elapsedSeconds = totalSec) else item
                        }
                    } else {
                        state.queueItems
                    }
                    state.copy(elapsedSeconds = totalSec, queueItems = updatedQueue)
                }
                delay(250L)
            }
        }
    }

    fun pauseTimer() {
        if (_studioState.value.timerRunning) {
            val deltaSec = ((System.currentTimeMillis() - timerAnchorEpochMs) / 1000L).toInt().coerceAtLeast(0)
            accumulatedSecondsBeforeRun = (accumulatedSecondsBeforeRun + deltaSec).coerceAtLeast(0)
        } else {
            accumulatedSecondsBeforeRun = _studioState.value.elapsedSeconds
        }
        _studioState.update { it.copy(timerRunning = false, elapsedSeconds = accumulatedSecondsBeforeRun) }
        timerJob?.cancel()
        timerJob = null
        viewModelScope.launch {
            preferences.saveTimerState(
                isRunning = false,
                startEpochMs = 0L,
                accumulatedSec = accumulatedSecondsBeforeRun
            )
        }
    }

    fun resetTimer() {
        pauseTimer()
        accumulatedSecondsBeforeRun = 0
        timerAnchorEpochMs = 0L
        _studioState.update { state ->
            val updatedQueue = if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                state.queueItems.mapIndexed { idx, item ->
                    if (idx == state.activeQueueIndex) item.copy(elapsedSeconds = 0) else item
                }
            } else {
                state.queueItems
            }
            state.copy(elapsedSeconds = 0, queueItems = updatedQueue)
        }
        viewModelScope.launch {
            preferences.saveTimerState(isRunning = false, startEpochMs = 0L, accumulatedSec = 0)
        }
    }

    fun adjustTimerMinutes(deltaMinutes: Int) {
        val wasRunning = _studioState.value.timerRunning
        val newTotal = (_studioState.value.elapsedSeconds + deltaMinutes * 60).coerceAtLeast(0)
        accumulatedSecondsBeforeRun = newTotal
        if (wasRunning) {
            timerAnchorEpochMs = System.currentTimeMillis()
        }
        _studioState.update { state ->
            val updatedQueue = if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                state.queueItems.mapIndexed { idx, item ->
                    if (idx == state.activeQueueIndex) item.copy(elapsedSeconds = newTotal) else item
                }
            } else {
                state.queueItems
            }
            state.copy(elapsedSeconds = newTotal, queueItems = updatedQueue)
        }
        viewModelScope.launch {
            preferences.saveTimerState(
                isRunning = wasRunning,
                startEpochMs = if (wasRunning) timerAnchorEpochMs else 0L,
                accumulatedSec = newTotal
            )
        }
    }

    fun setTargetDurationMinutes(targetMinutes: Int) {
        val clamped = targetMinutes.coerceIn(0, 240)
        _studioState.update { state ->
            val updatedQueue = if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                state.queueItems.mapIndexed { idx, item ->
                    if (idx == state.activeQueueIndex) item.copy(targetMinutes = clamped) else item
                }
            } else {
                state.queueItems
            }
            state.copy(targetDurationMinutes = clamped, queueItems = updatedQueue)
        }
    }

    // --- Precision Metronome Controls ---

    fun setMetronomeBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(20, 300)
        _studioState.update { state ->
            val updatedQueue = if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                state.queueItems.mapIndexed { idx, item ->
                    if (idx == state.activeQueueIndex) item.copy(practicedBpm = clamped) else item
                }
            } else {
                state.queueItems
            }
            state.copy(metronomeBpm = clamped, queueItems = updatedQueue)
        }
        syncMetronomeEngine()
    }

    fun setTimeSignature(beats: Int, noteValue: Int = _studioState.value.noteValuePerBeat) {
        val clampedBeats = beats.coerceIn(1, 16)
        val newPattern = MetronomeEngine.defaultBeatPattern(clampedBeats)
        _studioState.update {
            it.copy(
                beatsPerMeasure = clampedBeats,
                noteValuePerBeat = noteValue,
                beatPattern = newPattern
            )
        }
        syncMetronomeEngine()
    }

    fun cycleBeatAccent(beatOneBased: Int) {
        val idx = beatOneBased - 1
        _studioState.update { state ->
            if (idx !in state.beatPattern.indices) return@update state
            val updated = state.beatPattern.toMutableList()
            updated[idx] = updated[idx].next()
            state.copy(beatPattern = updated)
        }
        syncMetronomeEngine()
    }

    fun applyJazzBackbeatPattern() {
        _studioState.update { state ->
            val beats = 4
            val pattern = listOf(
                BeatAccentLevel.MUTE,
                BeatAccentLevel.ACCENT,
                BeatAccentLevel.MUTE,
                BeatAccentLevel.ACCENT
            )
            state.copy(
                beatsPerMeasure = beats,
                noteValuePerBeat = 4,
                beatPattern = pattern,
                subdivisionMode = SubdivisionMode.SWING_EIGHTH
            )
        }
        syncMetronomeEngine()
    }

    fun setSubdivisionMode(mode: SubdivisionMode) {
        _studioState.update { it.copy(subdivisionMode = mode) }
        syncMetronomeEngine()
    }

    fun toggleMetronomeMute() {
        _studioState.update { it.copy(metronomeMuted = !it.metronomeMuted) }
        syncMetronomeEngine()
    }

    fun toggleMetronomePlayback() {
        syncMetronomeEngine()
        metronomeEngine.toggle()
    }

    private fun syncMetronomeEngine() {
        val s = _studioState.value
        metronomeEngine.configure(
            bpm = s.metronomeBpm,
            beatsPerMeasure = s.beatsPerMeasure,
            beatPattern = s.beatPattern,
            subdivisionMode = s.subdivisionMode,
            isMuted = s.metronomeMuted
        )
    }

    fun registerTapTempo() {
        val now = System.currentTimeMillis()
        if (tapTimestamps.isNotEmpty() && now - tapTimestamps.last() > 2500L) {
            tapTimestamps.clear()
        }
        tapTimestamps.addLast(now)
        if (tapTimestamps.size > 5) {
            tapTimestamps.removeFirst()
        }
        if (tapTimestamps.size >= 2) {
            val intervals = tapTimestamps.zipWithNext { a, b -> b - a }
            val avgIntervalMs = intervals.average()
            if (avgIntervalMs > 0) {
                val calculatedBpm = (60_000.0 / avgIntervalMs).toInt().coerceIn(20, 300)
                setMetronomeBpm(calculatedBpm)
            }
        }
    }

    // --- Single-Block & Multi-Item Routine Queue Management ---

    fun selectPieceForPractice(piece: RepertoirePieceEntity?, section: PieceSectionEntity? = null) {
        if (piece == null) {
            _studioState.update {
                it.copy(
                    isQueueMode = false,
                    activeRoutineName = "",
                    selectedPieceId = null,
                    selectedSectionId = null,
                    customPieceTitle = "Warmup & Scale Technique",
                    sectionName = "",
                    customComposer = "",
                    selectedCategory = PieceCategory.TECHNIQUE,
                    targetBpm = 120,
                    measuresPracticed = "",
                    nextSessionGoal = ""
                )
            }
        } else {
            val beats = section?.timeSignatureBeats ?: piece.timeSignatureBeats
            val noteVal = section?.timeSignatureNoteValue ?: piece.timeSignatureNoteValue
            val startBpm = section?.lastPracticedBpm ?: piece.lastPracticedBpm
            val targetBpm = section?.targetBpm ?: piece.targetBpm
            val defaultSub = if (noteVal == 8 && beats % 3 == 0) SubdivisionMode.QUARTER else _studioState.value.subdivisionMode

            _studioState.update {
                it.copy(
                    isQueueMode = false,
                    activeRoutineName = "",
                    selectedPieceId = piece.id,
                    selectedSectionId = section?.id,
                    customPieceTitle = piece.title,
                    sectionName = section?.name ?: "",
                    customComposer = piece.composer,
                    selectedInstrument = piece.instrument,
                    selectedCategory = piece.pieceCategory,
                    metronomeBpm = startBpm,
                    targetBpm = targetBpm,
                    beatsPerMeasure = beats,
                    noteValuePerBeat = noteVal,
                    beatPattern = MetronomeEngine.defaultBeatPattern(beats),
                    subdivisionMode = defaultSub,
                    measuresPracticed = "",
                    nextSessionGoal = ""
                )
            }
            syncMetronomeEngine()
        }
    }

    fun loadRoutineIntoQueue(routineWithItems: RoutineWithItems) {
        pauseTimer()
        val piecesMap = piecesWithSections.value.associateBy { it.piece.id }

        val builtQueue = routineWithItems.sortedItems.map { item ->
            val matchPieceWithSecs = item.pieceId?.let { piecesMap[it] }
            val matchPiece = matchPieceWithSecs?.piece
            val matchSection = item.sectionId?.let { secId ->
                matchPieceWithSecs?.sections?.find { it.id == secId }
            }

            val beats = matchSection?.timeSignatureBeats ?: matchPiece?.timeSignatureBeats ?: 4
            val noteVal = matchSection?.timeSignatureNoteValue ?: matchPiece?.timeSignatureNoteValue ?: 4
            val targetBpm = matchSection?.targetBpm ?: matchPiece?.targetBpm ?: item.suggestedBpm
            val initialBpm = matchSection?.lastPracticedBpm ?: matchPiece?.lastPracticedBpm ?: item.suggestedBpm

            ActiveQueueItem(
                pieceId = matchPiece?.id,
                sectionId = matchSection?.id,
                title = matchPiece?.title ?: item.customTitle,
                sectionName = matchSection?.name ?: "",
                composer = matchPiece?.composer ?: "",
                instrument = matchPiece?.instrument ?: routineWithItems.routine.instrument.ifBlank { "Piano" },
                category = matchPiece?.pieceCategory
                    ?: runCatching { PieceCategory.valueOf(item.category) }.getOrDefault(PieceCategory.TECHNIQUE),
                targetMinutes = item.targetMinutes,
                elapsedSeconds = 0,
                practicedBpm = initialBpm,
                targetBpm = targetBpm,
                timeSignatureBeats = beats,
                timeSignatureNoteValue = noteVal,
                notesPrompt = item.notesPrompt
            )
        }

        if (builtQueue.isEmpty()) return

        _studioState.update {
            it.copy(
                isQueueMode = true,
                activeRoutineName = routineWithItems.routine.name,
                queueItems = builtQueue,
                activeQueueIndex = 0,
                lastSavedBanner = "Loaded routine: ${routineWithItems.routine.name}"
            )
        }
        applyQueueItemToStudio(0)
    }

    fun addAdHocItemToQueue(
        piece: RepertoirePieceEntity?,
        section: PieceSectionEntity?,
        customTitle: String,
        targetMinutes: Int
    ) {
        val currentState = _studioState.value
        val newItem = ActiveQueueItem(
            pieceId = piece?.id,
            sectionId = section?.id,
            title = piece?.title ?: customTitle.ifBlank { "Practice Block" },
            sectionName = section?.name ?: "",
            composer = piece?.composer ?: "",
            instrument = piece?.instrument ?: currentState.selectedInstrument,
            category = piece?.pieceCategory ?: PieceCategory.TECHNIQUE,
            targetMinutes = targetMinutes.coerceIn(1, 180),
            practicedBpm = section?.lastPracticedBpm ?: piece?.lastPracticedBpm ?: currentState.metronomeBpm,
            targetBpm = section?.targetBpm ?: piece?.targetBpm ?: currentState.targetBpm,
            timeSignatureBeats = section?.timeSignatureBeats ?: piece?.timeSignatureBeats ?: 4,
            timeSignatureNoteValue = section?.timeSignatureNoteValue ?: piece?.timeSignatureNoteValue ?: 4
        )

        val existingQueue = if (currentState.isQueueMode) {
            syncCurrentFieldsBackToQueue(currentState.queueItems, currentState.activeQueueIndex)
        } else {
            emptyList()
        }
        val updatedQueue = existingQueue + newItem
        val targetIdx = if (currentState.isQueueMode) currentState.activeQueueIndex else 0

        _studioState.update {
            it.copy(
                isQueueMode = true,
                activeRoutineName = it.activeRoutineName.ifBlank { "Custom Practice Queue" },
                queueItems = updatedQueue,
                activeQueueIndex = targetIdx
            )
        }
        if (!currentState.isQueueMode) {
            applyQueueItemToStudio(0)
        }
    }

    fun selectQueueItemIndex(index: Int) {
        val st = _studioState.value
        if (!st.isQueueMode || index !in st.queueItems.indices || index == st.activeQueueIndex) return
        pauseTimer()
        val synced = syncCurrentFieldsBackToQueue(st.queueItems, st.activeQueueIndex)
        _studioState.update { it.copy(queueItems = synced, activeQueueIndex = index) }
        applyQueueItemToStudio(index)
    }

    fun exitQueueMode() {
        pauseTimer()
        _studioState.update {
            it.copy(
                isQueueMode = false,
                activeRoutineName = "",
                queueItems = emptyList(),
                activeQueueIndex = 0,
                targetDurationMinutes = 0
            )
        }
    }

    private fun syncCurrentFieldsBackToQueue(
        items: List<ActiveQueueItem>,
        index: Int
    ): List<ActiveQueueItem> {
        val s = _studioState.value
        if (index !in items.indices) return items
        return items.mapIndexed { idx, item ->
            if (idx == index) {
                item.copy(
                    title = s.customPieceTitle.ifBlank { item.title },
                    instrument = s.selectedInstrument,
                    elapsedSeconds = s.elapsedSeconds,
                    targetMinutes = s.targetDurationMinutes.takeIf { it > 0 } ?: item.targetMinutes,
                    practicedBpm = s.metronomeBpm,
                    measuresPracticed = s.measuresPracticed,
                    selectedFocusTags = s.selectedFocusTags,
                    sessionNotes = s.sessionNotes,
                    nextSessionGoal = s.nextSessionGoal,
                    moodRating = s.moodRating
                )
            } else {
                item
            }
        }
    }

    private fun applyQueueItemToStudio(index: Int) {
        val item = _studioState.value.queueItems.getOrNull(index) ?: return
        accumulatedSecondsBeforeRun = item.elapsedSeconds
        timerAnchorEpochMs = 0L
        viewModelScope.launch {
            preferences.saveTimerState(isRunning = false, startEpochMs = 0L, accumulatedSec = item.elapsedSeconds)
        }
        _studioState.update {
            it.copy(
                activeQueueIndex = index,
                selectedPieceId = item.pieceId,
                selectedSectionId = item.sectionId,
                customPieceTitle = item.title,
                sectionName = item.sectionName,
                customComposer = item.composer,
                selectedInstrument = item.instrument,
                selectedCategory = item.category,
                elapsedSeconds = item.elapsedSeconds,
                targetDurationMinutes = item.targetMinutes,
                metronomeBpm = item.practicedBpm,
                targetBpm = item.targetBpm,
                beatsPerMeasure = item.timeSignatureBeats,
                noteValuePerBeat = item.timeSignatureNoteValue,
                beatPattern = MetronomeEngine.defaultBeatPattern(item.timeSignatureBeats),
                measuresPracticed = item.measuresPracticed,
                selectedFocusTags = item.selectedFocusTags,
                sessionNotes = item.sessionNotes,
                nextSessionGoal = item.nextSessionGoal,
                moodRating = item.moodRating
            )
        }
        syncMetronomeEngine()
    }

    // --- Studio Field Setters ---

    fun setSelectedInstrument(instrument: String) {
        _studioState.update { it.copy(selectedInstrument = instrument) }
    }

    fun setSelectedCategory(category: PieceCategory) {
        _studioState.update { it.copy(selectedCategory = category) }
    }

    fun setCustomPieceTitle(title: String) {
        _studioState.update { it.copy(customPieceTitle = title) }
    }

    fun updateMeasuresPracticed(measures: String) {
        _studioState.update { it.copy(measuresPracticed = measures) }
    }

    fun toggleFocusTag(tag: String) {
        _studioState.update { state ->
            val updated = state.selectedFocusTags.toMutableSet()
            if (!updated.add(tag)) updated.remove(tag)
            state.copy(selectedFocusTags = updated)
        }
    }

    fun updateSessionNotes(notes: String) {
        _studioState.update { it.copy(sessionNotes = notes) }
    }

    fun updateNextSessionGoal(goal: String) {
        _studioState.update { it.copy(nextSessionGoal = goal) }
    }

    fun appendNoteTemplate(snippet: String) {
        _studioState.update { state ->
            val prefix = if (state.sessionNotes.isBlank()) "• " else "\n• "
            state.copy(sessionNotes = state.sessionNotes + prefix + snippet)
        }
    }

    fun setMoodRating(rating: Int) {
        _studioState.update { it.copy(moodRating = rating.coerceIn(1, 5)) }
    }

    // --- Saving Live Session / Queue Item ---

    fun saveCurrentPracticeSession() {
        val state = _studioState.value
        pauseTimer()
        metronomeEngine.stop()

        val fallbackMins = if (state.targetDurationMinutes > 0) state.targetDurationMinutes else 15
        val computedMinutes = if (state.elapsedSeconds >= 60) {
            (state.elapsedSeconds + 30) / 60
        } else {
            fallbackMins
        }
        val totalSecs = if (state.elapsedSeconds >= 60) state.elapsedSeconds else computedMinutes * 60

        viewModelScope.launch {
            repository.logPracticeSession(
                pieceId = state.selectedPieceId,
                sectionId = state.selectedSectionId,
                pieceTitle = state.customPieceTitle.ifBlank { "Technique & Sight-Reading" },
                sectionName = state.sectionName,
                composer = state.customComposer,
                instrument = state.selectedInstrument,
                category = state.selectedCategory,
                durationMinutes = computedMinutes,
                durationSecondsTotal = totalSecs,
                practicedBpm = state.metronomeBpm,
                targetBpmSnapshot = state.targetBpm,
                measuresPracticed = state.measuresPracticed,
                focusTags = state.selectedFocusTags.toList(),
                sessionNotes = state.sessionNotes.ifBlank {
                    "Practiced at ${state.metronomeBpm} BPM focusing on ${state.selectedFocusTags.joinToString(", ").ifBlank { "tone and consistency" }}."
                },
                nextSessionGoal = state.nextSessionGoal,
                moodRating = state.moodRating,
                routineName = state.activeRoutineName
            )

            accumulatedSecondsBeforeRun = 0
            timerAnchorEpochMs = 0L
            preferences.saveTimerState(isRunning = false, startEpochMs = 0L, accumulatedSec = 0)

            if (state.isQueueMode && state.activeQueueIndex in state.queueItems.indices) {
                val markedQueue = syncCurrentFieldsBackToQueue(state.queueItems, state.activeQueueIndex)
                    .mapIndexed { idx, q ->
                        if (idx == state.activeQueueIndex) q.copy(isLogged = true, elapsedSeconds = 0) else q
                    }
                val nextUnloggedIdx = markedQueue.indexOfFirst { !it.isLogged }
                _studioState.update {
                    it.copy(
                        queueItems = markedQueue,
                        elapsedSeconds = 0,
                        sessionNotes = "",
                        measuresPracticed = "",
                        nextSessionGoal = "",
                        lastSavedBanner = if (nextUnloggedIdx >= 0) {
                            "Logged ${computedMinutes}m (${state.customPieceTitle})! Ready for next block."
                        } else {
                            "Full routine '${state.activeRoutineName}' completed & logged!"
                        }
                    )
                }
                if (nextUnloggedIdx >= 0) {
                    applyQueueItemToStudio(nextUnloggedIdx)
                }
            } else {
                _studioState.update {
                    it.copy(
                        elapsedSeconds = 0,
                        measuresPracticed = "",
                        sessionNotes = "",
                        nextSessionGoal = "",
                        lastSavedBanner = "Logged ${computedMinutes}m @ ${state.metronomeBpm} BPM (Best & Last BPM updated)!"
                    )
                }
            }
        }
    }

    // --- Retroactive / Past Session Logging ---

    fun logPastPracticeSession(
        timestampEpochMillis: Long,
        durationMinutes: Int,
        piece: RepertoirePieceEntity?,
        section: PieceSectionEntity?,
        customTitle: String,
        instrument: String,
        category: PieceCategory,
        practicedBpm: Int,
        targetBpm: Int,
        measuresPracticed: String,
        focusTags: List<String>,
        sessionNotes: String,
        nextSessionGoal: String,
        moodRating: Int
    ) {
        viewModelScope.launch {
            repository.logPracticeSession(
                pieceId = piece?.id,
                sectionId = section?.id,
                pieceTitle = piece?.title ?: customTitle.ifBlank { "Past Practice Session" },
                sectionName = section?.name ?: "",
                composer = piece?.composer ?: "",
                instrument = piece?.instrument ?: instrument.ifBlank { "Piano" },
                category = piece?.pieceCategory ?: category,
                durationMinutes = durationMinutes.coerceIn(1, 600),
                durationSecondsTotal = durationMinutes.coerceIn(1, 600) * 60,
                practicedBpm = practicedBpm,
                targetBpmSnapshot = section?.targetBpm ?: piece?.targetBpm ?: targetBpm,
                measuresPracticed = measuresPracticed,
                focusTags = focusTags,
                sessionNotes = sessionNotes,
                nextSessionGoal = nextSessionGoal,
                moodRating = moodRating,
                timestampEpochMillis = timestampEpochMillis
            )
            _studioState.update {
                it.copy(lastSavedBanner = "Logged past ${durationMinutes}m session!")
            }
        }
    }

    fun dismissSavedBanner() {
        _studioState.update { it.copy(lastSavedBanner = null) }
    }

    // --- Routine Template Persistence ---

    fun saveRoutineTemplate(
        existingId: Long?,
        name: String,
        description: String,
        instrument: String,
        items: List<RoutineTemplateItemEntity>
    ) {
        if (name.isBlank() || items.isEmpty()) return
        viewModelScope.launch {
            repository.saveRoutineTemplate(existingId, name, description, instrument, items)
        }
    }

    fun saveActiveQueueAsRoutineTemplate(name: String, description: String) {
        val state = _studioState.value
        val itemsToSave = if (state.isQueueMode && state.queueItems.isNotEmpty()) {
            state.queueItems.mapIndexed { idx, q ->
                RoutineTemplateItemEntity(
                    routineId = 0L,
                    orderIndex = idx,
                    pieceId = q.pieceId,
                    sectionId = q.sectionId,
                    customTitle = q.title,
                    category = q.category.name,
                    targetMinutes = q.targetMinutes.coerceAtLeast(5),
                    suggestedBpm = q.practicedBpm,
                    notesPrompt = q.notesPrompt
                )
            }
        } else {
            listOf(
                RoutineTemplateItemEntity(
                    routineId = 0L,
                    orderIndex = 0,
                    pieceId = state.selectedPieceId,
                    sectionId = state.selectedSectionId,
                    customTitle = state.customPieceTitle.ifBlank { "Practice Block" },
                    category = state.selectedCategory.name,
                    targetMinutes = state.targetDurationMinutes.takeIf { it > 0 } ?: 15,
                    suggestedBpm = state.metronomeBpm
                )
            )
        }
        saveRoutineTemplate(null, name, description, state.selectedInstrument, itemsToSave)
    }

    fun deleteRoutineTemplate(routine: RoutineTemplateEntity) {
        viewModelScope.launch {
            repository.deleteRoutineTemplate(routine)
        }
    }

    // --- Session Log Editing & Deletion ---

    fun updateExistingSession(
        session: PracticeSessionEntity,
        updatedNotes: String,
        updatedMeasures: String,
        updatedNextGoal: String,
        updatedTags: String,
        updatedBpm: Int
    ) {
        viewModelScope.launch {
            repository.updateSession(
                session = session,
                newNotes = updatedNotes,
                newMeasures = updatedMeasures,
                newNextGoal = updatedNextGoal,
                newTags = updatedTags,
                newBpm = updatedBpm
            )
        }
    }

    fun deleteSession(session: PracticeSessionEntity) {
        viewModelScope.launch { repository.deleteSession(session) }
    }

    // --- Repertoire & Sub-Sections CRUD ---

    fun addRepertoirePiece(
        title: String,
        composer: String,
        instrument: String,
        category: PieceCategory,
        keySignature: String,
        timeSignatureBeats: Int,
        timeSignatureNoteValue: Int,
        initialBpm: Int,
        targetBpm: Int,
        status: PieceStatus,
        practiceNotes: String,
        nextSessionGoal: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addPiece(
                title = title,
                composer = composer,
                instrument = instrument,
                category = category,
                keySignature = keySignature,
                timeSignatureBeats = timeSignatureBeats,
                timeSignatureNoteValue = timeSignatureNoteValue,
                initialBpm = initialBpm,
                targetBpm = targetBpm,
                status = status,
                practiceNotes = practiceNotes,
                nextSessionGoal = nextSessionGoal
            )
        }
    }

    fun updateRepertoirePiece(piece: RepertoirePieceEntity) {
        viewModelScope.launch { repository.updatePiece(piece) }
    }

    fun deleteRepertoirePiece(piece: RepertoirePieceEntity) {
        viewModelScope.launch { repository.deletePiece(piece) }
    }

    fun addPieceSection(
        pieceId: Long,
        name: String,
        orderIndex: Int,
        initialBpm: Int,
        targetBpm: Int,
        timeSignatureBeats: Int,
        timeSignatureNoteValue: Int,
        sectionNotes: String,
        nextSessionGoal: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addSection(
                pieceId = pieceId,
                name = name,
                orderIndex = orderIndex,
                initialBpm = initialBpm,
                targetBpm = targetBpm,
                timeSignatureBeats = timeSignatureBeats,
                timeSignatureNoteValue = timeSignatureNoteValue,
                sectionNotes = sectionNotes,
                nextSessionGoal = nextSessionGoal
            )
        }
    }

    fun updatePieceSection(section: PieceSectionEntity) {
        viewModelScope.launch { repository.updateSection(section) }
    }

    fun deletePieceSection(section: PieceSectionEntity) {
        viewModelScope.launch { repository.deleteSection(section) }
    }

    // --- Backup, Restore, Teacher Export & Demo Reset ---

    fun generateBackupJson(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val snapshot = repository.captureDatabaseSnapshot(
                weeklyGoalMinutes = weeklyGoalMinutes.value,
                customInstruments = customInstruments.value,
                customFocusTags = customFocusTags.value
            )
            val json = BackupAndReportManager.serializeBackupToJson(snapshot)
            onComplete(json)
        }
    }

    fun restoreFromBackupJson(jsonString: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val outcome = runCatching {
                val payload = BackupAndReportManager.parseBackupFromJson(jsonString)
                repository.restoreDatabaseSnapshot(payload)
                preferences.restorePreferencesFromBackup(
                    weeklyGoalMinutes = payload.weeklyGoalMinutes,
                    customInstruments = payload.customInstruments,
                    customTags = payload.customFocusTags
                )
                "Restored ${payload.pieces.size} pieces and ${payload.sessions.size} sessions."
            }
            onResult(outcome)
        }
    }

    fun buildTeacherReportMarkdown(daysWindow: Int = 7): String {
        val streak = AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions = sessions.value,
            weeklyGoalMinutes = weeklyGoalMinutes.value
        )
        return BackupAndReportManager.generateTeacherReportMarkdown(
            sessions = sessions.value,
            pieces = pieces.value,
            daysWindow = daysWindow,
            weeklyStreak = streak
        )
    }

    fun buildSessionsCsv(): String {
        return BackupAndReportManager.generateSessionsCsv(sessions.value)
    }

    fun clearAllData() {
        pauseTimer()
        metronomeEngine.stop()
        viewModelScope.launch {
            repository.clearAllData()
            preferences.markInitialSeedCompleted()
            _studioState.update {
                PracticeStudioState(lastSavedBanner = "All demo & practice data cleared. Ready for a fresh start!")
            }
        }
    }

    fun reloadDemoData() {
        pauseTimer()
        metronomeEngine.stop()
        viewModelScope.launch {
            repository.reseedDemoData()
            preferences.markInitialSeedCompleted()
            _studioState.update {
                it.copy(lastSavedBanner = "Musician demo repertoire, routines & history loaded!")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        metronomeEngine.release()
    }
}

class MainViewModelFactory(
    private val repository: PracticeRepository,
    private val preferences: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
