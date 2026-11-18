import Foundation

struct DailyPracticeStat: Identifiable {
    let dayLabel: String
    let dateKey: String
    let minutesPracticed: Int
    let isToday: Bool

    var id: String { dateKey }
}

struct HeatmapDayCell: Identifiable {
    let dayOfMonth: Int
    let dateKey: String
    let totalMinutes: Int
    let sessionCount: Int
    let isToday: Bool
    let isCurrentMonth: Bool

    var id: String { dateKey }
}

struct BpmHistoryPoint: Identifiable {
    let dateLabel: String
    let bpm: Int
    let measures: String
    let sessionNotes: String

    var id: String { "\(dateLabel)-\(bpm)-\(measures)" }
}

struct ShareSlice: Identifiable {
    let label: String
    let minutes: Int
    let fraction: Double

    var id: String { label }
}

struct PracticeBreakdownStats {
    let byCategory: [ShareSlice]
    let byInstrument: [ShareSlice]
    let topPieces: [ShareSlice]
}

struct RustyPieceAlert: Identifiable {
    let piece: RepertoirePiece
    /// `nil` when the piece has never been practiced.
    let daysSincePracticed: Int?

    var id: Int64 { piece.id }
}

/// All analytics math is ported 1:1 from the Android `AnalyticsCalculator`.
/// Weeks are always Monday–Sunday, matching the original streak rules.
enum AnalyticsCalculator {

    static var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.firstWeekday = 2
        cal.timeZone = .current
        cal.locale = Locale(identifier: "en_US_POSIX")
        return cal
    }

    static let dateKeyFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    static let dayNameFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "EEE"
        return formatter
    }()

    static let shortDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMM d"
        return formatter
    }()

    static let monthTitleFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMMM yyyy"
        return formatter
    }()

    static func date(from epochMillis: Int64) -> Date {
        Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
    }

    static func epochMillis(from date: Date) -> Int64 {
        Int64((date.timeIntervalSince1970 * 1000.0).rounded())
    }

    static func dateKey(for epochMillis: Int64) -> String {
        dateKeyFormatter.string(from: date(from: epochMillis))
    }

    static func dateKey(for date: Date) -> String {
        dateKeyFormatter.string(from: date)
    }

    /// Monday 00:00 local time of the ISO week containing `epochMillis`.
    static func mondayStart(of epochMillis: Int64) -> Int64 {
        let cal = calendar
        let startOfDay = cal.startOfDay(for: date(from: epochMillis))
        let weekday = cal.component(.weekday, from: startOfDay) // Sunday = 1 ... Saturday = 7
        let daysFromMonday = (weekday + 5) % 7
        guard let monday = cal.date(byAdding: .day, value: -daysFromMonday, to: startOfDay) else {
            return epochMillis
        }
        return Self.epochMillis(from: monday)
    }

    static func advanceWeeks(_ mondayMillis: Int64, by deltaWeeks: Int) -> Int64 {
        let cal = calendar
        guard let advanced = cal.date(byAdding: .weekOfYear, value: deltaWeeks, to: date(from: mondayMillis)) else {
            return mondayMillis
        }
        return epochMillis(from: cal.startOfDay(for: advanced))
    }

    static func calculateCurrentWeekMinutes(
        sessions: [PracticeSession],
        nowEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> Int {
        let thisMonday = mondayStart(of: nowEpochMillis)
        let nextMonday = advanceWeeks(thisMonday, by: 1)
        return sessions
            .filter { $0.timestampEpochMillis >= thisMonday && $0.timestampEpochMillis < nextMonday }
            .reduce(0) { $0 + $1.durationMinutes }
    }

    /// Counts consecutive Monday–Sunday weeks meeting the weekly goal.
    /// An in-progress week that already hit the goal counts; otherwise the
    /// streak stays alive from the previous completed week.
    static func calculateWeeklyConsistencyStreak(
        sessions: [PracticeSession],
        weeklyGoalMinutes: Int,
        nowEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> Int {
        guard !sessions.isEmpty, weeklyGoalMinutes > 0 else { return 0 }

        var minutesByMonday: [Int64: Int] = [:]
        for session in sessions {
            let key = mondayStart(of: session.timestampEpochMillis)
            minutesByMonday[key, default: 0] += session.durationMinutes
        }

        let currentMonday = mondayStart(of: nowEpochMillis)
        let currentWeekMinutes = minutesByMonday[currentMonday] ?? 0

        var streak = 0
        var cursor = advanceWeeks(currentMonday, by: -1)

        if currentWeekMinutes >= weeklyGoalMinutes {
            streak = 1
        }

        while true {
            let minutes = minutesByMonday[cursor] ?? 0
            if minutes >= weeklyGoalMinutes {
                streak += 1
                cursor = advanceWeeks(cursor, by: -1)
            } else {
                break
            }
        }
        return streak
    }

    static func calculateLast7DaysStats(
        sessions: [PracticeSession],
        nowEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> [DailyPracticeStat] {
        let cal = calendar
        let now = date(from: nowEpochMillis)

        var minutesByDay: [String: Int] = [:]
        for session in sessions {
            minutesByDay[dateKey(for: session.timestampEpochMillis), default: 0] += session.durationMinutes
        }

        var result: [DailyPracticeStat] = []
        for offset in stride(from: 6, through: 0, by: -1) {
            guard let day = cal.date(byAdding: .day, value: -offset, to: now) else { continue }
            let key = dateKey(for: day)
            result.append(
                DailyPracticeStat(
                    dayLabel: dayNameFormatter.string(from: day),
                    dateKey: key,
                    minutesPracticed: minutesByDay[key] ?? 0,
                    isToday: offset == 0
                )
            )
        }
        return result
    }

    /// Monday-aligned 7-column calendar grid for `monthZeroBased` (0 = January).
    static func buildMonthlyHeatmapCells(
        sessions: [PracticeSession],
        year: Int,
        monthZeroBased: Int,
        nowEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> [HeatmapDayCell] {
        let cal = calendar
        let todayKey = dateKey(for: nowEpochMillis)

        var sessionsByDay: [String: [PracticeSession]] = [:]
        for session in sessions {
            sessionsByDay[dateKey(for: session.timestampEpochMillis), default: []].append(session)
        }

        var components = DateComponents()
        components.year = year
        components.month = monthZeroBased + 1
        components.day = 1
        guard let firstOfMonth = cal.date(from: components) else { return [] }

        let weekday = cal.component(.weekday, from: firstOfMonth)
        let leadingEmptyDays = (weekday + 5) % 7
        let daysInMonth = cal.range(of: .day, in: .month, for: firstOfMonth)?.count ?? 30

        func makeCell(for day: Date, isCurrentMonth: Bool) -> HeatmapDayCell {
            let key = dateKey(for: day)
            let daySessions = sessionsByDay[key] ?? []
            return HeatmapDayCell(
                dayOfMonth: cal.component(.day, from: day),
                dateKey: key,
                totalMinutes: daySessions.reduce(0) { $0 + $1.durationMinutes },
                sessionCount: daySessions.count,
                isToday: key == todayKey,
                isCurrentMonth: isCurrentMonth
            )
        }

        var cells: [HeatmapDayCell] = []

        if leadingEmptyDays > 0 {
            for offset in stride(from: leadingEmptyDays, through: 1, by: -1) {
                if let day = cal.date(byAdding: .day, value: -offset, to: firstOfMonth) {
                    cells.append(makeCell(for: day, isCurrentMonth: false))
                }
            }
        }

        for dayOffset in 0..<daysInMonth {
            if let day = cal.date(byAdding: .day, value: dayOffset, to: firstOfMonth) {
                cells.append(makeCell(for: day, isCurrentMonth: true))
            }
        }

        let remainder = cells.count % 7
        if remainder != 0 {
            let trailingCount = 7 - remainder
            guard let lastDay = cal.date(byAdding: .day, value: daysInMonth - 1, to: firstOfMonth) else {
                return cells
            }
            for offset in 1...trailingCount {
                if let day = cal.date(byAdding: .day, value: offset, to: lastDay) {
                    cells.append(makeCell(for: day, isCurrentMonth: false))
                }
            }
        }

        return cells
    }

    static func calculateBreakdownStats(sessions: [PracticeSession]) -> PracticeBreakdownStats {
        let totalMinutes = max(sessions.reduce(0) { $0 + $1.durationMinutes }, 1)

        let byCategory = PieceCategory.allCases.map { category in
            let minutes = sessions
                .filter { $0.pieceCategory == category }
                .reduce(0) { $0 + $1.durationMinutes }
            return ShareSlice(
                label: category.shortLabel,
                minutes: minutes,
                fraction: min(max(Double(minutes) / Double(totalMinutes), 0), 1)
            )
        }.sorted { $0.minutes > $1.minutes }

        let instrumentGroups = Dictionary(grouping: sessions) { session in
            session.instrument.isEmpty ? "Other" : session.instrument
        }
        let byInstrument = instrumentGroups.map { instrument, group in
            let minutes = group.reduce(0) { $0 + $1.durationMinutes }
            return ShareSlice(
                label: instrument,
                minutes: minutes,
                fraction: min(max(Double(minutes) / Double(totalMinutes), 0), 1)
            )
        }.sorted { $0.minutes > $1.minutes }

        let pieceGroups = Dictionary(grouping: sessions) { $0.pieceTitle }
        let topPieces = pieceGroups.map { title, group in
            let minutes = group.reduce(0) { $0 + $1.durationMinutes }
            return ShareSlice(
                label: title,
                minutes: minutes,
                fraction: min(max(Double(minutes) / Double(totalMinutes), 0), 1)
            )
        }
        .sorted { $0.minutes > $1.minutes }
        .prefix(5)

        return PracticeBreakdownStats(
            byCategory: byCategory,
            byInstrument: byInstrument,
            topPieces: Array(topPieces)
        )
    }

    /// Active repertoire/etudes not practiced in at least `thresholdDays` days,
    /// or never practiced at all.
    static func findRustyPieces(
        pieces: [RepertoirePiece],
        nowEpochMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        thresholdDays: Int = 5
    ) -> [RustyPieceAlert] {
        let oneDayMs: Int64 = 86_400_000

        let alerts: [RustyPieceAlert] = pieces.compactMap { piece in
            guard piece.pieceCategory != .TECHNIQUE || piece.pieceStatus != .MASTERED else { return nil }
            guard let lastEpoch = piece.lastPracticedAtEpochMillis else {
                return RustyPieceAlert(piece: piece, daysSincePracticed: nil)
            }
            let days = max(Int((nowEpochMillis - lastEpoch) / oneDayMs), 0)
            return days >= thresholdDays ? RustyPieceAlert(piece: piece, daysSincePracticed: days) : nil
        }

        return alerts.sorted { lhs, rhs in
            let leftDays = lhs.daysSincePracticed ?? Int.max
            let rightDays = rhs.daysSincePracticed ?? Int.max
            if leftDays != rightDays { return leftDays > rightDays }
            let leftMastered = lhs.piece.pieceStatus == .MASTERED ? 1 : 0
            let rightMastered = rhs.piece.pieceStatus == .MASTERED ? 1 : 0
            return leftMastered < rightMastered
        }
    }

    static func buildBpmProgression(
        pieceId: Int64,
        sectionId: Int64?,
        sessions: [PracticeSession]
    ) -> [BpmHistoryPoint] {
        sessions
            .filter { session in
                session.pieceId == pieceId && (sectionId == nil || session.sectionId == sectionId)
            }
            .sorted { $0.timestampEpochMillis < $1.timestampEpochMillis }
            .map { session in
                BpmHistoryPoint(
                    dateLabel: shortDateFormatter.string(from: date(from: session.timestampEpochMillis)),
                    bpm: session.practicedBpm,
                    measures: session.measuresPracticed,
                    sessionNotes: session.sessionNotes
                )
            }
    }
}
