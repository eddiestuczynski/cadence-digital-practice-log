import SwiftUI
import UIKit

private let quickNoteTemplates = [
    "Isolated difficult measures at slow tempo",
    "Practiced with metronome on beats 2 & 4",
    "Focused on relaxed posture & tone projection",
    "Clean run-through without stopping"
]

struct StudioView: View {
    @Environment(StudioViewModel.self) private var studio
    @Environment(CadenceStore.self) private var store
    @Environment(PreferencesStore.self) private var preferences
    @Environment(\.cadencePalette) private var palette

    let piecesWithSections: [PieceWithSections]
    let sessions: [PracticeSession]
    let routines: [RoutineWithItems]
    let allInstruments: [String]
    let allFocusTags: [String]
    let keepScreenAwakeEnabled: Bool

    @State private var showRoutines = false
    @State private var showAddBlock = false
    @State private var showSaveRoutine = false
    @State private var showLogPast = false
    @State private var showCustomTag = false
    @State private var customTagText = ""
    @State private var beatFlash = false
    @State private var flashTask: Task<Void, Never>?

    private var currentPieceWithSections: PieceWithSections? {
        piecesWithSections.first { $0.piece.id == studio.selectedPieceId }
    }

    private var currentPiece: RepertoirePiece? {
        currentPieceWithSections?.piece
    }

    private var currentSection: PieceSection? {
        guard let currentPieceWithSections, let sectionID = studio.selectedSectionId else { return nil }
        return currentPieceWithSections.sections.first { $0.id == sectionID }
    }

    private var latestMatchingSession: PracticeSession? {
        guard let pieceID = studio.selectedPieceId else { return nil }
        return sessions.first { session in
            session.pieceId == pieceID &&
                (studio.selectedSectionId == nil || session.sectionId == studio.selectedSectionId)
        }
    }

    private var pinnedNextGoal: String {
        let sectionGoal = currentSection?.nextSessionGoal.trimmed ?? ""
        if !sectionGoal.isEmpty { return sectionGoal }
        return currentPiece?.nextSessionGoal.trimmed ?? ""
    }

    private var standingScoreNotes: String {
        let sectionNotes = currentSection?.sectionNotes.trimmed ?? ""
        if !sectionNotes.isEmpty { return sectionNotes }
        return currentPiece?.practiceNotes.trimmed ?? ""
    }

    private var previousSessionSummary: String {
        guard let latestMatchingSession else { return "" }
        var text = "\(latestMatchingSession.practicedBpm) BPM"
        if !latestMatchingSession.measuresPracticed.isEmpty {
            text += " [\(latestMatchingSession.measuresPracticed)]"
        }
        if !latestMatchingSession.sessionNotes.isEmpty {
            text += " — \(latestMatchingSession.sessionNotes)"
        }
        return text
    }

    private var shouldStayAwake: Bool {
        keepScreenAwakeEnabled &&
            (studio.timerRunning || studio.metronome.isPlaying || studio.isMusicStandModeOpen)
    }

    private var metronomeBorderColor: Color {
        if beatFlash && studio.metronome.currentBeat == 1 { return palette.primary }
        if beatFlash { return palette.secondary }
        return palette.outline.opacity(0.5)
    }

    private var metronomeBorderWidth: CGFloat {
        guard beatFlash else { return 1 }
        return studio.metronomeMuted ? 3.5 : 2
    }

    private var pieceMenuLabel: String {
        if let currentPiece, let currentSection {
            return "\(currentPiece.title) — \(currentSection.name) (\(currentSection.timeSignatureLabel))"
        }
        if let currentPiece {
            return "\(currentPiece.title) • \(currentPiece.timeSignatureLabel) (Best \(currentPiece.bestBpm)/\(currentPiece.targetBpm) BPM)"
        }
        return "Custom / General Warmup & Technique"
    }

    private var saveButtonLabel: String {
        let fallback = studio.targetDurationMinutes > 0 ? studio.targetDurationMinutes : 15
        let display = studio.elapsedSeconds >= 60
            ? "\((studio.elapsedSeconds + 30) / 60)m"
            : "\(fallback)m Quick"
        if studio.isQueueMode {
            return "Log Block & Advance (\(display) @ \(studio.metronomeBpm) BPM)"
        }
        return "Save Practice Session (\(display) @ \(studio.metronomeBpm) BPM)"
    }

    var body: some View {
        @Bindable var vm = studio

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                quickToolbar

                if let banner = studio.lastSavedBanner {
                    CadenceBanner(message: banner) {
                        studio.dismissSavedBanner()
                    }
                }

                if studio.isQueueMode && !studio.queueItems.isEmpty {
                    queueCard
                }

                stopwatchCard(vm: $vm)
                pieceCard(vm: $vm)
                metronomeCard(vm: $vm)
                notesCard(vm: $vm)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(palette.background.ignoresSafeArea())
        .onChange(of: studio.metronome.beatPulseID) { _, _ in
            guard studio.metronome.isPlaying else { return }
            flashTask?.cancel()
            withAnimation(.easeOut(duration: 0.08)) { beatFlash = true }
            flashTask = Task {
                try? await Task.sleep(nanoseconds: 105_000_000)
                guard !Task.isCancelled else { return }
                withAnimation(.easeOut(duration: 0.12)) { beatFlash = false }
            }
        }
        .onChange(of: studio.metronome.isPlaying) { _, isPlaying in
            if !isPlaying {
                flashTask?.cancel()
                beatFlash = false
            }
            updateIdleTimer()
        }
        .onChange(of: shouldStayAwake) { _, _ in
            updateIdleTimer()
        }
        .onAppear { updateIdleTimer() }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .sheet(isPresented: $showRoutines) {
            RoutinesSheet(routines: routines, onStart: { routine in
                studio.loadRoutineIntoQueue(routine)
                showRoutines = false
            }, onDelete: { routine in
                studio.deleteRoutineTemplate(routine)
            }, onAddBlock: {
                showRoutines = false
                showAddBlock = true
            })
        }
        .sheet(isPresented: $showAddBlock) {
            AddBlockSheet(piecesWithSections: piecesWithSections) { piece, section, title, minutes in
                studio.addAdHocItemToQueue(
                    piece: piece,
                    section: section,
                    customTitle: title,
                    targetMinutes: minutes
                )
                showAddBlock = false
            }
        }
        .sheet(isPresented: $showSaveRoutine) {
            SaveRoutineSheet(initialName: studio.activeRoutineName) { name, description in
                studio.saveActiveQueueAsRoutineTemplate(name: name, descriptionText: description)
                showSaveRoutine = false
            }
        }
        .sheet(isPresented: $showLogPast) {
            LogPastSessionView(
                piecesWithSections: piecesWithSections,
                allInstruments: allInstruments,
                allFocusTags: allFocusTags
            ) { payload in
                studio.logPastPracticeSession(
                    timestampEpochMillis: payload.timestampEpochMillis,
                    durationMinutes: payload.durationMinutes,
                    piece: payload.piece,
                    section: payload.section,
                    customTitle: payload.customTitle,
                    instrument: payload.instrument,
                    category: payload.category,
                    practicedBpm: payload.practicedBpm,
                    targetBpm: payload.targetBpm,
                    measuresPracticed: payload.measuresPracticed,
                    focusTags: payload.focusTags,
                    sessionNotes: payload.sessionNotes,
                    nextSessionGoal: payload.nextSessionGoal,
                    moodRating: payload.moodRating
                )
                showLogPast = false
            }
        }
        .alert("Create Custom Focus Tag", isPresented: $showCustomTag) {
            TextField("Tag name (e.g. Double Stops, Rubato)", text: $customTagText)
            Button("Add & Select") {
                let tag = customTagText.trimmed
                guard !tag.isEmpty else { return }
                preferences.addCustomFocusTag(tag)
                studio.toggleFocusTag(tag)
                customTagText = ""
            }
            Button("Cancel", role: .cancel) {
                customTagText = ""
            }
        } message: {
            Text("Custom tags appear in the Studio, Journal and Log Past Session pickers.")
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { studio.isMusicStandModeOpen },
                set: { studio.setMusicStandModeOpen($0) }
            )
        ) {
            MusicStandView(
                pinnedNextGoal: pinnedNextGoal,
                standingScoreNotes: standingScoreNotes,
                previousSessionSummary: previousSessionSummary
            )
        }
    }

    // MARK: - Quick toolbar

    private var quickToolbar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                Button {
                    studio.setMusicStandModeOpen(true)
                } label: {
                    Label("Music Stand Mode", systemImage: "arrow.up.left.and.arrow.down.right")
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primaryContainer)
                .foregroundStyle(palette.onPrimaryContainer)

                Button {
                    showRoutines = true
                } label: {
                    Label(
                        studio.isQueueMode ? "Routine Queue (\(studio.queueItems.count))" : "Routines & Queue",
                        systemImage: "list.bullet.rectangle"
                    )
                }
                .buttonStyle(.bordered)

                Button {
                    showLogPast = true
                } label: {
                    Label("Log Past Session", systemImage: "clock.arrow.circlepath")
                }
                .buttonStyle(.bordered)
            }
        }
    }

    // MARK: - Queue card

    private var queueCard: some View {
        CadenceCard(
            fill: palette.primaryContainer.opacity(0.45),
            borderColor: palette.primary.opacity(0.5)
        ) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("ACTIVE SESSION QUEUE")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(palette.primary)
                        Text(studio.activeRoutineName)
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                    }
                    Spacer()
                    Button("+ Block") { showAddBlock = true }
                        .font(.footnote.weight(.semibold))
                    Button("Save Template") { showSaveRoutine = true }
                        .font(.footnote.weight(.semibold))
                    Button("Exit") { studio.exitQueueMode() }
                        .font(.footnote.weight(.semibold))
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(studio.queueItems.indices, id: \.self) { index in
                            let item = studio.queueItems[index]
                            let prefix = item.isLogged ? "✓ " : (index == studio.activeQueueIndex ? "▶ " : "\(index + 1). ")
                            ChipButton(
                                title: "\(prefix)\(item.title) (\(item.targetMinutes)m)",
                                isSelected: index == studio.activeQueueIndex
                            ) {
                                studio.selectQueueItemIndex(index)
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - Stopwatch card

    private func stopwatchCard(vm: Bindable<StudioViewModel>) -> some View {
        CadenceCard(padding: 20) {
            VStack(spacing: 10) {
                HStack(spacing: 8) {
                    Text(studio.timerRunning ? "SESSION IN PROGRESS" : "LIVE PRACTICE STOPWATCH")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(studio.timerRunning ? palette.primary : palette.inkSecondary)
                    if (studio.timerRunning || studio.metronome.isPlaying) && keepScreenAwakeEnabled {
                        HStack(spacing: 4) {
                            Image(systemName: "eye.fill").font(.caption2)
                            Text("Awake").font(.caption2.weight(.semibold))
                        }
                        .foregroundStyle(palette.onSecondaryContainer)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(palette.secondaryContainer))
                    }
                }

                Text(Formatters.clock(studio.elapsedSeconds))
                    .font(.system(size: 54, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(palette.inkPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)

                if studio.targetDurationMinutes > 0 {
                    let targetSeconds = max(studio.targetDurationMinutes * 60, 1)
                    let progress = min(max(Double(studio.elapsedSeconds) / Double(targetSeconds), 0), 1)
                    VStack(spacing: 5) {
                        CadenceProgressBar(
                            value: progress,
                            color: progress >= 1 ? palette.tertiary : palette.primary,
                            height: 8
                        )
                        Text(progress >= 1
                             ? "\(studio.targetDurationMinutes)m target reached — continuing count-up"
                             : "Silent Target: \(studio.targetDurationMinutes) min (\(Int(progress * 100))%)")
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                    }
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        Text("Target:")
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                        ForEach([0, 10, 15, 20, 30], id: \.self) { minutes in
                            ChipButton(
                                title: minutes == 0 ? "Open" : "\(minutes)m",
                                isSelected: studio.targetDurationMinutes == minutes
                            ) {
                                studio.setTargetDurationMinutes(minutes)
                            }
                        }
                    }
                }

                HStack(spacing: 8) {
                    Button("-5m") { studio.adjustTimerMinutes(-5) }
                        .buttonStyle(.bordered)
                    Button("+5m") { studio.adjustTimerMinutes(5) }
                        .buttonStyle(.bordered)
                    Button("+15m") { studio.adjustTimerMinutes(15) }
                        .buttonStyle(.bordered)
                }

                HStack(spacing: 12) {
                    Button {
                        studio.toggleTimer()
                    } label: {
                        Label(
                            studio.timerRunning ? "Pause Stopwatch" : "Start Practice",
                            systemImage: studio.timerRunning ? "pause.fill" : "play.fill"
                        )
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)
                    .controlSize(.large)

                    Button {
                        studio.resetTimer()
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                    .accessibilityLabel("Reset stopwatch")
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Piece card

    private func pieceCard(vm: Bindable<StudioViewModel>) -> some View {
        CadenceCard(padding: 18) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Repertoire, Movement & Continuity")
                        .font(.headline)
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    if !studio.isQueueMode {
                        Button("+ Queue") { showAddBlock = true }
                            .font(.footnote.weight(.semibold))
                    }
                }

                Menu {
                    Button("Custom / General Warmup & Technique") {
                        studio.selectPieceForPractice(nil)
                    }
                    ForEach(piecesWithSections) { pws in
                        Section(pws.piece.title) {
                            Button("\(pws.piece.title) (\(pws.piece.pieceCategory.shortLabel))") {
                                studio.selectPieceForPractice(pws.piece)
                            }
                            ForEach(pws.sortedSections) { section in
                                Button("↳ \(section.name) (\(section.timeSignatureLabel) • Best \(section.bestBpm)/\(section.targetBpm) BPM)") {
                                    studio.selectPieceForPractice(pws.piece, section: section)
                                }
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text(pieceMenuLabel)
                            .font(.subheadline)
                            .lineLimit(1)
                            .foregroundStyle(palette.inkPrimary)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                    }
                    .padding(11)
                    .background(
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .fill(palette.surfaceVariant.opacity(0.6))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .stroke(palette.outline.opacity(0.6), lineWidth: 1)
                    )
                }

                if let currentPieceWithSections, !currentPieceWithSections.sortedSections.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ChipButton(
                                title: "Full Piece",
                                isSelected: studio.selectedSectionId == nil
                            ) {
                                studio.selectPieceForPractice(currentPieceWithSections.piece)
                            }
                            ForEach(currentPieceWithSections.sortedSections) { section in
                                ChipButton(
                                    title: section.name,
                                    isSelected: studio.selectedSectionId == section.id
                                ) {
                                    studio.selectPieceForPractice(currentPieceWithSections.piece, section: section)
                                }
                            }
                        }
                    }
                }

                if !pinnedNextGoal.isEmpty || !standingScoreNotes.isEmpty || !previousSessionSummary.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        if !pinnedNextGoal.isEmpty {
                            HStack(spacing: 6) {
                                Image(systemName: "flag.fill")
                                    .font(.caption)
                                    .foregroundStyle(palette.primary)
                                Text("Pinned Goal for Today:")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(palette.primary)
                            }
                            Text(pinnedNextGoal)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(palette.inkPrimary)
                        }
                        if !standingScoreNotes.isEmpty {
                            Text("Score Notes: \(standingScoreNotes)")
                                .font(.caption)
                                .foregroundStyle(palette.inkPrimary)
                        }
                        if !previousSessionSummary.isEmpty {
                            Text("Last Session: \(previousSessionSummary)")
                                .font(.caption)
                                .foregroundStyle(palette.inkSecondary)
                        }
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(palette.primaryContainer.opacity(0.5))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(palette.primary.opacity(0.35), lineWidth: 1)
                    )
                }

                CadenceTextField(
                    label: "Piece / Exercise Title",
                    placeholder: "e.g. Clair de Lune",
                    text: vm.customPieceTitle
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(PieceCategory.allCases) { category in
                            ChipButton(
                                title: category.shortLabel,
                                isSelected: studio.selectedCategory == category
                            ) {
                                studio.setSelectedCategory(category)
                            }
                        }
                    }
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(allInstruments, id: \.self) { instrument in
                            ChipButton(
                                title: instrument,
                                isSelected: studio.selectedInstrument.caseInsensitiveCompare(instrument) == .orderedSame
                            ) {
                                studio.setSelectedInstrument(instrument)
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - Metronome card

    private func metronomeCard(vm: Bindable<StudioViewModel>) -> some View {
        CadenceCard(borderColor: metronomeBorderColor, borderWidth: metronomeBorderWidth) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "speedometer")
                            .foregroundStyle(palette.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Precision Metronome")
                                .font(.headline)
                                .foregroundStyle(palette.inkPrimary)
                            Text("\(MetronomeEngine.tempoMarking(forBpm: studio.metronomeBpm)) • \(studio.beatsPerMeasure)/\(studio.noteValuePerBeat) • Tap beat circles for Accent/Mute")
                                .font(.caption2)
                                .foregroundStyle(palette.secondary)
                        }
                    }
                    Spacer()
                    Button {
                        studio.toggleMetronomeMute()
                    } label: {
                        Image(systemName: studio.metronomeMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .foregroundStyle(studio.metronomeMuted ? palette.primary : palette.inkSecondary)
                    }
                    .accessibilityLabel("Mute or unmute metronome; visual pulse stays active")
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(studio.beatPattern.indices, id: \.self) { index in
                            beatCircle(index: index, level: studio.beatPattern[index])
                        }
                    }
                    .frame(maxWidth: .infinity)
                }

                HStack {
                    Button("-5") { studio.setMetronomeBpm(studio.metronomeBpm - 5) }
                        .buttonStyle(.bordered)
                    Button {
                        studio.setMetronomeBpm(studio.metronomeBpm - 1)
                    } label: {
                        Image(systemName: "minus")
                    }
                    .buttonStyle(.bordered)

                    Spacer()

                    VStack(spacing: 0) {
                        Text("\(studio.metronomeBpm)")
                            .font(.system(size: 40, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(palette.inkPrimary)
                        Text(bestTempoReference)
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                    }

                    Spacer()

                    Button {
                        studio.setMetronomeBpm(studio.metronomeBpm + 1)
                    } label: {
                        Image(systemName: "plus")
                    }
                    .buttonStyle(.bordered)
                    Button("+5") { studio.setMetronomeBpm(studio.metronomeBpm + 5) }
                        .buttonStyle(.bordered)
                }

                Slider(
                    value: Binding(
                        get: { Double(studio.metronomeBpm) },
                        set: { studio.setMetronomeBpm(Int($0.rounded())) }
                    ),
                    in: 30...240
                )
                .tint(palette.primary)

                CadenceSectionLabel(text: "Subdivisions")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(SubdivisionMode.allCases) { mode in
                            ChipButton(
                                title: mode.label,
                                isSelected: studio.subdivisionMode == mode
                            ) {
                                studio.setSubdivisionMode(mode)
                            }
                        }
                    }
                }

                CadenceSectionLabel(text: "Meter & Feel Presets")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(meterPresets) { preset in
                            ChipButton(
                                title: preset.label,
                                isSelected: studio.beatsPerMeasure == preset.beats &&
                                    studio.noteValuePerBeat == preset.noteValue
                            ) {
                                studio.setTimeSignature(beats: preset.beats, noteValue: preset.noteValue)
                            }
                        }
                        Button("2 & 4 Swing") { studio.applyJazzBackbeatPattern() }
                            .buttonStyle(.bordered)
                            .font(.footnote.weight(.semibold))
                    }
                }

                if studio.targetBpm > 0 {
                    let ratio = min(max(Double(studio.metronomeBpm) / Double(studio.targetBpm), 0), 1)
                    VStack(spacing: 4) {
                        HStack {
                            Text("Current Tempo vs. Target")
                                .font(.caption2)
                                .foregroundStyle(palette.inkSecondary)
                            Spacer()
                            Text("\(Int(ratio * 100))%")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.primary)
                        }
                        CadenceProgressBar(value: ratio, height: 8)
                    }
                }

                HStack(spacing: 10) {
                    Button {
                        studio.toggleMetronomePlayback()
                    } label: {
                        Label(
                            metronomePlayLabel,
                            systemImage: studio.metronome.isPlaying ? "pause.fill" : "play.fill"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(studio.metronome.isPlaying ? palette.secondary : palette.primary)

                    Button {
                        studio.registerTapTempo()
                    } label: {
                        Label("Tap Tempo", systemImage: "hand.tap.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }

    private func beatCircle(index: Int, level: BeatAccentLevel) -> some View {
        let beat = index + 1
        let isActive = studio.metronome.isPlaying && studio.metronome.currentBeat == beat
        let diameter: CGFloat = isActive ? 38 : 32

        let fill: Color = {
            if isActive && level == .ACCENT { return palette.primary }
            if isActive && level == .NORMAL { return palette.secondary }
            if isActive && level == .MUTE { return palette.surfaceVariant }
            if level == .MUTE { return .clear }
            return palette.surfaceVariant
        }()

        let textColor: Color = {
            if isActive && level != .MUTE { return palette.onPrimary }
            return palette.inkSecondary
        }()

        return Button {
            studio.cycleBeatAccent(beatOneBased: beat)
        } label: {
            Text(level == .MUTE ? "–" : "\(beat)")
                .font(.footnote.weight(.bold))
                .foregroundStyle(textColor)
                .frame(width: diameter, height: diameter)
                .background(Circle().fill(fill))
                .overlay(
                    Circle().stroke(
                        level == .ACCENT ? palette.primary : palette.outline,
                        lineWidth: level == .ACCENT ? 2 : 1
                    )
                )
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.09), value: isActive)
    }

    private var bestTempoReference: String {
        if let best = currentSection?.bestBpm ?? currentPiece?.bestBpm {
            return "Best \(best) • Target \(studio.targetBpm) BPM"
        }
        return "BPM (Target: \(studio.targetBpm))"
    }

    private var metronomePlayLabel: String {
        if studio.metronome.isPlaying {
            return studio.metronomeMuted ? "Stop Visual Pulse" : "Stop Click"
        }
        return studio.metronomeMuted ? "Start Visual Pulse" : "Start Metronome"
    }

    private var meterPresets: [MeterPreset] {
        [
            MeterPreset(beats: 2, noteValue: 4, label: "2/4"),
            MeterPreset(beats: 3, noteValue: 4, label: "3/4"),
            MeterPreset(beats: 4, noteValue: 4, label: "4/4"),
            MeterPreset(beats: 5, noteValue: 4, label: "5/4"),
            MeterPreset(beats: 6, noteValue: 8, label: "6/8"),
            MeterPreset(beats: 9, noteValue: 8, label: "9/8")
        ]
    }

    // MARK: - Notes card

    private func notesCard(vm: Bindable<StudioViewModel>) -> some View {
        CadenceCard(padding: 18) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "square.and.pencil")
                        .foregroundStyle(palette.primary)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Practice Session Notes & Next Goal")
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                        Text("Track exact measures, technical notes, and pin your next goal")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                    }
                }

                CadenceTextField(
                    label: "Measures / Section Practiced (optional)",
                    placeholder: "e.g., m. 24–32, Cadenza, Bridge",
                    text: vm.measuresPracticed
                )

                CadenceTextField(
                    label: "Session Notes (Technique, Fingerings, Breakthroughs)",
                    placeholder: "e.g., Isolated m. 24–32 hands separately at 76 BPM...",
                    text: vm.sessionNotes,
                    lineLimit: 4...8
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(quickNoteTemplates, id: \.self) { snippet in
                            Button("+ \(snippet)") {
                                studio.appendNoteTemplate(snippet)
                            }
                            .buttonStyle(.bordered)
                            .font(.caption2)
                        }
                    }
                }

                CadenceTextField(
                    label: "Goal for Next Session (Pins to top next time)",
                    placeholder: "e.g., Connect m. 32–40 at 84 BPM without pedal blur",
                    text: vm.nextSessionGoal
                )

                HStack {
                    CadenceSectionLabel(text: "Session Focus Tags")
                    Spacer()
                    Button("+ Custom Tag") { showCustomTag = true }
                        .font(.caption.weight(.semibold))
                }

                FlowLayout(spacing: 8) {
                    ForEach(allFocusTags, id: \.self) { tag in
                        ChipButton(
                            title: tag,
                            isSelected: studio.selectedFocusTags.contains(tag)
                        ) {
                            studio.toggleFocusTag(tag)
                        }
                    }
                }

                HStack {
                    Text("Session Quality & Focus:")
                        .font(.caption)
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    StarRatingView(rating: studio.moodRating, size: 22) { rating in
                        studio.setMoodRating(rating)
                    }
                }

                Button {
                    studio.saveCurrentPracticeSession()
                } label: {
                    Label(saveButtonLabel, systemImage: "square.and.arrow.down.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
                .controlSize(.large)
            }
        }
    }

    // MARK: - Helpers

    private func updateIdleTimer() {
        UIApplication.shared.isIdleTimerDisabled = shouldStayAwake
    }
}

// MARK: - Routines sheet

private struct RoutinesSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.cadencePalette) private var palette

    let routines: [RoutineWithItems]
    let onStart: (RoutineWithItems) -> Void
    let onDelete: (RoutineTemplate) -> Void
    let onAddBlock: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Load a saved multi-item routine template or build an ad-hoc queue for today's sitting:")
                        .font(.subheadline)
                        .foregroundStyle(palette.inkSecondary)

                    Button {
                        onAddBlock()
                    } label: {
                        Label("Add Item to Today's Session Queue", systemImage: "plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)

                    Divider()

                    if routines.isEmpty {
                        Text("No saved routine templates yet. Add blocks to a queue and tap 'Save Template'!")
                            .font(.subheadline)
                            .foregroundStyle(palette.inkSecondary)
                    } else {
                        ForEach(routines) { routine in
                            CadenceCard(padding: 12, fill: palette.surfaceVariant.opacity(0.6)) {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text(routine.routine.name)
                                            .font(.headline)
                                            .foregroundStyle(palette.inkPrimary)
                                        Spacer()
                                        Button {
                                            onDelete(routine.routine)
                                        } label: {
                                            Image(systemName: "trash")
                                                .foregroundStyle(palette.inkSecondary)
                                        }
                                        .buttonStyle(.plain)
                                        .accessibilityLabel("Delete routine")
                                    }
                                    if !routine.routine.descriptionText.isEmpty {
                                        Text(routine.routine.descriptionText)
                                            .font(.caption)
                                            .foregroundStyle(palette.inkSecondary)
                                    }
                                    Text("\(routine.items.count) blocks • \(routine.totalTargetMinutes) min target")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(palette.primary)
                                    Button("Start Routine") {
                                        onStart(routine)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(palette.primary)
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Practice Routines & Queue")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Add block sheet

private struct AddBlockSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.cadencePalette) private var palette

    let piecesWithSections: [PieceWithSections]
    let onAdd: (RepertoirePiece?, PieceSection?, String, Int) -> Void

    @State private var pickedPiece: RepertoirePiece?
    @State private var pickedSection: PieceSection?
    @State private var blockTitle = "Warmup & Scales"
    @State private var targetMinutesText = "15"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Menu {
                        Button("Custom Block / Warmup") {
                            pickedPiece = nil
                            pickedSection = nil
                        }
                        ForEach(piecesWithSections) { pws in
                            Section(pws.piece.title) {
                                Button(pws.piece.title) {
                                    pickedPiece = pws.piece
                                    pickedSection = nil
                                    blockTitle = pws.piece.title
                                }
                                ForEach(pws.sortedSections) { section in
                                    Button("↳ \(section.name)") {
                                        pickedPiece = pws.piece
                                        pickedSection = section
                                        blockTitle = "\(pws.piece.title) — \(section.name)"
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(blockPickerLabel)
                                .lineLimit(1)
                                .foregroundStyle(palette.inkPrimary)
                            Spacer()
                            Image(systemName: "chevron.up.chevron.down")
                                .foregroundStyle(palette.inkSecondary)
                        }
                        .padding(11)
                        .background(
                            RoundedRectangle(cornerRadius: 11, style: .continuous)
                                .fill(palette.surfaceVariant.opacity(0.65))
                        )
                    }

                    if pickedPiece == nil {
                        CadenceTextField(label: "Block Title", text: $blockTitle)
                    }

                    CadenceTextField(
                        label: "Target Minutes (Silent Progress Bar)",
                        text: $targetMinutesText,
                        keyboardType: .numberPad
                    )
                }
                .padding(16)
            }
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Add Block to Session Queue")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add to Queue") {
                        let minutes = Int(targetMinutesText) ?? 15
                        onAdd(pickedPiece, pickedSection, blockTitle, minutes)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private var blockPickerLabel: String {
        if let pickedPiece, let pickedSection {
            return "\(pickedPiece.title) — \(pickedSection.name)"
        }
        if let pickedPiece {
            return pickedPiece.title
        }
        return "Custom Block / Warmup"
    }
}

// MARK: - Save routine sheet

private struct SaveRoutineSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.cadencePalette) private var palette

    let initialName: String
    let onSave: (String, String) -> Void

    @State private var name = ""
    @State private var descriptionText = ""

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                CadenceTextField(label: "Routine Name", text: $name)
                CadenceTextField(label: "Description (optional)", text: $descriptionText, lineLimit: 2...4)
                Spacer()
            }
            .padding(16)
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Save Queue as Routine Template")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save Template") {
                        guard !name.trimmed.isEmpty else { return }
                        onSave(name, descriptionText)
                    }
                }
            }
        }
        .presentationDetents([.medium])
        .onAppear {
            if name.isEmpty { name = initialName }
        }
    }
}
