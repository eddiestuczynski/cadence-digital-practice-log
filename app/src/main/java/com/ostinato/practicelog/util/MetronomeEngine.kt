package com.ostinato.practicelog.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SubdivisionMode(val label: String, val shortLabel: String, val subdivisionsPerBeat: Int) {
    QUARTER("Quarter Notes", "1x", 1),
    EIGHTH("Eighth Notes", "2x", 2),
    TRIPLET("Triplets", "3x", 3),
    SIXTEENTH("Sixteenth Notes", "4x", 4),
    SWING_EIGHTH("Swing Eighths", "Swing", 2)
}

enum class BeatAccentLevel(val label: String) {
    ACCENT("Loud Accent"),
    NORMAL("Normal Click"),
    MUTE("Silent Beat");

    fun next(): BeatAccentLevel = when (this) {
        ACCENT -> NORMAL
        NORMAL -> MUTE
        MUTE -> ACCENT
    }
}

/**
 * Drift-compensated PCM audio metronome engine using reusable static [AudioTrack] buffers.
 * Supports subdivisions (quarter, eighth, triplet, sixteenth, swing), per-beat accent/mute patterns,
 * and visual beat pulse emission even when audio is muted for loud instruments.
 */
class MetronomeEngine(private val scope: CoroutineScope) {

    private val sampleRate = 44100

    private val accentClickPcm: ShortArray = synthesizeClick(frequencyHz = 1360.0, durationMs = 26, gain = 0.85)
    private val regularClickPcm: ShortArray = synthesizeClick(frequencyHz = 900.0, durationMs = 22, gain = 0.68)
    private val subdivisionClickPcm: ShortArray = synthesizeClick(frequencyHz = 640.0, durationMs = 16, gain = 0.36)

    private var accentTrack: AudioTrack? = null
    private var regularTrack: AudioTrack? = null
    private var subdivisionTrack: AudioTrack? = null

    private var metronomeJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _currentSubdivision = MutableStateFlow(0)
    val currentSubdivision: StateFlow<Int> = _currentSubdivision.asStateFlow()

    // Monotonically increments on every main beat so visual flash animations trigger reliably
    private val _beatPulseId = MutableStateFlow(0L)
    val beatPulseId: StateFlow<Long> = _beatPulseId.asStateFlow()

    @Volatile
    private var bpm: Int = 100

    @Volatile
    private var beatsPerMeasure: Int = 4

    @Volatile
    private var beatPattern: List<BeatAccentLevel> = defaultBeatPattern(4)

    @Volatile
    private var subdivisionMode: SubdivisionMode = SubdivisionMode.QUARTER

    @Volatile
    private var isMuted: Boolean = false

    fun configure(
        bpm: Int = this.bpm,
        beatsPerMeasure: Int = this.beatsPerMeasure,
        beatPattern: List<BeatAccentLevel> = this.beatPattern,
        subdivisionMode: SubdivisionMode = this.subdivisionMode,
        isMuted: Boolean = this.isMuted
    ) {
        val clampedBeats = beatsPerMeasure.coerceIn(1, 16)
        this.bpm = bpm.coerceIn(20, 300)
        this.beatsPerMeasure = clampedBeats
        this.beatPattern = normalizePattern(beatPattern, clampedBeats)
        this.subdivisionMode = subdivisionMode
        this.isMuted = isMuted
    }

    fun start() {
        if (_isPlaying.value) return
        ensureAudioTracksInitialized()

        _isPlaying.value = true
        metronomeJob = scope.launch(Dispatchers.Default) {
            var beatIndex = 0
            var nextEventNs = System.nanoTime()

            while (isActive && _isPlaying.value) {
                val totalBeats = beatsPerMeasure.coerceIn(1, 16)
                val activeBeat = (beatIndex % totalBeats) + 1
                val patternSnapshot = beatPattern
                val accentLevel = patternSnapshot.getOrElse(activeBeat - 1) { BeatAccentLevel.NORMAL }
                val subMode = subdivisionMode
                val subCount = subMode.subdivisionsPerBeat

                for (subIdx in 0 until subCount) {
                    if (!isActive || !_isPlaying.value) break

                    _currentBeat.value = activeBeat
                    _currentSubdivision.value = subIdx + 1
                    if (subIdx == 0) {
                        _beatPulseId.value = _beatPulseId.value + 1L
                    }

                    if (!isMuted && accentLevel != BeatAccentLevel.MUTE) {
                        if (subIdx == 0) {
                            when (accentLevel) {
                                BeatAccentLevel.ACCENT -> triggerStaticTrack(accentTrack)
                                BeatAccentLevel.NORMAL -> triggerStaticTrack(regularTrack)
                                BeatAccentLevel.MUTE -> Unit
                            }
                        } else {
                            triggerStaticTrack(subdivisionTrack)
                        }
                    }

                    val beatDurationNs = (60_000_000_000L / bpm.coerceIn(20, 300).toLong())
                    val stepDurationNs = when {
                        subMode == SubdivisionMode.SWING_EIGHTH && subIdx == 0 -> (beatDurationNs * 2L) / 3L
                        subMode == SubdivisionMode.SWING_EIGHTH && subIdx == 1 -> beatDurationNs / 3L
                        else -> beatDurationNs / subCount.toLong()
                    }

                    nextEventNs += stepDurationNs
                    val sleepMs = (nextEventNs - System.nanoTime()) / 1_000_000L
                    if (sleepMs > 1L) {
                        delay(sleepMs)
                    } else {
                        // Resync if thread stalled (e.g., BPM changed drastically)
                        if (sleepMs < -150L) {
                            nextEventNs = System.nanoTime()
                        }
                        delay(2L)
                    }
                }

                beatIndex++
            }
            _currentBeat.value = 0
            _currentSubdivision.value = 0
        }
    }

    fun stop() {
        _isPlaying.value = false
        metronomeJob?.cancel()
        metronomeJob = null
        _currentBeat.value = 0
        _currentSubdivision.value = 0
    }

    fun toggle() {
        if (_isPlaying.value) stop() else start()
    }

    fun release() {
        stop()
        runCatching { accentTrack?.release() }
        runCatching { regularTrack?.release() }
        runCatching { subdivisionTrack?.release() }
        accentTrack = null
        regularTrack = null
        subdivisionTrack = null
    }

    private fun ensureAudioTracksInitialized() {
        if (accentTrack == null) accentTrack = createStaticTrack(accentClickPcm)
        if (regularTrack == null) regularTrack = createStaticTrack(regularClickPcm)
        if (subdivisionTrack == null) subdivisionTrack = createStaticTrack(subdivisionClickPcm)
    }

    private fun createStaticTrack(samples: ShortArray): AudioTrack? {
        return runCatching {
            val byteCount = samples.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount.coerceAtLeast(minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .apply {
                    write(samples, 0, samples.size)
                }
        }.getOrNull()
    }

    private fun triggerStaticTrack(track: AudioTrack?) {
        if (track == null) return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        }
    }

    private fun synthesizeClick(frequencyHz: Double, durationMs: Int, gain: Double): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val output = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-i.toDouble() / (totalSamples * 0.21))
            val fundamental = sin(2.0 * PI * frequencyHz * t)
            val overtone = 0.35 * sin(2.0 * PI * (frequencyHz * 1.52) * t)
            val transient = if (i < 45) 0.22 * sin(2.0 * PI * 2900.0 * t) else 0.0
            val sampleValue = ((fundamental + overtone + transient) * envelope * gain * Short.MAX_VALUE).toInt()
            output[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    companion object {
        fun defaultBeatPattern(beatsPerMeasure: Int): List<BeatAccentLevel> {
            val clamped = beatsPerMeasure.coerceIn(1, 16)
            return List(clamped) { idx ->
                when {
                    idx == 0 -> BeatAccentLevel.ACCENT
                    clamped == 6 && idx == 3 -> BeatAccentLevel.ACCENT // 6/8 secondary pulse on beat 4
                    clamped == 9 && (idx == 3 || idx == 6) -> BeatAccentLevel.ACCENT // 9/8 compound pulse
                    clamped == 12 && (idx == 3 || idx == 6 || idx == 9) -> BeatAccentLevel.ACCENT
                    else -> BeatAccentLevel.NORMAL
                }
            }
        }

        fun normalizePattern(existing: List<BeatAccentLevel>, beatsPerMeasure: Int): List<BeatAccentLevel> {
            val clamped = beatsPerMeasure.coerceIn(1, 16)
            if (existing.size == clamped) return existing
            return List(clamped) { idx ->
                existing.getOrElse(idx) {
                    if (idx == 0) BeatAccentLevel.ACCENT else BeatAccentLevel.NORMAL
                }
            }
        }

        fun tempoMarkingForBpm(bpm: Int): String = when {
            bpm < 40 -> "Grave"
            bpm < 60 -> "Largo / Lento"
            bpm < 66 -> "Larghetto"
            bpm < 76 -> "Adagio"
            bpm < 108 -> "Andante"
            bpm < 120 -> "Moderato"
            bpm < 156 -> "Allegro"
            bpm < 176 -> "Vivace"
            bpm < 200 -> "Presto"
            else -> "Prestissimo"
        }
    }
}
