import Foundation

/// First-launch sample library, ported 1:1 from `CadenceDatabase.seedSampleData`
/// on Android. Explicit ids keep the demo deterministic and let the id
/// high-water marks advance safely.
enum DemoData {

    @MainActor
    static func seed(into store: CadenceStore) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let hour: Int64 = 3_600_000
        let day: Int64 = 86_400_000

        // MARK: Repertoire pieces

        let clairDeLune = RepertoirePiece(
            id: 1,
            title: "Clair de Lune (Suite bergamasque)",
            composer: "Claude Debussy",
            instrument: "Piano",
            categoryRaw: PieceCategory.REPERTOIRE.rawValue,
            keySignature: "Db Major",
            timeSignatureBeats: 9,
            timeSignatureNoteValue: 8,
            bestBpm: 64,
            lastPracticedBpm: 62,
            targetBpm: 66,
            statusRaw: PieceStatus.POLISHING.rawValue,
            practiceNotes: "Keep left-hand arpeggios in m. 27–42 velvet-soft (pp). Voice top melody with warm arm weight.",
            nextSessionGoal: "Connect Tempo rubato into Un poco mosso (m. 25–30) without rushing the 9/8 pulse.",
            totalMinutesPracticed: 145,
            lastPracticedAtEpochMillis: now - 4 * hour,
            createdAtEpochMillis: now - 24 * day
        )

        let bachPartita = RepertoirePiece(
            id: 2,
            title: "Partita No. 2 in D Minor, BWV 1004",
            composer: "J.S. Bach",
            instrument: "Violin",
            categoryRaw: PieceCategory.REPERTOIRE.rawValue,
            keySignature: "D Minor",
            timeSignatureBeats: 4,
            timeSignatureNoteValue: 4,
            bestBpm: 60,
            lastPracticedBpm: 56,
            targetBpm: 72,
            statusRaw: PieceStatus.LEARNING.rawValue,
            practiceNotes: "Natural resonant bow weight; listen for sympathetic open D & A string ringing on cadences.",
            nextSessionGoal: "Allemande m. 15–22 string crossings with drone D.",
            totalMinutesPracticed: 185,
            lastPracticedAtEpochMillis: now - day - 2 * hour,
            createdAtEpochMillis: now - 28 * day
        )

        let chopinEtude = RepertoirePiece(
            id: 3,
            title: "Étude Op. 10, No. 12 ('Revolutionary')",
            composer: "Frédéric Chopin",
            instrument: "Piano",
            categoryRaw: PieceCategory.ETUDE.rawValue,
            keySignature: "C Minor",
            timeSignatureBeats: 4,
            timeSignatureNoteValue: 4,
            bestBpm: 124,
            lastPracticedBpm: 104,
            targetBpm: 160,
            statusRaw: PieceStatus.LEARNING.rawValue,
            practiceNotes: "Practice left-hand descending cascades in dotted rhythms with supple wrist rotation.",
            nextSessionGoal: "Run m. 1–18 in dotted-rhythm pairs at 112 BPM, then test 126 BPM.",
            totalMinutesPracticed: 115,
            lastPracticedAtEpochMillis: now - 2 * day,
            createdAtEpochMillis: now - 20 * day
        )

        let scales = RepertoirePiece(
            id: 4,
            title: "4-Octave Major & Melodic Minor Scales + Arpeggios",
            composer: "Daily Technique",
            instrument: "Piano",
            categoryRaw: PieceCategory.TECHNIQUE.rawValue,
            keySignature: "Circle of Fifths",
            timeSignatureBeats: 4,
            timeSignatureNoteValue: 4,
            bestBpm: 116,
            lastPracticedBpm: 112,
            targetBpm: 132,
            statusRaw: PieceStatus.POLISHING.rawValue,
            practiceNotes: "Subdivide sixteenth notes evenly; listen for thumb-under legato match.",
            nextSessionGoal: "Focus on B Major & G# Melodic Minor contrary motion.",
            totalMinutesPracticed: 95,
            lastPracticedAtEpochMillis: now - 5 * hour,
            createdAtEpochMillis: now - 30 * day
        )

        let autumnLeaves = RepertoirePiece(
            id: 5,
            title: "Autumn Leaves (Chord Melody & Solo)",
            composer: "Joseph Kosma",
            instrument: "Guitar",
            categoryRaw: PieceCategory.REPERTOIRE.rawValue,
            keySignature: "G Minor",
            timeSignatureBeats: 4,
            timeSignatureNoteValue: 4,
            bestBpm: 136,
            lastPracticedBpm: 132,
            targetBpm: 144,
            statusRaw: PieceStatus.POLISHING.rawValue,
            practiceNotes: "Metronome on beats 2 & 4 only; guide-tone voice leading through II-V-I.",
            nextSessionGoal: "Refresh chorus 2 drop-2 voicings so they stay performance-ready.",
            totalMinutesPracticed: 90,
            lastPracticedAtEpochMillis: now - 11 * day,
            createdAtEpochMillis: now - 32 * day
        )

        store.insertRawPiece(clairDeLune)
        store.insertRawPiece(bachPartita)
        store.insertRawPiece(chopinEtude)
        store.insertRawPiece(scales)
        store.insertRawPiece(autumnLeaves)

        // MARK: Movements

        let allemande = PieceSection(
            id: 1,
            pieceId: bachPartita.id,
            name: "I. Allemande",
            orderIndex: 0,
            bestBpm: 60,
            lastPracticedBpm: 56,
            targetBpm: 72,
            timeSignatureBeats: 4,
            timeSignatureNoteValue: 4,
            sectionNotes: "Detaché upper-half bow control; breathe at harmonic cadences.",
            nextSessionGoal: "Spot-practice diminished leaps in m. 15–17 slowly at 48 BPM first.",
            totalMinutesPracticed: 120,
            lastPracticedAtEpochMillis: now - day - 2 * hour
        )

        let courante = PieceSection(
            id: 2,
            pieceId: bachPartita.id,
            name: "II. Courante",
            orderIndex: 1,
            bestBpm: 96,
            lastPracticedBpm: 92,
            targetBpm: 116,
            timeSignatureBeats: 3,
            timeSignatureNoteValue: 4,
            sectionNotes: "Crisp dotted figures on up-bow pickups; keep triplet slurs buoyant.",
            nextSessionGoal: "Clarify articulation contrast between slurred triplets and dotted eighths.",
            totalMinutesPracticed: 65,
            lastPracticedAtEpochMillis: now - 5 * day
        )

        let chaconne = PieceSection(
            id: 3,
            pieceId: bachPartita.id,
            name: "V. Chaconne (Opening & Arpeggios)",
            orderIndex: 2,
            bestBpm: 48,
            lastPracticedBpm: 48,
            targetBpm: 60,
            timeSignatureBeats: 3,
            timeSignatureNoteValue: 4,
            sectionNotes: "Roll four-note chords from bass note cleanly onto beat 2.",
            nextSessionGoal: "Map chord voicings m. 1–32 without excess bow pressure.",
            totalMinutesPracticed: 0,
            lastPracticedAtEpochMillis: nil
        )

        store.insertRawSection(allemande)
        store.insertRawSection(courante)
        store.insertRawSection(chaconne)

        // MARK: Session history

        let sessions: [PracticeSession] = [
            PracticeSession(
                id: 1,
                pieceId: scales.id,
                pieceTitle: "4-Octave Major & Melodic Minor Scales + Arpeggios",
                composer: "Daily Technique",
                instrument: "Piano",
                categoryRaw: PieceCategory.TECHNIQUE.rawValue,
                durationMinutes: 15,
                durationSecondsTotal: 15 * 60,
                practicedBpm: 112,
                targetBpmSnapshot: 132,
                measuresPracticed: "Db, Ab, Eb Major & Minor",
                focusTags: "Technique,Articulation,Rhythm",
                sessionNotes: "Warmed up with 16th-note subdivisions at 112 BPM. Left-hand 4th finger crossover in Ab minor was much cleaner.",
                nextSessionGoal: "Focus on B Major & G# Melodic Minor contrary motion.",
                moodRating: 5,
                routineName: "45-Min Piano Daily Routine",
                timestampEpochMillis: now - 5 * hour
            ),
            PracticeSession(
                id: 2,
                pieceId: clairDeLune.id,
                pieceTitle: "Clair de Lune (Suite bergamasque)",
                composer: "Claude Debussy",
                instrument: "Piano",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 35,
                durationSecondsTotal: 35 * 60,
                practicedBpm: 62,
                targetBpmSnapshot: 66,
                measuresPracticed: "m. 27–42 (Un poco mosso)",
                focusTags: "Dynamics,Voicing,Pedaling",
                sessionNotes: "Isolated right-hand pinky melody while playing left-hand arpeggios staccato to check balance. Half-pedal changes are crystal clear.",
                nextSessionGoal: "Connect Tempo rubato into Un poco mosso (m. 25–30) without rushing the 9/8 pulse.",
                moodRating: 5,
                routineName: "45-Min Piano Daily Routine",
                timestampEpochMillis: now - 4 * hour
            ),
            PracticeSession(
                id: 3,
                pieceId: bachPartita.id,
                sectionId: allemande.id,
                pieceTitle: "Partita No. 2 in D Minor, BWV 1004",
                sectionName: "I. Allemande",
                composer: "J.S. Bach",
                instrument: "Violin",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 40,
                durationSecondsTotal: 40 * 60,
                practicedBpm: 56,
                targetBpmSnapshot: 72,
                measuresPracticed: "m. 9–20",
                focusTags: "Intonation,Bow Control,Slow Practice",
                sessionNotes: "Played against a D drone after hitting 60 BPM earlier this week; settled at 56 BPM today to refine B-flat intonation in m. 16.",
                nextSessionGoal: "Spot-practice diminished leaps in m. 15–17 slowly at 48 BPM first.",
                moodRating: 4,
                timestampEpochMillis: now - day - 2 * hour
            ),
            PracticeSession(
                id: 4,
                pieceId: chopinEtude.id,
                pieceTitle: "Étude Op. 10, No. 12 ('Revolutionary')",
                composer: "Frédéric Chopin",
                instrument: "Piano",
                categoryRaw: PieceCategory.ETUDE.rawValue,
                durationMinutes: 35,
                durationSecondsTotal: 35 * 60,
                practicedBpm: 104,
                targetBpmSnapshot: 160,
                measuresPracticed: "m. 1–28",
                focusTags: "Slow Practice,Technique,Articulation",
                sessionNotes: "Deliberate slow maintenance pass at 104 BPM (down from 124 BPM peak) focusing on zero forearm tension at cascade bottoms.",
                nextSessionGoal: "Run m. 1–18 in dotted-rhythm pairs at 112 BPM, then test 126 BPM.",
                moodRating: 5,
                timestampEpochMillis: now - 2 * day - 3 * hour
            ),
            PracticeSession(
                id: 5,
                pieceId: chopinEtude.id,
                pieceTitle: "Étude Op. 10, No. 12 ('Revolutionary')",
                composer: "Frédéric Chopin",
                instrument: "Piano",
                categoryRaw: PieceCategory.ETUDE.rawValue,
                durationMinutes: 40,
                durationSecondsTotal: 40 * 60,
                practicedBpm: 124,
                targetBpmSnapshot: 160,
                measuresPracticed: "m. 1–41",
                focusTags: "Technique,Rhythm",
                sessionNotes: "Reached a clean personal best of 124 BPM on the first page! Wrist stayed supple through m. 29 modulation.",
                nextSessionGoal: "Do a slow tension-release pass next session.",
                moodRating: 5,
                timestampEpochMillis: now - 4 * day - 2 * hour
            ),
            PracticeSession(
                id: 6,
                pieceId: bachPartita.id,
                sectionId: courante.id,
                pieceTitle: "Partita No. 2 in D Minor, BWV 1004",
                sectionName: "II. Courante",
                composer: "J.S. Bach",
                instrument: "Violin",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 35,
                durationSecondsTotal: 35 * 60,
                practicedBpm: 92,
                targetBpmSnapshot: 116,
                measuresPracticed: "First half",
                focusTags: "Articulation,Phrasing",
                sessionNotes: "Worked on buoyant 3/4 dance pulse and crisp up-bow dotted pickups.",
                nextSessionGoal: "Clarify articulation contrast between slurred triplets and dotted eighths.",
                moodRating: 4,
                timestampEpochMillis: now - 5 * day - 3 * hour
            ),
            PracticeSession(
                id: 7,
                pieceId: bachPartita.id,
                sectionId: allemande.id,
                pieceTitle: "Partita No. 2 in D Minor, BWV 1004",
                sectionName: "I. Allemande",
                composer: "J.S. Bach",
                instrument: "Violin",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 40,
                durationSecondsTotal: 40 * 60,
                practicedBpm: 60,
                targetBpmSnapshot: 72,
                measuresPracticed: "m. 1–16",
                focusTags: "Phrasing,Bow Control",
                sessionNotes: "First clean play-through of Part A at 60 BPM with musical breathing at cadences.",
                nextSessionGoal: "Check m. 15–20 intonation with drone.",
                moodRating: 5,
                timestampEpochMillis: now - 6 * day - 2 * hour
            ),
            PracticeSession(
                id: 8,
                pieceId: clairDeLune.id,
                pieceTitle: "Clair de Lune (Suite bergamasque)",
                composer: "Claude Debussy",
                instrument: "Piano",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 50,
                durationSecondsTotal: 50 * 60,
                practicedBpm: 64,
                targetBpmSnapshot: 66,
                measuresPracticed: "Full piece",
                focusTags: "Memorization,Dynamics",
                sessionNotes: "Full memory run at 64 BPM! Only slight hesitation entering the Calmato section.",
                nextSessionGoal: "Polish m. 27–42 left-hand pp texture.",
                moodRating: 5,
                timestampEpochMillis: now - 8 * day
            ),
            PracticeSession(
                id: 9,
                pieceId: scales.id,
                pieceTitle: "4-Octave Major & Melodic Minor Scales + Arpeggios",
                composer: "Daily Technique",
                instrument: "Piano",
                categoryRaw: PieceCategory.TECHNIQUE.rawValue,
                durationMinutes: 40,
                durationSecondsTotal: 40 * 60,
                practicedBpm: 116,
                targetBpmSnapshot: 132,
                measuresPracticed: "Sharp keys",
                focusTags: "Technique,Rhythm",
                sessionNotes: "Metronome on beats 2 & 4 only to test internal subdivision stability.",
                nextSessionGoal: "Flat keys next week.",
                moodRating: 4,
                timestampEpochMillis: now - 9 * day
            ),
            PracticeSession(
                id: 10,
                pieceId: chopinEtude.id,
                pieceTitle: "Étude Op. 10, No. 12 ('Revolutionary')",
                composer: "Frédéric Chopin",
                instrument: "Piano",
                categoryRaw: PieceCategory.ETUDE.rawValue,
                durationMinutes: 50,
                durationSecondsTotal: 50 * 60,
                practicedBpm: 114,
                targetBpmSnapshot: 160,
                measuresPracticed: "m. 1–28",
                focusTags: "Technique,Rhythm",
                sessionNotes: "Stepped up from 108 to 114 BPM in 4-note rhythmic bursts.",
                nextSessionGoal: "Aim for 120+ BPM on opening cascade.",
                moodRating: 4,
                timestampEpochMillis: now - 10 * day
            ),
            PracticeSession(
                id: 11,
                pieceId: autumnLeaves.id,
                pieceTitle: "Autumn Leaves (Chord Melody & Solo)",
                composer: "Joseph Kosma",
                instrument: "Guitar",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 55,
                durationSecondsTotal: 55 * 60,
                practicedBpm: 132,
                targetBpmSnapshot: 144,
                measuresPracticed: "Head + 3 Choruses",
                focusTags: "Phrasing,Rhythm",
                sessionNotes: "Swing eighth subdivisions locked in with clicks on 2 & 4 (66 half-note click).",
                nextSessionGoal: "Refresh chorus 2 drop-2 voicings.",
                moodRating: 5,
                timestampEpochMillis: now - 11 * day
            ),
            PracticeSession(
                id: 12,
                pieceId: clairDeLune.id,
                pieceTitle: "Clair de Lune (Suite bergamasque)",
                composer: "Claude Debussy",
                instrument: "Piano",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 65,
                durationSecondsTotal: 65 * 60,
                practicedBpm: 58,
                targetBpmSnapshot: 66,
                measuresPracticed: "Pages 2–4",
                focusTags: "Sight-Reading,Memorization",
                sessionNotes: "Memorized harmonic skeleton of the middle section.",
                nextSessionGoal: "Bring tempo toward 62–64 BPM.",
                moodRating: 4,
                timestampEpochMillis: now - 15 * day
            ),
            PracticeSession(
                id: 13,
                pieceId: bachPartita.id,
                sectionId: allemande.id,
                pieceTitle: "Partita No. 2 in D Minor, BWV 1004",
                sectionName: "I. Allemande",
                composer: "J.S. Bach",
                instrument: "Violin",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                durationMinutes: 65,
                durationSecondsTotal: 65 * 60,
                practicedBpm: 50,
                targetBpmSnapshot: 72,
                measuresPracticed: "m. 1–24",
                focusTags: "Intonation,Slow Practice",
                sessionNotes: "Mapped fingerings and bow divisions across Part A.",
                nextSessionGoal: "Build comfort to 56–60 BPM.",
                moodRating: 4,
                timestampEpochMillis: now - 16 * day
            ),
            PracticeSession(
                id: 14,
                pieceId: chopinEtude.id,
                pieceTitle: "Étude Op. 10, No. 12 ('Revolutionary')",
                composer: "Frédéric Chopin",
                instrument: "Piano",
                categoryRaw: PieceCategory.ETUDE.rawValue,
                durationMinutes: 60,
                durationSecondsTotal: 60 * 60,
                practicedBpm: 104,
                targetBpmSnapshot: 160,
                measuresPracticed: "m. 1–18",
                focusTags: "Technique,Slow Practice",
                sessionNotes: "Left-hand alone blocking chords to eliminate thumb tension.",
                nextSessionGoal: "Step up to 114 BPM.",
                moodRating: 4,
                timestampEpochMillis: now - 18 * day
            )
        ]

        for session in sessions {
            store.insertRawSession(session)
        }

        // MARK: Routine templates

        let pianoRoutine = RoutineTemplate(
            id: 1,
            name: "45-Min Piano Daily Routine",
            descriptionText: "Balanced warmup scales, Chopin technical study, and Debussy repertoire.",
            instrument: "Piano",
            createdAtEpochMillis: now - 20 * day
        )
        store.insertRawRoutine(pianoRoutine)

        let pianoItems: [RoutineTemplateItem] = [
            RoutineTemplateItem(
                id: 1,
                routineId: pianoRoutine.id,
                orderIndex: 0,
                pieceId: scales.id,
                customTitle: "4-Octave Major & Melodic Minor Scales + Arpeggios",
                categoryRaw: PieceCategory.TECHNIQUE.rawValue,
                targetMinutes: 10,
                suggestedBpm: 112,
                notesPrompt: "Listen for even 16th-note subdivisions and relaxed thumbs."
            ),
            RoutineTemplateItem(
                id: 2,
                routineId: pianoRoutine.id,
                orderIndex: 1,
                pieceId: chopinEtude.id,
                customTitle: "Étude Op. 10, No. 12 ('Revolutionary')",
                categoryRaw: PieceCategory.ETUDE.rawValue,
                targetMinutes: 15,
                suggestedBpm: 116,
                notesPrompt: "Dotted-rhythm left-hand cascades before full tempo."
            ),
            RoutineTemplateItem(
                id: 3,
                routineId: pianoRoutine.id,
                orderIndex: 2,
                pieceId: clairDeLune.id,
                customTitle: "Clair de Lune (Suite bergamasque)",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                targetMinutes: 20,
                suggestedBpm: 62,
                notesPrompt: "Shape top voice cantabile over pp left-hand arpeggios."
            )
        ]

        let violinRoutine = RoutineTemplate(
            id: 2,
            name: "35-Min Baroque Violin Focus",
            descriptionText: "Intonation drone warmup followed by Bach Partita movements.",
            instrument: "Violin",
            createdAtEpochMillis: now - 14 * day
        )
        store.insertRawRoutine(violinRoutine)

        let violinItems: [RoutineTemplateItem] = [
            RoutineTemplateItem(
                id: 4,
                routineId: violinRoutine.id,
                orderIndex: 0,
                pieceId: nil,
                sectionId: nil,
                customTitle: "Open-String Resonance & Double-Stop Drones",
                categoryRaw: PieceCategory.TECHNIQUE.rawValue,
                targetMinutes: 10,
                suggestedBpm: 60,
                notesPrompt: "Pure fifths and relaxed bow arm weight."
            ),
            RoutineTemplateItem(
                id: 5,
                routineId: violinRoutine.id,
                orderIndex: 1,
                pieceId: bachPartita.id,
                sectionId: allemande.id,
                customTitle: "Partita No. 2 — I. Allemande",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                targetMinutes: 15,
                suggestedBpm: 56,
                notesPrompt: "Check diminished leaps in m. 15–17."
            ),
            RoutineTemplateItem(
                id: 6,
                routineId: violinRoutine.id,
                orderIndex: 2,
                pieceId: bachPartita.id,
                sectionId: courante.id,
                customTitle: "Partita No. 2 — II. Courante",
                categoryRaw: PieceCategory.REPERTOIRE.rawValue,
                targetMinutes: 10,
                suggestedBpm: 92,
                notesPrompt: "Buoyant 3/4 dance feel on dotted pickups."
            )
        ]

        for item in pianoItems + violinItems {
            store.insertRawRoutineItem(item)
        }

        store.commitRawInserts()
    }
}
