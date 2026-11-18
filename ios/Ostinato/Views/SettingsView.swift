import SwiftUI
import UniformTypeIdentifiers

/// Preferences, theme selection, custom instruments/tags, teacher reports,
/// CSV export and Android-compatible JSON backup/restore.
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(PreferencesStore.self) private var preferences
    @Environment(StudioViewModel.self) private var studio
    @Environment(\.ostinatoPalette) private var palette

    @State private var statusMessage: String?
    @State private var showAddInstrument = false
    @State private var newInstrument = ""
    @State private var showAddTag = false
    @State private var newTag = ""
    @State private var reportDays = 7

    @State private var jsonDocument: OstinatoBackupDocument?
    @State private var csvDocument: OstinatoBackupDocument?
    @State private var showJsonExporter = false
    @State private var showCsvExporter = false
    @State private var showImporter = false
    @State private var showClearConfirm = false
    @State private var showReseedConfirm = false

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "2.0.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "2"
        return "\(version) (\(build))"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if let statusMessage {
                        OstinatoBanner(message: statusMessage) {
                            self.statusMessage = nil
                        }
                    }
                    themeCard
                    practicePreferencesCard
                    instrumentCard
                    focusTagCard
                    reportsCard
                    backupCard
                    dataCard
                    aboutCard
                }
                .padding(16)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(palette.background.ignoresSafeArea())
            .navigationTitle("Settings & Data")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
        .fileExporter(
            isPresented: $showJsonExporter,
            document: jsonDocument,
            contentType: .json,
            defaultFilename: "ostinato-backup-\(Formatters.backupStamp.string(from: Date()))"
        ) { result in
            switch result {
            case .success:
                statusMessage = "JSON backup exported. Keep it somewhere safe."
            case .failure(let error):
                statusMessage = "Backup export failed: \(error.localizedDescription)"
            }
        }
        .fileExporter(
            isPresented: $showCsvExporter,
            document: csvDocument,
            contentType: .commaSeparatedText,
            defaultFilename: "ostinato-sessions-\(Formatters.backupStamp.string(from: Date()))"
        ) { result in
            switch result {
            case .success:
                statusMessage = "Session CSV exported."
            case .failure(let error):
                statusMessage = "CSV export failed: \(error.localizedDescription)"
            }
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                importBackup(from: url)
            case .failure(let error):
                statusMessage = "Import failed: \(error.localizedDescription)"
            }
        }
    }

    // MARK: - Theme

    private var themeCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("VISUAL MODE")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                Text(
                    OstinatoTheme.subtitle(
                        for: preferences.themeMode,
                        variant: preferences.preferredDarkVariant,
                        systemDark: false
                    )
                )
                .font(.subheadline)
                .foregroundStyle(palette.inkSecondary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(ThemeMode.allCases) { mode in
                            ChipButton(
                                title: mode.shortLabel,
                                isSelected: preferences.themeMode == mode
                            ) {
                                preferences.setThemeMode(mode)
                            }
                        }
                    }
                }

                if preferences.themeMode == .SYSTEM
                    || preferences.themeMode == .NIGHT_STAGE
                    || preferences.themeMode == .NIGHT_OLED {
                    OstinatoSectionLabel(text: "Dark variant")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(DarkVariant.allCases) { variant in
                                ChipButton(
                                    title: variant.shortLabel,
                                    isSelected: preferences.preferredDarkVariant == variant
                                ) {
                                    preferences.setPreferredDarkVariant(variant)
                                }
                            }
                        }
                    }
                }

                Text("The theme button in the toolbar cycles Day → Stage Amber → OLED Pit → System Auto.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }

    // MARK: - Practice preferences

    private var practicePreferencesCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("PRACTICE PREFERENCES")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                Toggle(
                    isOn: Binding(
                        get: { preferences.keepScreenAwake },
                        set: { preferences.keepScreenAwake = $0 }
                    )
                ) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Keep screen awake")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(palette.inkPrimary)
                        Text("Prevents sleep while the stopwatch, metronome or Music Stand is active.")
                            .font(.caption2)
                            .foregroundStyle(palette.inkSecondary)
                    }
                }
                .tint(palette.primary)

                Divider().overlay(palette.outline.opacity(0.4))

                HStack {
                    Text("Weekly goal")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    Text("\(preferences.weeklyGoalMinutes) min")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(palette.primary)
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

                Divider().overlay(palette.outline.opacity(0.4))

                HStack {
                    Text("Global instrument filter")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(palette.inkPrimary)
                    Spacer()
                    Picker(
                        "Global instrument filter",
                        selection: Binding(
                            get: { preferences.globalInstrumentFilter },
                            set: { preferences.globalInstrumentFilter = $0 }
                        )
                    ) {
                        Text("All").tag("All")
                        ForEach(preferences.allInstruments, id: \.self) { instrument in
                            Text(instrument).tag(instrument)
                        }
                    }
                    .labelsHidden()
                    .pickerStyle(.menu)
                }
            }
        }
    }

    // MARK: - Instruments

    private var instrumentCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 9) {
                HStack {
                    Text("INSTRUMENTS")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.primary)
                    Spacer()
                    Button {
                        newInstrument = ""
                        showAddInstrument = true
                    } label: {
                        Label("Add", systemImage: "plus")
                            .font(.caption.weight(.semibold))
                    }
                    .buttonStyle(.bordered)
                    .alert("Add Custom Instrument", isPresented: $showAddInstrument) {
                        TextField("Instrument name", text: $newInstrument)
                        Button("Add") {
                            preferences.addCustomInstrument(newInstrument)
                            newInstrument = ""
                        }
                        Button("Cancel", role: .cancel) {
                            newInstrument = ""
                        }
                    } message: {
                        Text("Custom instruments appear in the Studio, Repertoire and Journal pickers.")
                    }
                }

                FlowLayout(spacing: 6) {
                    ForEach(preferences.allInstruments, id: \.self) { instrument in
                        let isCustom = preferences.customInstruments.contains {
                            $0.caseInsensitiveCompare(instrument) == .orderedSame
                        }
                        HStack(spacing: 4) {
                            Text(instrument)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.inkPrimary)
                            if isCustom {
                                Button {
                                    preferences.removeCustomInstrument(instrument)
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.caption2)
                                        .foregroundStyle(palette.inkSecondary)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Remove \(instrument)")
                            }
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(palette.surfaceVariant.opacity(0.8)))
                    }
                }
            }
        }
    }

    // MARK: - Focus tags

    private var focusTagCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 9) {
                HStack {
                    Text("FOCUS TAGS")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.primary)
                    Spacer()
                    Button {
                        newTag = ""
                        showAddTag = true
                    } label: {
                        Label("Add", systemImage: "plus")
                            .font(.caption.weight(.semibold))
                    }
                    .buttonStyle(.bordered)
                    .alert("Add Custom Focus Tag", isPresented: $showAddTag) {
                        TextField("Tag name", text: $newTag)
                        Button("Add") {
                            preferences.addCustomFocusTag(newTag)
                            newTag = ""
                        }
                        Button("Cancel", role: .cancel) {
                            newTag = ""
                        }
                    } message: {
                        Text("Custom tags appear alongside the built-in focus tags.")
                    }
                }

                FlowLayout(spacing: 6) {
                    ForEach(preferences.allFocusTags, id: \.self) { tag in
                        let isCustom = preferences.customFocusTags.contains {
                            $0.caseInsensitiveCompare(tag) == .orderedSame
                        }
                        HStack(spacing: 4) {
                            Text(tag)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(palette.inkPrimary)
                            if isCustom {
                                Button {
                                    preferences.removeCustomFocusTag(tag)
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.caption2)
                                        .foregroundStyle(palette.inkSecondary)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Remove \(tag)")
                            }
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(palette.surfaceVariant.opacity(0.8)))
                    }
                }
            }
        }
    }

    // MARK: - Reports

    private var reportsCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("TEACHER REPORTS & EXPORT")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                Picker("Report window", selection: $reportDays) {
                    Text("Last 7 days").tag(7)
                    Text("Last 14 days").tag(14)
                    Text("Last 30 days").tag(30)
                }
                .pickerStyle(.segmented)

                ShareLink(item: studio.buildTeacherReportMarkdown(daysWindow: reportDays)) {
                    Label("Share Teacher Report (Markdown)", systemImage: "doc.text.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)

                Button {
                    csvDocument = OstinatoBackupDocument(text: studio.buildSessionsCsv())
                    showCsvExporter = true
                } label: {
                    Label("Export Sessions CSV", systemImage: "tablecells")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Text("Reports include the selected window, weekly streak, active repertoire, tempo progress and full session notes.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }

    // MARK: - Backup

    private var backupCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("JSON BACKUP & RESTORE")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)

                Button {
                    jsonDocument = OstinatoBackupDocument(text: studio.generateBackupJson())
                    showJsonExporter = true
                } label: {
                    Label("Export JSON Backup", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(palette.primary)

                Button {
                    showImporter = true
                } label: {
                    Label("Import JSON Backup", systemImage: "square.and.arrow.down")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Text("Schema v\(BackupManager.backupSchemaVersion) is fully compatible with the Android Ostinato app. Importing replaces all current pieces, movements, sessions and routines.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }

    // MARK: - Data management

    private var dataCard: some View {
        OstinatoCard(
            padding: 16,
            cornerRadius: 18,
            fill: palette.secondaryContainer.opacity(0.35),
            borderColor: palette.secondary.opacity(0.35)
        ) {
            VStack(alignment: .leading, spacing: 10) {
                Text("DATA MANAGEMENT")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.secondary)

                Button {
                    showReseedConfirm = true
                } label: {
                    Label("Reload Demo Repertoire & History", systemImage: "arrow.clockwise")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .alert("Reload demo library?", isPresented: $showReseedConfirm) {
                    Button("Reload Demo Data", role: .destructive) {
                        studio.reloadDemoData()
                        statusMessage = studio.lastSavedBanner
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This clears all current data and restores the built-in sample repertoire, routines and 14 sessions.")
                }

                Button(role: .destructive) {
                    showClearConfirm = true
                } label: {
                    Label("Clear All Practice Data", systemImage: "trash")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(palette.secondary)
                .alert("Clear all data?", isPresented: $showClearConfirm) {
                    Button("Clear Everything", role: .destructive) {
                        studio.clearAllData()
                        statusMessage = studio.lastSavedBanner
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Every piece, movement, session and routine will be permanently deleted. Export a backup first if you might want this data later.")
                }
            }
        }
    }

    // MARK: - About

    private var aboutCard: some View {
        OstinatoCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 5) {
                Text("OSTINATO")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)
                Text("Digital Practice Log for Musicians")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(palette.inkPrimary)
                Text("Version \(appVersion) • Native SwiftUI for iPhone & iPad")
                    .font(.caption)
                    .foregroundStyle(palette.inkSecondary)
                Text("A deliberate-practice studio: stopwatch, drift-compensated metronome, repertoire continuity, routines, journal, heatmaps and tempo tracking.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }

    // MARK: - Import

    private func importBackup(from url: URL) {
        let didAccess = url.startAccessingSecurityScopedResource()
        defer {
            if didAccess { url.stopAccessingSecurityScopedResource() }
        }

        do {
            let data = try Data(contentsOf: url)
            guard let text = String(data: data, encoding: .utf8) else {
                statusMessage = "Import failed: the file is not UTF-8 text."
                return
            }
            switch studio.restoreFromBackupJson(text) {
            case .success(let message):
                statusMessage = message
            case .failure(let error):
                statusMessage = "Import failed: \(error.localizedDescription)"
            }
        } catch {
            statusMessage = "Import failed: \(error.localizedDescription)"
        }
    }
}

// MARK: - File document

/// Simple UTF-8 text document used for both JSON backups and CSV exports.
struct OstinatoBackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .commaSeparatedText, .plainText] }

    var text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        guard
            let data = configuration.file.regularFileContents,
            let string = String(data: data, encoding: .utf8)
        else {
            throw CocoaError(.fileReadCorruptFile)
        }
        text = string
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
