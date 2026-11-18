import SwiftUI

enum OstinatoTab: String, CaseIterable, Identifiable {
    case studio
    case journal
    case repertoire
    case analytics

    var id: String { rawValue }

    var label: String {
        switch self {
        case .studio: return "Studio"
        case .journal: return "Journal"
        case .repertoire: return "Repertoire"
        case .analytics: return "Stats & BPM"
        }
    }

    var shortLabel: String {
        switch self {
        case .studio: return "Studio"
        case .journal: return "Journal"
        case .repertoire: return "Repertoire"
        case .analytics: return "Stats"
        }
    }

    var systemImage: String {
        switch self {
        case .studio: return "play.circle.fill"
        case .journal: return "clock.arrow.circlepath"
        case .repertoire: return "music.note.list"
        case .analytics: return "chart.bar.xaxis"
        }
    }
}

struct RootView: View {
    @Environment(OstinatoStore.self) private var store
    @Environment(PreferencesStore.self) private var preferences
    @Environment(StudioViewModel.self) private var studio
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    @State private var selectedTab: OstinatoTab = .studio
    @State private var showSettings = false

    private var palette: OstinatoPalette {
        OstinatoTheme.palette(
            for: preferences.themeMode,
            variant: preferences.preferredDarkVariant,
            systemDark: colorScheme == .dark
        )
    }

    private var resolvedColorScheme: ColorScheme? {
        switch preferences.themeMode {
        case .DAY: return .light
        case .NIGHT_STAGE, .NIGHT_OLED: return .dark
        case .SYSTEM: return nil
        }
    }

    private var filteredPieces: [PieceWithSections] {
        let filter = preferences.globalInstrumentFilter
        guard filter != "All" else { return store.piecesWithSections }
        return store.piecesWithSections.filter {
            $0.piece.instrument.caseInsensitiveCompare(filter) == .orderedSame
        }
    }

    private var filteredSessions: [PracticeSession] {
        let filter = preferences.globalInstrumentFilter
        guard filter != "All" else { return store.sessions }
        return store.sessions.filter {
            $0.instrument.caseInsensitiveCompare(filter) == .orderedSame
        }
    }

    private var weeklyStreak: Int {
        AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions: filteredSessions,
            weeklyGoalMinutes: preferences.weeklyGoalMinutes
        )
    }

    var body: some View {
        Group {
            if horizontalSizeClass == .regular {
                NavigationSplitView {
                    List {
                        Section("Ostinato") {
                            ForEach(OstinatoTab.allCases) { tab in
                                Button {
                                    selectedTab = tab
                                } label: {
                                    HStack(spacing: 10) {
                                        Label(tab.label, systemImage: tab.systemImage)
                                        Spacer(minLength: 0)
                                        if selectedTab == tab {
                                            Image(systemName: "checkmark")
                                                .font(.caption.weight(.bold))
                                                .foregroundStyle(palette.primary)
                                        }
                                    }
                                    .foregroundStyle(selectedTab == tab ? palette.primary : palette.inkPrimary)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .navigationTitle("Ostinato")
                } detail: {
                    NavigationStack {
                        tabContent(selectedTab)
                            .background(palette.background.ignoresSafeArea())
                            .navigationTitle(selectedTab.label)
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar { toolbarContent }
                    }
                }
            } else {
                TabView(selection: $selectedTab) {
                    ForEach(OstinatoTab.allCases) { tab in
                        NavigationStack {
                            tabContent(tab)
                                .background(palette.background.ignoresSafeArea())
                                .navigationTitle("Ostinato")
                                .navigationBarTitleDisplayMode(.inline)
                                .toolbar { toolbarContent }
                        }
                        .tabItem {
                            Label(tab.shortLabel, systemImage: tab.systemImage)
                        }
                        .tag(tab)
                    }
                }
            }
        }
        .tint(palette.primary)
        .preferredColorScheme(resolvedColorScheme)
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
        .task {
            studio.selectInitialPieceIfNeeded()
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            Button {
                preferences.cycleVisualMode()
            } label: {
                Image(systemName: preferences.themeMode.systemImage)
            }
            .accessibilityLabel("Cycle visual mode")
        }

        ToolbarItem(placement: .topBarTrailing) {
            HStack(spacing: 10) {
                OstinatoPill(
                    text: "\(weeklyStreak)w streak",
                    foreground: palette.onSecondaryContainer,
                    background: palette.secondaryContainer
                )
                Button {
                    showSettings = true
                } label: {
                    Image(systemName: "gearshape.fill")
                }
                .accessibilityLabel("Settings, export and backup")
            }
        }
    }

    @ViewBuilder
    private func tabContent(_ tab: OstinatoTab) -> some View {
        switch tab {
        case .studio:
            StudioView(
                piecesWithSections: filteredPieces.isEmpty ? store.piecesWithSections : filteredPieces,
                sessions: store.sessions,
                routines: store.routinesWithItems,
                allInstruments: preferences.allInstruments,
                allFocusTags: preferences.allFocusTags,
                keepScreenAwakeEnabled: preferences.keepScreenAwake
            )

        case .journal:
            JournalView(
                sessions: filteredSessions,
                piecesWithSections: store.piecesWithSections,
                allInstruments: preferences.allInstruments,
                allFocusTags: preferences.allFocusTags
            )

        case .repertoire:
            RepertoireView(
                piecesWithSections: filteredPieces,
                allInstruments: preferences.allInstruments,
                onPracticeNow: practiceNow
            )

        case .analytics:
            AnalyticsView(
                sessions: filteredSessions,
                piecesWithSections: filteredPieces,
                weeklyGoalMinutes: preferences.weeklyGoalMinutes,
                onPracticeNow: practiceNow
            )
        }
    }

    private func practiceNow(_ piece: RepertoirePiece, _ section: PieceSection?) {
        studio.selectPieceForPractice(piece, section: section)
        selectedTab = .studio
    }
}
