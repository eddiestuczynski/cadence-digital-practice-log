import SwiftUI
import UIKit

/// Distraction-free, large-type performance view designed to be read from a
/// music stand. Mirrors the Android `MusicStandOverlay`: giant clock, silent
/// target progress, live beat circles, tempo controls and pinned cues.
struct MusicStandView: View {
    @Environment(StudioViewModel.self) private var studio
    @Environment(PreferencesStore.self) private var preferences
    @Environment(\.cadencePalette) private var palette

    let pinnedNextGoal: String
    let standingScoreNotes: String
    let previousSessionSummary: String

    @State private var quickNote = ""

    private var targetProgress: Double {
        guard studio.targetDurationMinutes > 0 else { return 0 }
        let targetSeconds = max(studio.targetDurationMinutes * 60, 1)
        return min(max(Double(studio.elapsedSeconds) / Double(targetSeconds), 0), 1)
    }

    private var titleLine: String {
        if studio.sectionName.isEmpty { return studio.customPieceTitle }
        return "\(studio.customPieceTitle) — \(studio.sectionName)"
    }

    var body: some View {
        ZStack {
            palette.background.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header
                    clockBlock
                    if studio.targetDurationMinutes > 0 { targetBlock }
                    beatBlock
                    tempoBlock
                    if !pinnedNextGoal.isEmpty || !standingScoreNotes.isEmpty || !previousSessionSummary.isEmpty {
                        cuesBlock
                    }
                    quickNotesBlock
                    actionBlock
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 18)
                .frame(maxWidth: 900)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onAppear { updateIdleTimer() }
        .onDisappear { updateIdleTimer() }
    }

    // MARK: - Header

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text("MUSIC STAND MODE")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.primary)
                Text(titleLine.isEmpty ? "Practice Session" : titleLine)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(palette.inkPrimary)
                    .lineLimit(2)
                Text("\(studio.selectedInstrument) • \(studio.beatsPerMeasure)/\(studio.noteValuePerBeat) • \(MetronomeEngine.tempoMarking(forBpm: studio.metronomeBpm))")
                    .font(.caption)
                    .foregroundStyle(palette.inkSecondary)
            }
            Spacer()
            Button {
                studio.setMusicStandModeOpen(false)
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.title2)
                    .foregroundStyle(palette.inkSecondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Exit music stand mode")
        }
    }

    // MARK: - Clock

    private var clockBlock: some View {
        CadenceCard(padding: 20, cornerRadius: 22) {
            VStack(spacing: 8) {
                Text(studio.timerRunning ? "SESSION IN PROGRESS" : "STOPWATCH PAUSED")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(studio.timerRunning ? palette.primary : palette.inkSecondary)

                Text(Formatters.clock(studio.elapsedSeconds))
                    .font(.system(size: 76, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(palette.inkPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.4)
                    .frame(maxWidth: .infinity)

                HStack(spacing: 10) {
                    Button {
                        studio.toggleTimer()
                    } label: {
                        Label(
                            studio.timerRunning ? "Pause" : "Start",
                            systemImage: studio.timerRunning ? "pause.fill" : "play.fill"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)
                    .controlSize(.large)

                    Button {
                        studio.resetTimer()
                    } label: {
                        Label("Reset", systemImage: "arrow.counterclockwise")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.large)
                }
            }
        }
    }

    private var targetBlock: some View {
        CadenceCard(padding: 16, cornerRadius: 18) {
            VStack(spacing: 7) {
                HStack {
                    Text("SILENT TARGET")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.inkSecondary)
                    Spacer()
                    Text("\(studio.targetDurationMinutes) min • \(Int(targetProgress * 100))%")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(targetProgress >= 1 ? palette.tertiary : palette.primary)
                }
                CadenceProgressBar(
                    value: targetProgress,
                    color: targetProgress >= 1 ? palette.tertiary : palette.primary,
                    height: 12
                )
            }
        }
    }

    // MARK: - Beats

    private var beatBlock: some View {
        CadenceCard(padding: 16, cornerRadius: 18) {
            VStack(spacing: 12) {
                HStack {
                    Text("BEAT PULSE")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(palette.inkSecondary)
                    Spacer()
                    Button {
                        studio.toggleMetronomeMute()
                    } label: {
                        Label(
                            studio.metronomeMuted ? "Muted" : "Audible",
                            systemImage: studio.metronomeMuted ? "speaker.slash.fill" : "speaker.wave.2.fill"
                        )
                        .font(.caption.weight(.semibold))
                    }
                    .buttonStyle(.bordered)
                }

                HStack(spacing: 10) {
                    ForEach(studio.beatPattern.indices, id: \.self) { index in
                        beatDot(beat: index + 1, level: studio.beatPattern[index])
                    }
                }
                .frame(maxWidth: .infinity)
                .animation(.easeOut(duration: 0.08), value: studio.metronome.currentBeat)

                Text("Tap a beat to cycle Accent → Normal → Silent. The visual pulse keeps running when muted.")
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func beatDot(beat: Int, level: BeatAccentLevel) -> some View {
        let isActive = studio.metronome.isPlaying && studio.metronome.currentBeat == beat
        let diameter: CGFloat = isActive ? 52 : 44

        let fill: Color = {
            if isActive && level == .ACCENT { return palette.primary }
            if isActive && level == .NORMAL { return palette.secondary }
            if isActive && level == .MUTE { return palette.surfaceVariant }
            if level == .MUTE { return .clear }
            return palette.surfaceVariant.opacity(0.8)
        }()

        let textColor: Color = {
            if isActive && level != .MUTE { return palette.onPrimary }
            return palette.inkSecondary
        }()

        return Button {
            studio.cycleBeatAccent(beatOneBased: beat)
        } label: {
            Text(level == .MUTE ? "–" : "\(beat)")
                .font(.headline.weight(.bold))
                .foregroundStyle(textColor)
                .frame(width: diameter, height: diameter)
                .background(Circle().fill(fill))
                .overlay(
                    Circle().stroke(
                        level == .ACCENT ? palette.primary : palette.outline,
                        lineWidth: level == .ACCENT ? 2.5 : 1
                    )
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Tempo

    private var tempoBlock: some View {
        CadenceCard(padding: 16, cornerRadius: 18) {
            VStack(spacing: 12) {
                HStack(spacing: 14) {
                    Button {
                        studio.setMetronomeBpm(studio.metronomeBpm - 5)
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.title)
                            .foregroundStyle(palette.inkSecondary)
                    }
                    .buttonStyle(.plain)

                    VStack(spacing: 0) {
                        Text("\(studio.metronomeBpm)")
                            .font(.system(size: 58, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(palette.inkPrimary)
                        Text("BPM • Target \(studio.targetBpm)")
                            .font(.caption)
                            .foregroundStyle(palette.inkSecondary)
                    }
                    .frame(maxWidth: .infinity)

                    Button {
                        studio.setMetronomeBpm(studio.metronomeBpm + 5)
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title)
                            .foregroundStyle(palette.inkSecondary)
                    }
                    .buttonStyle(.plain)
                }

                Slider(
                    value: Binding(
                        get: { Double(studio.metronomeBpm) },
                        set: { studio.setMetronomeBpm(Int($0.rounded())) }
                    ),
                    in: 30...240
                )
                .tint(palette.primary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(SubdivisionMode.allCases) { mode in
                            ChipButton(
                                title: mode.label,
                                isSelected: studio.subdivisionMode == mode
                            ) {
                                studio.setSubdivisionMode(mode)
                            }
                        }
                    }
                }

                Button {
                    studio.toggleMetronomePlayback()
                } label: {
                    Label(
                        studio.metronome.isPlaying ? "Stop Metronome" : "Start Metronome",
                        systemImage: studio.metronome.isPlaying ? "stop.fill" : "play.fill"
                    )
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(studio.metronome.isPlaying ? palette.secondary : palette.primary)
                .controlSize(.large)
            }
        }
    }

    // MARK: - Cues

    private var cuesBlock: some View {
        CadenceCard(
            padding: 16,
            cornerRadius: 18,
            fill: palette.primaryContainer.opacity(0.45),
            borderColor: palette.primary.opacity(0.4)
        ) {
            VStack(alignment: .leading, spacing: 10) {
                if !pinnedNextGoal.isEmpty {
                    VStack(alignment: .leading, spacing: 3) {
                        Label("PINNED GOAL FOR TODAY", systemImage: "flag.fill")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.primary)
                        Text(pinnedNextGoal)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(palette.inkPrimary)
                    }
                }
                if !standingScoreNotes.isEmpty {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("SCORE NOTES")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.inkSecondary)
                        Text(standingScoreNotes)
                            .font(.subheadline)
                            .foregroundStyle(palette.inkPrimary)
                    }
                }
                if !previousSessionSummary.isEmpty {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("LAST SESSION")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(palette.inkSecondary)
                        Text(previousSessionSummary)
                            .font(.subheadline)
                            .foregroundStyle(palette.inkPrimary)
                    }
                }
            }
        }
    }

    // MARK: - Quick notes

    private var quickNotesBlock: some View {
        CadenceCard(padding: 16, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 10) {
                Text("QUICK NOTE")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(palette.inkSecondary)

                HStack(spacing: 8) {
                    TextField("Jot a fingering, tempo or breakthrough…", text: $quickNote, axis: .vertical)
                        .lineLimit(1...3)
                        .padding(10)
                        .background(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(palette.surfaceVariant.opacity(0.65))
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .stroke(palette.outline.opacity(0.5), lineWidth: 1)
                        )

                    Button {
                        let clean = quickNote.trimmed
                        guard !clean.isEmpty else { return }
                        studio.appendNoteTemplate(clean)
                        quickNote = ""
                    } label: {
                        Image(systemName: "plus")
                            .font(.headline)
                            .padding(12)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(palette.primary)
                }

                if !studio.sessionNotes.isEmpty {
                    Text(studio.sessionNotes)
                        .font(.caption)
                        .foregroundStyle(palette.inkSecondary)
                        .lineLimit(4)
                }
            }
        }
    }

    // MARK: - Actions

    private var actionBlock: some View {
        VStack(spacing: 10) {
            Button {
                studio.saveCurrentPracticeSession()
                studio.setMusicStandModeOpen(false)
            } label: {
                Label("Finish & Log Session", systemImage: "square.and.arrow.down.fill")
                    .font(.title3.weight(.bold))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(palette.primary)
            .controlSize(.large)

            Button {
                studio.setMusicStandModeOpen(false)
            } label: {
                Label("Back to Studio (Keep Practicing)", systemImage: "chevron.down")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
        }
    }

    // MARK: - Idle timer

    private func updateIdleTimer() {
        UIApplication.shared.isIdleTimerDisabled =
            preferences.keepScreenAwake && (studio.timerRunning || studio.metronome.isPlaying)
    }
}
