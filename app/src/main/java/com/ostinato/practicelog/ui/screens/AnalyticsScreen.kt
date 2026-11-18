package com.ostinato.practicelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceStatus
import com.ostinato.practicelog.data.local.PieceWithSections
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.util.AnalyticsCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    sessions: List<PracticeSessionEntity>,
    piecesWithSections: List<PieceWithSections>,
    weeklyGoalMinutes: Int,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onPracticePieceNow: (RepertoirePieceEntity, PieceSectionEntity?) -> Unit
) {
    val scrollState = rememberScrollState()
    var showGoalDialog by remember { mutableStateOf(false) }

    val pieces = remember(piecesWithSections) { piecesWithSections.map { it.piece } }
    val weeklyStreak = remember(sessions, weeklyGoalMinutes) {
        AnalyticsCalculator.calculateWeeklyConsistencyStreak(sessions, weeklyGoalMinutes)
    }
    val currentMonSunMinutes = remember(sessions) {
        AnalyticsCalculator.calculateCurrentWeekMinutes(sessions)
    }
    val last7DaysStats = remember(sessions) {
        AnalyticsCalculator.calculateLast7DaysStats(sessions)
    }
    val breakdownStats = remember(sessions) {
        AnalyticsCalculator.calculateBreakdownStats(sessions)
    }
    val rustyPieces = remember(pieces) {
        AnalyticsCalculator.findRustyPieces(pieces, thresholdDays = 5)
    }

    val allTimeMinutes = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val masteredCount = remember(pieces) { pieces.count { it.pieceStatus == PieceStatus.MASTERED } }

    // Calendar Heatmap Month State
    val nowCal = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }

    val heatmapCells = remember(sessions, calYear, calMonth) {
        AnalyticsCalculator.buildMonthlyHeatmapCells(sessions, calYear, calMonth)
    }
    val monthTitle = remember(calYear, calMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, calYear)
            set(Calendar.MONTH, calMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(c.time)
    }
    val selectedDateSessions = remember(selectedDateKey, sessions) {
        if (selectedDateKey == null) emptyList()
        else {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sessions.filter { fmt.format(Date(it.timestampEpochMillis)) == selectedDateKey }
        }
    }

    // Breakdown View Mode (0 = Category, 1 = Instrument, 2 = Top Pieces)
    var breakdownMode by remember { mutableIntStateOf(0) }

    // BPM Progression Piece + Section Selector
    var selectedPwsForChart by remember(piecesWithSections) {
        mutableStateOf(piecesWithSections.firstOrNull())
    }
    var selectedSectionForChart by remember(selectedPwsForChart) {
        mutableStateOf<PieceSectionEntity?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics, Streaks & BPM",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. SUMMARY METRIC CARDS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = "Weekly Streak",
                value = "$weeklyStreak Wks",
                subtitle = "Mon–Sun goal",
                icon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Total Logged",
                value = "${allTimeMinutes / 60}h ${allTimeMinutes % 60}m",
                subtitle = "${sessions.size} sessions",
                icon = {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Mastered",
                value = "$masteredCount / ${pieces.size}",
                subtitle = "Library",
                icon = {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 2. WEEKLY CONSISTENCY GOAL CARD (Mon–Sun)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Consistency Goal (Mon–Sun)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currentMonSunMinutes of $weeklyGoalMinutes min logged this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { showGoalDialog = true }) {
                        Text("Edit Goal")
                    }
                }

                val goalProgress = if (weeklyGoalMinutes <= 0) 0f else {
                    (currentMonSunMinutes.toFloat() / weeklyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                }

                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (goalProgress >= 1f) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = if (goalProgress >= 1f) {
                        "This week's goal is complete! Take rest days freely without losing your ${weeklyStreak}-week streak."
                    } else {
                        "${(weeklyGoalMinutes - currentMonSunMinutes).coerceAtLeast(0)} min remaining by Sunday to extend your weekly streak (rest days never break it!)."
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (goalProgress >= 1f) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // 3. RUSTY / NEEDS ATTENTION REPERTOIRE CARD
        if (rustyPieces.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Rusty / Needs Attention",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pieces not practiced in 5+ days — keep them fresh!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    rustyPieces.take(4).forEach { alert ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.piece.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val gapLabel = if (alert.daysSincePracticed == null) {
                                        "Not practiced yet"
                                    } else {
                                        "${alert.daysSincePracticed} days since last session"
                                    }
                                    Text(
                                        text = "${alert.piece.instrument} • ${alert.piece.pieceStatus.label} • $gapLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onPracticePieceNow(alert.piece, null) }
                                ) {
                                    Text("Practice")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. MONTHLY PRACTICE CALENDAR / HEATMAP (Tap any date to inspect sessions & notes)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                if (calMonth == 0) {
                                    calMonth = 11
                                    calYear -= 1
                                } else {
                                    calMonth -= 1
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }
                        IconButton(
                            onClick = {
                                if (calMonth == 11) {
                                    calMonth = 0
                                    calYear += 1
                                } else {
                                    calMonth += 1
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }

                // Weekday Header (Mon..Sun)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7-Column Heatmap Rows
                heatmapCells.chunked(7).forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekRow.forEach { cell ->
                            val isSelected = selectedDateKey == cell.dateKey
                            val bg = when {
                                !cell.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                cell.totalMinutes >= 60 -> MaterialTheme.colorScheme.primary
                                cell.totalMinutes >= 30 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                                cell.totalMinutes > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                            val textColor = when {
                                !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                cell.totalMinutes >= 30 -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(
                                        width = when {
                                            isSelected -> 2.5.dp
                                            cell.isToday -> 1.5.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.secondary
                                            cell.isToday -> MaterialTheme.colorScheme.primary
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedDateKey = if (isSelected) null else cell.dateKey
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = cell.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (cell.isToday || cell.totalMinutes > 0) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = textColor
                                    )
                                    if (cell.totalMinutes > 0 && cell.isCurrentMonth) {
                                        Text(
                                            text = "${cell.totalMinutes}m",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tapped Date Session & Notes Drilldown
                selectedDateKey?.let { dateKey ->
                    HorizontalDivider()
                    Text(
                        text = "Sessions on $dateKey (${selectedDateSessions.sumOf { it.durationMinutes }} min total):",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedDateSessions.isEmpty()) {
                        Text(
                            text = "Rest day — no sessions logged on this date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectedDateSessions.forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val secPart = if (s.sectionName.isNotBlank()) " — ${s.sectionName}" else ""
                                    Text(
                                        text = "${s.pieceTitle}$secPart • ${s.durationMinutes}m @ ${s.practicedBpm} BPM",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (s.measuresPracticed.isNotBlank()) {
                                        Text(
                                            text = "Measures: ${s.measuresPracticed}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (s.sessionNotes.isNotBlank()) {
                                        Text(
                                            text = s.sessionNotes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. PRACTICE TIME BREAKDOWN (Category / Instrument / Top Pieces)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Practice Time Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "By Category", 1 to "By Instrument", 2 to "Top Pieces").forEach { (idx, lbl) ->
                        FilterChip(
                            selected = breakdownMode == idx,
                            onClick = { breakdownMode = idx },
                            label = { Text(lbl) }
                        )
                    }
                }

                val activeSlices = when (breakdownMode) {
                    0 -> breakdownStats.byCategory
                    1 -> breakdownStats.byInstrument
                    else -> breakdownStats.topPieces
                }

                if (sessions.isEmpty() || activeSlices.isEmpty()) {
                    Text(
                        text = "Log sessions to see your practice distribution.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeSlices.forEach { slice ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${slice.minutes}m (${(slice.fraction * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { slice.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 6. LAST 7 DAYS PRACTICE ACTIVITY BAR CHART
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Last 7 Days Practice Activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val maxDayMins = (last7DaysStats.maxOfOrNull { it.minutesPracticed } ?: 45).coerceAtLeast(30)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7DaysStats.forEach { stat ->
                        val heightFraction = (stat.minutesPracticed.toFloat() / maxDayMins.toFloat()).coerceIn(0.06f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = if (stat.minutesPracticed > 0) "${stat.minutesPracticed}m" else "-",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(heightFraction * 0.72f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        when {
                                            stat.isToday -> MaterialTheme.colorScheme.primary
                                            stat.minutesPracticed > 0 -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stat.dayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (stat.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 7. BPM TEMPO PROGRESSION TRACKER (Per Piece OR Movement)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "BPM Tempo Progression Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Inspect tempo history (including slow-practice passes) vs. Best & Target BPM",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    piecesWithSections.forEach { pws ->
                        val selected = selectedPwsForChart?.piece?.id == pws.piece.id
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedPwsForChart = pws
                                selectedSectionForChart = null
                            },
                            label = { Text(pws.piece.title) }
                        )
                    }
                }

                val activePws = selectedPwsForChart ?: piecesWithSections.firstOrNull()
                if (activePws != null) {
                    val activePiece = activePws.piece
                    val activeSections = activePws.sortedSections

                    if (activeSections.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedSectionForChart == null,
                                onClick = { selectedSectionForChart = null },
                                label = { Text("All Movements") }
                            )
                            activeSections.forEach { sec ->
                                FilterChip(
                                    selected = selectedSectionForChart?.id == sec.id,
                                    onClick = { selectedSectionForChart = sec },
                                    label = { Text(sec.name) }
                                )
                            }
                        }
                    }

                    val history = remember(activePiece.id, selectedSectionForChart?.id, sessions) {
                        AnalyticsCalculator.buildBpmProgression(
                            pieceId = activePiece.id,
                            sectionId = selectedSectionForChart?.id,
                            sessions = sessions
                        )
                    }

                    val bestRef = selectedSectionForChart?.bestBpm ?: activePiece.bestBpm
                    val lastRef = selectedSectionForChart?.lastPracticedBpm ?: activePiece.lastPracticedBpm
                    val targetRef = selectedSectionForChart?.targetBpm ?: activePiece.targetBpm

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Best $bestRef • Last $lastRef BPM",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Target: $targetRef BPM",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (history.isEmpty()) {
                        Text(
                            text = "No logged sessions for this selection yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        BpmLineChart(
                            points = history.map { it.bpm },
                            labels = history.map { it.dateLabel },
                            targetBpm = targetRef
                        )
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        var sliderVal by remember { mutableFloatStateOf(weeklyGoalMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Weekly Practice Goal (Mon–Sun)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${sliderVal.toInt()} minutes / week (${sliderVal.toInt() / 60}h ${sliderVal.toInt() % 60}m)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 30f..600f,
                        steps = 18
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateWeeklyGoal(sliderVal.toInt())
                        showGoalDialog = false
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BpmLineChart(
    points: List<Int>,
    labels: List<String>,
    targetBpm: Int
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val targetLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    val minVal = ((points.minOrNull() ?: 40).coerceAtMost(targetBpm) - 10).coerceAtLeast(20)
    val maxVal = ((points.maxOrNull() ?: 120).coerceAtLeast(targetBpm) + 10).coerceAtLeast(minVal + 20)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val range = (maxVal - minVal).toFloat().coerceAtLeast(1f)

                val targetY = height - ((targetBpm - minVal) / range) * height
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, targetY.coerceIn(0f, height)),
                    end = Offset(width, targetY.coerceIn(0f, height)),
                    strokeWidth = 3f
                )

                drawLine(
                    color = gridColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 2f
                )

                if (points.size == 1) {
                    val y = height - ((points.first() - minVal) / range) * height
                    drawCircle(
                        color = lineColor,
                        radius = 10f,
                        center = Offset(width / 2f, y)
                    )
                } else if (points.size > 1) {
                    val stepX = width / (points.size - 1).toFloat()
                    val path = Path()
                    points.forEachIndexed { idx, bpm ->
                        val x = idx * stepX
                        val y = height - ((bpm - minVal) / range) * height
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    points.forEachIndexed { idx, bpm ->
                        val x = idx * stepX
                        val y = height - ((bpm - minVal) / range) * height
                        drawCircle(
                            color = lineColor,
                            radius = 9f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.zip(labels).forEach { (bpm, dateStr) ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$dateStr: $bpm BPM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
