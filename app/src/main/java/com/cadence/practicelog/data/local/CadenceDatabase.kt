package com.cadence.practicelog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        RepertoirePieceEntity::class,
        PieceSectionEntity::class,
        PracticeSessionEntity::class,
        RoutineTemplateEntity::class,
        RoutineTemplateItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CadenceDatabase : RoomDatabase() {

    abstract fun practiceDao(): PracticeDao

    companion object {
        @Volatile
        private var INSTANCE: CadenceDatabase? = null

        fun getDatabase(context: Context): CadenceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CadenceDatabase::class.java,
                    "cadence_practice_log.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedSampleData(dao: PracticeDao, forceReseed: Boolean = false) {
            if (!forceReseed && dao.getPieceCount() > 0) return
            if (forceReseed) {
                dao.clearAllData()
            }

            val now = System.currentTimeMillis()
            val oneDay = TimeUnit.DAYS.toMillis(1)

            // 1. Repertoire Piece (9/8, Db Major)
            val clairDeLuneId = dao.insertPiece(
                RepertoirePieceEntity(
                    title = "Clair de Lune (Suite bergamasque)",
                    composer = "Claude Debussy",
                    instrument = "Piano",
                    category = PieceCategory.REPERTOIRE.name,
                    keySignature = "Db Major",
                    timeSignatureBeats = 9,
                    timeSignatureNoteValue = 8,
                    bestBpm = 64,
                    lastPracticedBpm = 62,
                    targetBpm = 66,
                    status = PieceStatus.POLISHING.name,
                    practiceNotes = "Keep left-hand arpeggios in m. 27–42 velvet-soft (pp). Voice top melody with warm arm weight.",
                    nextSessionGoal = "Connect Tempo rubato into Un poco mosso (m. 25–30) without rushing the 9/8 pulse.",
                    totalMinutesPracticed = 145,
                    lastPracticedAtEpochMillis = now - TimeUnit.HOURS.toMillis(4),
                    createdAtEpochMillis = now - 24 * oneDay
                )
            )

            // 2. Multi-Movement Repertoire Piece (Bach Partita No. 2)
            val bachPartitaId = dao.insertPiece(
                RepertoirePieceEntity(
                    title = "Partita No. 2 in D Minor, BWV 1004",
                    composer = "J.S. Bach",
                    instrument = "Violin",
                    category = PieceCategory.REPERTOIRE.name,
                    keySignature = "D Minor",
                    timeSignatureBeats = 4,
                    timeSignatureNoteValue = 4,
                    bestBpm = 60,
                    lastPracticedBpm = 56,
                    targetBpm = 72,
                    status = PieceStatus.LEARNING.name,
                    practiceNotes = "Natural resonant bow weight; listen for sympathetic open D & A string ringing on cadences.",
                    nextSessionGoal = "Allemande m. 15–22 string crossings with drone D.",
                    totalMinutesPracticed = 185,
                    lastPracticedAtEpochMillis = now - oneDay - TimeUnit.HOURS.toMillis(2),
                    createdAtEpochMillis = now - 28 * oneDay
                )
            )

            val allemandeSectionId = dao.insertSection(
                PieceSectionEntity(
                    pieceId = bachPartitaId,
                    name = "I. Allemande",
                    orderIndex = 0,
                    bestBpm = 60,
                    lastPracticedBpm = 56,
                    targetBpm = 72,
                    timeSignatureBeats = 4,
                    timeSignatureNoteValue = 4,
                    sectionNotes = "Detaché upper-half bow control; breathe at harmonic cadences.",
                    nextSessionGoal = "Spot-practice diminished leaps in m. 15–17 slowly at 48 BPM first.",
                    totalMinutesPracticed = 120,
                    lastPracticedAtEpochMillis = now - oneDay - TimeUnit.HOURS.toMillis(2)
                )
            )

            val couranteSectionId = dao.insertSection(
                PieceSectionEntity(
                    pieceId = bachPartitaId,
                    name = "II. Courante",
                    orderIndex = 1,
                    bestBpm = 96,
                    lastPracticedBpm = 92,
                    targetBpm = 116,
                    timeSignatureBeats = 3,
                    timeSignatureNoteValue = 4,
                    sectionNotes = "Crisp dotted figures on up-bow pickups; keep triplet slurs buoyant.",
                    nextSessionGoal = "Clarify articulation contrast between slurred triplets and dotted eighths.",
                    totalMinutesPracticed = 65,
                    lastPracticedAtEpochMillis = now - 5 * oneDay
                )
            )

            dao.insertSection(
                PieceSectionEntity(
                    pieceId = bachPartitaId,
                    name = "V. Chaconne (Opening & Arpeggios)",
                    orderIndex = 2,
                    bestBpm = 48,
                    lastPracticedBpm = 48,
                    targetBpm = 60,
                    timeSignatureBeats = 3,
                    timeSignatureNoteValue = 4,
                    sectionNotes = "Roll four-note chords from bass note cleanly onto beat 2.",
                    nextSessionGoal = "Map chord voicings m. 1–32 without excess bow pressure.",
                    totalMinutesPracticed = 0,
                    lastPracticedAtEpochMillis = null
                )
            )

            // 3. Etude / Study
            val chopinEtudeId = dao.insertPiece(
                RepertoirePieceEntity(
                    title = "Étude Op. 10, No. 12 ('Revolutionary')",
                    composer = "Frédéric Chopin",
                    instrument = "Piano",
                    category = PieceCategory.ETUDE.name,
                    keySignature = "C Minor",
                    timeSignatureBeats = 4,
                    timeSignatureNoteValue = 4,
                    bestBpm = 124,
                    lastPracticedBpm = 104, // Demonstrates slow practice not overwriting Best BPM (124)!
                    targetBpm = 160,
                    status = PieceStatus.LEARNING.name,
                    practiceNotes = "Practice left-hand descending cascades in dotted rhythms with supple wrist rotation.",
                    nextSessionGoal = "Run m. 1–18 in dotted-rhythm pairs at 112 BPM, then test 126 BPM.",
                    totalMinutesPracticed = 115,
                    lastPracticedAtEpochMillis = now - 2 * oneDay,
                    createdAtEpochMillis = now - 20 * oneDay
                )
            )

            // 4. Technique / Scale Item
            val scalesId = dao.insertPiece(
                RepertoirePieceEntity(
                    title = "4-Octave Major & Melodic Minor Scales + Arpeggios",
                    composer = "Daily Technique",
                    instrument = "Piano",
                    category = PieceCategory.TECHNIQUE.name,
                    keySignature = "Circle of Fifths",
                    timeSignatureBeats = 4,
                    timeSignatureNoteValue = 4,
                    bestBpm = 116,
                    lastPracticedBpm = 112,
                    targetBpm = 132,
                    status = PieceStatus.POLISHING.name,
                    practiceNotes = "Subdivide sixteenth notes evenly; listen for thumb-under legato match.",
                    nextSessionGoal = "Focus on B Major & G# Melodic Minor contrary motion.",
                    totalMinutesPracticed = 95,
                    lastPracticedAtEpochMillis = now - TimeUnit.HOURS.toMillis(5),
                    createdAtEpochMillis = now - 30 * oneDay
                )
            )

            // 5. Polishing Piece Not Practiced Recently (triggers "Rusty / Needs Attention" insight)
            val autumnLeavesId = dao.insertPiece(
                RepertoirePieceEntity(
                    title = "Autumn Leaves (Chord Melody & Solo)",
                    composer = "Joseph Kosma",
                    instrument = "Guitar",
                    category = PieceCategory.REPERTOIRE.name,
                    keySignature = "G Minor",
                    timeSignatureBeats = 4,
                    timeSignatureNoteValue = 4,
                    bestBpm = 136,
                    lastPracticedBpm = 132,
                    targetBpm = 144,
                    status = PieceStatus.POLISHING.name,
                    practiceNotes = "Metronome on beats 2 & 4 only; guide-tone voice leading through II-V-I.",
                    nextSessionGoal = "Refresh chorus 2 drop-2 voicings so they stay performance-ready.",
                    totalMinutesPracticed = 90,
                    lastPracticedAtEpochMillis = now - 11 * oneDay,
                    createdAtEpochMillis = now - 32 * oneDay
                )
            )

            // Seed multi-week session history (supporting a multi-week Mon-Sun consistency streak & rich BPM curves)
            val sampleSessions = listOf(
                // Today / Recent days (Current Week)
                PracticeSessionEntity(
                    pieceId = scalesId,
                    pieceTitle = "4-Octave Major & Melodic Minor Scales + Arpeggios",
                    composer = "Daily Technique",
                    instrument = "Piano",
                    category = PieceCategory.TECHNIQUE.name,
                    durationMinutes = 15,
                    durationSecondsTotal = 15 * 60,
                    practicedBpm = 112,
                    targetBpmSnapshot = 132,
                    measuresPracticed = "Db, Ab, Eb Major & Minor",
                    focusTags = "Technique,Articulation,Rhythm",
                    sessionNotes = "Warmed up with 16th-note subdivisions at 112 BPM. Left-hand 4th finger crossover in Ab minor was much cleaner.",
                    nextSessionGoal = "Focus on B Major & G# Melodic Minor contrary motion.",
                    moodRating = 5,
                    routineName = "45-Min Piano Daily Routine",
                    timestampEpochMillis = now - TimeUnit.HOURS.toMillis(5)
                ),
                PracticeSessionEntity(
                    pieceId = clairDeLuneId,
                    pieceTitle = "Clair de Lune (Suite bergamasque)",
                    composer = "Claude Debussy",
                    instrument = "Piano",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 35,
                    durationSecondsTotal = 35 * 60,
                    practicedBpm = 62,
                    targetBpmSnapshot = 66,
                    measuresPracticed = "m. 27–42 (Un poco mosso)",
                    focusTags = "Dynamics,Voicing,Pedaling",
                    sessionNotes = "Isolated right-hand pinky melody while playing left-hand arpeggios staccato to check balance. Half-pedal changes are crystal clear.",
                    nextSessionGoal = "Connect Tempo rubato into Un poco mosso (m. 25–30) without rushing the 9/8 pulse.",
                    moodRating = 5,
                    routineName = "45-Min Piano Daily Routine",
                    timestampEpochMillis = now - TimeUnit.HOURS.toMillis(4)
                ),
                PracticeSessionEntity(
                    pieceId = bachPartitaId,
                    sectionId = allemandeSectionId,
                    pieceTitle = "Partita No. 2 in D Minor, BWV 1004",
                    sectionName = "I. Allemande",
                    composer = "J.S. Bach",
                    instrument = "Violin",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 40,
                    durationSecondsTotal = 40 * 60,
                    practicedBpm = 56,
                    targetBpmSnapshot = 72,
                    measuresPracticed = "m. 9–20",
                    focusTags = "Intonation,Bow Control,Slow Practice",
                    sessionNotes = "Played against a D drone after hitting 60 BPM earlier this week; settled at 56 BPM today to refine B-flat intonation in m. 16.",
                    nextSessionGoal = "Spot-practice diminished leaps in m. 15–17 slowly at 48 BPM first.",
                    moodRating = 4,
                    timestampEpochMillis = now - oneDay - TimeUnit.HOURS.toMillis(2)
                ),
                PracticeSessionEntity(
                    pieceId = chopinEtudeId,
                    pieceTitle = "Étude Op. 10, No. 12 ('Revolutionary')",
                    composer = "Frédéric Chopin",
                    instrument = "Piano",
                    category = PieceCategory.ETUDE.name,
                    durationMinutes = 35,
                    durationSecondsTotal = 35 * 60,
                    practicedBpm = 104,
                    targetBpmSnapshot = 160,
                    measuresPracticed = "m. 1–28",
                    focusTags = "Slow Practice,Technique,Articulation",
                    sessionNotes = "Deliberate slow maintenance pass at 104 BPM (down from 124 BPM peak) focusing on zero forearm tension at cascade bottoms.",
                    nextSessionGoal = "Run m. 1–18 in dotted-rhythm pairs at 112 BPM, then test 126 BPM.",
                    moodRating = 5,
                    timestampEpochMillis = now - 2 * oneDay - TimeUnit.HOURS.toMillis(3)
                ),
                // Earlier sessions (~4 to 6 days ago)
                PracticeSessionEntity(
                    pieceId = chopinEtudeId,
                    pieceTitle = "Étude Op. 10, No. 12 ('Revolutionary')",
                    composer = "Frédéric Chopin",
                    instrument = "Piano",
                    category = PieceCategory.ETUDE.name,
                    durationMinutes = 40,
                    durationSecondsTotal = 40 * 60,
                    practicedBpm = 124,
                    targetBpmSnapshot = 160,
                    measuresPracticed = "m. 1–41",
                    focusTags = "Technique,Rhythm",
                    sessionNotes = "Reached a clean personal best of 124 BPM on the first page! Wrist stayed supple through m. 29 modulation.",
                    nextSessionGoal = "Do a slow tension-release pass next session.",
                    moodRating = 5,
                    timestampEpochMillis = now - 4 * oneDay - TimeUnit.HOURS.toMillis(2)
                ),
                PracticeSessionEntity(
                    pieceId = bachPartitaId,
                    sectionId = couranteSectionId,
                    pieceTitle = "Partita No. 2 in D Minor, BWV 1004",
                    sectionName = "II. Courante",
                    composer = "J.S. Bach",
                    instrument = "Violin",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 35,
                    durationSecondsTotal = 35 * 60,
                    practicedBpm = 92,
                    targetBpmSnapshot = 116,
                    measuresPracticed = "First half",
                    focusTags = "Articulation,Phrasing",
                    sessionNotes = "Worked on buoyant 3/4 dance pulse and crisp up-bow dotted pickups.",
                    nextSessionGoal = "Clarify articulation contrast between slurred triplets and dotted eighths.",
                    moodRating = 4,
                    timestampEpochMillis = now - 5 * oneDay - TimeUnit.HOURS.toMillis(3)
                ),
                PracticeSessionEntity(
                    pieceId = bachPartitaId,
                    sectionId = allemandeSectionId,
                    pieceTitle = "Partita No. 2 in D Minor, BWV 1004",
                    sectionName = "I. Allemande",
                    composer = "J.S. Bach",
                    instrument = "Violin",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 40,
                    durationSecondsTotal = 40 * 60,
                    practicedBpm = 60,
                    targetBpmSnapshot = 72,
                    measuresPracticed = "m. 1–16",
                    focusTags = "Phrasing,Bow Control",
                    sessionNotes = "First clean play-through of Part A at 60 BPM with musical breathing at cadences.",
                    nextSessionGoal = "Check m. 15–20 intonation with drone.",
                    moodRating = 5,
                    timestampEpochMillis = now - 6 * oneDay - TimeUnit.HOURS.toMillis(2)
                ),
                // Prior week (7–13 days ago: >180 mins total to maintain streak)
                PracticeSessionEntity(
                    pieceId = clairDeLuneId,
                    pieceTitle = "Clair de Lune (Suite bergamasque)",
                    composer = "Claude Debussy",
                    instrument = "Piano",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 50,
                    durationSecondsTotal = 50 * 60,
                    practicedBpm = 64,
                    targetBpmSnapshot = 66,
                    measuresPracticed = "Full piece",
                    focusTags = "Memorization,Dynamics",
                    sessionNotes = "Full memory run at 64 BPM! Only slight hesitation entering the Calmato section.",
                    nextSessionGoal = "Polish m. 27–42 left-hand pp texture.",
                    moodRating = 5,
                    timestampEpochMillis = now - 8 * oneDay
                ),
                PracticeSessionEntity(
                    pieceId = scalesId,
                    pieceTitle = "4-Octave Major & Melodic Minor Scales + Arpeggios",
                    composer = "Daily Technique",
                    instrument = "Piano",
                    category = PieceCategory.TECHNIQUE.name,
                    durationMinutes = 40,
                    durationSecondsTotal = 40 * 60,
                    practicedBpm = 116,
                    targetBpmSnapshot = 132,
                    measuresPracticed = "Sharp keys",
                    focusTags = "Technique,Rhythm",
                    sessionNotes = "Metronome on beats 2 & 4 only to test internal subdivision stability.",
                    nextSessionGoal = "Flat keys next week.",
                    moodRating = 4,
                    timestampEpochMillis = now - 9 * oneDay
                ),
                PracticeSessionEntity(
                    pieceId = chopinEtudeId,
                    pieceTitle = "Étude Op. 10, No. 12 ('Revolutionary')",
                    composer = "Frédéric Chopin",
                    instrument = "Piano",
                    category = PieceCategory.ETUDE.name,
                    durationMinutes = 50,
                    durationSecondsTotal = 50 * 60,
                    practicedBpm = 114,
                    targetBpmSnapshot = 160,
                    measuresPracticed = "m. 1–28",
                    focusTags = "Technique,Rhythm",
                    sessionNotes = "Stepped up from 108 to 114 BPM in 4-note rhythmic bursts.",
                    nextSessionGoal = "Aim for 120+ BPM on opening cascade.",
                    moodRating = 4,
                    timestampEpochMillis = now - 10 * oneDay
                ),
                PracticeSessionEntity(
                    pieceId = autumnLeavesId,
                    pieceTitle = "Autumn Leaves (Chord Melody & Solo)",
                    composer = "Joseph Kosma",
                    instrument = "Guitar",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 55,
                    durationSecondsTotal = 55 * 60,
                    practicedBpm = 132,
                    targetBpmSnapshot = 144,
                    measuresPracticed = "Head + 3 Choruses",
                    focusTags = "Phrasing,Rhythm",
                    sessionNotes = "Swing eighth subdivisions locked in with clicks on 2 & 4 (66 half-note click).",
                    nextSessionGoal = "Refresh chorus 2 drop-2 voicings.",
                    moodRating = 5,
                    timestampEpochMillis = now - 11 * oneDay
                ),
                // Two weeks ago (14–20 days ago: >180 mins total for multi-week streak)
                PracticeSessionEntity(
                    pieceId = clairDeLuneId,
                    pieceTitle = "Clair de Lune (Suite bergamasque)",
                    composer = "Claude Debussy",
                    instrument = "Piano",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 65,
                    durationSecondsTotal = 65 * 60,
                    practicedBpm = 58,
                    targetBpmSnapshot = 66,
                    measuresPracticed = "Pages 2–4",
                    focusTags = "Sight-Reading,Memorization",
                    sessionNotes = "Memorized harmonic skeleton of the middle section.",
                    nextSessionGoal = "Bring tempo toward 62–64 BPM.",
                    moodRating = 4,
                    timestampEpochMillis = now - 15 * oneDay
                ),
                PracticeSessionEntity(
                    pieceId = bachPartitaId,
                    sectionId = allemandeSectionId,
                    pieceTitle = "Partita No. 2 in D Minor, BWV 1004",
                    sectionName = "I. Allemande",
                    composer = "J.S. Bach",
                    instrument = "Violin",
                    category = PieceCategory.REPERTOIRE.name,
                    durationMinutes = 65,
                    durationSecondsTotal = 65 * 60,
                    practicedBpm = 50,
                    targetBpmSnapshot = 72,
                    measuresPracticed = "m. 1–24",
                    focusTags = "Intonation,Slow Practice",
                    sessionNotes = "Mapped fingerings and bow divisions across Part A.",
                    nextSessionGoal = "Build comfort to 56–60 BPM.",
                    moodRating = 4,
                    timestampEpochMillis = now - 16 * oneDay
                ),
                PracticeSessionEntity(
                    pieceId = chopinEtudeId,
                    pieceTitle = "Étude Op. 10, No. 12 ('Revolutionary')",
                    composer = "Frédéric Chopin",
                    instrument = "Piano",
                    category = PieceCategory.ETUDE.name,
                    durationMinutes = 60,
                    durationSecondsTotal = 60 * 60,
                    practicedBpm = 104,
                    targetBpmSnapshot = 160,
                    measuresPracticed = "m. 1–18",
                    focusTags = "Technique,Slow Practice",
                    sessionNotes = "Left-hand alone blocking chords to eliminate thumb tension.",
                    nextSessionGoal = "Step up to 114 BPM.",
                    moodRating = 4,
                    timestampEpochMillis = now - 18 * oneDay
                )
            )
            sampleSessions.forEach { dao.insertSession(it) }

            // Seed two saveable Routine Templates
            val pianoRoutineId = dao.insertRoutineTemplate(
                RoutineTemplateEntity(
                    name = "45-Min Piano Daily Routine",
                    description = "Balanced warmup scales, Chopin technical study, and Debussy repertoire.",
                    instrument = "Piano",
                    createdAtEpochMillis = now - 20 * oneDay
                )
            )
            dao.insertRoutineItems(
                listOf(
                    RoutineTemplateItemEntity(
                        routineId = pianoRoutineId,
                        orderIndex = 0,
                        pieceId = scalesId,
                        customTitle = "4-Octave Major & Melodic Minor Scales + Arpeggios",
                        category = PieceCategory.TECHNIQUE.name,
                        targetMinutes = 10,
                        suggestedBpm = 112,
                        notesPrompt = "Listen for even 16th-note subdivisions and relaxed thumbs."
                    ),
                    RoutineTemplateItemEntity(
                        routineId = pianoRoutineId,
                        orderIndex = 1,
                        pieceId = chopinEtudeId,
                        customTitle = "Étude Op. 10, No. 12 ('Revolutionary')",
                        category = PieceCategory.ETUDE.name,
                        targetMinutes = 15,
                        suggestedBpm = 116,
                        notesPrompt = "Dotted-rhythm left-hand cascades before full tempo."
                    ),
                    RoutineTemplateItemEntity(
                        routineId = pianoRoutineId,
                        orderIndex = 2,
                        pieceId = clairDeLuneId,
                        customTitle = "Clair de Lune (Suite bergamasque)",
                        category = PieceCategory.REPERTOIRE.name,
                        targetMinutes = 20,
                        suggestedBpm = 62,
                        notesPrompt = "Shape top voice cantabile over pp left-hand arpeggios."
                    )
                )
            )

            val violinRoutineId = dao.insertRoutineTemplate(
                RoutineTemplateEntity(
                    name = "35-Min Baroque Violin Focus",
                    description = "Intonation drone warmup followed by Bach Partita movements.",
                    instrument = "Violin",
                    createdAtEpochMillis = now - 14 * oneDay
                )
            )
            dao.insertRoutineItems(
                listOf(
                    RoutineTemplateItemEntity(
                        routineId = violinRoutineId,
                        orderIndex = 0,
                        pieceId = null,
                        sectionId = null,
                        customTitle = "Open-String Resonance & Double-Stop Drones",
                        category = PieceCategory.TECHNIQUE.name,
                        targetMinutes = 10,
                        suggestedBpm = 60,
                        notesPrompt = "Pure fifths and relaxed bow arm weight."
                    ),
                    RoutineTemplateItemEntity(
                        routineId = violinRoutineId,
                        orderIndex = 1,
                        pieceId = bachPartitaId,
                        sectionId = allemandeSectionId,
                        customTitle = "Partita No. 2 — I. Allemande",
                        category = PieceCategory.REPERTOIRE.name,
                        targetMinutes = 15,
                        suggestedBpm = 56,
                        notesPrompt = "Check diminished leaps in m. 15–17."
                    ),
                    RoutineTemplateItemEntity(
                        routineId = violinRoutineId,
                        orderIndex = 2,
                        pieceId = bachPartitaId,
                        sectionId = couranteSectionId,
                        customTitle = "Partita No. 2 — II. Courante",
                        category = PieceCategory.REPERTOIRE.name,
                        targetMinutes = 10,
                        suggestedBpm = 92,
                        notesPrompt = "Buoyant 3/4 dance feel on dotted pickups."
                    )
                )
            )
        }
    }
}
