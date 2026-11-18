import SwiftUI

/// Practice analytics: weekly goal + streak, monthly heatmap with day
/// drill-down, category/instrument breakdown, last-seven-day bars and a
/// tempo-progression chart for any piece or movement.
struct AnalyticsView: View {
    @Environment(PreferencesStore.self) private var preferences
    @Environment(\.ostinatoPalette) private var palette

    let sessions: [PracticeSession]
    let piecesWithSections: [PieceWithSections]
    let weeklyGoalMinutes: Int
    let onPracticeNow: (RepertoirePiece, PieceSection?) -> Void

    @State private var monthAnchor = Date()
    @State private var selectedDateKey: String?
    @State private var selectedPieceId: Int64?
    @State private var selectedSectionId: Int64?

    // MARK: - Derived values

    private var totalMinutes: Int {
        sessions.reduce(0) { $0 + $1.durationMinutes }
    }

    private var currentWeekMinutes: Int {
        AnalyticsCalculator.calculateCurrentWeekMinutes(sessions: sessions)
    }

    private var currentGoal: Int {
        max(preferences.weeklyGoalMinutes, 15)
    }

    private var goalProgress: Double {
        min(max(Double(currentWeekMinutes) / Double(currentGoal), 0), 1)
    }

    private var streak: Int {
        AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions: sessions,
            weeklyGoalMinutes: currentGoal
        )
    }

    private var rustyPieces: [RustyPieceAlert] {
        AnalyticsCalculator.findRustyPieces(pieces: piecesWithSections.map(\.piece))
    }

    private var breakdown: PracticeBreakdownStats {
        AnalyticsCalculator.calculateBreakdownStats(sessions: sessions)
    }

    private var monthComponents: (year: Int, monthZeroBased: Int) {
        let calendar = AnalyticsCalculator.calendar
        return (
            calendar.component(.year, from: monthAnchor),
            calendar.component(.month, from: monthAnchor) - 1
        )
    }

    private var heatmapCells: [HeatmapDayCell] {
        let components = monthComponents
        return AnalyticsCalculator.buildMonthlyHeatmapCells(
            sessions: sessions,
            year: components.year,
            monthZeroBased: components.monthZeroBased
        )
    }

    private var selectedDaySessions: [PracticeSession] {
        guard let selectedDateKey else { return [] }
        return sessions.filter {
            AnalyticsCalculator.dateKey(for: $0.timestampEpochMillis) == selectedDateKey
        }
    }

    private var selectedDateLabel: String {
        guard
            let selectedDateKey,
            let date = AnalyticsCalculator.dateKeyFormatter.date(from: selectedDateKey)
        else { return "" }
        return AnalyticsCalculator.shortDateFormatter.string(from: date)
    }

    private var selectedPieceWithSections: PieceWithSections? {
        guard let selectedPieceId else { return nil }
        return piecesWithSections.first { $0.piece.id == selectedPieceId }
    }

    private var selectedPieceTitle: String {
        selectedPieceWithSections?.piece.title ?? "Select a piece"
    }

    private var bpmPoints: [BpmHistoryPoint] {
        guard let selectedPieceId else { return [] }
        return AnalyticsCalculator.buildBpmProgression(
            pieceId: selectedPieceId,
            sectionId: selectedSectionId,
            sessions: sessions
        )
    }

    // MARK: - Body

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                summaryGrid
                weeklyGoalCard
                if !rustyPieces.isEmpty { rustyCard }
                heatmapCard
                if selectedDateKey != nil { dayDrilldownCard }
                breakdownCard
                last7DaysCard
                bpmCard
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(palette.background.ignoresSafeArea())
        .onAppear {
            if selectedPieceId == nil {
                selectedPieceId = piecesWithSections.first?.piece.id
            }
        }
    }

    // MARK: - Summary

    private var summaryGrid: some View {
        LazyVGrid(
            columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)],
            spacing: 10
        ) {
            MetricSummaryCard(
                title: "This Week",
                value: Formatters.hoursAndMinutes(currentWeekMinutes),
                subtitle: "Goal \(currentGoal) min",
                systemImage: "calendar",
                tint: palette.primary
            )
            MetricSummaryCard(
                title: "Weekly Streak",
                value: "\(streak)w",
                subtitle: streak == 1 ? "goal-met week" : "goal-met weeks",
                systemImage: "flame.fill",
                tint: palette.secondary
            )
            MetricSummaryCard(
                title: "Total Practice",
                value: Formatters.hoursAndMinutes(totalMinutes),
                subtitle: "all recorded time",
                systemImage: "clock.fill",
                tint: palette.tertiary
            )
            MetricSummaryCard(
                title: "Sessions",
                value: "\(sessions.count)",
                subtitle: sessions.isEmpty ? "none logged yet" : "in your journal",
                systemImage: "list.bullet.rectangle",
                tint: palette.secondary
            )
        }
    }

    // MARK: - Weekly goal

    private var weeklyGoalCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 9) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("WEEKLY CONSISTENCY GOAL")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.primary)
                        Text("\(currentWeekMinutes) / \(currentGoal) min this week")
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                    }
                    Spacer()
                    Text("\(Int(goalProgress * 100))%")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(goalProgress >= 1 ? palette.tertiary : palette.primary)
                }

                OstinatoProgressBar(
                    value: goalProgress,
                    color: goalProgress >= 1 ? palette.tertiary : palette.primary,
                    height: 10
                )

                HStack {
                    Text("Adjust weekly target")
                        .font(.caption)
                        .foregroundStyle(palette.inkSecondary)
                    Spacer()
                    Stepper(
                        "",
                        value: Binding(
                            get: { preferences.weeklyGoalMinutes },
                            set: { preferences.setWeeklyGoalMinutes($0) }
                        ),
                        in: 15...2400,
                        step: 15
                    )
                    .labelsHidden()
                }
            }
        }
    }

    // MARK: - Rusty pieces

    private var rustyCard: some View {
        OstinatoCard(
            padding: 16,
            cornerRadius: 18,
            fill: palette.secondaryContainer.opacity(0.45),
            borderColor: palette.secondary.opacity(0.4)
        ) {
            VStack(alignment: .leading, spacing: 9) {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.caption)
                        .foregroundStyle(palette.secondary)
                    Text("NEEDS ATTENTION")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.onSecondaryContainer)
                }

                ForEach(rustyPieces.prefix(5)) { alert in
                    HStack(alignment: .center, spacing: 8) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(alert.piece.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(palette.inkPrimary)
                                .lineLimit(2)
                            Text(
                                alert.daysSincePracticed.map { "\($0) days since last practice" }
                                    ?? "Never practiced yet"
                            )
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                        }
                        Spacer(minLength: 4)
                        Button {
                            onPracticeNow(alert.piece, nil)
                        } label: {
                            Text("Practice")
                                .font(.caption.weight(.semibold))
                        }
                        .buttonStyle(.bordered)
                        .tint(palette.secondary)
                    }
                }

                Text("Pieces not practiced in 5+ days, sorted by longest gap.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }

    // MARK: - Heatmap

    private var heatmapCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("PRACTICE HEATMAP")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.primary)
                        Text(AnalyticsCalculator.monthTitleFormatter.string(from: monthAnchor))
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                    }
                    Spacer()
                    HStack(spacing: 4) {
                        Button {
                            shiftMonth(-1)
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .buttonStyle(.bordered)
                        .accessibilityLabel("Previous month")

                        Button("Today") {
                            monthAnchor = Date()
                            selectedDateKey = nil
                        }
                        .buttonStyle(.bordered)

                        Button {
                            shiftMonth(1)
                        } label: {
                            Image(systemName: "chevron.right")
                        }
                        .buttonStyle(.bordered)
                        .accessibilityLabel("Next month")
                    }
                }

                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7),
                    spacing: 4
                ) {
                    ForEach(["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"], id: \.self) { day in
                        Text(day)
                            .font(.system(size: 9, weight: .semibold))
                            .foregroundStyle(palette.inkSecondary)
                            .frame(maxWidth: .infinity)
                    }

                    ForEach(heatmapCells) { cell in
                        heatmapCell(cell)
                    }
                }

                HStack(spacing: 6) {
                    Text("Less")
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                    ForEach([0.15, 0.35, 0.55, 0.75, 0.95], id: \.self) { opacity in
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(palette.primary.opacity(opacity))
                            .frame(width: 14, height: 14)
                    }
                    Text("More")
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                    Spacer()
                    Text("Tap a day for details")
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                }
            }
        }
    }

    private func heatmapCell(_ cell: HeatmapDayCell) -> some View {
        let isSelected = selectedDateKey == cell.dateKey
        let fill: Color = {
            guard cell.isCurrentMonth else { return .clear }
            if cell.totalMinutes == 0 { return palette.surfaceVariant.opacity(0.35) }
            let level = min(Double(cell.totalMinutes) / 120.0, 1.0)
            return palette.primary.opacity(0.18 + level * 0.72)
        }()

        let textColor: Color = {
            guard cell.isCurrentMonth else { return palette.inkSecondary.opacity(0.35) }
            return cell.totalMinutes > 45 ? .white : palette.inkPrimary
        }()

        return Button {
            guard cell.isCurrentMonth else { return }
            selectedDateKey = isSelected ? nil : cell.dateKey
        } label: {
            VStack(spacing: 1) {
                Text("\(cell.dayOfMonth)")
                    .font(.system(size: 11, weight: cell.isToday ? .bold : .medium))
                    .foregroundStyle(textColor)
                if cell.totalMinutes > 0 {
                    Text("\(cell.totalMinutes)m")
                        .font(.system(size: 8, weight: .semibold))
                        .foregroundStyle(textColor.opacity(0.85))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 36)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(fill)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(
                        isSelected ? palette.inkPrimary : (cell.isToday ? palette.secondary : .clear),
                        lineWidth: isSelected ? 2 : 1.5
                    )
            )
        }
        .buttonStyle(.plain)
        .disabled(!cell.isCurrentMonth)
    }

    // MARK: - Day drill-down

    private var dayDrilldownCard: some View {
        OstinatoCard(
            padding: 16,
            cornerRadius: 18,
            fill: palette.surfaceVariant.opacity(0.55)
        ) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Sessions on \(selectedDateLabel)")
                        .font(.headline)
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    Button {
                        selectedDateKey = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(palette.inkSecondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close day details")
                }

                if selectedDaySessions.isEmpty {
                    Text("No practice logged on this day.")
                        .font(.subheadline)
                        .foregroundStyle(palette.inkSecondary)
                } else {
                    ForEach(selectedDaySessions) { session in
                        VStack(alignment: .leading, spacing: 3) {
                            HStack {
                                Text(session.pieceTitle)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(palette.inkPrimary)
                                Spacer()
                                Text("\(session.durationMinutes)m @ \(session.practicedBpm)")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(palette.primary)
                            }
                            if !session.sectionName.isEmpty {
                                Text(session.sectionName)
                                    .font(.caption)
                                    .foregroundStyle(palette.secondary)
                            }
                            if !session.sessionNotes.isEmpty {
                                Text(session.sessionNotes)
                                    .font(.caption)
                                    .foregroundStyle(palette.inkSecondary)
                                    .lineLimit(3)
                            }
                        }
                        .padding(9)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(palette.surface)
                        )
                    }
                }
            }
        }
    }

    // MARK: - Breakdown

    private var breakdownCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 12) {
                Text("WHERE YOUR TIME GOES")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                breakdownSection("By Category", slices: breakdown.byCategory)
                Divider().overlay(palette.outline.opacity(0.4))
                breakdownSection("By Instrument", slices: breakdown.byInstrument)
                Divider().overlay(palette.outline.opacity(0.4))
                breakdownSection("Top Pieces", slices: breakdown.topPieces)
            }
        }
    }

    @ViewBuilder
    private func breakdownSection(_ title: String, slices: [ShareSlice]) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            OstinatoSectionLabel(text: title)
            if slices.isEmpty || slices.allSatisfy({ $0.minutes == 0 }) {
                Text("No data yet")
                    .font(.caption)
                    .foregroundStyle(palette.inkSecondary)
            } else {
                ForEach(slices.filter { $0.minutes > 0 }) { slice in
                    VStack(spacing: 3) {
                        HStack {
                            Text(slice.label)
                                .font(.caption)
                                .foregroundStyle(palette.inkPrimary)
                                .lineLimit(1)
                            Spacer()
                            Text(Formatters.hoursAndMinutes(slice.minutes))
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(palette.inkSecondary)
                        }
                        OstinatoProgressBar(value: slice.fraction, height: 6)
                    }
                }
            }
        }
    }

    // MARK: - Last 7 days

    private var last7DaysCard: some View {
        let stats = AnalyticsCalculator.calculateLast7DaysStats(sessions: sessions)
        let maxMinutes = max(stats.map(\.minutesPracticed).max() ?? 0, 1)

        return OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("LAST 7 DAYS")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(stats) { stat in
                        VStack(spacing: 5) {
                            Text(stat.minutesPracticed > 0 ? "\(stat.minutesPracticed)" : "")
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundStyle(palette.inkSecondary)
                            RoundedRectangle(cornerRadius: 5, style: .continuous)
                                .fill(stat.minutesPracticed > 0 ? palette.primary : palette.surfaceVariant)
                                .frame(
                                    height: max(
                                        6,
                                        80 * CGFloat(stat.minutesPracticed) / CGFloat(maxMinutes)
                                    )
                                )
                            Text(stat.dayLabel)
                                .font(.caption2)
                                .fontWeight(stat.isToday ? .bold : .regular)
                                .foregroundStyle(stat.isToday ? palette.primary : palette.inkSecondary)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .frame(height: 125, alignment: .bottom)
            }
        }
    }

    // MARK: - BPM progression

    private var bpmCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("TEMPO PROGRESSION")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.primary)
                        Text(selectedPieceTitle)
                            .font(.headline)
                            .foregroundStyle(palette.inkPrimary)
                            .lineLimit(2)
                    }
                    Spacer(minLength: 8)
                    Menu {
                        Button("Select a piece") {
                            selectPiece(nil)
                        }
                        ForEach(piecesWithSections) { pws in
                            Button(pws.piece.title) {
                                selectPiece(pws.piece)
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Text("Choose")
                                .font(.caption.weight(.semibold))
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.caption2)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(palette.surfaceVariant.opacity(0.8)))
                    }
                }

                if let pws = selectedPieceWithSections, !pws.sortedSections.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ChipButton(
                                title: "Full Piece",
                                isSelected: selectedSectionId == nil
                            ) {
                                selectedSectionId = nil
                            }
                            ForEach(pws.sortedSections) { section in
                                ChipButton(
                                    title: section.name,
                                    isSelected: selectedSectionId == section.id
                                ) {
                                    selectedSectionId = section.id
                                }
                            }
                        }
                    }
                }

                if bpmPoints.isEmpty {
                    VStack(spacing: 6) {
                        Image(systemName: "chart.line.uptrend.xyaxis")
                            .font(.title2)
                            .foregroundStyle(palette.outline)
                        Text("No tempo history yet for this selection. Log a session to start the curve.")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
                } else {
                    BpmProgressionChart(points: bpmPoints)
                        .frame(height: 160)

                    HStack {
                        Text("\(bpmPoints.count) session\(bpmPoints.count == 1 ? "" : "s")")
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                        Spacer()
                        if let first = bpmPoints.first, let last = bpmPoints.last {
                            Text("\(first.bpm) → \(last.bpm) BPM")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.primary)
                        }
                    }

                    if let latest = bpmPoints.last, !latest.sessionNotes.isEmpty {
                        Text("Latest: \(latest.sessionNotes)")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                            .lineLimit(3)
                    }
                }
            }
        }
    }

    // MARK: - Actions

    private func shiftMonth(_ delta: Int) {
        if let shifted = AnalyticsCalculator.calendar.date(
            byAdding: .month,
            value: delta,
            to: monthAnchor
        ) {
            monthAnchor = shifted
            selectedDateKey = nil
        }
    }

    private func selectPiece(_ piece: RepertoirePiece?) {
        selectedPieceId = piece?.id
        selectedSectionId = nil
    }
}

// MARK: - Chart geometry

private struct ChartGeometry {
    let points: [BpmHistoryPoint]
    let size: CGSize

    var minBpm: Int { points.map(\.bpm).min() ?? 0 }
    var maxBpm: Int { points.map(\.bpm).max() ?? 1 }
    var span: Int { max(maxBpm - minBpm, 1) }

    var positions: [CGPoint] {
        guard !points.isEmpty else { return [] }
        let topPadding: CGFloat = 10
        let bottomPadding: CGFloat = 10
        let usableHeight = max(size.height - topPadding - bottomPadding, 1)
        let stepX = points.count > 1 ? size.width / CGFloat(points.count - 1) : size.width
        return points.indices.map { index in
            let x = points.count > 1 ? CGFloat(index) * stepX : size.width / 2
            let ratio = CGFloat(points[index].bpm - minBpm) / CGFloat(span)
            let y = topPadding + (1 - ratio) * usableHeight
            return CGPoint(x: x, y: y)
        }
    }
}

private struct BpmProgressionChart: View {
    @Environment(\.ostinatoPalette) private var palette

    let points: [BpmHistoryPoint]

    var body: some View {
        GeometryReader { geometry in
            let layout = ChartGeometry(points: points, size: geometry.size)

            ZStack(alignment: .topLeading) {
                ForEach(0..<4, id: \.self) { line in
                    let y = 10 + (max(geometry.size.height - 20, 1) * CGFloat(line) / 3)
                    Path { path in
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addLine(to: CGPoint(x: geometry.size.width, y: y))
                    }
                    .stroke(palette.outline.opacity(0.25), lineWidth: 1)
                }

                if layout.positions.count > 1 {
                    Path { path in
                        for (index, position) in layout.positions.enumerated() {
                            if index == 0 {
                                path.move(to: position)
                            } else {
                                path.addLine(to: position)
                            }
                        }
                    }
                    .stroke(
                        palette.primary,
                        style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round)
                    )
                }

                ForEach(layout.positions.indices, id: \.self) { index in
                    Circle()
                        .fill(palette.primary)
                        .frame(width: 7, height: 7)
                        .position(layout.positions[index])
                }
            }
        }
    }
}
