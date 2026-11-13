import AVFoundation
import Darwin
import Foundation
import Observation

/// Drift-compensated metronome built on `AVAudioEngine`.
///
/// Clicks are synthesized once into reusable `AVAudioPCMBuffer`s and scheduled
/// against the audio hardware's host clock, which is the iOS equivalent of the
/// Android engine's static `AudioTrack` buffers. Visual beat state is published
/// even when the click is muted, so players with loud instruments can follow the
/// pulse on screen.
@MainActor
@Observable
final class MetronomeEngine {

    // MARK: - Observable state

    private(set) var isPlaying = false
    private(set) var currentBeat = 0
    private(set) var currentSubdivision = 0
    /// Increments on every downbeat so views can trigger a flash reliably.
    private(set) var beatPulseID: Int64 = 0

    // MARK: - Audio plumbing

    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let bufferFormat: AVAudioFormat

    private var accentBuffer: AVAudioPCMBuffer?
    private var regularBuffer: AVAudioPCMBuffer?
    private var subdivisionBuffer: AVAudioPCMBuffer?

    private var audioConfigured = false
    private var loopTask: Task<Void, Never>?
    private var generation = 0

    // MARK: - Configuration

    private var bpm = 100
    private var beatsPerMeasure = 4
    private var beatPattern: [BeatAccentLevel] = MetronomeEngine.defaultBeatPattern(4)
    private var subdivisionMode: SubdivisionMode = .QUARTER
    private var isMuted = false

    init() {
        guard let format = AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 1) else {
            fatalError("Unable to create metronome audio format")
        }
        bufferFormat = format
        accentBuffer = Self.synthesizeClick(format: format, frequencyHz: 1360, durationMs: 26, gain: 0.85)
        regularBuffer = Self.synthesizeClick(format: format, frequencyHz: 900, durationMs: 22, gain: 0.68)
        subdivisionBuffer = Self.synthesizeClick(format: format, frequencyHz: 640, durationMs: 16, gain: 0.36)
    }

    // MARK: - Public API

    func configure(
        bpm: Int? = nil,
        beatsPerMeasure: Int? = nil,
        beatPattern: [BeatAccentLevel]? = nil,
        subdivisionMode: SubdivisionMode? = nil,
        isMuted: Bool? = nil
    ) {
        if let bpm {
            self.bpm = min(max(bpm, 20), 300)
        }
        if let beatsPerMeasure {
            let clamped = min(max(beatsPerMeasure, 1), 16)
            self.beatsPerMeasure = clamped
            self.beatPattern = Self.normalizePattern(self.beatPattern, beatsPerMeasure: clamped)
        }
        if let beatPattern {
            self.beatPattern = Self.normalizePattern(beatPattern, beatsPerMeasure: self.beatsPerMeasure)
        }
        if let subdivisionMode {
            self.subdivisionMode = subdivisionMode
        }
        if let isMuted {
            self.isMuted = isMuted
        }
    }

    func start() {
        guard !isPlaying else { return }
        ensureAudioReady()

        isPlaying = true
        generation += 1
        let myGeneration = generation

        loopTask?.cancel()
        loopTask = Task { [weak self] in
            guard let self else { return }
            await self.runLoop(generation: myGeneration)
        }
    }

    func stop() {
        isPlaying = false
        generation += 1
        loopTask?.cancel()
        loopTask = nil
        player.stop()
        currentBeat = 0
        currentSubdivision = 0
    }

    func toggle() {
        if isPlaying { stop() } else { start() }
    }

    func release() {
        stop()
        if engine.isRunning {
            engine.stop()
        }
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    // MARK: - Engine lifecycle

    private func ensureAudioReady() {
        if !audioConfigured {
            engine.attach(player)
            engine.connect(player, to: engine.mainMixerNode, format: bufferFormat)
            audioConfigured = true
        }

        if !engine.isRunning {
            let session = AVAudioSession.sharedInstance()
            try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try? session.setActive(true)
            engine.prepare()
            do {
                try engine.start()
            } catch {
                print("MetronomeEngine could not start audio engine: \(error)")
            }
        }

        if !player.isPlaying {
            player.play()
        }
    }

    // MARK: - Scheduling loop

    private func runLoop(generation myGeneration: Int) async {
        // Schedule audio slightly ahead of the visual pulse to absorb wake-up jitter.
        let leadSeconds = 0.12
        var nextHostTime = Self.hostTime(after: leadSeconds)
        var beatIndex = 0

        while !Task.isCancelled && isPlaying && generation == myGeneration {
            let totalBeats = min(max(beatsPerMeasure, 1), 16)
            let activeBeat = (beatIndex % totalBeats) + 1
            let level = beatPattern.indices.contains(activeBeat - 1)
                ? beatPattern[activeBeat - 1]
                : BeatAccentLevel.NORMAL
            let mode = subdivisionMode
            let subCount = max(mode.subdivisionsPerBeat, 1)
            let beatDuration = 60.0 / Double(min(max(bpm, 20), 300))

            for subIndex in 0..<subCount {
                if Task.isCancelled || !isPlaying || generation != myGeneration { return }

                let stepDuration: Double
                if mode == .SWING_EIGHTH {
                    stepDuration = subIndex == 0 ? beatDuration * 2.0 / 3.0 : beatDuration / 3.0
                } else {
                    stepDuration = beatDuration / Double(subCount)
                }

                let now = Self.nowSeconds()
                var eventSeconds = Self.seconds(forHostTime: nextHostTime)
                if eventSeconds < now + 0.02 {
                    // The loop stalled (app resumed, BPM changed sharply, ...). Re-anchor.
                    nextHostTime = Self.hostTime(after: leadSeconds)
                    eventSeconds = Self.seconds(forHostTime: nextHostTime)
                }

                if !isMuted && level != .MUTE {
                    let buffer: AVAudioPCMBuffer?
                    if subIndex == 0 {
                        buffer = level == .ACCENT ? accentBuffer : regularBuffer
                    } else {
                        buffer = subdivisionBuffer
                    }
                    if let buffer {
                        player.scheduleBuffer(
                            buffer,
                            at: AVAudioTime(hostTime: nextHostTime),
                            options: [],
                            completionHandler: nil
                        )
                    }
                }

                let beatForVisual = activeBeat
                let subdivisionForVisual = subIndex + 1
                let firesPulse = subIndex == 0
                let visualDelay = max(0, eventSeconds - now - 0.008)

                Task { @MainActor [weak self] in
                    if visualDelay > 0 {
                        try? await Task.sleep(nanoseconds: UInt64(visualDelay * 1_000_000_000))
                    }
                    guard let self, self.isPlaying, self.generation == myGeneration else { return }
                    self.currentBeat = beatForVisual
                    self.currentSubdivision = subdivisionForVisual
                    if firesPulse {
                        self.beatPulseID &+= 1
                    }
                }

                nextHostTime &+= Self.hostTime(forDuration: stepDuration)

                let sleepTarget = eventSeconds - leadSeconds * 0.5
                let sleepSeconds = sleepTarget - Self.nowSeconds()
                if sleepSeconds > 0 {
                    try? await Task.sleep(nanoseconds: UInt64(sleepSeconds * 1_000_000_000))
                }
            }

            beatIndex += 1
        }
    }

    // MARK: - Click synthesis

    private static func synthesizeClick(
        format: AVAudioFormat,
        frequencyHz: Double,
        durationMs: Double,
        gain: Double
    ) -> AVAudioPCMBuffer? {
        let frameCount = AVAudioFrameCount(format.sampleRate * durationMs / 1000.0)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frameCount) else {
            return nil
        }
        buffer.frameLength = frameCount
        guard let channel = buffer.floatChannelData?[0] else { return nil }

        let totalSamples = Int(frameCount)
        for index in 0..<totalSamples {
            let time = Double(index) / format.sampleRate
            let envelope = exp(-Double(index) / (Double(totalSamples) * 0.21))
            let fundamental = sin(2.0 * Double.pi * frequencyHz * time)
            let overtone = 0.35 * sin(2.0 * Double.pi * (frequencyHz * 1.52) * time)
            let transient = index < 45 ? 0.22 * sin(2.0 * Double.pi * 2900.0 * time) : 0.0
            channel[index] = Float((fundamental + overtone + transient) * envelope * gain)
        }
        return buffer
    }

    // MARK: - Host clock helpers

    private static func nowSeconds() -> Double {
        AVAudioTime.seconds(forHostTime: mach_absolute_time())
    }

    private static func seconds(forHostTime hostTime: UInt64) -> Double {
        AVAudioTime.seconds(forHostTime: hostTime)
    }

    private static func hostTime(forDuration seconds: Double) -> UInt64 {
        AVAudioTime.hostTime(forSeconds: max(seconds, 0))
    }

    private static func hostTime(after seconds: Double) -> UInt64 {
        mach_absolute_time() &+ AVAudioTime.hostTime(forSeconds: max(seconds, 0))
    }

    // MARK: - Pattern helpers

    static func defaultBeatPattern(_ beatsPerMeasure: Int) -> [BeatAccentLevel] {
        let clamped = min(max(beatsPerMeasure, 1), 16)
        return (0..<clamped).map { index in
            if index == 0 { return .ACCENT }
            if clamped == 6 && index == 3 { return .ACCENT }          // 6/8 secondary pulse
            if clamped == 9 && (index == 3 || index == 6) { return .ACCENT }
            if clamped == 12 && (index == 3 || index == 6 || index == 9) { return .ACCENT }
            return .NORMAL
        }
    }

    static func normalizePattern(_ existing: [BeatAccentLevel], beatsPerMeasure: Int) -> [BeatAccentLevel] {
        let clamped = min(max(beatsPerMeasure, 1), 16)
        if existing.count == clamped { return existing }
        return (0..<clamped).map { index in
            if existing.indices.contains(index) { return existing[index] }
            return index == 0 ? .ACCENT : .NORMAL
        }
    }

    static func tempoMarking(forBpm bpm: Int) -> String {
        switch bpm {
        case ..<40: return "Grave"
        case 40..<60: return "Largo / Lento"
        case 60..<66: return "Larghetto"
        case 66..<76: return "Adagio"
        case 76..<108: return "Andante"
        case 108..<120: return "Moderato"
        case 120..<156: return "Allegro"
        case 156..<176: return "Vivace"
        case 176..<200: return "Presto"
        default: return "Prestissimo"
        }
    }
}
