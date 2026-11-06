package com.cadence.practicelog.util

import com.cadence.practicelog.data.local.PieceCategory
import com.cadence.practicelog.data.local.PieceStatus
import com.cadence.practicelog.data.local.PracticeSessionEntity
import com.cadence.practicelog.data.local.RepertoirePieceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DailyPracticeStat(
    val dayLabel: String,
    val dateKey: String,
    val minutesPracticed: Int,
    val isToday: Boolean
)

data class HeatmapDayCell(
    val dayOfMonth: Int,
    val dateKey: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

data class BpmHistoryPoint(
    val dateLabel: String,
    val bpm: Int,
    val measures: String,
    val sessionNotes: String
)

data class ShareSlice(
    val label: String,
    val minutes: Int,
    val fraction: Float
)

data class PracticeBreakdownStats(
    val byCategory: List<ShareSlice>,
    val byInstrument: List<ShareSlice>,
    val topPieces: List<ShareSlice>
)

data class RustyPieceAlert(
    val piece: RepertoirePieceEntity,
    val daysSincePracticed: Int? // null if never practiced yet
)

object AnalyticsCalculator {

    /**
     * Returns the Monday 00:00:00.000 timestamp of the ISO week (Mon–Sun) containing [epochMillis].
     */
    fun getMondayStartOfWeekMillis(epochMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        return cal.timeInMillis
    }

    /**
     * Calculates total practice minutes logged during the current Mon–Sun week.
     */
    fun calculateCurrentWeekMinutes(
        sessions: List<PracticeSessionEntity>,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Int {
        val thisWeekMonday = getMondayStartOfWeekMillis(nowEpochMillis)
        val nextWeekMonday = advanceWeeks(thisWeekMonday, 1)
        return sessions
            .filter { it.timestampEpochMillis in thisWeekMonday until nextWeekMonday }
            .sumOf { it.durationMinutes }
    }

    /**
     * Weekly Consistency Streak:
     * Counts consecutive Monday–Sunday calendar weeks meeting [weeklyGoalMinutes].
     * - If the current in-progress Mon–Sun week already meets [weeklyGoalMinutes], it counts (+1) plus consecutive prior weeks.
     * - If the current Mon–Sun week hasn't met the goal yet, the streak stays alive from last week's completed streak!
     */
    fun calculateWeeklyConsistencyStreak(
        sessions: List<PracticeSessionEntity>,
        weeklyGoalMinutes: Int,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Int {
        if (sessions.isEmpty() || weeklyGoalMinutes <= 0) return 0

        val minutesByMondayStart = sessions.groupBy { getMondayStartOfWeekMillis(it.timestampEpochMillis) }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }

        val currentMonday = getMondayStartOfWeekMillis(nowEpochMillis)
        val currentWeekMins = minutesByMondayStart[currentMonday] ?: 0

        var streak = 0
        var cursorMonday = if (currentWeekMins >= weeklyGoalMinutes) {
            streak = 1
            advanceWeeks(currentMonday, -1)
        } else {
            // Current week still in progress; check prior weeks starting from last Monday
            advanceWeeks(currentMonday, -1)
        }

        // Walk backward week-by-week using Calendar so DST transitions never shift Monday midnight
        while (true) {
            val mins = minutesByMondayStart[cursorMonday] ?: 0
            if (mins >= weeklyGoalMinutes) {
                streak++
                cursorMonday = advanceWeeks(cursorMonday, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun advanceWeeks(mondayMillis: Long, deltaWeeks: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = mondayMillis
        cal.add(Calendar.WEEK_OF_YEAR, deltaWeeks)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun calculateLast7DaysStats(
        sessions: List<PracticeSessionEntity>,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<DailyPracticeStat> {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNameFmt = SimpleDateFormat("EEE", Locale.US)
        val minutesByDay = sessions.groupBy { dateFmt.format(Date(it.timestampEpochMillis)) }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }

        val result = mutableListOf<DailyPracticeStat>()
        for (offset in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = nowEpochMillis
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val key = dateFmt.format(cal.time)
            val dayLabel = dayNameFmt.format(cal.time)
            result.add(
                DailyPracticeStat(
                    dayLabel = dayLabel,
                    dateKey = key,
                    minutesPracticed = minutesByDay[key] ?: 0,
                    isToday = (offset == 0)
                )
            )
        }
        return result
    }

    /**
     * Builds a Monday-aligned 7-column calendar grid for the given [year] and [monthZeroBased] (0 = Jan .. 11 = Dec).
     */
    fun buildMonthlyHeatmapCells(
        sessions: List<PracticeSessionEntity>,
        year: Int,
        monthZeroBased: Int,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<HeatmapDayCell> {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = dateFmt.format(Date(nowEpochMillis))

        val sessionsByDay = sessions.groupBy { dateFmt.format(Date(it.timestampEpochMillis)) }

        val firstOfMonth = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val dow = firstOfMonth.get(Calendar.DAY_OF_WEEK)
        val leadingEmptyDays = if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY)
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<HeatmapDayCell>()

        // Leading days from previous month
        for (i in leadingEmptyDays downTo 1) {
            val c = (firstOfMonth.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -i)
            }
            val key = dateFmt.format(c.time)
            val daySessions = sessionsByDay[key].orEmpty()
            cells.add(
                HeatmapDayCell(
                    dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
                    dateKey = key,
                    totalMinutes = daySessions.sumOf { it.durationMinutes },
                    sessionCount = daySessions.size,
                    isToday = key == todayKey,
                    isCurrentMonth = false
                )
            )
        }

        // Current month days
        for (day in 1..daysInMonth) {
            val c = (firstOfMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val key = dateFmt.format(c.time)
            val daySessions = sessionsByDay[key].orEmpty()
            cells.add(
                HeatmapDayCell(
                    dayOfMonth = day,
                    dateKey = key,
                    totalMinutes = daySessions.sumOf { it.durationMinutes },
                    sessionCount = daySessions.size,
                    isToday = key == todayKey,
                    isCurrentMonth = true
                )
            )
        }

        // Trailing days to complete final 7-day row
        val remainder = cells.size % 7
        if (remainder != 0) {
            val trailingCount = 7 - remainder
            val lastDayCal = (firstOfMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, daysInMonth)
            }
            for (i in 1..trailingCount) {
                lastDayCal.add(Calendar.DAY_OF_MONTH, 1)
                val key = dateFmt.format(lastDayCal.time)
                val daySessions = sessionsByDay[key].orEmpty()
                cells.add(
                    HeatmapDayCell(
                        dayOfMonth = lastDayCal.get(Calendar.DAY_OF_MONTH),
                        dateKey = key,
                        totalMinutes = daySessions.sumOf { it.durationMinutes },
                        sessionCount = daySessions.size,
                        isToday = key == todayKey,
                        isCurrentMonth = false
                    )
                )
            }
        }

        return cells
    }

    fun calculateBreakdownStats(sessions: List<PracticeSessionEntity>): PracticeBreakdownStats {
        val totalMins = sessions.sumOf { it.durationMinutes }.coerceAtLeast(1)

        val byCategory = PieceCategory.entries.map { cat ->
            val mins = sessions.filter { it.pieceCategory == cat }.sumOf { it.durationMinutes }
            ShareSlice(
                label = cat.shortLabel,
                minutes = mins,
                fraction = (mins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.minutes }

        val byInstrument = sessions
            .groupBy { it.instrument.ifBlank { "Other" } }
            .map { (inst, list) ->
                val mins = list.sumOf { it.durationMinutes }
                ShareSlice(
                    label = inst,
                    minutes = mins,
                    fraction = (mins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.minutes }

        val topPieces = sessions
            .groupBy { it.pieceTitle }
            .map { (title, list) ->
                val mins = list.sumOf { it.durationMinutes }
                ShareSlice(
                    label = title,
                    minutes = mins,
                    fraction = (mins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.minutes }
            .take(5)

        return PracticeBreakdownStats(
            byCategory = byCategory,
            byInstrument = byInstrument,
            topPieces = topPieces
        )
    }

    /**
     * Identifies active repertoire/etudes (or Polishing/Mastered items) that haven't been practiced
     * in at least [thresholdDays] days (or never practiced yet).
     */
    fun findRustyPieces(
        pieces: List<RepertoirePieceEntity>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        thresholdDays: Int = 5
    ): List<RustyPieceAlert> {
        val oneDayMs = TimeUnit.DAYS.toMillis(1)
        return pieces
            .filter { it.pieceCategory != PieceCategory.TECHNIQUE || it.pieceStatus != PieceStatus.MASTERED }
            .mapNotNull { piece ->
                val lastEpoch = piece.lastPracticedAtEpochMillis
                if (lastEpoch == null) {
                    RustyPieceAlert(piece = piece, daysSincePracticed = null)
                } else {
                    val days = ((nowEpochMillis - lastEpoch) / oneDayMs).toInt().coerceAtLeast(0)
                    if (days >= thresholdDays) {
                        RustyPieceAlert(piece = piece, daysSincePracticed = days)
                    } else {
                        null
                    }
                }
            }
            .sortedWith(
                compareByDescending<RustyPieceAlert> { it.daysSincePracticed ?: Int.MAX_VALUE }
                    .thenBy { it.piece.pieceStatus == PieceStatus.MASTERED }
            )
    }

    fun buildBpmProgression(
        pieceId: Long,
        sectionId: Long?,
        sessions: List<PracticeSessionEntity>
    ): List<BpmHistoryPoint> {
        val shortDateFmt = SimpleDateFormat("MMM d", Locale.US)
        return sessions
            .filter { session ->
                session.pieceId == pieceId && (sectionId == null || session.sectionId == sectionId)
            }
            .sortedBy { it.timestampEpochMillis }
            .map { session ->
                BpmHistoryPoint(
                    dateLabel = shortDateFmt.format(Date(session.timestampEpochMillis)),
                    bpm = session.practicedBpm,
                    measures = session.measuresPracticed,
                    sessionNotes = session.sessionNotes
                )
            }
    }
}
