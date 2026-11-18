package com.ostinato.practicelog

import com.ostinato.practicelog.data.local.PieceCategory
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceStatus
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.data.local.RoutineTemplateEntity
import com.ostinato.practicelog.data.local.RoutineTemplateItemEntity
import com.ostinato.practicelog.util.AnalyticsCalculator
import com.ostinato.practicelog.util.BackupAndReportManager
import com.ostinato.practicelog.util.BackupPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakAndBackupTest {

    @Test
    fun weeklyConsistencyStreak_preservesStreakAcrossRestDaysAndInProgressWeek() {
        val wednesdayNow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 4, 12, 0, 0) // Wednesday Nov 4, 2026
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisMonday = AnalyticsCalculator.getMondayStartOfWeekMillis(wednesdayNow)
        val oneDay = 24L * 60L * 60L * 1000L
        val lastMonday = thisMonday - 7 * oneDay
        val twoWeeksAgoMonday = thisMonday - 14 * oneDay

        val sessions = listOf(
            // Current week in progress (only 45 mins so far < 180 goal)
            sampleSession(id = 1L, mins = 45, ts = thisMonday + oneDay),
            // Last week: 190 mins spread over 2 days (5 rest days!) -> meets 180 goal
            sampleSession(id = 2L, mins = 100, ts = lastMonday + oneDay),
            sampleSession(id = 3L, mins = 90, ts = lastMonday + 4 * oneDay),
            // Two weeks ago: 180 mins -> meets 180 goal
            sampleSession(id = 4L, mins = 180, ts = twoWeeksAgoMonday + 2 * oneDay)
        )

        val streakWhileInProgress = AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions = sessions,
            weeklyGoalMinutes = 180,
            nowEpochMillis = wednesdayNow
        )
        assertEquals(2, streakWhileInProgress)

        // Once current week hits 180 mins, streak advances to 3 immediately
        val sessionsGoalMetToday = sessions + sampleSession(id = 5L, mins = 140, ts = wednesdayNow)
        val streakAfterGoalMet = AnalyticsCalculator.calculateWeeklyConsistencyStreak(
            sessions = sessionsGoalMetToday,
            weeklyGoalMinutes = 180,
            nowEpochMillis = wednesdayNow
        )
        assertEquals(3, streakAfterGoalMet)
    }

    @Test
    fun backupJsonRoundTrip_preservesPiecesMovementsSessionsAndRoutines() {
        val piece = RepertoirePieceEntity(
            id = 10L,
            title = "Partita No. 2",
            composer = "J.S. Bach",
            instrument = "Violin",
            category = PieceCategory.REPERTOIRE.name,
            keySignature = "D Minor",
            timeSignatureBeats = 4,
            timeSignatureNoteValue = 4,
            bestBpm = 64,
            lastPracticedBpm = 56,
            targetBpm = 72,
            status = PieceStatus.POLISHING.name,
            practiceNotes = "Relaxed right wrist",
            nextSessionGoal = "Spot check m. 16",
            totalMinutesPracticed = 95,
            lastPracticedAtEpochMillis = 1700000000000L
        )
        val section = PieceSectionEntity(
            id = 20L,
            pieceId = 10L,
            name = "I. Allemande",
            orderIndex = 0,
            bestBpm = 64,
            lastPracticedBpm = 56,
            targetBpm = 72,
            timeSignatureBeats = 4,
            timeSignatureNoteValue = 4,
            sectionNotes = "Upper half detaché",
            nextSessionGoal = "Drone D intonation",
            totalMinutesPracticed = 95
        )
        val session = sampleSession(id = 30L, mins = 40, ts = 1700000000000L)
        val routine = RoutineTemplateEntity(id = 40L, name = "Morning Warmup", instrument = "Violin")
        val routineItem = RoutineTemplateItemEntity(
            id = 50L,
            routineId = 40L,
            orderIndex = 0,
            pieceId = 10L,
            sectionId = 20L,
            customTitle = "Partita Allemande",
            targetMinutes = 20,
            suggestedBpm = 56
        )

        val original = BackupPayload(
            pieces = listOf(piece),
            sections = listOf(section),
            sessions = listOf(session),
            routines = listOf(routine),
            routineItems = listOf(routineItem),
            weeklyGoalMinutes = 210,
            customInstruments = listOf("Baroque Violin"),
            customFocusTags = listOf("Drone Intonation")
        )

        val json = BackupAndReportManager.serializeBackupToJson(original)
        val restored = BackupAndReportManager.parseBackupFromJson(json)

        assertEquals(210, restored.weeklyGoalMinutes)
        assertEquals(listOf("Baroque Violin"), restored.customInstruments)
        assertEquals(listOf("Drone Intonation"), restored.customFocusTags)
        assertEquals(1, restored.pieces.size)
        assertEquals(64, restored.pieces.first().bestBpm)
        assertEquals(56, restored.pieces.first().lastPracticedBpm)
        assertEquals(1, restored.sections.size)
        assertEquals("I. Allemande", restored.sections.first().name)
        assertEquals(1, restored.routines.size)
        assertEquals(1, restored.routineItems.size)
        assertTrue(json.contains("Partita No. 2"))
    }

    private fun sampleSession(id: Long, mins: Int, ts: Long) = PracticeSessionEntity(
        id = id,
        pieceId = 10L,
        sectionId = 20L,
        pieceTitle = "Partita No. 2",
        sectionName = "I. Allemande",
        composer = "J.S. Bach",
        instrument = "Violin",
        category = PieceCategory.REPERTOIRE.name,
        durationMinutes = mins,
        durationSecondsTotal = mins * 60,
        practicedBpm = 58,
        targetBpmSnapshot = 72,
        measuresPracticed = "m. 1–16",
        focusTags = "Intonation",
        sessionNotes = "Clean tone",
        nextSessionGoal = "m. 17–24",
        moodRating = 5,
        timestampEpochMillis = ts
    )
}
