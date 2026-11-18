import SwiftUI

/// Chronological practice log with search, category/instrument filters,
/// inline editing, deletion and retroactive logging.
struct JournalView: View {
    @Environment(StudioViewModel.self) private var studio
    @Environment(\.ostinatoPalette) private var palette

    let sessions: [PracticeSession]
    let piecesWithSections: [PieceWithSections]
    let allInstruments: [String]
    let allFocusTags: [String]

    @State private var searchText = ""
    @State private var selectedCategory: PieceCategory?
    @State private var selectedInstrument = "All"
    @State private var editingSession: PracticeSession?
    @State private var pendingDelete: PracticeSession?
    @State private var showLogPast = false

    private var filteredSessions: [PracticeSession] {
        let query = searchText.trimmed.lowercased()
        return sessions.filter { session in
            if let selectedCategory, session.pieceCategory != selectedCategory {
                return false
            }
            if selectedInstrument != "All",
               session.instrument.caseInsensitiveCompare(selectedInstrument) != .orderedSame {
                return false
            }
            guard !query.isEmpty else { return true }
            let haystack = [
                session.pieceTitle,
                session.sectionName,
                session.composer,
                session.instrument,
                session.measuresPracticed,
                session.sessionNotes,
                session.focusTags,
                session.nextSessionGoal,
                session.routineName
            ]
            .joined(separator: " ")
            .lowercased()
            return haystack.contains(query)
        }
    }

    private var filteredMinutes: Int {
        filteredSessions.reduce(0) { $0 + $1.durationMinutes }
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { pendingDelete != nil },
            set: { newValue in
                if !newValue { pendingDelete = nil }
            }
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                summaryCard
                searchField
                filterChips

                if filteredSessions.isEmpty {
                    emptyState
                } else {
                    ForEach(filteredSessions) { session in
                        sessionCard(session)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(palette.background.ignoresSafeArea())
        .sheet(item: $editingSession) { session in
            EditSessionSheet(session: session, allFocusTags: allFocusTags)
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
        .alert(
            "Delete this session?",
            isPresented: deleteBinding,
            presenting: pendingDelete
        ) { session in
            Button("Delete", role: .destructive) {
                studio.deleteSession(session)
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) {
                pendingDelete = nil
            }
        } message: { session in
            Text("\"\(session.pieceTitle)\" will be removed from your journal. Piece totals are not recalculated.")
        }
    }

    // MARK: - Summary

    private var summaryCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("SESSION JOURNAL")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.primary)
                    Text("\(filteredSessions.count) session\(filteredSessions.count == 1 ? "" : "s")")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(palette.inkPrimary)
                    Text("\(Formatters.hoursAndMinutes(filteredMinutes)) of documented practice")
                        .font(.caption)
                        .foregroundStyle(palette.inkSecondary)
                }
                Spacer()
                Button {
                    showLogPast = true
                } label: {
                    Label("Log Past", systemImage: "plus.circle.fill")
                        .font(.footnote.weight(.semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
            }
        }
    }

    // MARK: - Search & filters

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(palette.inkSecondary)
            TextField("Search pieces, notes, measures, tags…", text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .foregroundStyle(palette.inkPrimary)
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(palette.inkSecondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(11)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(palette.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(palette.outline.opacity(0.55), lineWidth: 1)
        )
    }

    private var filterChips: some View {
        VStack(alignment: .leading, spacing: 8) {
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

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ChipButton(title: "All Instruments", isSelected: selectedInstrument == "All") {
                        selectedInstrument = "All"
                    }
                    ForEach(allInstruments, id: \.self) { instrument in
                        ChipButton(
                            title: instrument,
                            isSelected: selectedInstrument.caseInsensitiveCompare(instrument) == .orderedSame
                        ) {
                            selectedInstrument = instrument
                        }
                    }
                }
            }
        }
    }

    // MARK: - Session card

    private func sessionCard(_ session: PracticeSession) -> some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 9) {
                HStack(alignment: .top, spacing: 10) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(Formatters.journalDate.string(from: AnalyticsCalculator.date(from: session.timestampEpochMillis)))
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(palette.inkSecondary)
                        Text(session.pieceTitle)
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        if !session.sectionName.isEmpty {
                            Text(session.sectionName)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(palette.secondary)
                        }
                        if !session.composer.isEmpty {
                            Text(session.composer)
                                .font(.caption)
                                .foregroundStyle(palette.inkSecondary)
                        }
                    }
                    Spacer(minLength: 4)
                    VStack(alignment: .trailing, spacing: 6) {
                        StarRatingView(rating: session.moodRating, size: 13)
                        Text("\(session.durationMinutes)m")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(palette.primary)
                    }
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        OstinatoPill(
                            text: session.pieceCategory.shortLabel,
                            foreground: palette.onPrimaryContainer,
                            background: palette.primaryContainer
                        )
                        OstinatoPill(
                            text: session.instrument,
                            foreground: palette.onSecondaryContainer,
                            background: palette.secondaryContainer
                        )
                        OstinatoPill(
                            text: "\(session.practicedBpm) / \(session.targetBpmSnapshot) BPM",
                            foreground: palette.onTertiaryContainer,
                            background: palette.tertiaryContainer
                        )
                        if !session.routineName.isEmpty {
                            OstinatoPill(
                                text: session.routineName,
                                foreground: palette.inkSecondary,
                                background: palette.surfaceVariant
                            )
                        }
                    }
                }

                if !session.measuresPracticed.isEmpty {
                    Text("Measures: \(session.measuresPracticed)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(palette.inkPrimary)
                }

                if !session.sessionNotes.isEmpty {
                    Text(session.sessionNotes)
                        .font(.subheadline)
                        .foregroundStyle(palette.inkPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if !session.focusTagList.isEmpty {
                    FlowLayout(spacing: 6) {
                        ForEach(session.focusTagList, id: \.self) { tag in
                            OstinatoPill(
                                text: tag,
                                foreground: palette.inkSecondary,
                                background: palette.surfaceVariant.opacity(0.8)
                            )
                        }
                    }
                }

                if !session.nextSessionGoal.isEmpty {
                    HStack(alignment: .top, spacing: 6) {
                        Image(systemName: "flag.fill")
                            .font(.caption2)
                            .foregroundStyle(palette.primary)
                        Text("Next: \(session.nextSessionGoal)")
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

                HStack(spacing: 8) {
                    Button {
                        editingSession = session
                    } label: {
                        Label("Edit", systemImage: "pencil")
                            .font(.footnote.weight(.semibold))
                    }
                    .buttonStyle(.bordered)

                    Button {
                        pendingDelete = session
                    } label: {
                        Label("Delete", systemImage: "trash")
                            .font(.footnote.weight(.semibold))
                    }
                    .buttonStyle(.bordered)
                    .tint(palette.secondary)

                    Spacer()
                }
            }
        }
    }

    // MARK: - Empty state

    private var emptyState: some View {
        OstinatoCard(padding: 22, cornerRadius: 18) {
            VStack(spacing: 10) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 34))
                    .foregroundStyle(palette.primary)
                Text(sessions.isEmpty ? "No practice sessions yet" : "No sessions match your filters")
                    .font(.headline)
                    .foregroundStyle(palette.inkPrimary)
                Text(
                    sessions.isEmpty
                        ? "Run the Studio stopwatch or log a past session to start building your practice history."
                        : "Try clearing the search text or choosing a different category or instrument."
                )
                .font(.subheadline)
                .foregroundStyle(palette.inkSecondary)
                .multilineTextAlignment(.center)
                Button {
                    showLogPast = true
                } label: {
                    Label("Log Past Session", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

// MARK: - Edit sheet

private struct EditSessionSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(StudioViewModel.self) private var studio
    @Environment(\.ostinatoPalette) private var palette

    let session: PracticeSession
    let allFocusTags: [String]

    @State private var notes: String
    @State private var measures: String
    @State private var nextGoal: String
    @State private var bpm: Int
    @State private var selectedTags: Set<String>

    init(session: PracticeSession, allFocusTags: [String]) {
        self.session = session
        self.allFocusTags = allFocusTags
        _notes = State(initialValue: session.sessionNotes)
        _measures = State(initialValue: session.measuresPracticed)
        _nextGoal = State(initialValue: session.nextSessionGoal)
        _bpm = State(initialValue: session.practicedBpm)
        _selectedTags = State(initialValue: Set(session.focusTagList))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    OstinatoCard(padding: 14, cornerRadius: 16, fill: palette.surfaceVariant.opacity(0.5)) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(session.pieceTitle)
                                .font(.headline)
                                .foregroundStyle(palette.inkPrimary)
                            Text(Formatters.journalDate.string(from: AnalyticsCalculator.date(from: session.timestampEpochMillis)))
                                .font(.caption)
                                .foregroundStyle(palette.inkSecondary)
                            Text("\(session.durationMinutes) min • \(session.instrument)")
                                .font(.caption)
                                .foregroundStyle(palette.inkSecondary)
                        }
                    }

                    OstinatoTextField(
                        label: "Measures / section",
                        text: $measures
                    )

                    OstinatoTextField(
                        label: "Session notes",
                        text: $notes,
                        lineLimit: 4...8
                    )

                    OstinatoTextField(
                        label: "Goal for next session",
                        text: $nextGoal
                    )

                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("PRACTICED TEMPO")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(palette.inkSecondary)
                            Text("\(bpm) BPM")
                                .font(.title3.weight(.bold))
                                .foregroundStyle(palette.inkPrimary)
                        }
                        Spacer()
                        HStack(spacing: 8) {
                            Button("-5") { bpm = max(bpm - 5, 20) }
                                .buttonStyle(.bordered)
                            Button("+5") { bpm = min(bpm + 5, 320) }
                                .buttonStyle(.bordered)
                        }
                    }

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

                    Button {
                        save()
                    } label: {
                        Label("Save Changes", systemImage: "checkmark.circle.fill")
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
            .navigationTitle("Edit Session")
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

    private func save() {
        studio.updateExistingSession(
            session,
            notes: notes,
            measures: measures,
            nextGoal: nextGoal,
            tags: selectedTags.sorted().joined(separator: ", "),
            bpm: bpm
        )
        dismiss()
    }
}
