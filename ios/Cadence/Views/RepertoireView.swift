import SwiftUI

/// Repertoire library: filter by category/status, cycle piece status, manage
/// movements, edit metadata and jump straight into a practice session.
struct RepertoireView: View {
    @Environment(CadenceStore.self) private var store
    @Environment(\.cadencePalette) private var palette

    let piecesWithSections: [PieceWithSections]
    let allInstruments: [String]
    let onPracticeNow: (RepertoirePiece, PieceSection?) -> Void

    @State private var selectedCategory: PieceCategory?
    @State private var selectedStatus: PieceStatus?
    @State private var showAddPiece = false
    @State private var editingPiece: RepertoirePiece?
    @State private var addMovementToPiece: RepertoirePiece?
    @State private var pendingDeletePiece: RepertoirePiece?
    @State private var pendingDeleteSection: PieceSection?

    private var filteredPieces: [PieceWithSections] {
        piecesWithSections.filter { pws in
            if let selectedCategory, pws.piece.pieceCategory != selectedCategory {
                return false
            }
            if let selectedStatus, pws.piece.pieceStatus != selectedStatus {
                return false
            }
            return true
        }
    }

    private var pieceDeleteBinding: Binding<Bool> {
        Binding(
            get: { pendingDeletePiece != nil },
            set: { if !$0 { pendingDeletePiece = nil } }
        )
    }

    private var sectionDeleteBinding: Binding<Bool> {
        Binding(
            get: { pendingDeleteSection != nil },
            set: { if !$0 { pendingDeleteSection = nil } }
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                summaryCard
                categoryFilters
                statusFilters

                if filteredPieces.isEmpty {
                    emptyState
                } else {
                    ForEach(filteredPieces) { pws in
                        pieceCard(pws)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(palette.background.ignoresSafeArea())
        .sheet(isPresented: $showAddPiece) {
            PieceEditorSheet(piece: nil, allInstruments: allInstruments)
        }
        .sheet(item: $editingPiece) { piece in
            PieceEditorSheet(piece: piece, allInstruments: allInstruments)
        }
        .sheet(item: $addMovementToPiece) { piece in
            MovementEditorSheet(piece: piece)
        }
        .alert(
            "Delete this piece?",
            isPresented: pieceDeleteBinding,
            presenting: pendingDeletePiece
        ) { piece in
            Button("Delete", role: .destructive) {
                store.deletePiece(piece)
                pendingDeletePiece = nil
            }
            Button("Cancel", role: .cancel) {
                pendingDeletePiece = nil
            }
        } message: { piece in
            Text("\"\(piece.title)\" and its movements will be deleted. Existing journal sessions are kept.")
        }
        .confirmationDialog(
            "Delete this movement?",
            isPresented: sectionDeleteBinding,
            titleVisibility: .visible,
            presenting: pendingDeleteSection
        ) { section in
            Button("Delete", role: .destructive) {
                store.deleteSection(section)
                pendingDeleteSection = nil
            }
            Button("Cancel", role: .cancel) {
                pendingDeleteSection = nil
            }
        } message: { section in
            Text("\"\(section.name)\" will be removed from this piece.")
        }
    }

    // MARK: - Summary

    private var summaryCard: some View {
        CadenceCard(padding: 16, cornerRadius: 18) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("REPERTOIRE LIBRARY")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.primary)
                    Text("\(filteredPieces.count) active piece\(filteredPieces.count == 1 ? "" : "s")")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(palette.inkPrimary)
                    Text("\(filteredPieces.reduce(0) { $0 + $1.sections.count }) movement\(filteredPieces.reduce(0) { $0 + $1.sections.count } == 1 ? "" : "s") tracked")
                        .font(.caption)
                        .foregroundStyle(palette.inkSecondary)
                }
                Spacer()
                Button {
                    showAddPiece = true
                } label: {
                    Label("Add Piece", systemImage: "plus.circle.fill")
                        .font(.footnote.weight(.semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
            }
        }
    }

    // MARK: - Filters

    private var categoryFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ChipButton(title: "All Categories", isSelected: selectedCategory == nil) {
                    selectedCategory = nil
                }
                ForEach(PieceCategory.allCases) { category in
                    ChipButton(
                        title: category.shortLabel,
                        isSelected: selectedCategory == category
                    ) {
                        selectedCategory = category
                    }
                }
            }
        }
    }

    private var statusFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ChipButton(title: "All Statuses", isSelected: selectedStatus == nil) {
                    selectedStatus = nil
                }
                ForEach(PieceStatus.allCases) { status in
                    ChipButton(
                        title: status.label,
                        isSelected: selectedStatus == status
                    ) {
                        selectedStatus = status
                    }
                }
            }
        }
    }

    // MARK: - Piece card

    private func pieceCard(_ pws: PieceWithSections) -> some View {
        let piece = pws.piece

        return CadenceCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(piece.title)
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        if !piece.composer.isEmpty {
                            Text(piece.composer)
                                .font(.subheadline)
                                .foregroundStyle(palette.inkSecondary)
                        }
                        Text("\(piece.instrument) • \(piece.pieceCategory.shortLabel)\(piece.keySignature.isEmpty ? "" : " • \(piece.keySignature)") • \(piece.timeSignatureLabel)")
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                    }
                    Spacer(minLength: 4)
                    Button {
                        piece.statusRaw = piece.pieceStatus.next.rawValue
                        store.updatePiece(piece)
                    } label: {
                        CadencePill(
                            text: piece.pieceStatus.label,
                            foreground: statusForeground(piece.pieceStatus),
                            background: statusBackground(piece.pieceStatus)
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Cycle piece status")
                }

                VStack(spacing: 4) {
                    HStack {
                        Text("Best \(piece.bestBpm) • Last \(piece.lastPracticedBpm) • Target \(piece.targetBpm) BPM")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                        Spacer()
                        Text("\(Int(piece.tempoMasteryProgress * 100))%")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(palette.primary)
                    }
                    CadenceProgressBar(
                        value: piece.tempoMasteryProgress,
                        color: piece.tempoMasteryProgress >= 1 ? palette.tertiary : palette.primary,
                        height: 8
                    )
                }

                HStack(spacing: 10) {
                    Label("\(piece.totalMinutesPracticed) min total", systemImage: "clock")
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                    if let last = piece.lastPracticedAtEpochMillis {
                        Label(
                            Formatters.reportDate.string(from: AnalyticsCalculator.date(from: last)),
                            systemImage: "calendar"
                        )
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                    } else {
                        Text("Never practiced")
                            .font(.caption2)
                            .foregroundStyle(palette.secondary)
                    }
                }

                if !piece.practiceNotes.isEmpty {
                    Text(piece.practiceNotes)
                        .font(.caption)
                        .foregroundStyle(palette.inkPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if !piece.nextSessionGoal.isEmpty {
                    HStack(alignment: .top, spacing: 6) {
                        Image(systemName: "flag.fill")
                            .font(.caption2)
                            .foregroundStyle(palette.primary)
                        Text("Next: \(piece.nextSessionGoal)")
                            .font(.caption)
                            .foregroundStyle(palette.inkPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(9)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(palette.primaryContainer.opacity(0.45))
                    )
                }

                if !pws.sortedSections.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        CadenceSectionLabel(text: "Movements")
                        ForEach(pws.sortedSections) { section in
                            movementRow(section, piece: piece)
                        }
                    }
                }

                HStack(spacing: 8) {
                    Button {
                        onPracticeNow(piece, nil)
                    } label: {
                        Label("Practice", systemImage: "play.fill")
                            .font(.footnote.weight(.semibold))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)

                    Button {
                        addMovementToPiece = piece
                    } label: {
                        Label("Movement", systemImage: "plus")
                            .font(.footnote.weight(.semibold))
                    }
                    .buttonStyle(.bordered)

                    Button {
                        editingPiece = piece
                    } label: {
                        Label("Edit", systemImage: "pencil")
                            .font(.footnote.weight(.semibold))
                    }
                    .buttonStyle(.bordered)

                    Spacer()

                    Button {
                        pendingDeletePiece = piece
                    } label: {
                        Image(systemName: "trash")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(palette.secondary)
                    }
                    .buttonStyle(.bordered)
                    .tint(palette.secondary)
                    .accessibilityLabel("Delete piece")
                }
            }
        }
    }

    private func movementRow(_ section: PieceSection, piece: RepertoirePiece) -> some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(section.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(palette.inkPrimary)
                Text("\(section.timeSignatureLabel) • Best \(section.bestBpm) / Target \(section.targetBpm) BPM • \(section.totalMinutesPracticed) min")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
            Spacer(minLength: 4)
            Button {
                onPracticeNow(piece, section)
            } label: {
                Image(systemName: "play.circle.fill")
                    .font(.title3)
                    .foregroundStyle(palette.primary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Practice \(section.name)")

            Button {
                pendingDeleteSection = section
            } label: {
                Image(systemName: "minus.circle")
                    .font(.title3)
                    .foregroundStyle(palette.inkSecondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Delete \(section.name)")
        }
        .padding(9)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(palette.surfaceVariant.opacity(0.55))
        )
    }

    // MARK: - Empty state

    private var emptyState: some View {
        CadenceCard(padding: 22, cornerRadius: 18) {
            VStack(spacing: 10) {
                Image(systemName: "music.note.list")
                    .font(.system(size: 34))
                    .foregroundStyle(palette.primary)
                Text(piecesWithSections.isEmpty ? "Your repertoire is empty" : "No pieces match your filters")
                    .font(.headline)
                    .foregroundStyle(palette.inkPrimary)
                Text(
                    piecesWithSections.isEmpty
                        ? "Add the pieces, études and technical exercises you are working on."
                        : "Try a different category or status filter."
                )
                .font(.subheadline)
                .foregroundStyle(palette.inkSecondary)
                .multilineTextAlignment(.center)
                Button {
                    showAddPiece = true
                } label: {
                    Label("Add Your First Piece", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Status colors

    private func statusForeground(_ status: PieceStatus) -> Color {
        switch status {
        case .LEARNING: return palette.onSecondaryContainer
        case .POLISHING: return palette.onPrimaryContainer
        case .MASTERED: return palette.onTertiaryContainer
        }
    }

    private func statusBackground(_ status: PieceStatus) -> Color {
        switch status {
        case .LEARNING: return palette.secondaryContainer
        case .POLISHING: return palette.primaryContainer
        case .MASTERED: return palette.tertiaryContainer
        }
    }
}

// MARK: - Piece editor

private struct PieceEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(CadenceStore.self) private var store
    @Environment(\.cadencePalette) private var palette

    let piece: RepertoirePiece?
    let allInstruments: [String]

    @State private var title: String
    @State private var composer: String
    @State private var instrument: String
    @State private var category: PieceCategory
    @State private var keySignature: String
    @State private var beatsText: String
    @State private var noteValueText: String
    @State private var initialBpmText: String
    @State private var targetBpmText: String
    @State private var status: PieceStatus
    @State private var practiceNotes: String
    @State private var nextGoal: String

    init(piece: RepertoirePiece?, allInstruments: [String]) {
        self.piece = piece
        self.allInstruments = allInstruments
        _title = State(initialValue: piece?.title ?? "")
        _composer = State(initialValue: piece?.composer ?? "")
        _instrument = State(initialValue: piece?.instrument ?? (allInstruments.first ?? "Piano"))
        _category = State(initialValue: piece?.pieceCategory ?? .REPERTOIRE)
        _keySignature = State(initialValue: piece?.keySignature ?? "")
        _beatsText = State(initialValue: "\(piece?.timeSignatureBeats ?? 4)")
        _noteValueText = State(initialValue: "\(piece?.timeSignatureNoteValue ?? 4)")
        _initialBpmText = State(initialValue: "\(piece?.lastPracticedBpm ?? 72)")
        _targetBpmText = State(initialValue: "\(piece?.targetBpm ?? 120)")
        _status = State(initialValue: piece?.pieceStatus ?? .LEARNING)
        _practiceNotes = State(initialValue: piece?.practiceNotes ?? "")
        _nextGoal = State(initialValue: piece?.nextSessionGoal ?? "")
    }

    private var isEditing: Bool { piece != nil }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    CadenceTextField(
                        label: "Title",
                        placeholder: "e.g. Clair de Lune",
                        text: $title
                    )

                    CadenceTextField(
                        label: "Composer / source",
                        placeholder: "e.g. Claude Debussy",
                        text: $composer
                    )

                    CadenceSectionLabel(text: "Instrument")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(allInstruments, id: \.self) { option in
                                ChipButton(
                                    title: option,
                                    isSelected: instrument.caseInsensitiveCompare(option) == .orderedSame
                                ) {
                                    instrument = option
                                }
                            }
                        }
                    }

                    CadenceSectionLabel(text: "Category")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(PieceCategory.allCases) { option in
                                ChipButton(
                                    title: option.shortLabel,
                                    isSelected: category == option
                                ) {
                                    category = option
                                }
                            }
                        }
                    }

                    CadenceTextField(
                        label: "Key signature",
                        placeholder: "e.g. Db Major",
                        text: $keySignature
                    )

                    CadenceSectionLabel(text: "Time signature")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(meterPresets) { preset in
                                ChipButton(
                                    title: preset.label,
                                    isSelected: beatsText == "\(preset.beats)" && noteValueText == "\(preset.noteValue)"
                                ) {
                                    beatsText = "\(preset.beats)"
                                    noteValueText = "\(preset.noteValue)"
                                }
                            }
                        }
                    }

                    HStack(spacing: 10) {
                        CadenceTextField(
                            label: "Beats",
                            text: $beatsText,
                            keyboardType: .numberPad
                        )
                        CadenceTextField(
                            label: "Note value",
                            text: $noteValueText,
                            keyboardType: .numberPad
                        )
                    }

                    if isEditing {
                        if let piece {
                            HStack {
                                Text("Best \(piece.bestBpm) BPM • Last \(piece.lastPracticedBpm) BPM")
                                    .font(.caption)
                                    .foregroundStyle(palette.inkSecondary)
                                Spacer()
                            }
                        }
                    } else {
                        CadenceTextField(
                            label: "Starting BPM",
                            text: $initialBpmText,
                            keyboardType: .numberPad
                        )
                    }

                    CadenceTextField(
                        label: "Target BPM",
                        text: $targetBpmText,
                        keyboardType: .numberPad
                    )

                    CadenceSectionLabel(text: "Status")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(PieceStatus.allCases) { option in
                                ChipButton(
                                    title: option.label,
                                    isSelected: status == option
                                ) {
                                    status = option
                                }
                            }
                        }
                    }

                    CadenceTextField(
                        label: "Standing score / practice notes",
                        placeholder: "Fingerings, bowings, interpretive cues…",
                        text: $practiceNotes,
                        lineLimit: 3...6
                    )

                    CadenceTextField(
                        label: "Next session goal",
                        placeholder: "What should future-you focus on?",
                        text: $nextGoal,
                        lineLimit: 2...4
                    )

                    Button {
                        save()
                    } label: {
                        Label(isEditing ? "Save Piece" : "Add Piece", systemImage: "checkmark.circle.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)
                    .controlSize(.large)
                }
                .padding(16)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(palette.background.ignoresSafeArea())
            .navigationTitle(isEditing ? "Edit Piece" : "Add Piece")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private var meterPresets: [MeterPreset] {
        [
            MeterPreset(beats: 2, noteValue: 4, label: "2/4"),
            MeterPreset(beats: 3, noteValue: 4, label: "3/4"),
            MeterPreset(beats: 4, noteValue: 4, label: "4/4"),
            MeterPreset(beats: 5, noteValue: 4, label: "5/4"),
            MeterPreset(beats: 6, noteValue: 8, label: "6/8"),
            MeterPreset(beats: 9, noteValue: 8, label: "9/8"),
            MeterPreset(beats: 12, noteValue: 8, label: "12/8")
        ]
    }

    private func save() {
        let cleanTitle = title.trimmed
        guard !cleanTitle.isEmpty else { return }

        let beats = min(max(Int(beatsText) ?? 4, 1), 16)
        let noteValue = [2, 4, 8, 16].contains(Int(noteValueText) ?? 4) ? (Int(noteValueText) ?? 4) : 4
        let target = min(max(Int(targetBpmText) ?? 120, 20), 320)

        if let piece {
            piece.title = cleanTitle
            piece.composer = composer.trimmed
            piece.instrument = instrument.trimmed.isEmpty ? piece.instrument : instrument.trimmed
            piece.categoryRaw = category.rawValue
            piece.keySignature = keySignature.trimmed
            piece.timeSignatureBeats = beats
            piece.timeSignatureNoteValue = noteValue
            piece.targetBpm = target
            piece.statusRaw = status.rawValue
            piece.practiceNotes = practiceNotes.trimmed
            piece.nextSessionGoal = nextGoal.trimmed
            store.updatePiece(piece)
        } else {
            store.addPiece(
                title: cleanTitle,
                composer: composer,
                instrument: instrument,
                category: category,
                keySignature: keySignature,
                timeSignatureBeats: beats,
                timeSignatureNoteValue: noteValue,
                initialBpm: Int(initialBpmText) ?? 72,
                targetBpm: target,
                status: status,
                practiceNotes: practiceNotes,
                nextSessionGoal: nextGoal
            )
        }
        dismiss()
    }
}

// MARK: - Movement editor

private struct MovementEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(CadenceStore.self) private var store
    @Environment(\.cadencePalette) private var palette

    let piece: RepertoirePiece

    @State private var name = ""
    @State private var initialBpmText: String
    @State private var targetBpmText: String
    @State private var beatsText: String
    @State private var noteValueText: String
    @State private var sectionNotes = ""
    @State private var nextGoal = ""

    init(piece: RepertoirePiece) {
        self.piece = piece
        _initialBpmText = State(initialValue: "\(piece.lastPracticedBpm)")
        _targetBpmText = State(initialValue: "\(piece.targetBpm)")
        _beatsText = State(initialValue: "\(piece.timeSignatureBeats)")
        _noteValueText = State(initialValue: "\(piece.timeSignatureNoteValue)")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    CadenceCard(padding: 14, cornerRadius: 16, fill: palette.surfaceVariant.opacity(0.5)) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(piece.title)
                                .font(.headline)
                                .foregroundStyle(palette.inkPrimary)
                            Text("Add a movement, section or passage to track separately.")
                                .font(.caption)
                                .foregroundStyle(palette.inkSecondary)
                        }
                    }

                    CadenceTextField(
                        label: "Movement / section name",
                        placeholder: "e.g. I. Allemande or m. 24–48",
                        text: $name
                    )

                    CadenceSectionLabel(text: "Time signature")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(meterPresets) { preset in
                                ChipButton(
                                    title: preset.label,
                                    isSelected: beatsText == "\(preset.beats)" && noteValueText == "\(preset.noteValue)"
                                ) {
                                    beatsText = "\(preset.beats)"
                                    noteValueText = "\(preset.noteValue)"
                                }
                            }
                        }
                    }

                    HStack(spacing: 10) {
                        CadenceTextField(
                            label: "Beats",
                            text: $beatsText,
                            keyboardType: .numberPad
                        )
                        CadenceTextField(
                            label: "Note value",
                            text: $noteValueText,
                            keyboardType: .numberPad
                        )
                    }

                    HStack(spacing: 10) {
                        CadenceTextField(
                            label: "Starting BPM",
                            text: $initialBpmText,
                            keyboardType: .numberPad
                        )
                        CadenceTextField(
                            label: "Target BPM",
                            text: $targetBpmText,
                            keyboardType: .numberPad
                        )
                    }

                    CadenceTextField(
                        label: "Section notes",
                        text: $sectionNotes,
                        lineLimit: 2...5
                    )

                    CadenceTextField(
                        label: "Next session goal",
                        text: $nextGoal,
                        lineLimit: 2...4
                    )

                    Button {
                        save()
                    } label: {
                        Label("Add Movement", systemImage: "checkmark.circle.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)
                    .controlSize(.large)
                }
                .padding(16)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Add Movement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { save() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private var meterPresets: [MeterPreset] {
        [
            MeterPreset(beats: 2, noteValue: 4, label: "2/4"),
            MeterPreset(beats: 3, noteValue: 4, label: "3/4"),
            MeterPreset(beats: 4, noteValue: 4, label: "4/4"),
            MeterPreset(beats: 5, noteValue: 4, label: "5/4"),
            MeterPreset(beats: 6, noteValue: 8, label: "6/8"),
            MeterPreset(beats: 9, noteValue: 8, label: "9/8"),
            MeterPreset(beats: 12, noteValue: 8, label: "12/8")
        ]
    }

    private func save() {
        let cleanName = name.trimmed
        guard !cleanName.isEmpty else { return }

        let beats = min(max(Int(beatsText) ?? 4, 1), 16)
        let noteValue = [2, 4, 8, 16].contains(Int(noteValueText) ?? 4) ? (Int(noteValueText) ?? 4) : 4
        let existingCount = store.sections.filter { $0.pieceId == piece.id }.count

        store.addSection(
            pieceId: piece.id,
            name: cleanName,
            orderIndex: existingCount,
            initialBpm: Int(initialBpmText) ?? piece.lastPracticedBpm,
            targetBpm: Int(targetBpmText) ?? piece.targetBpm,
            timeSignatureBeats: beats,
            timeSignatureNoteValue: noteValue,
            sectionNotes: sectionNotes,
            nextSessionGoal: nextGoal
        )
        dismiss()
    }
}
