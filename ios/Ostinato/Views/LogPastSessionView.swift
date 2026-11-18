import SwiftUI

/// Value handed back to the Studio or Journal when the user retroactively
/// logs a session. Kept as a plain struct so sheets never mutate the store
/// while they are still on screen.
struct LogPastSessionPayload {
    let timestampEpochMillis: Int64
    let durationMinutes: Int
    let piece: RepertoirePiece?
    let section: PieceSection?
    let customTitle: String
    let instrument: String
    let category: PieceCategory
    let practicedBpm: Int
    let targetBpm: Int
    let measuresPracticed: String
    let focusTags: [String]
    let sessionNotes: String
    let nextSessionGoal: String
    let moodRating: Int
}

struct LogPastSessionView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.ostinatoPalette) private var palette

    let piecesWithSections: [PieceWithSections]
    let allInstruments: [String]
    let allFocusTags: [String]
    let onSave: (LogPastSessionPayload) -> Void

    @State private var selectedPieceId: Int64?
    @State private var selectedSectionId: Int64?
    @State private var customTitle = ""
    @State private var selectedDate = Date()
    @State private var durationText = "30"
    @State private var practicedBpm = 92
    @State private var targetBpm = 120
    @State private var instrument = "Piano"
    @State private var category: PieceCategory = .REPERTOIRE
    @State private var measures = ""
    @State private var selectedTags: Set<String> = ["Technique"]
    @State private var notes = ""
    @State private var nextGoal = ""
    @State private var moodRating = 4

    private var selectedPieceWithSections: PieceWithSections? {
        guard let selectedPieceId else { return nil }
        return piecesWithSections.first { $0.piece.id == selectedPieceId }
    }

    private var selectedPiece: RepertoirePiece? {
        selectedPieceWithSections?.piece
    }

    private var selectedSection: PieceSection? {
        guard let selectedPieceWithSections, let selectedSectionId else { return nil }
        return selectedPieceWithSections.sections.first { $0.id == selectedSectionId }
    }

    private var durationMinutes: Int {
        min(max(Int(durationText) ?? 30, 1), 600)
    }

    private var pieceMenuLabel: String {
        if let selectedPiece, let selectedSection {
            return "\(selectedPiece.title) — \(selectedSection.name)"
        }
        if let selectedPiece {
            return selectedPiece.title
        }
        return "Custom / General Practice"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    dateCard
                    pieceCard
                    durationCard
                    focusCard
                }
                .padding(16)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Log Past Session")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save Session") { save() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    // MARK: - Date & time

    private var dateCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                OstinatoSectionLabel(text: "When did you practice?")

                DatePicker(
                    "Session date & time",
                    selection: $selectedDate,
                    displayedComponents: [.date, .hourAndMinute]
                )
                .datePickerStyle(.compact)
                .labelsHidden()
                .frame(maxWidth: .infinity, alignment: .leading)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ChipButton(title: "Now", isSelected: false) {
                            selectedDate = Date()
                        }
                        ChipButton(title: "1h ago", isSelected: false) {
                            selectedDate = Date().addingTimeInterval(-3_600)
                        }
                        ChipButton(title: "Yesterday", isSelected: false) {
                            selectedDate = Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date()
                        }
                        ChipButton(title: "3 days ago", isSelected: false) {
                            selectedDate = Calendar.current.date(byAdding: .day, value: -3, to: Date()) ?? Date()
                        }
                    }
                }
            }
        }
    }

    // MARK: - Piece

    private var pieceCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                OstinatoSectionLabel(text: "Repertoire or exercise")

                Menu {
                    Button("Custom / General Practice") {
                        selectedPieceId = nil
                        selectedSectionId = nil
                    }
                    ForEach(piecesWithSections) { pws in
                        Section(pws.piece.title) {
                            Button("\(pws.piece.title) (Full piece)") {
                                selectPiece(pws.piece, section: nil)
                            }
                            ForEach(pws.sortedSections) { section in
                                Button("↳ \(section.name)") {
                                    selectPiece(pws.piece, section: section)
                                }
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text(pieceMenuLabel)
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
                            .fill(palette.surfaceVariant.opacity(0.65))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                            .stroke(palette.outline.opacity(0.6), lineWidth: 1)
                    )
                }

                if selectedPiece == nil {
                    OstinatoTextField(
                        label: "Custom Title",
                        placeholder: "e.g. Warmup & Scale Technique",
                        text: $customTitle
                    )
                }

                OstinatoSectionLabel(text: "Instrument")
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

                OstinatoSectionLabel(text: "Category")
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
            }
        }
    }

    // MARK: - Duration & tempo

    private var durationCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                OstinatoSectionLabel(text: "Duration")

                OstinatoTextField(
                    label: "Minutes practiced",
                    text: $durationText,
                    keyboardType: .numberPad
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach([10, 15, 20, 30, 45, 60, 90], id: \.self) { minutes in
                            ChipButton(
                                title: "\(minutes)m",
                                isSelected: durationMinutes == minutes
                            ) {
                                durationText = "\(minutes)"
                            }
                        }
                    }
                }

                Divider().overlay(palette.outline.opacity(0.4))

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("PRACTICED TEMPO")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(palette.inkSecondary)
                        Text("\(practicedBpm) BPM")
                            .font(.title3.weight(.bold))
                            .foregroundStyle(palette.inkPrimary)
                    }
                    Spacer()
                    HStack(spacing: 8) {
                        Button("-5") { practicedBpm = max(practicedBpm - 5, 20) }
                            .buttonStyle(.bordered)
                        Button("+5") { practicedBpm = min(practicedBpm + 5, 320) }
                            .buttonStyle(.bordered)
                    }
                }

                HStack {
                    Text("Target for this piece: \(targetBpm) BPM")
                        .font(.caption)
                        .foregroundStyle(palette.inkSecondary)
                    Spacer()
                    Stepper("", value: $targetBpm, in: 20...320, step: 2)
                        .labelsHidden()
                }
            }
        }
    }

    // MARK: - Focus & notes

    private var focusCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                OstinatoSectionLabel(text: "Measures / section")

                OstinatoTextField(
                    label: "Measures practiced (optional)",
                    placeholder: "e.g. m. 24–32, Cadenza",
                    text: $measures
                )

                OstinatoSectionLabel(text: "Focus tags")
                FlowLayout(spacing: 8) {
                    ForEach(allFocusTags, id: \.self) { tag in
                        ChipButton(
                            title: tag,
                            isSelected: selectedTags.contains(tag)
                        ) {
                            if selectedTags.contains(tag) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.insert(tag)
                            }
                        }
                    }
                }

                OstinatoTextField(
                    label: "Session notes",
                    placeholder: "What did you work on?",
                    text: $notes,
                    lineLimit: 3...6
                )

                OstinatoTextField(
                    label: "Goal for next session",
                    placeholder: "e.g. Connect m. 32–40 at 84 BPM",
                    text: $nextGoal
                )

                HStack {
                    Text("Session quality")
                        .font(.caption)
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    StarRatingView(rating: moodRating, size: 22) { rating in
                        moodRating = rating
                    }
                }

                Button {
                    save()
                } label: {
                    Label("Save Past Session (\(durationMinutes)m @ \(practicedBpm) BPM)", systemImage: "checkmark.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
                .controlSize(.large)
            }
        }
    }

    // MARK: - Actions

    private func selectPiece(_ piece: RepertoirePiece, section: PieceSection?) {
        selectedPieceId = piece.id
        selectedSectionId = section?.id
        customTitle = piece.title
        instrument = piece.instrument
        category = piece.pieceCategory
        practicedBpm = section?.lastPracticedBpm ?? piece.lastPracticedBpm
        targetBpm = section?.targetBpm ?? piece.targetBpm
    }

    private func save() {
        let payload = LogPastSessionPayload(
            timestampEpochMillis: AnalyticsCalculator.epochMillis(from: selectedDate),
            durationMinutes: durationMinutes,
            piece: selectedPiece,
            section: selectedSection,
            customTitle: customTitle.trimmed,
            instrument: instrument,
            category: category,
            practicedBpm: min(max(practicedBpm, 20), 320),
            targetBpm: min(max(targetBpm, 20), 320),
            measuresPracticed: measures.trimmed,
            focusTags: Array(selectedTags),
            sessionNotes: notes.trimmed,
            nextSessionGoal: nextGoal.trimmed,
            moodRating: min(max(moodRating, 1), 5)
        )
        onSave(payload)
        dismiss()
    }
}
