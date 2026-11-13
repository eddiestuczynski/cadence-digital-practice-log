import Foundation
import Observation
import SwiftData

/// A small draft type used when saving routine templates, so the UI never has
/// to hand managed `RoutineTemplateItem` objects back to the store.
struct RoutineItemDraft {
    var pieceId: Int64?
    var sectionId: Int64?
    var customTitle: String
    var category: PieceCategory
    var targetMinutes: Int
    var suggestedBpm: Int
    var notesPrompt: String

    init(
        pieceId: Int64? = nil,
        sectionId: Int64? = nil,
        customTitle: String,
        category: PieceCategory = .TECHNIQUE,
        targetMinutes: Int = 10,
        suggestedBpm: Int = 92,
        notesPrompt: String = ""
    ) {
        self.pieceId = pieceId
        self.sectionId = sectionId
        self.customTitle = customTitle
        self.category = category
        self.targetMinutes = targetMinutes
        self.suggestedBpm = suggestedBpm
        self.notesPrompt = notesPrompt
    }
}

/// Owns the SwiftData stack and mirrors the Android `PracticeRepository`.
/// All ids and foreign keys stay scalar, exactly like the Room schema, so the
/// JSON backup format remains interchangeable between Android and iOS.
@MainActor
@Observable
final class CadenceStore {

    private let context: ModelContext
    private let defaults: UserDefaults

    private(set) var pieces: [RepertoirePiece] = []
    private(set) var sections: [PieceSection] = []
    private(set) var sessions: [PracticeSession] = []
    private(set) var routines: [RoutineTemplate] = []
    private(set) var routineItems: [RoutineTemplateItem] = []

    init(context: ModelContext, defaults: UserDefaults = .standard) {
        self.context = context
        self.defaults = defaults
        refresh()
    }

    // MARK: - Fetching

    func refresh() {
        pieces = fetchAll(RepertoirePiece.self).sorted { lhs, rhs in
            let left = Self.statusRank(lhs.pieceStatus)
            let right = Self.statusRank(rhs.pieceStatus)
            if left != right { return left < right }
            return lhs.title.localizedCaseInsensitiveCompare(rhs.title) == .orderedAscending
        }

        sections = fetchAll(PieceSection.self).sorted { lhs, rhs in
            if lhs.pieceId != rhs.pieceId { return lhs.pieceId < rhs.pieceId }
            if lhs.orderIndex != rhs.orderIndex { return lhs.orderIndex < rhs.orderIndex }
            return lhs.id < rhs.id
        }

        sessions = fetchAll(PracticeSession.self).sorted { $0.timestampEpochMillis > $1.timestampEpochMillis }

        routines = fetchAll(RoutineTemplate.self).sorted { $0.createdAtEpochMillis > $1.createdAtEpochMillis }

        routineItems = fetchAll(RoutineTemplateItem.self).sorted { lhs, rhs in
            if lhs.routineId != rhs.routineId { return lhs.routineId < rhs.routineId }
            if lhs.orderIndex != rhs.orderIndex { return lhs.orderIndex < rhs.orderIndex }
            return lhs.id < rhs.id
        }
    }

    var piecesWithSections: [PieceWithSections] {
        let grouped = Dictionary(grouping: sections, by: \.pieceId)
        return pieces.map { PieceWithSections(piece: $0, sections: grouped[$0.id] ?? []) }
    }

    var routinesWithItems: [RoutineWithItems] {
        let grouped = Dictionary(grouping: routineItems, by: \.routineId)
        return routines.map { RoutineWithItems(routine: $0, items: grouped[$0.id] ?? []) }
    }

    private func fetchAll<T: PersistentModel>(_ type: T.Type) -> [T] {
        (try? context.fetch(FetchDescriptor<T>())) ?? []
    }

    private static func statusRank(_ status: PieceStatus) -> Int {
        switch status {
        case .LEARNING: return 1
        case .POLISHING: return 2
        case .MASTERED: return 3
        }
    }

    // MARK: - Repertoire pieces

    @discardableResult
    func addPiece(
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
    ) -> Int64 {
        let clampedInitial = min(max(initialBpm, 20), 320)
        let piece = RepertoirePiece(
            id: nextId(.piece, existing: pieces.map(\.id)),
            title: title.trimmed,
            composer: composer.trimmed,
            instrument: instrument.trimmed.isEmpty ? "Piano" : instrument.trimmed,
            categoryRaw: category.rawValue,
            keySignature: keySignature.trimmed,
            timeSignatureBeats: min(max(timeSignatureBeats, 1), 16),
            timeSignatureNoteValue: [2, 4, 8, 16].contains(timeSignatureNoteValue) ? timeSignatureNoteValue : 4,
            bestBpm: clampedInitial,
            lastPracticedBpm: clampedInitial,
            targetBpm: min(max(targetBpm, 20), 320),
            statusRaw: status.rawValue,
            practiceNotes: practiceNotes.trimmed,
            nextSessionGoal: nextSessionGoal.trimmed
        )
        context.insert(piece)
        save()
        return piece.id
    }

    func updatePiece(_ piece: RepertoirePiece) {
        save()
    }

    func deletePiece(_ piece: RepertoirePiece) {
        for section in sections where section.pieceId == piece.id {
            context.delete(section)
        }
        context.delete(piece)
        save()
    }

    // MARK: - Sections / movements

    @discardableResult
    func addSection(
        pieceId: Int64,
        name: String,
        orderIndex: Int,
        initialBpm: Int,
        targetBpm: Int,
        timeSignatureBeats: Int,
        timeSignatureNoteValue: Int,
        sectionNotes: String,
        nextSessionGoal: String
    ) -> Int64 {
        let clampedBpm = min(max(initialBpm, 20), 320)
        let section = PieceSection(
            id: nextId(.section, existing: sections.map(\.id)),
            pieceId: pieceId,
            name: name.trimmed,
            orderIndex: orderIndex,
            bestBpm: clampedBpm,
            lastPracticedBpm: clampedBpm,
            targetBpm: min(max(targetBpm, 20), 320),
            timeSignatureBeats: min(max(timeSignatureBeats, 1), 16),
            timeSignatureNoteValue: [2, 4, 8, 16].contains(timeSignatureNoteValue) ? timeSignatureNoteValue : 4,
            sectionNotes: sectionNotes.trimmed,
            nextSessionGoal: nextSessionGoal.trimmed
        )
        context.insert(section)
        save()
        return section.id
    }

    func updateSection(_ section: PieceSection) {
        save()
    }

    func deleteSection(_ section: PieceSection) {
        context.delete(section)
        save()
    }

    // MARK: - Sessions

    @discardableResult
    func logPracticeSession(
        pieceId: Int64?,
        sectionId: Int64?,
        pieceTitle: String,
        sectionName: String,
        composer: String,
        instrument: String,
        category: PieceCategory,
        durationMinutes: Int,
        durationSecondsTotal: Int,
        practicedBpm: Int,
        targetBpmSnapshot: Int,
        measuresPracticed: String,
        focusTags: [String],
        sessionNotes: String,
        nextSessionGoal: String,
        moodRating: Int,
        routineName: String = "",
        timestampEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> Int64 {
        let cleanMins = max(durationMinutes, 1)
        let cleanSecs = max(durationSecondsTotal, cleanMins * 60)
        let cleanBpm = min(max(practicedBpm, 20), 320)
        let cleanGoal = nextSessionGoal.trimmed
        let tags = focusTags
            .map { $0.trimmed }
            .filter { !$0.isEmpty }
            .joined(separator: ",")

        let session = PracticeSession(
            id: nextId(.session, existing: sessions.map(\.id)),
            pieceId: pieceId,
            sectionId: sectionId,
            pieceTitle: pieceTitle.trimmed.isEmpty ? "General Technique & Warmup" : pieceTitle.trimmed,
            sectionName: sectionName.trimmed,
            composer: composer.trimmed,
            instrument: instrument.trimmed.isEmpty ? "Piano" : instrument.trimmed,
            categoryRaw: category.rawValue,
            durationMinutes: cleanMins,
            durationSecondsTotal: cleanSecs,
            practicedBpm: cleanBpm,
            targetBpmSnapshot: min(max(targetBpmSnapshot, 20), 320),
            measuresPracticed: measuresPracticed.trimmed,
            focusTags: tags,
            sessionNotes: sessionNotes.trimmed,
            nextSessionGoal: cleanGoal,
            moodRating: min(max(moodRating, 1), 5),
            routineName: routineName.trimmed,
            timestampEpochMillis: timestampEpochMillis
        )
        context.insert(session)

        // Roll up the section first, matching the Android repository rules.
        if let sectionId, let section = sections.first(where: { $0.id == sectionId }) {
            let isNewerOrSame = section.lastPracticedAtEpochMillis == nil
                || timestampEpochMillis >= (section.lastPracticedAtEpochMillis ?? 0)

            section.bestBpm = max(section.bestBpm, cleanBpm)
            if isNewerOrSame {
                section.lastPracticedBpm = cleanBpm
                if !cleanGoal.isEmpty {
                    section.nextSessionGoal = cleanGoal
                }
            }
            section.totalMinutesPracticed += cleanMins
            section.lastPracticedAtEpochMillis = max(section.lastPracticedAtEpochMillis ?? 0, timestampEpochMillis)
        }

        // Then roll the totals and goals up to the parent piece.
        if let pieceId, let piece = pieces.first(where: { $0.id == pieceId }) {
            let isNewerOrSame = piece.lastPracticedAtEpochMillis == nil
                || timestampEpochMillis >= (piece.lastPracticedAtEpochMillis ?? 0)

            if sectionId == nil {
                piece.bestBpm = max(piece.bestBpm, cleanBpm)
                if isNewerOrSame {
                    piece.lastPracticedBpm = cleanBpm
                }
            }
            if !cleanGoal.isEmpty && isNewerOrSame {
                piece.nextSessionGoal = cleanGoal
            }
            piece.totalMinutesPracticed += cleanMins
            piece.lastPracticedAtEpochMillis = max(piece.lastPracticedAtEpochMillis ?? 0, timestampEpochMillis)
            // PieceStatus stays strictly manual, as on Android.
        }

        save()
        return session.id
    }

    func updateSession(
        _ session: PracticeSession,
        notes: String,
        measures: String,
        nextGoal: String,
        tags: String,
        bpm: Int
    ) {
        session.sessionNotes = notes.trimmed
        session.measuresPracticed = measures.trimmed
        session.nextSessionGoal = nextGoal.trimmed
        session.focusTags = tags.trimmed
        session.practicedBpm = min(max(bpm, 20), 320)
        save()
    }

    func deleteSession(_ session: PracticeSession) {
        context.delete(session)
        save()
    }

    // MARK: - Routine templates

    @discardableResult
    func saveRoutineTemplate(
        existingId: Int64?,
        name: String,
        descriptionText: String,
        instrument: String,
        items: [RoutineItemDraft]
    ) -> Int64? {
        let cleanName = name.trimmed
        guard !cleanName.isEmpty, !items.isEmpty else { return nil }

        let routineId: Int64
        if let existingId, existingId > 0, let routine = routines.first(where: { $0.id == existingId }) {
            routine.name = cleanName
            routine.descriptionText = descriptionText.trimmed
            routine.instrument = instrument.trimmed
            for item in routineItems where item.routineId == existingId {
                context.delete(item)
            }
            routineId = existingId
        } else {
            let routine = RoutineTemplate(
                id: nextId(.routine, existing: routines.map(\.id)),
                name: cleanName,
                descriptionText: descriptionText.trimmed,
                instrument: instrument.trimmed
            )
            context.insert(routine)
            routineId = routine.id
        }

        for (index, draft) in items.enumerated() {
            let item = RoutineTemplateItem(
                id: nextId(.routineItem, existing: routineItems.map(\.id)),
                routineId: routineId,
                orderIndex: index,
                pieceId: draft.pieceId,
                sectionId: draft.sectionId,
                customTitle: draft.customTitle.trimmed.isEmpty ? "Practice Block \(index + 1)" : draft.customTitle.trimmed,
                categoryRaw: draft.category.rawValue,
                targetMinutes: min(max(draft.targetMinutes, 1), 240),
                suggestedBpm: min(max(draft.suggestedBpm, 20), 320),
                notesPrompt: draft.notesPrompt.trimmed
            )
            context.insert(item)
        }

        save()
        return routineId
    }

    func deleteRoutine(_ routine: RoutineTemplate) {
        for item in routineItems where item.routineId == routine.id {
            context.delete(item)
        }
        context.delete(routine)
        save()
    }

    // MARK: - Bulk data

    func clearAllData() {
        for session in sessions { context.delete(session) }
        for item in routineItems { context.delete(item) }
        for routine in routines { context.delete(routine) }
        for section in sections { context.delete(section) }
        for piece in pieces { context.delete(piece) }
        save()
    }

    func seedDemoData() {
        DemoData.seed(into: self)
    }

    func captureSnapshot(
        weeklyGoalMinutes: Int,
        customInstruments: [String],
        customFocusTags: [String]
    ) -> BackupPayload {
        BackupPayload(
            pieces: pieces,
            sections: sections,
            sessions: sessions,
            routines: routines,
            routineItems: routineItems,
            weeklyGoalMinutes: weeklyGoalMinutes,
            customInstruments: customInstruments,
            customFocusTags: customFocusTags
        )
    }

    func restoreSnapshot(_ payload: BackupPayload) {
        clearAllData()

        for piece in payload.pieces {
            bumpHighWater(.piece, to: piece.id)
            context.insert(piece)
        }
        for section in payload.sections {
            bumpHighWater(.section, to: section.id)
            context.insert(section)
        }
        for session in payload.sessions {
            bumpHighWater(.session, to: session.id)
            context.insert(session)
        }
        for routine in payload.routines {
            bumpHighWater(.routine, to: routine.id)
            context.insert(routine)
        }
        for item in payload.routineItems {
            bumpHighWater(.routineItem, to: item.id)
            context.insert(item)
        }

        save()
    }

    // MARK: - Raw inserts used by the demo seed

    @discardableResult
    func insertRawPiece(_ piece: RepertoirePiece) -> Int64 {
        if piece.id == 0 {
            piece.id = nextId(.piece, existing: pieces.map(\.id))
        } else {
            bumpHighWater(.piece, to: piece.id)
        }
        context.insert(piece)
        return piece.id
    }

    @discardableResult
    func insertRawSection(_ section: PieceSection) -> Int64 {
        if section.id == 0 {
            section.id = nextId(.section, existing: sections.map(\.id))
        } else {
            bumpHighWater(.section, to: section.id)
        }
        context.insert(section)
        return section.id
    }

    func insertRawSession(_ session: PracticeSession) {
        if session.id == 0 {
            session.id = nextId(.session, existing: sessions.map(\.id))
        } else {
            bumpHighWater(.session, to: session.id)
        }
        context.insert(session)
    }

    @discardableResult
    func insertRawRoutine(_ routine: RoutineTemplate) -> Int64 {
        if routine.id == 0 {
            routine.id = nextId(.routine, existing: routines.map(\.id))
        } else {
            bumpHighWater(.routine, to: routine.id)
        }
        context.insert(routine)
        return routine.id
    }

    func insertRawRoutineItem(_ item: RoutineTemplateItem) {
        if item.id == 0 {
            item.id = nextId(.routineItem, existing: routineItems.map(\.id))
        } else {
            bumpHighWater(.routineItem, to: item.id)
        }
        context.insert(item)
    }

    func commitRawInserts() {
        save()
    }

    // MARK: - Id generation

    private enum EntityKind: String {
        case piece
        case section
        case session
        case routine
        case routineItem
    }

    private func idKey(_ kind: EntityKind) -> String {
        "cadence.next_id.\(kind.rawValue)"
    }

    private func nextId(_ kind: EntityKind, existing: [Int64]) -> Int64 {
        let stored = Int64(defaults.integer(forKey: idKey(kind)))
        let next = max(stored, existing.max() ?? 0) + 1
        defaults.set(Int(next), forKey: idKey(kind))
        return next
    }

    private func bumpHighWater(_ kind: EntityKind, to id: Int64) {
        let stored = Int64(defaults.integer(forKey: idKey(kind)))
        if id > stored {
            defaults.set(Int(id), forKey: idKey(kind))
        }
    }

    private func save() {
        do {
            try context.save()
        } catch {
            print("CadenceStore save error: \(error)")
        }
        refresh()
    }
}

extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
