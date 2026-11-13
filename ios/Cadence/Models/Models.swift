import Foundation
import SwiftData

// MARK: - Repertoire piece

@Model
final class RepertoirePiece {
    @Attribute(.unique) var id: Int64 = 0
    var title: String = ""
    var composer: String = ""
    var instrument: String = "Piano"
    var categoryRaw: String = PieceCategory.REPERTOIRE.rawValue
    var keySignature: String = ""
    var timeSignatureBeats: Int = 4
    var timeSignatureNoteValue: Int = 4
    var bestBpm: Int = 72
    var lastPracticedBpm: Int = 72
    var targetBpm: Int = 120
    var statusRaw: String = PieceStatus.LEARNING.rawValue
    var practiceNotes: String = ""
    var nextSessionGoal: String = ""
    var totalMinutesPracticed: Int = 0
    var lastPracticedAtEpochMillis: Int64?
    var createdAtEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    init(
        id: Int64 = 0,
        title: String,
        composer: String = "",
        instrument: String = "Piano",
        categoryRaw: String = PieceCategory.REPERTOIRE.rawValue,
        keySignature: String = "",
        timeSignatureBeats: Int = 4,
        timeSignatureNoteValue: Int = 4,
        bestBpm: Int = 72,
        lastPracticedBpm: Int = 72,
        targetBpm: Int = 120,
        statusRaw: String = PieceStatus.LEARNING.rawValue,
        practiceNotes: String = "",
        nextSessionGoal: String = "",
        totalMinutesPracticed: Int = 0,
        lastPracticedAtEpochMillis: Int64? = nil,
        createdAtEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.title = title
        self.composer = composer
        self.instrument = instrument
        self.categoryRaw = categoryRaw
        self.keySignature = keySignature
        self.timeSignatureBeats = timeSignatureBeats
        self.timeSignatureNoteValue = timeSignatureNoteValue
        self.bestBpm = bestBpm
        self.lastPracticedBpm = lastPracticedBpm
        self.targetBpm = targetBpm
        self.statusRaw = statusRaw
        self.practiceNotes = practiceNotes
        self.nextSessionGoal = nextSessionGoal
        self.totalMinutesPracticed = totalMinutesPracticed
        self.lastPracticedAtEpochMillis = lastPracticedAtEpochMillis
        self.createdAtEpochMillis = createdAtEpochMillis
    }

    var pieceStatus: PieceStatus {
        PieceStatus(rawValue: statusRaw) ?? .LEARNING
    }

    var pieceCategory: PieceCategory {
        PieceCategory(rawValue: categoryRaw) ?? .REPERTOIRE
    }

    var timeSignatureLabel: String {
        "\(timeSignatureBeats)/\(timeSignatureNoteValue)"
    }

    var tempoMasteryProgress: Double {
        guard targetBpm > 0 else { return 0 }
        return min(max(Double(bestBpm) / Double(targetBpm), 0), 1)
    }
}

// MARK: - Piece section / movement

@Model
final class PieceSection {
    @Attribute(.unique) var id: Int64 = 0
    var pieceId: Int64 = 0
    var name: String = ""
    var orderIndex: Int = 0
    var bestBpm: Int = 72
    var lastPracticedBpm: Int = 72
    var targetBpm: Int = 120
    var timeSignatureBeats: Int = 4
    var timeSignatureNoteValue: Int = 4
    var sectionNotes: String = ""
    var nextSessionGoal: String = ""
    var totalMinutesPracticed: Int = 0
    var lastPracticedAtEpochMillis: Int64?

    init(
        id: Int64 = 0,
        pieceId: Int64,
        name: String,
        orderIndex: Int = 0,
        bestBpm: Int = 72,
        lastPracticedBpm: Int = 72,
        targetBpm: Int = 120,
        timeSignatureBeats: Int = 4,
        timeSignatureNoteValue: Int = 4,
        sectionNotes: String = "",
        nextSessionGoal: String = "",
        totalMinutesPracticed: Int = 0,
        lastPracticedAtEpochMillis: Int64? = nil
    ) {
        self.id = id
        self.pieceId = pieceId
        self.name = name
        self.orderIndex = orderIndex
        self.bestBpm = bestBpm
        self.lastPracticedBpm = lastPracticedBpm
        self.targetBpm = targetBpm
        self.timeSignatureBeats = timeSignatureBeats
        self.timeSignatureNoteValue = timeSignatureNoteValue
        self.sectionNotes = sectionNotes
        self.nextSessionGoal = nextSessionGoal
        self.totalMinutesPracticed = totalMinutesPracticed
        self.lastPracticedAtEpochMillis = lastPracticedAtEpochMillis
    }

    var timeSignatureLabel: String {
        "\(timeSignatureBeats)/\(timeSignatureNoteValue)"
    }

    var tempoMasteryProgress: Double {
        guard targetBpm > 0 else { return 0 }
        return min(max(Double(bestBpm) / Double(targetBpm), 0), 1)
    }
}

// MARK: - Practice session

@Model
final class PracticeSession {
    @Attribute(.unique) var id: Int64 = 0
    var pieceId: Int64?
    var sectionId: Int64?
    var pieceTitle: String = ""
    var sectionName: String = ""
    var composer: String = ""
    var instrument: String = "Piano"
    var categoryRaw: String = PieceCategory.REPERTOIRE.rawValue
    var durationMinutes: Int = 0
    var durationSecondsTotal: Int = 0
    var practicedBpm: Int = 92
    var targetBpmSnapshot: Int = 120
    var measuresPracticed: String = ""
    var focusTags: String = ""
    var sessionNotes: String = ""
    var nextSessionGoal: String = ""
    var moodRating: Int = 4
    var routineName: String = ""
    var timestampEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    init(
        id: Int64 = 0,
        pieceId: Int64? = nil,
        sectionId: Int64? = nil,
        pieceTitle: String,
        sectionName: String = "",
        composer: String = "",
        instrument: String = "Piano",
        categoryRaw: String = PieceCategory.REPERTOIRE.rawValue,
        durationMinutes: Int,
        durationSecondsTotal: Int,
        practicedBpm: Int,
        targetBpmSnapshot: Int,
        measuresPracticed: String = "",
        focusTags: String = "",
        sessionNotes: String = "",
        nextSessionGoal: String = "",
        moodRating: Int = 4,
        routineName: String = "",
        timestampEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.pieceId = pieceId
        self.sectionId = sectionId
        self.pieceTitle = pieceTitle
        self.sectionName = sectionName
        self.composer = composer
        self.instrument = instrument
        self.categoryRaw = categoryRaw
        self.durationMinutes = durationMinutes
        self.durationSecondsTotal = durationSecondsTotal
        self.practicedBpm = practicedBpm
        self.targetBpmSnapshot = targetBpmSnapshot
        self.measuresPracticed = measuresPracticed
        self.focusTags = focusTags
        self.sessionNotes = sessionNotes
        self.nextSessionGoal = nextSessionGoal
        self.moodRating = moodRating
        self.routineName = routineName
        self.timestampEpochMillis = timestampEpochMillis
    }

    var pieceCategory: PieceCategory {
        PieceCategory(rawValue: categoryRaw) ?? .REPERTOIRE
    }

    var focusTagList: [String] {
        focusTags.split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }
}

// MARK: - Routine templates

@Model
final class RoutineTemplate {
    @Attribute(.unique) var id: Int64 = 0
    var name: String = ""
    var descriptionText: String = ""
    var instrument: String = ""
    var createdAtEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    init(
        id: Int64 = 0,
        name: String,
        descriptionText: String = "",
        instrument: String = "",
        createdAtEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.name = name
        self.descriptionText = descriptionText
        self.instrument = instrument
        self.createdAtEpochMillis = createdAtEpochMillis
    }
}

@Model
final class RoutineTemplateItem {
    @Attribute(.unique) var id: Int64 = 0
    var routineId: Int64 = 0
    var orderIndex: Int = 0
    var pieceId: Int64?
    var sectionId: Int64?
    var customTitle: String = ""
    var categoryRaw: String = PieceCategory.TECHNIQUE.rawValue
    var targetMinutes: Int = 10
    var suggestedBpm: Int = 92
    var notesPrompt: String = ""

    init(
        id: Int64 = 0,
        routineId: Int64,
        orderIndex: Int,
        pieceId: Int64? = nil,
        sectionId: Int64? = nil,
        customTitle: String,
        categoryRaw: String = PieceCategory.TECHNIQUE.rawValue,
        targetMinutes: Int = 10,
        suggestedBpm: Int = 92,
        notesPrompt: String = ""
    ) {
        self.id = id
        self.routineId = routineId
        self.orderIndex = orderIndex
        self.pieceId = pieceId
        self.sectionId = sectionId
        self.customTitle = customTitle
        self.categoryRaw = categoryRaw
        self.targetMinutes = targetMinutes
        self.suggestedBpm = suggestedBpm
        self.notesPrompt = notesPrompt
    }

    var pieceCategory: PieceCategory {
        PieceCategory(rawValue: categoryRaw) ?? .TECHNIQUE
    }
}

// MARK: - Aggregates

struct PieceWithSections: Identifiable {
    let piece: RepertoirePiece
    let sections: [PieceSection]

    var id: Int64 { piece.id }

    var sortedSections: [PieceSection] {
        sections.sorted { lhs, rhs in
            if lhs.orderIndex == rhs.orderIndex { return lhs.id < rhs.id }
            return lhs.orderIndex < rhs.orderIndex
        }
    }
}

struct RoutineWithItems: Identifiable {
    let routine: RoutineTemplate
    let items: [RoutineTemplateItem]

    var id: Int64 { routine.id }

    var sortedItems: [RoutineTemplateItem] {
        items.sorted { lhs, rhs in
            if lhs.orderIndex == rhs.orderIndex { return lhs.id < rhs.id }
            return lhs.orderIndex < rhs.orderIndex
        }
    }

    var totalTargetMinutes: Int {
        items.reduce(0) { $0 + $1.targetMinutes }
    }
}
