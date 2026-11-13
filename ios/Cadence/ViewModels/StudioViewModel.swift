import Foundation
import Observation

/// One block in a routine queue. Mirrors Android's `ActiveQueueItem`.
struct ActiveQueueItem: Identifiable {
    let id: UUID
    var pieceId: Int64?
    var sectionId: Int64?
    var title: String
    var sectionName: String
    var composer: String
    var instrument: String
    var category: PieceCategory
    var targetMinutes: Int
    var elapsedSeconds: Int
    var practicedBpm: Int
    var targetBpm: Int
    var timeSignatureBeats: Int
    var timeSignatureNoteValue: Int
    var measuresPracticed: String
    var selectedFocusTags: Set<String>
    var sessionNotes: String
    var nextSessionGoal: String
    var moodRating: Int
    var notesPrompt: String
    var isLogged: Bool

    init(
        id: UUID = UUID(),
        pieceId: Int64? = nil,
        sectionId: Int64? = nil,
        title: String,
        sectionName: String = "",
        composer: String = "",
        instrument: String = "Piano",
        category: PieceCategory = .TECHNIQUE,
        targetMinutes: Int = 10,
        elapsedSeconds: Int = 0,
        practicedBpm: Int = 92,
        targetBpm: Int = 120,
        timeSignatureBeats: Int = 4,
        timeSignatureNoteValue: Int = 4,
        measuresPracticed: String = "",
        selectedFocusTags: Set<String> = ["Technique"],
        sessionNotes: String = "",
        nextSessionGoal: String = "",
        moodRating: Int = 4,
        notesPrompt: String = "",
        isLogged: Bool = false
    ) {
        self.id = id
        self.pieceId = pieceId
        self.sectionId = sectionId
        self.title = title
        self.sectionName = sectionName
        self.composer = composer
        self.instrument = instrument
        self.category = category
        self.targetMinutes = targetMinutes
        self.elapsedSeconds = elapsedSeconds
        self.practicedBpm = practicedBpm
        self.targetBpm = targetBpm
        self.timeSignatureBeats = timeSignatureBeats
        self.timeSignatureNoteValue = timeSignatureNoteValue
        self.measuresPracticed = measuresPracticed
        self.selectedFocusTags = selectedFocusTags
        self.sessionNotes = sessionNotes
        self.nextSessionGoal = nextSessionGoal
        self.moodRating = moodRating
        self.notesPrompt = notesPrompt
        self.isLogged = isLogged
    }
}

/// Owns all live-studio state: the timestamp-anchored stopwatch, metronome
/// configuration, single-block fields, and the multi-item routine queue.
@MainActor
@Observable
final class StudioViewModel {

    let store: CadenceStore
    let preferences: PreferencesStore
    let metronome: MetronomeEngine

    // Studio state
    var isQueueMode = false
    var activeRoutineName = ""
    var queueItems: [ActiveQueueItem] = []
    var activeQueueIndex = 0
    var selectedPieceId: Int64?
    var selectedSectionId: Int64?
    var customPieceTitle = ""
    var sectionName = ""
    var customComposer = ""
    var selectedInstrument = "Piano"
    var selectedCategory: PieceCategory = .REPERTOIRE

    // Stopwatch
    var timerRunning = false
    var elapsedSeconds = 0
    var targetDurationMinutes = 0

    // Metronome
    var metronomeBpm = 92
    var targetBpm = 120
    var beatsPerMeasure = 4
    var noteValuePerBeat = 4
    var beatPattern: [BeatAccentLevel] = MetronomeEngine.defaultBeatPattern(4)
    var subdivisionMode: SubdivisionMode = .QUARTER
    var metronomeMuted = false

    // Session notes
    var measuresPracticed = ""
    var selectedFocusTags: Set<String> = ["Technique", "Dynamics"]
    var sessionNotes = ""
    var nextSessionGoal = ""
    var moodRating = 4

    // Presentation
    var isMusicStandModeOpen = false
    var lastSavedBanner: String?
    var settingsStatusMessage: String?

    // Stopwatch internals
    private var timerTask: Task<Void, Never>?
    private var timerAnchorEpochMs: Int64 = 0
    private var accumulatedSecondsBeforeRun = 0
    private var tapTimestamps: [Int64] = []

    init(store: CadenceStore, preferences: PreferencesStore) {
        self.store = store
        self.preferences = preferences
        self.metronome = MetronomeEngine()

        restorePersistedTimer()
        syncMetronomeEngine()
        selectInitialPieceIfNeeded()
    }

    // MARK: - Initial selection

    func selectInitialPieceIfNeeded() {
        guard !isQueueMode,
              selectedPieceId == nil,
              customPieceTitle.trimmed.isEmpty,
              let first = store.piecesWithSections.first else { return }
        selectPieceForPractice(first.piece, section: first.sortedSections.first)
    }

    // MARK: - Stopwatch

    func toggleTimer() {
        if timerRunning {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private func startTimer() {
        guard !timerRunning else { return }
        accumulatedSecondsBeforeRun = elapsedSeconds
        timerAnchorEpochMs = Self.nowMillis()
        preferences.saveTimerState(
            isRunning: true,
            startEpochMs: timerAnchorEpochMs,
            accumulatedSec: accumulatedSecondsBeforeRun
        )
        resumeAnchoredTimerLoop()
    }

    private func resumeAnchoredTimerLoop() {
        timerRunning = true
        lastSavedBanner = nil
        timerTask?.cancel()
        timerTask = Task { [weak self] in
            guard let self else { return }
            while !Task.isCancelled && self.timerRunning {
                let delta = max(Int((Self.nowMillis() - self.timerAnchorEpochMs) / 1000), 0)
                let total = self.accumulatedSecondsBeforeRun + delta
                self.elapsedSeconds = total
                if self.isQueueMode, self.queueItems.indices.contains(self.activeQueueIndex) {
                    self.queueItems[self.activeQueueIndex].elapsedSeconds = total
                }
                try? await Task.sleep(nanoseconds: 250_000_000)
            }
        }
    }

    func pauseTimer() {
        if timerRunning {
            let delta = max(Int((Self.nowMillis() - timerAnchorEpochMs) / 1000), 0)
            accumulatedSecondsBeforeRun = max(accumulatedSecondsBeforeRun + delta, 0)
        } else {
            accumulatedSecondsBeforeRun = elapsedSeconds
        }
        timerRunning = false
        elapsedSeconds = accumulatedSecondsBeforeRun
        timerTask?.cancel()
        timerTask = nil
        preferences.saveTimerState(
            isRunning: false,
            startEpochMs: 0,
            accumulatedSec: accumulatedSecondsBeforeRun
        )
    }

    func resetTimer() {
        pauseTimer()
        accumulatedSecondsBeforeRun = 0
        timerAnchorEpochMs = 0
        elapsedSeconds = 0
        if isQueueMode, queueItems.indices.contains(activeQueueIndex) {
            queueItems[activeQueueIndex].elapsedSeconds = 0
        }
        preferences.saveTimerState(isRunning: false, startEpochMs: 0, accumulatedSec: 0)
    }

    func adjustTimerMinutes(_ deltaMinutes: Int) {
        let wasRunning = timerRunning
        let newTotal = max(elapsedSeconds + deltaMinutes * 60, 0)
        accumulatedSecondsBeforeRun = newTotal
        if wasRunning {
            timerAnchorEpochMs = Self.nowMillis()
        }
        elapsedSeconds = newTotal
        if isQueueMode, queueItems.indices.contains(activeQueueIndex) {
            queueItems[activeQueueIndex].elapsedSeconds = newTotal
        }
        preferences.saveTimerState(
            isRunning: wasRunning,
            startEpochMs: wasRunning ? timerAnchorEpochMs : 0,
            accumulatedSec: newTotal
        )
    }

    func setTargetDurationMinutes(_ minutes: Int) {
        let clamped = min(max(minutes, 0), 240)
        targetDurationMinutes = clamped
        if isQueueMode, queueItems.indices.contains(activeQueueIndex) {
            queueItems[activeQueueIndex].targetMinutes = clamped
        }
    }

    private func restorePersistedTimer() {
        let snapshot = preferences.timerSnapshot
        if snapshot.isRunning && snapshot.startEpochMs > 0 {
            accumulatedSecondsBeforeRun = snapshot.accumulatedSeconds
            timerAnchorEpochMs = snapshot.startEpochMs
            let delta = max(Int((Self.nowMillis() - snapshot.startEpochMs) / 1000), 0)
            elapsedSeconds = snapshot.accumulatedSeconds + delta
            resumeAnchoredTimerLoop()
        } else if snapshot.accumulatedSeconds > 0 {
            accumulatedSecondsBeforeRun = snapshot.accumulatedSeconds
            elapsedSeconds = snapshot.accumulatedSeconds
        }
    }

    // MARK: - Metronome

    func setMetronomeBpm(_ newBpm: Int) {
        let clamped = min(max(newBpm, 20), 300)
        metronomeBpm = clamped
        if isQueueMode, queueItems.indices.contains(activeQueueIndex) {
            queueItems[activeQueueIndex].practicedBpm = clamped
        }
        syncMetronomeEngine()
    }

    func setTimeSignature(beats: Int, noteValue: Int? = nil) {
        let clampedBeats = min(max(beats, 1), 16)
        beatsPerMeasure = clampedBeats
        if let noteValue {
            noteValuePerBeat = noteValue
        }
        beatPattern = MetronomeEngine.defaultBeatPattern(clampedBeats)
        syncMetronomeEngine()
    }

    func cycleBeatAccent(beatOneBased: Int) {
        let index = beatOneBased - 1
        guard beatPattern.indices.contains(index) else { return }
        beatPattern[index] = beatPattern[index].next
        syncMetronomeEngine()
    }

    func applyJazzBackbeatPattern() {
        beatsPerMeasure = 4
        noteValuePerBeat = 4
        beatPattern = [.MUTE, .ACCENT, .MUTE, .ACCENT]
        subdivisionMode = .SWING_EIGHTH
        syncMetronomeEngine()
    }

    func setSubdivisionMode(_ mode: SubdivisionMode) {
        subdivisionMode = mode
        syncMetronomeEngine()
    }

    func toggleMetronomeMute() {
        metronomeMuted.toggle()
        syncMetronomeEngine()
    }

    func toggleMetronomePlayback() {
        syncMetronomeEngine()
        metronome.toggle()
    }

    func syncMetronomeEngine() {
        metronome.configure(
            bpm: metronomeBpm,
            beatsPerMeasure: beatsPerMeasure,
            beatPattern: beatPattern,
            subdivisionMode: subdivisionMode,
            isMuted: metronomeMuted
        )
    }

    func registerTapTempo() {
        let now = Self.nowMillis()
        if let last = tapTimestamps.last, now - last > 2_500 {
            tapTimestamps.removeAll()
        }
        tapTimestamps.append(now)
        if tapTimestamps.count > 5 {
            tapTimestamps.removeFirst()
        }
        guard tapTimestamps.count >= 2 else { return }
        let intervals = zip(tapTimestamps, tapTimestamps.dropFirst()).map { Double($1 - $0) }
        let average = intervals.reduce(0, +) / Double(intervals.count)
        guard average > 0 else { return }
        let calculated = Int((60_000.0 / average).rounded())
        setMetronomeBpm(calculated)
    }

    // MARK: - Piece / movement selection

    func selectPieceForPractice(_ piece: RepertoirePiece?, section: PieceSection? = nil) {
        guard let piece else {
            isQueueMode = false
            activeRoutineName = ""
            selectedPieceId = nil
            selectedSectionId = nil
            customPieceTitle = "Warmup & Scale Technique"
            sectionName = ""
            customComposer = ""
            selectedCategory = .TECHNIQUE
            targetBpm = 120
            measuresPracticed = ""
            nextSessionGoal = ""
            syncMetronomeEngine()
            return
        }

        let beats = section?.timeSignatureBeats ?? piece.timeSignatureBeats
        let noteValue = section?.timeSignatureNoteValue ?? piece.timeSignatureNoteValue
        let startBpm = section?.lastPracticedBpm ?? piece.lastPracticedBpm
        let target = section?.targetBpm ?? piece.targetBpm
        let defaultSubdivision: SubdivisionMode = (noteValue == 8 && beats % 3 == 0)
            ? .QUARTER
            : subdivisionMode

        isQueueMode = false
        activeRoutineName = ""
        selectedPieceId = piece.id
        selectedSectionId = section?.id
        customPieceTitle = piece.title
        sectionName = section?.name ?? ""
        customComposer = piece.composer
        selectedInstrument = piece.instrument
        selectedCategory = piece.pieceCategory
        metronomeBpm = startBpm
        targetBpm = target
        beatsPerMeasure = beats
        noteValuePerBeat = noteValue
        beatPattern = MetronomeEngine.defaultBeatPattern(beats)
        subdivisionMode = defaultSubdivision
        measuresPracticed = ""
        nextSessionGoal = ""
        syncMetronomeEngine()
    }

    // MARK: - Routine queue

    func loadRoutineIntoQueue(_ routine: RoutineWithItems) {
        pauseTimer()
        let piecesByID = Dictionary(uniqueKeysWithValues: store.piecesWithSections.map { ($0.piece.id, $0) })

        let builtQueue: [ActiveQueueItem] = routine.sortedItems.map { item in
            let matchedPieceWithSections = item.pieceId.flatMap { piecesByID[$0] }
            let matchedPiece = matchedPieceWithSections?.piece
            let matchedSection = item.sectionId.flatMap { sectionID in
                matchedPieceWithSections?.sections.first { $0.id == sectionID }
            }

            let beats = matchedSection?.timeSignatureBeats ?? matchedPiece?.timeSignatureBeats ?? 4
            let noteValue = matchedSection?.timeSignatureNoteValue ?? matchedPiece?.timeSignatureNoteValue ?? 4
            let target = matchedSection?.targetBpm ?? matchedPiece?.targetBpm ?? item.suggestedBpm
            let initial = matchedSection?.lastPracticedBpm ?? matchedPiece?.lastPracticedBpm ?? item.suggestedBpm

            return ActiveQueueItem(
                pieceId: matchedPiece?.id,
                sectionId: matchedSection?.id,
                title: matchedPiece?.title ?? item.customTitle,
                sectionName: matchedSection?.name ?? "",
                composer: matchedPiece?.composer ?? "",
                instrument: matchedPiece?.instrument ?? (routine.routine.instrument.isEmpty ? "Piano" : routine.routine.instrument),
                category: matchedPiece?.pieceCategory ?? item.pieceCategory,
                targetMinutes: item.targetMinutes,
                elapsedSeconds: 0,
                practicedBpm: initial,
                targetBpm: target,
                timeSignatureBeats: beats,
                timeSignatureNoteValue: noteValue,
                notesPrompt: item.notesPrompt
            )
        }

        guard !builtQueue.isEmpty else { return }

        isQueueMode = true
        activeRoutineName = routine.routine.name
        queueItems = builtQueue
        activeQueueIndex = 0
        lastSavedBanner = "Loaded routine: \(routine.routine.name)"
        applyQueueItemToStudio(0)
    }

    func addAdHocItemToQueue(
        piece: RepertoirePiece?,
        section: PieceSection?,
        customTitle: String,
        targetMinutes: Int
    ) {
        let newItem = ActiveQueueItem(
            pieceId: piece?.id,
            sectionId: section?.id,
            title: piece?.title ?? (customTitle.trimmed.isEmpty ? "Practice Block" : customTitle.trimmed),
            sectionName: section?.name ?? "",
            composer: piece?.composer ?? "",
            instrument: piece?.instrument ?? selectedInstrument,
            category: piece?.pieceCategory ?? .TECHNIQUE,
            targetMinutes: min(max(targetMinutes, 1), 180),
            practicedBpm: section?.lastPracticedBpm ?? piece?.lastPracticedBpm ?? metronomeBpm,
            targetBpm: section?.targetBpm ?? piece?.targetBpm ?? targetBpm,
            timeSignatureBeats: section?.timeSignatureBeats ?? piece?.timeSignatureBeats ?? 4,
            timeSignatureNoteValue: section?.timeSignatureNoteValue ?? piece?.timeSignatureNoteValue ?? 4
        )

        let existingQueue = isQueueMode ? syncCurrentFieldsBackToQueue() : []
        let targetIndex = isQueueMode ? activeQueueIndex : 0
        queueItems = existingQueue + [newItem]
        isQueueMode = true
        if activeRoutineName.trimmed.isEmpty {
            activeRoutineName = "Custom Practice Queue"
        }
        activeQueueIndex = targetIndex

        if existingQueue.isEmpty {
            applyQueueItemToStudio(0)
        }
    }

    func selectQueueItemIndex(_ index: Int) {
        guard isQueueMode, queueItems.indices.contains(index), index != activeQueueIndex else { return }
        pauseTimer()
        queueItems = syncCurrentFieldsBackToQueue()
        activeQueueIndex = index
        applyQueueItemToStudio(index)
    }

    func exitQueueMode() {
        pauseTimer()
        isQueueMode = false
        activeRoutineName = ""
        queueItems = []
        activeQueueIndex = 0
        targetDurationMinutes = 0
    }

    private func syncCurrentFieldsBackToQueue() -> [ActiveQueueItem] {
        guard queueItems.indices.contains(activeQueueIndex) else { return queueItems }
        var updated = queueItems
        let index = activeQueueIndex
        updated[index].title = customPieceTitle.trimmed.isEmpty ? updated[index].title : customPieceTitle
        updated[index].instrument = selectedInstrument
        updated[index].elapsedSeconds = elapsedSeconds
        if targetDurationMinutes > 0 {
            updated[index].targetMinutes = targetDurationMinutes
        }
        updated[index].practicedBpm = metronomeBpm
        updated[index].measuresPracticed = measuresPracticed
        updated[index].selectedFocusTags = selectedFocusTags
        updated[index].sessionNotes = sessionNotes
        updated[index].nextSessionGoal = nextSessionGoal
        updated[index].moodRating = moodRating
        return updated
    }

    private func applyQueueItemToStudio(_ index: Int) {
        guard queueItems.indices.contains(index) else { return }
        let item = queueItems[index]

        accumulatedSecondsBeforeRun = item.elapsedSeconds
        timerAnchorEpochMs = 0
        preferences.saveTimerState(isRunning: false, startEpochMs: 0, accumulatedSec: item.elapsedSeconds)

        activeQueueIndex = index
        selectedPieceId = item.pieceId
        selectedSectionId = item.sectionId
        customPieceTitle = item.title
        sectionName = item.sectionName
        customComposer = item.composer
        selectedInstrument = item.instrument
        selectedCategory = item.category
        elapsedSeconds = item.elapsedSeconds
        targetDurationMinutes = item.targetMinutes
        metronomeBpm = item.practicedBpm
        targetBpm = item.targetBpm
        beatsPerMeasure = item.timeSignatureBeats
        noteValuePerBeat = item.timeSignatureNoteValue
        beatPattern = MetronomeEngine.defaultBeatPattern(item.timeSignatureBeats)
        measuresPracticed = item.measuresPracticed
        selectedFocusTags = item.selectedFocusTags
        sessionNotes = item.sessionNotes
        nextSessionGoal = item.nextSessionGoal
        moodRating = item.moodRating
        syncMetronomeEngine()
    }

    // MARK: - Studio field setters

    func setSelectedInstrument(_ instrument: String) {
        selectedInstrument = instrument
    }

    func setSelectedCategory(_ category: PieceCategory) {
        selectedCategory = category
    }

    func setCustomPieceTitle(_ title: String) {
        customPieceTitle = title
    }

    func updateMeasuresPracticed(_ measures: String) {
        measuresPracticed = measures
    }

    func toggleFocusTag(_ tag: String) {
        if selectedFocusTags.contains(tag) {
            selectedFocusTags.remove(tag)
        } else {
            selectedFocusTags.insert(tag)
        }
    }

    func updateSessionNotes(_ notes: String) {
        sessionNotes = notes
    }

    func updateNextSessionGoal(_ goal: String) {
        nextSessionGoal = goal
    }

    func appendNoteTemplate(_ snippet: String) {
        let prefix = sessionNotes.trimmed.isEmpty ? "• " : "\n• "
        sessionNotes += prefix + snippet
    }

    func setMoodRating(_ rating: Int) {
        moodRating = min(max(rating, 1), 5)
    }

    func setMusicStandModeOpen(_ open: Bool) {
        isMusicStandModeOpen = open
    }

    func dismissSavedBanner() {
        lastSavedBanner = nil
    }

    // MARK: - Saving a live session

    func saveCurrentPracticeSession() {
        let fallbackMinutes = targetDurationMinutes > 0 ? targetDurationMinutes : 15
        let computedMinutes = elapsedSeconds >= 60 ? (elapsedSeconds + 30) / 60 : fallbackMinutes
        let totalSeconds = elapsedSeconds >= 60 ? elapsedSeconds : computedMinutes * 60
        let tags = Array(selectedFocusTags)
        let focusText = tags.isEmpty ? "tone and consistency" : tags.joined(separator: ", ")
        let title = customPieceTitle.trimmed.isEmpty ? "Technique & Sight-Reading" : customPieceTitle
        let notes = sessionNotes.trimmed.isEmpty
            ? "Practiced at \(metronomeBpm) BPM focusing on \(focusText)."
            : sessionNotes
        let savedTitle = customPieceTitle
        let wasQueueMode = isQueueMode
        let savedBpm = metronomeBpm

        pauseTimer()
        metronome.stop()

        store.logPracticeSession(
            pieceId: selectedPieceId,
            sectionId: selectedSectionId,
            pieceTitle: title,
            sectionName: sectionName,
            composer: customComposer,
            instrument: selectedInstrument,
            category: selectedCategory,
            durationMinutes: computedMinutes,
            durationSecondsTotal: totalSeconds,
            practicedBpm: metronomeBpm,
            targetBpmSnapshot: targetBpm,
            measuresPracticed: measuresPracticed,
            focusTags: tags,
            sessionNotes: notes,
            nextSessionGoal: nextSessionGoal,
            moodRating: moodRating,
            routineName: activeRoutineName
        )

        accumulatedSecondsBeforeRun = 0
        timerAnchorEpochMs = 0
        preferences.saveTimerState(isRunning: false, startEpochMs: 0, accumulatedSec: 0)

        if wasQueueMode, queueItems.indices.contains(activeQueueIndex) {
            var marked = syncCurrentFieldsBackToQueue()
            marked[activeQueueIndex].isLogged = true
            marked[activeQueueIndex].elapsedSeconds = 0
            queueItems = marked
            elapsedSeconds = 0
            sessionNotes = ""
            measuresPracticed = ""
            nextSessionGoal = ""

            if let nextUnlogged = marked.firstIndex(where: { !$0.isLogged }) {
                lastSavedBanner = "Logged \(computedMinutes)m (\(savedTitle))! Ready for next block."
                applyQueueItemToStudio(nextUnlogged)
            } else {
                lastSavedBanner = "Full routine '\(activeRoutineName)' completed & logged!"
            }
        } else {
            elapsedSeconds = 0
            measuresPracticed = ""
            sessionNotes = ""
            nextSessionGoal = ""
            lastSavedBanner = "Logged \(computedMinutes)m @ \(savedBpm) BPM (Best & Last BPM updated)!"
        }
    }

    // MARK: - Retroactive logging

    func logPastPracticeSession(
        timestampEpochMillis: Int64,
        durationMinutes: Int,
        piece: RepertoirePiece?,
        section: PieceSection?,
        customTitle: String,
        instrument: String,
        category: PieceCategory,
        practicedBpm: Int,
        targetBpm: Int,
        measuresPracticed: String,
        focusTags: [String],
        sessionNotes: String,
        nextSessionGoal: String,
        moodRating: Int
    ) {
        let cleanMinutes = min(max(durationMinutes, 1), 600)
        store.logPracticeSession(
            pieceId: piece?.id,
            sectionId: section?.id,
            pieceTitle: piece?.title ?? (customTitle.trimmed.isEmpty ? "Past Practice Session" : customTitle),
            sectionName: section?.name ?? "",
            composer: piece?.composer ?? "",
            instrument: piece?.instrument ?? (instrument.trimmed.isEmpty ? "Piano" : instrument),
            category: piece?.pieceCategory ?? category,
            durationMinutes: cleanMinutes,
            durationSecondsTotal: cleanMinutes * 60,
            practicedBpm: practicedBpm,
            targetBpmSnapshot: section?.targetBpm ?? piece?.targetBpm ?? targetBpm,
            measuresPracticed: measuresPracticed,
            focusTags: focusTags,
            sessionNotes: sessionNotes,
            nextSessionGoal: nextSessionGoal,
            moodRating: moodRating,
            timestampEpochMillis: timestampEpochMillis
        )
        lastSavedBanner = "Logged past \(cleanMinutes)m session!"
    }

    // MARK: - Routine templates

    func saveActiveQueueAsRoutineTemplate(name: String, descriptionText: String) {
        guard !name.trimmed.isEmpty else { return }

        let items: [RoutineItemDraft]
        if isQueueMode && !queueItems.isEmpty {
            items = queueItems.map { item in
                RoutineItemDraft(
                    pieceId: item.pieceId,
                    sectionId: item.sectionId,
                    customTitle: item.title,
                    category: item.category,
                    targetMinutes: max(item.targetMinutes, 5),
                    suggestedBpm: item.practicedBpm,
                    notesPrompt: item.notesPrompt
                )
            }
        } else {
            items = [
                RoutineItemDraft(
                    pieceId: selectedPieceId,
                    sectionId: selectedSectionId,
                    customTitle: customPieceTitle.trimmed.isEmpty ? "Practice Block" : customPieceTitle,
                    category: selectedCategory,
                    targetMinutes: targetDurationMinutes > 0 ? targetDurationMinutes : 15,
                    suggestedBpm: metronomeBpm
                )
            ]
        }

        _ = store.saveRoutineTemplate(
            existingId: nil,
            name: name,
            descriptionText: descriptionText,
            instrument: selectedInstrument,
            items: items
        )
    }

    func deleteRoutineTemplate(_ routine: RoutineTemplate) {
        store.deleteRoutine(routine)
    }

    // MARK: - Journal helpers

    func updateExistingSession(
        _ session: PracticeSession,
        notes: String,
        measures: String,
        nextGoal: String,
        tags: String,
        bpm: Int
    ) {
        store.updateSession(
            session,
            notes: notes,
            measures: measures,
            nextGoal: nextGoal,
            tags: tags,
            bpm: bpm
        )
    }

    func deleteSession(_ session: PracticeSession) {
        store.deleteSession(session)
    }

    // MARK: - Backup / reports

    func generateBackupJson() -> String {
        let snapshot = store.captureSnapshot(
            weeklyGoalMinutes: preferences.weeklyGoalMinutes,
            customInstruments: preferences.customInstruments,
            customFocusTags: preferences.customFocusTags
        )
        return BackupManager.serializeBackupToJson(snapshot)
    }

    func restoreFromBackupJson(_ json: String) -> Result<String, Error> {
        do {
            let payload = try BackupManager.parseBackupFromJson(json)
            store.restoreSnapshot(payload)
            preferences.restoreFromBackup(
                weeklyGoalMinutes: payload.weeklyGoalMinutes,
                customInstruments: payload.customInstruments,
                customTags: payload.customFocusTags
            )
            resetStudioState()
            selectInitialPieceIfNeeded()
            return .success("Restored \(payload.pieces.count) pieces and \(payload.sessions.count) sessions.")
        } catch {
            return .failure(error)
        }
    }

    func buildTeacherReportMarkdown(daysWindow: Int = 7) -> String {
        let streak = AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions: store.sessions,
            weeklyGoalMinutes: preferences.weeklyGoalMinutes
        )
        return BackupManager.generateTeacherReportMarkdown(
            sessions: store.sessions,
            pieces: store.pieces,
            daysWindow: daysWindow,
            weeklyStreak: streak
        )
    }

    func buildSessionsCsv() -> String {
        BackupManager.generateSessionsCsv(store.sessions)
    }

    func clearAllData() {
        pauseTimer()
        metronome.stop()
        store.clearAllData()
        preferences.markInitialSeedCompleted()
        resetStudioState()
        lastSavedBanner = "All demo & practice data cleared. Ready for a fresh start!"
    }

    func reloadDemoData() {
        pauseTimer()
        metronome.stop()
        store.clearAllData()
        DemoData.seed(into: store)
        preferences.markInitialSeedCompleted()
        resetStudioState()
        selectInitialPieceIfNeeded()
        lastSavedBanner = "Musician demo repertoire, routines & history loaded!"
    }

    private func resetStudioState() {
        isQueueMode = false
        activeRoutineName = ""
        queueItems = []
        activeQueueIndex = 0
        selectedPieceId = nil
        selectedSectionId = nil
        customPieceTitle = ""
        sectionName = ""
        customComposer = ""
        selectedInstrument = "Piano"
        selectedCategory = .REPERTOIRE
        timerRunning = false
        elapsedSeconds = 0
        targetDurationMinutes = 0
        metronomeBpm = 92
        targetBpm = 120
        beatsPerMeasure = 4
        noteValuePerBeat = 4
        beatPattern = MetronomeEngine.defaultBeatPattern(4)
        subdivisionMode = .QUARTER
        metronomeMuted = false
        measuresPracticed = ""
        selectedFocusTags = ["Technique", "Dynamics"]
        sessionNotes = ""
        nextSessionGoal = ""
        moodRating = 4
        accumulatedSecondsBeforeRun = 0
        timerAnchorEpochMs = 0
        preferences.saveTimerState(isRunning: false, startEpochMs: 0, accumulatedSec: 0)
        syncMetronomeEngine()
    }

    // MARK: - Utilities

    static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
