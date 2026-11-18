import Foundation

struct BackupPayload {
    var pieces: [RepertoirePiece]
    var sections: [PieceSection]
    var sessions: [PracticeSession]
    var routines: [RoutineTemplate]
    var routineItems: [RoutineTemplateItem]
    var weeklyGoalMinutes: Int
    var customInstruments: [String]
    var customFocusTags: [String]
}

enum BackupError: LocalizedError {
    case invalidJSON
    case invalidRoot

    var errorDescription: String? {
        switch self {
        case .invalidJSON: return "The selected file is not valid JSON."
        case .invalidRoot: return "The backup file does not contain a Ostinato backup object."
        }
    }
}

/// JSON backup/restore plus teacher report and CSV generation.
/// The JSON schema is byte-for-byte compatible with the Android app's
/// `BackupAndReportManager` (schema version 2), so users can move data
/// between Android, iPhone and iPad.
enum BackupManager {

    static let backupSchemaVersion = 2

    // MARK: - Serialization

    static func serializeBackupToJson(_ payload: BackupPayload) -> String {
        var root: [String: Any] = [
            "schemaVersion": backupSchemaVersion,
            "exportedAtEpochMillis": Int64(Date().timeIntervalSince1970 * 1000),
            "weeklyGoalMinutes": payload.weeklyGoalMinutes,
            "customInstruments": payload.customInstruments,
            "customFocusTags": payload.customFocusTags
        ]

        root["pieces"] = payload.pieces.map { piece -> [String: Any] in
            var value: [String: Any] = [
                "id": piece.id,
                "title": piece.title,
                "composer": piece.composer,
                "instrument": piece.instrument,
                "category": piece.categoryRaw,
                "keySignature": piece.keySignature,
                "timeSignatureBeats": piece.timeSignatureBeats,
                "timeSignatureNoteValue": piece.timeSignatureNoteValue,
                "bestBpm": piece.bestBpm,
                "lastPracticedBpm": piece.lastPracticedBpm,
                "targetBpm": piece.targetBpm,
                "status": piece.statusRaw,
                "practiceNotes": piece.practiceNotes,
                "nextSessionGoal": piece.nextSessionGoal,
                "totalMinutesPracticed": piece.totalMinutesPracticed,
                "createdAtEpochMillis": piece.createdAtEpochMillis
            ]
            if let last = piece.lastPracticedAtEpochMillis {
                value["lastPracticedAtEpochMillis"] = last
            }
            return value
        }

        root["sections"] = payload.sections.map { section -> [String: Any] in
            var value: [String: Any] = [
                "id": section.id,
                "pieceId": section.pieceId,
                "name": section.name,
                "orderIndex": section.orderIndex,
                "bestBpm": section.bestBpm,
                "lastPracticedBpm": section.lastPracticedBpm,
                "targetBpm": section.targetBpm,
                "timeSignatureBeats": section.timeSignatureBeats,
                "timeSignatureNoteValue": section.timeSignatureNoteValue,
                "sectionNotes": section.sectionNotes,
                "nextSessionGoal": section.nextSessionGoal,
                "totalMinutesPracticed": section.totalMinutesPracticed
            ]
            if let last = section.lastPracticedAtEpochMillis {
                value["lastPracticedAtEpochMillis"] = last
            }
            return value
        }

        root["sessions"] = payload.sessions.map { session -> [String: Any] in
            var value: [String: Any] = [
                "id": session.id,
                "pieceTitle": session.pieceTitle,
                "sectionName": session.sectionName,
                "composer": session.composer,
                "instrument": session.instrument,
                "category": session.categoryRaw,
                "durationMinutes": session.durationMinutes,
                "durationSecondsTotal": session.durationSecondsTotal,
                "practicedBpm": session.practicedBpm,
                "targetBpmSnapshot": session.targetBpmSnapshot,
                "measuresPracticed": session.measuresPracticed,
                "focusTags": session.focusTags,
                "sessionNotes": session.sessionNotes,
                "nextSessionGoal": session.nextSessionGoal,
                "moodRating": session.moodRating,
                "routineName": session.routineName,
                "timestampEpochMillis": session.timestampEpochMillis
            ]
            if let pieceId = session.pieceId { value["pieceId"] = pieceId }
            if let sectionId = session.sectionId { value["sectionId"] = sectionId }
            return value
        }

        root["routines"] = payload.routines.map { routine -> [String: Any] in
            [
                "id": routine.id,
                "name": routine.name,
                "description": routine.descriptionText,
                "instrument": routine.instrument,
                "createdAtEpochMillis": routine.createdAtEpochMillis
            ]
        }

        root["routineItems"] = payload.routineItems.map { item -> [String: Any] in
            var value: [String: Any] = [
                "id": item.id,
                "routineId": item.routineId,
                "orderIndex": item.orderIndex,
                "customTitle": item.customTitle,
                "category": item.categoryRaw,
                "targetMinutes": item.targetMinutes,
                "suggestedBpm": item.suggestedBpm,
                "notesPrompt": item.notesPrompt
            ]
            if let pieceId = item.pieceId { value["pieceId"] = pieceId }
            if let sectionId = item.sectionId { value["sectionId"] = sectionId }
            return value
        }

        guard
            let data = try? JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys]),
            let json = String(data: data, encoding: .utf8)
        else {
            return "{}"
        }
        return json
    }

    // MARK: - Parsing

    static func parseBackupFromJson(_ jsonString: String) throws -> BackupPayload {
        guard let data = jsonString.data(using: .utf8) else {
            throw BackupError.invalidJSON
        }
        let object: Any
        do {
            object = try JSONSerialization.jsonObject(with: data, options: [])
        } catch {
            throw BackupError.invalidJSON
        }
        guard let root = object as? [String: Any] else {
            throw BackupError.invalidRoot
        }

        let now = Int64(Date().timeIntervalSince1970 * 1000)

        func intValue(_ dictionary: [String: Any], _ key: String, _ fallback: Int) -> Int {
            (dictionary[key] as? NSNumber)?.intValue ?? fallback
        }

        func int64Value(_ dictionary: [String: Any], _ key: String, _ fallback: Int64) -> Int64 {
            (dictionary[key] as? NSNumber)?.int64Value ?? fallback
        }

        func optionalInt64(_ dictionary: [String: Any], _ key: String) -> Int64? {
            guard let number = dictionary[key] as? NSNumber else { return nil }
            return number.int64Value
        }

        func stringValue(_ dictionary: [String: Any], _ key: String, _ fallback: String) -> String {
            dictionary[key] as? String ?? fallback
        }

        func stringList(_ value: Any?) -> [String] {
            guard let array = value as? [Any] else { return [] }
            return array.compactMap { $0 as? String }
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
        }

        let pieceDictionaries = root["pieces"] as? [[String: Any]] ?? []
        let pieces: [RepertoirePiece] = pieceDictionaries.compactMap { dictionary in
            guard let id = optionalInt64(dictionary, "id") else { return nil }
            let fallbackBpm = intValue(dictionary, "currentBpm", 80)
            return RepertoirePiece(
                id: id,
                title: stringValue(dictionary, "title", "Untitled Piece"),
                composer: stringValue(dictionary, "composer", ""),
                instrument: stringValue(dictionary, "instrument", "Piano"),
                categoryRaw: stringValue(dictionary, "category", PieceCategory.REPERTOIRE.rawValue),
                keySignature: stringValue(dictionary, "keySignature", ""),
                timeSignatureBeats: intValue(dictionary, "timeSignatureBeats", 4),
                timeSignatureNoteValue: intValue(dictionary, "timeSignatureNoteValue", 4),
                bestBpm: intValue(dictionary, "bestBpm", fallbackBpm),
                lastPracticedBpm: intValue(dictionary, "lastPracticedBpm", fallbackBpm),
                targetBpm: intValue(dictionary, "targetBpm", 120),
                statusRaw: stringValue(dictionary, "status", PieceStatus.LEARNING.rawValue),
                practiceNotes: stringValue(dictionary, "practiceNotes", ""),
                nextSessionGoal: stringValue(dictionary, "nextSessionGoal", ""),
                totalMinutesPracticed: intValue(dictionary, "totalMinutesPracticed", 0),
                lastPracticedAtEpochMillis: optionalInt64(dictionary, "lastPracticedAtEpochMillis"),
                createdAtEpochMillis: int64Value(dictionary, "createdAtEpochMillis", now)
            )
        }

        let sectionDictionaries = root["sections"] as? [[String: Any]] ?? []
        let sections: [PieceSection] = sectionDictionaries.enumerated().compactMap { index, dictionary in
            guard
                let id = optionalInt64(dictionary, "id"),
                let pieceId = optionalInt64(dictionary, "pieceId")
            else { return nil }
            return PieceSection(
                id: id,
                pieceId: pieceId,
                name: stringValue(dictionary, "name", "Movement"),
                orderIndex: intValue(dictionary, "orderIndex", index),
                bestBpm: intValue(dictionary, "bestBpm", 80),
                lastPracticedBpm: intValue(dictionary, "lastPracticedBpm", 80),
                targetBpm: intValue(dictionary, "targetBpm", 120),
                timeSignatureBeats: intValue(dictionary, "timeSignatureBeats", 4),
                timeSignatureNoteValue: intValue(dictionary, "timeSignatureNoteValue", 4),
                sectionNotes: stringValue(dictionary, "sectionNotes", ""),
                nextSessionGoal: stringValue(dictionary, "nextSessionGoal", ""),
                totalMinutesPracticed: intValue(dictionary, "totalMinutesPracticed", 0),
                lastPracticedAtEpochMillis: optionalInt64(dictionary, "lastPracticedAtEpochMillis")
            )
        }

        let sessionDictionaries = root["sessions"] as? [[String: Any]] ?? []
        let sessions: [PracticeSession] = sessionDictionaries.compactMap { dictionary in
            guard let id = optionalInt64(dictionary, "id") else { return nil }
            let minutes = intValue(dictionary, "durationMinutes", 15)
            return PracticeSession(
                id: id,
                pieceId: optionalInt64(dictionary, "pieceId"),
                sectionId: optionalInt64(dictionary, "sectionId"),
                pieceTitle: stringValue(dictionary, "pieceTitle", "Practice Session"),
                sectionName: stringValue(dictionary, "sectionName", ""),
                composer: stringValue(dictionary, "composer", ""),
                instrument: stringValue(dictionary, "instrument", "Piano"),
                categoryRaw: stringValue(dictionary, "category", PieceCategory.REPERTOIRE.rawValue),
                durationMinutes: minutes,
                durationSecondsTotal: intValue(dictionary, "durationSecondsTotal", minutes * 60),
                practicedBpm: intValue(dictionary, "practicedBpm", 90),
                targetBpmSnapshot: intValue(dictionary, "targetBpmSnapshot", 120),
                measuresPracticed: stringValue(dictionary, "measuresPracticed", ""),
                focusTags: stringValue(dictionary, "focusTags", ""),
                sessionNotes: stringValue(dictionary, "sessionNotes", ""),
                nextSessionGoal: stringValue(dictionary, "nextSessionGoal", ""),
                moodRating: intValue(dictionary, "moodRating", 4),
                routineName: stringValue(dictionary, "routineName", ""),
                timestampEpochMillis: int64Value(dictionary, "timestampEpochMillis", now)
            )
        }

        let routineDictionaries = root["routines"] as? [[String: Any]] ?? []
        let routines: [RoutineTemplate] = routineDictionaries.compactMap { dictionary in
            guard let id = optionalInt64(dictionary, "id") else { return nil }
            return RoutineTemplate(
                id: id,
                name: stringValue(dictionary, "name", "Practice Routine"),
                descriptionText: stringValue(dictionary, "description", ""),
                instrument: stringValue(dictionary, "instrument", ""),
                createdAtEpochMillis: int64Value(dictionary, "createdAtEpochMillis", now)
            )
        }

        let itemDictionaries = root["routineItems"] as? [[String: Any]] ?? []
        let routineItems: [RoutineTemplateItem] = itemDictionaries.enumerated().compactMap { index, dictionary in
            guard
                let id = optionalInt64(dictionary, "id"),
                let routineId = optionalInt64(dictionary, "routineId")
            else { return nil }
            return RoutineTemplateItem(
                id: id,
                routineId: routineId,
                orderIndex: intValue(dictionary, "orderIndex", index),
                pieceId: optionalInt64(dictionary, "pieceId"),
                sectionId: optionalInt64(dictionary, "sectionId"),
                customTitle: stringValue(dictionary, "customTitle", "Block"),
                categoryRaw: stringValue(dictionary, "category", PieceCategory.TECHNIQUE.rawValue),
                targetMinutes: intValue(dictionary, "targetMinutes", 10),
                suggestedBpm: intValue(dictionary, "suggestedBpm", 90),
                notesPrompt: stringValue(dictionary, "notesPrompt", "")
            )
        }

        return BackupPayload(
            pieces: pieces,
            sections: sections,
            sessions: sessions,
            routines: routines,
            routineItems: routineItems,
            weeklyGoalMinutes: intValue(root, "weeklyGoalMinutes", 180),
            customInstruments: stringList(root["customInstruments"]),
            customFocusTags: stringList(root["customFocusTags"])
        )
    }

    // MARK: - Teacher report

    static func generateTeacherReportMarkdown(
        sessions: [PracticeSession],
        pieces: [RepertoirePiece],
        daysWindow: Int = 7,
        weeklyStreak: Int = 0
    ) -> String {
        let now = Date()
        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)
        let cutoff = nowMillis - Int64(daysWindow) * 86_400_000
        let recent = sessions
            .filter { $0.timestampEpochMillis >= cutoff }
            .sorted { $0.timestampEpochMillis > $1.timestampEpochMillis }

        let totalMinutes = recent.reduce(0) { $0 + $1.durationMinutes }
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60

        var lines: [String] = []
        lines.append("# Ostinato Practice Report (Last \(daysWindow) Days)")
        lines.append("Generated: \(Formatters.reportDate.string(from: now))")
        lines.append("Total Practice Time: **\(hours)h \(minutes)m** across **\(recent.count) sessions**")
        lines.append("Weekly Consistency Streak: **\(weeklyStreak) week(s)**")
        lines.append("")

        let activePieces = pieces.filter { $0.pieceStatus != .MASTERED }
        if !activePieces.isEmpty {
            lines.append("## Current Repertoire & Tempo Progress")
            for piece in activePieces {
                lines.append("- **\(piece.title)** (\(piece.instrument), \(piece.pieceStatus.label)): Best \(piece.bestBpm) BPM / Last \(piece.lastPracticedBpm) BPM → Target \(piece.targetBpm) BPM")
                if !piece.nextSessionGoal.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    lines.append("  - Next Focus: \(piece.nextSessionGoal)")
                }
            }
            lines.append("")
        }

        lines.append("## Detailed Session Log")
        if recent.isEmpty {
            lines.append("_No sessions logged in the last \(daysWindow) days._")
        } else {
            for session in recent {
                let sectionSuffix = session.sectionName.isEmpty ? "" : " — \(session.sectionName)"
                let measuresSuffix = session.measuresPracticed.isEmpty ? "" : " [\(session.measuresPracticed)]"
                lines.append("### \(Formatters.reportSession.string(from: AnalyticsCalculator.date(from: session.timestampEpochMillis)))")
                lines.append("- **Piece**: \(session.pieceTitle)\(sectionSuffix)\(measuresSuffix) (\(session.instrument))")
                lines.append("- **Duration & Tempo**: \(session.durationMinutes) min @ \(session.practicedBpm) BPM (Target \(session.targetBpmSnapshot) BPM)")
                if !session.focusTags.isEmpty {
                    lines.append("- **Focus**: \(session.focusTags)")
                }
                if !session.sessionNotes.isEmpty {
                    lines.append("- **Notes**: \(session.sessionNotes)")
                }
                if !session.nextSessionGoal.isEmpty {
                    lines.append("- **Next Session Goal**: \(session.nextSessionGoal)")
                }
                lines.append("")
            }
        }

        return lines.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - CSV

    static func generateSessionsCsv(_ sessions: [PracticeSession]) -> String {
        var lines: [String] = [
            "Date,Instrument,Category,Piece,Movement_Section,Measures,Duration_Min,Practiced_BPM,Target_BPM,Rating,Focus_Tags,Session_Notes,Next_Goal"
        ]

        let sorted = sessions.sorted { $0.timestampEpochMillis > $1.timestampEpochMillis }
        for session in sorted {
            let row = [
                Formatters.csvDate.string(from: AnalyticsCalculator.date(from: session.timestampEpochMillis)),
                session.instrument,
                session.pieceCategory.shortLabel,
                session.pieceTitle,
                session.sectionName,
                session.measuresPracticed,
                String(session.durationMinutes),
                String(session.practicedBpm),
                String(session.targetBpmSnapshot),
                String(session.moodRating),
                session.focusTags,
                session.sessionNotes,
                session.nextSessionGoal
            ]
            .map(escapeCsv)
            .joined(separator: ",")
            lines.append(row)
        }

        return lines.joined(separator: "\n") + "\n"
    }

    private static func escapeCsv(_ value: String) -> String {
        let normalized = value
            .replacingOccurrences(of: "\r\n", with: " ")
            .replacingOccurrences(of: "\n", with: " ")
        if normalized.contains(",") || normalized.contains("\"") {
            return "\"\(normalized.replacingOccurrences(of: "\"", with: "\"\""))\""
        }
        return normalized
    }
}

enum Formatters {
    static let reportDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMM d, yyyy"
        return formatter
    }()

    static let reportSession: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "EEE, MMM d • h:mm a"
        return formatter
    }()

    static let journalDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "EEE, MMM d • h:mm a"
        return formatter
    }()

    static let csvDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter
    }()

    static let backupStamp: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyyMMdd_HHmm"
        return formatter
    }()

    /// Count-up stopwatch clock. Hours are only shown once elapsed.
    static func clock(_ totalSeconds: Int) -> String {
        let safe = max(totalSeconds, 0)
        let hours = safe / 3600
        let minutes = (safe % 3600) / 60
        let seconds = safe % 60
        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%02d:%02d", minutes, seconds)
    }

    static func hoursAndMinutes(_ totalMinutes: Int) -> String {
        "\(totalMinutes / 60)h \(totalMinutes % 60)m"
    }
}
