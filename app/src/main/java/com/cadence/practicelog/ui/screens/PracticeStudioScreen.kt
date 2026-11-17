package com.cadence.practicelog.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cadence.practicelog.data.local.PieceCategory
import com.cadence.practicelog.data.local.PieceSectionEntity
import com.cadence.practicelog.data.local.PieceWithSections
import com.cadence.practicelog.data.local.PracticeSessionEntity
import com.cadence.practicelog.data.local.RepertoirePieceEntity
import com.cadence.practicelog.data.local.RoutineTemplateEntity
import com.cadence.practicelog.data.local.RoutineWithItems
import com.cadence.practicelog.ui.PracticeStudioState
import com.cadence.practicelog.ui.components.LogPastSessionDialog
import com.cadence.practicelog.ui.components.MusicStandFocusOverlay
import com.cadence.practicelog.util.BeatAccentLevel
import com.cadence.practicelog.util.MetronomeEngine
import com.cadence.practicelog.util.SubdivisionMode
import kotlinx.coroutines.delay
import java.util.Locale

private val QUICK_NOTE_TEMPLATES = listOf(
    "Isolated difficult measures at slow tempo",
    "Practiced with metronome on beats 2 & 4",
    "Focused on relaxed posture & tone projection",
    "Clean run-through without stopping"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeStudioScreen(
    studioState: PracticeStudioState,
    piecesWithSections: List<PieceWithSections>,
    sessions: List<PracticeSessionEntity>,
    routines: List<RoutineWithItems>,
    allInstruments: List<String>,
    allFocusTags: List<String>,
    keepScreenAwakeEnabled: Boolean,
    isMetronomePlaying: Boolean,
    currentBeat: Int,
    beatPulseId: Long,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onAdjustMinutes: (Int) -> Unit,
    onTargetDurationChange: (Int) -> Unit,
    onSelectPieceAndSection: (RepertoirePieceEntity?, PieceSectionEntity?) -> Unit,
    onSelectInstrument: (String) -> Unit,
    onSelectCategory: (PieceCategory) -> Unit,
    onCustomTitleChange: (String) -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (Int, Int) -> Unit,
    onCycleBeatAccent: (Int) -> Unit,
    onApplyJazzBackbeat: () -> Unit,
    onSubdivisionChange: (SubdivisionMode) -> Unit,
    onToggleMetronomeMute: () -> Unit,
    onToggleMetronomePlay: () -> Unit,
    onTapTempo: () -> Unit,
    onMeasuresChange: (String) -> Unit,
    onToggleFocusTag: (String) -> Unit,
    onAddCustomFocusTag: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onNextGoalChange: (String) -> Unit,
    onAppendNoteTemplate: (String) -> Unit,
    onMoodRatingChange: (Int) -> Unit,
    onSaveSession: () -> Unit,
    onLogPastSession: (
        Long, Int, RepertoirePieceEntity?, PieceSectionEntity?, String, String,
        PieceCategory, Int, Int, String, List<String>, String, String, Int
    ) -> Unit,
    onLoadRoutine: (RoutineWithItems) -> Unit,
    onAddAdHocQueueItem: (RepertoirePieceEntity?, PieceSectionEntity?, String, Int) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onExitQueueMode: () -> Unit,
    onSaveQueueAsTemplate: (String, String) -> Unit,
    onDeleteRoutineTemplate: (RoutineTemplateEntity) -> Unit,
    onSetMusicStandMode: (Boolean) -> Unit,
    onDismissBanner: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showPieceDropdown by remember { mutableStateOf(false) }
    var showRoutinesDialog by remember { mutableStateOf(false) }
    var showAddQueueBlockDialog by remember { mutableStateOf(false) }
    var showSaveRoutineDialog by remember { mutableStateOf(false) }
    var showLogPastDialog by remember { mutableStateOf(false) }
    var showQuickTagDialog by remember { mutableStateOf(false) }
    var showSessionDetails by remember { mutableStateOf(false) }

    // Resolve Continuity Context for the selected Piece / Movement
    val currentPieceWithSections = remember(studioState.selectedPieceId, piecesWithSections) {
        piecesWithSections.find { it.piece.id == studioState.selectedPieceId }
    }
    val currentPiece = currentPieceWithSections?.piece
    val currentSection = remember(studioState.selectedSectionId, currentPieceWithSections) {
        currentPieceWithSections?.sections?.find { it.id == studioState.selectedSectionId }
    }
    val latestMatchingSession = remember(studioState.selectedPieceId, studioState.selectedSectionId, sessions) {
        if (studioState.selectedPieceId == null) null
        else sessions.firstOrNull { s ->
            s.pieceId == studioState.selectedPieceId &&
                (studioState.selectedSectionId == null || s.sectionId == studioState.selectedSectionId)
        }
    }

    val pinnedNextGoal = (currentSection?.nextSessionGoal?.takeIf { it.isNotBlank() }
        ?: currentPiece?.nextSessionGoal?.takeIf { it.isNotBlank() }).orEmpty()
    val standingScoreNotes = (currentSection?.sectionNotes?.takeIf { it.isNotBlank() }
        ?: currentPiece?.practiceNotes?.takeIf { it.isNotBlank() }).orEmpty()
    val previousSessionSummary = remember(latestMatchingSession) {
        if (latestMatchingSession == null) ""
        else buildString {
            append("${latestMatchingSession.practicedBpm} BPM")
            if (latestMatchingSession.measuresPracticed.isNotBlank()) {
                append(" [${latestMatchingSession.measuresPracticed}]")
            }
            if (latestMatchingSession.sessionNotes.isNotBlank()) {
                append(" — ")
                append(latestMatchingSession.sessionNotes)
            }
        }
    }

    // Visual Beat Pulse on Metronome Card Border (especially prominent when muted!)
    var beatFlash by remember { mutableStateOf(false) }
    LaunchedEffect(beatPulseId, isMetronomePlaying) {
        if (isMetronomePlaying && beatPulseId > 0L) {
            beatFlash = true
            delay(105L)
            beatFlash = false
        } else {
            beatFlash = false
        }
    }
    val metronomeBorderColor by animateColorAsState(
        targetValue = when {
            beatFlash && currentBeat == 1 -> MaterialTheme.colorScheme.primary
            beatFlash -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 85),
        label = "metronomeCardPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // QUICK STUDIO TOOLBAR: Music Stand Focus Mode | Routines & Queue | Log Past Session
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(onClick = { onSetMusicStandMode(true) }) {
                Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Music Stand Mode")
            }

            OutlinedButton(onClick = { showRoutinesDialog = true }) {
                Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (studioState.isQueueMode) "Routine Queue (${studioState.queueItems.size})" else "Routines & Queue")
            }

            OutlinedButton(onClick = { showLogPastDialog = true }) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Past Session")
            }
        }

        // Confirmation Banner
        AnimatedVisibility(visible = studioState.lastSavedBanner != null) {
            studioState.lastSavedBanner?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 1. LIVE PRACTICE STOPWATCH CARD (Count-Up + Silent Target Progress Bar)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compact queue strip — replaces the old standalone queue card.
                if (studioState.isQueueMode && studioState.queueItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = studioState.activeRoutineName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        studioState.queueItems.forEachIndexed { idx, qItem ->
                            val isCurrent = idx == studioState.activeQueueIndex
                            val prefix = when {
                                qItem.isLogged -> "✓ "
                                isCurrent -> "▶ "
                                else -> "${idx + 1}. "
                            }
                            FilterChip(
                                selected = isCurrent,
                                onClick = { onSelectQueueIndex(idx) },
                                label = { Text("$prefix${qItem.title} (${qItem.targetMinutes}m)") }
                            )
                        }
                        TextButton(onClick = { showAddQueueBlockDialog = true }) {
                            Text("+ Block")
                        }
                        TextButton(onClick = { showSaveRoutineDialog = true }) {
                            Text("Save")
                        }
                        TextButton(onClick = onExitQueueMode) {
                            Text("Exit")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (studioState.timerRunning) "SESSION IN PROGRESS" else "LIVE PRACTICE STOPWATCH",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (studioState.timerRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if ((studioState.timerRunning || isMetronomePlaying) && keepScreenAwakeEnabled) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Awake",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatSecondsToClock(studioState.elapsedSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Silent Target Duration Progress Bar (no disruptive alarm when reached)
                Spacer(modifier = Modifier.height(8.dp))
                if (studioState.targetDurationMinutes > 0) {
                    val targetSecs = studioState.targetDurationMinutes * 60
                    val progress = (studioState.elapsedSeconds.toFloat() / targetSecs.toFloat()).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (progress >= 1f) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = if (progress >= 1f) {
                                "${studioState.targetDurationMinutes}m target reached — continuing count-up"
                            } else {
                                "Silent Target: ${studioState.targetDurationMinutes} min (${(progress * 100).toInt()}%)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Target Duration Presets + Elapsed Time Adjustments
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0 to "Open", 10 to "10m", 15 to "15m", 20 to "20m", 30 to "30m").forEach { (mins, lbl) ->
                        FilterChip(
                            selected = studioState.targetDurationMinutes == mins,
                            onClick = { onTargetDurationChange(mins) },
                            label = { Text(lbl) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onAdjustMinutes(-5) }) { Text("-5m") }
                    OutlinedButton(onClick = { onAdjustMinutes(5) }) { Text("+5m") }
                    OutlinedButton(onClick = { onAdjustMinutes(15) }) { Text("+15m") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggleTimer,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (studioState.timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (studioState.timerRunning) "Pause Stopwatch" else "Start Practice",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    FilledTonalIconButton(onClick = onResetTimer) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Stopwatch")
                    }
                }
            }
        }

        // 2. REPERTOIRE & SESSION DETAILS (combined piece selector + notes)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = null,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Repertoire & Session Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "What you are practicing, and what you want to remember",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!studioState.isQueueMode) {
                        TextButton(onClick = { showAddQueueBlockDialog = true }) {
                            Text("+ Queue")
                        }
                    }
                }

                // Compact one-line piece selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPieceDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val display = when {
                            currentPiece != null && currentSection != null ->
                                "${currentPiece.title} › ${currentSection.name}"
                            currentPiece != null ->
                                "${currentPiece.title} • ${currentPiece.timeSignatureLabel}"
                            else -> "Custom / General Warmup & Technique"
                        }
                        Text(
                            text = display,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Choose piece",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPieceDropdown,
                        onDismissRequest = { showPieceDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Custom / General Warmup & Technique") },
                            onClick = {
                                onSelectPieceAndSection(null, null)
                                showPieceDropdown = false
                            }
                        )
                        piecesWithSections.forEach { pws ->
                            val piece = pws.piece
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "${piece.title} (${piece.pieceCategory.shortLabel})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${piece.instrument} • ${piece.timeSignatureLabel} • Best ${piece.bestBpm} / Target ${piece.targetBpm} BPM",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectPieceAndSection(piece, null)
                                    showPieceDropdown = false
                                }
                            )
                            pws.sortedSections.forEach { sec ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "   ↳ ${sec.name} (${sec.timeSignatureLabel} • Best ${sec.bestBpm}/${sec.targetBpm} BPM)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        onSelectPieceAndSection(piece, sec)
                                        showPieceDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Movement Quick Switcher Chips when selected piece has sub-sections
                if (currentPieceWithSections != null && currentPieceWithSections.sortedSections.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = studioState.selectedSectionId == null,
                            onClick = { onSelectPieceAndSection(currentPiece, null) },
                            label = { Text("Full Piece") }
                        )
                        currentPieceWithSections.sortedSections.forEach { sec ->
                            FilterChip(
                                selected = studioState.selectedSectionId == sec.id,
                                onClick = { onSelectPieceAndSection(currentPiece, sec) },
                                label = { Text(sec.name) }
                            )
                        }
                    }
                }

                // Pinned Continuity Box: Next Session Goal + Standing Notes + Previous Session Notes
                if (pinnedNextGoal.isNotBlank() || standingScoreNotes.isNotBlank() || previousSessionSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (pinnedNextGoal.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pinned Goal for Today:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = pinnedNextGoal,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (standingScoreNotes.isNotBlank()) {
                                Text(
                                    text = "Score Notes: $standingScoreNotes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (previousSessionSummary.isNotBlank()) {
                                Text(
                                    text = "Last Session: $previousSessionSummary",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Session details live behind a disclosure so the card stays quiet
                val detailsFilled = listOf(
                    studioState.measuresPracticed.isNotBlank(),
                    studioState.sessionNotes.isNotBlank(),
                    studioState.nextSessionGoal.isNotBlank(),
                    studioState.selectedFocusTags.isNotEmpty(),
                    studioState.moodRating > 0
                ).count { it }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSessionDetails = !showSessionDetails }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showSessionDetails) "Hide session details" else "Add details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (detailsFilled > 0) {
                        Text(
                            text = "$detailsFilled added",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = if (showSessionDetails) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showSessionDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = studioState.customPieceTitle,
                            onValueChange = onCustomTitleChange,
                            label = { Text("Piece / Exercise Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Category & Instrument Selector Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PieceCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = studioState.selectedCategory == cat,
                                    onClick = { onSelectCategory(cat) },
                                    label = { Text(cat.shortLabel) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allInstruments.forEach { instrument ->
                                val selected = studioState.selectedInstrument.equals(instrument, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectInstrument(instrument) },
                                    label = { Text(instrument) }
                                )
                            }
                        }

                        // Measures / Passage Practiced Quick Input
                        OutlinedTextField(
                            value = studioState.measuresPracticed,
                            onValueChange = onMeasuresChange,
                            label = { Text("Measures / Section Practiced (optional)") },
                            placeholder = { Text("e.g., m. 24–32, Cadenza, Bridge") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Multi-line readable text input for practice session notes
                        OutlinedTextField(
                            value = studioState.sessionNotes,
                            onValueChange = onNotesChange,
                            label = { Text("Session Notes (Technique, Fingerings, Breakthroughs)") },
                            placeholder = {
                                Text("e.g., Isolated m. 24–32 hands separately at 76 BPM. Watch left-hand 4th finger in descending scale...")
                            },
                            minLines = 4,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick-Insert Musician Note Templates
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QUICK_NOTE_TEMPLATES.forEach { snippet ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onAppendNoteTemplate(snippet) }
                                ) {
                                    Text(
                                        text = "+ $snippet",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Next Session Goal (automatically pins to the piece/movement for next time!)
                        OutlinedTextField(
                            value = studioState.nextSessionGoal,
                            onValueChange = onNextGoalChange,
                            label = { Text("Goal for Next Session (Pins to top next time)") },
                            placeholder = { Text("e.g., Connect m. 32–40 at 84 BPM without pedal blur") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Focus Area Tags (Built-in + Custom + Quick Add)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Session Focus Tags",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showQuickTagDialog = true }) {
                                Text("+ Custom Tag")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allFocusTags.forEach { tag ->
                                val isSelected = studioState.selectedFocusTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleFocusTag(tag) },
                                    label = { Text(tag) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Session Quality / Focus Rating (1..5 stars)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Session Quality & Focus:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row {
                                for (star in 1..5) {
                                    IconButton(
                                        onClick = { onMoodRatingChange(star) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "$star Stars",
                                            tint = if (star <= studioState.moodRating) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. PRECISION METRONOME CARD (Subdivisions + Per-Beat Accents + Visual Pulse)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = if (beatFlash) {
                BorderStroke(
                    width = if (studioState.metronomeMuted) 3.5.dp else 2.dp,
                    color = metronomeBorderColor
                )
            } else {
                null
            },
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Precision Metronome",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${MetronomeEngine.tempoMarkingForBpm(studioState.metronomeBpm)} • ${studioState.beatsPerMeasure}/${studioState.noteValuePerBeat} • Tap beat circles for Accent/Mute",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(onClick = onToggleMetronomeMute) {
                        Icon(
                            imageVector = if (studioState.metronomeMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute/Unmute Metronome (Visual Pulse stays active)",
                            tint = if (studioState.metronomeMuted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // Interactive Per-Beat Accent / Mute Circles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    studioState.beatPattern.forEachIndexed { idx, accentLevel ->
                        val beat = idx + 1
                        val isActiveBeat = isMetronomePlaying && currentBeat == beat

                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isActiveBeat && accentLevel == BeatAccentLevel.ACCENT -> MaterialTheme.colorScheme.primary
                                isActiveBeat && accentLevel == BeatAccentLevel.NORMAL -> MaterialTheme.colorScheme.secondary
                                isActiveBeat && accentLevel == BeatAccentLevel.MUTE -> MaterialTheme.colorScheme.surfaceVariant
                                accentLevel == BeatAccentLevel.MUTE -> Color.Transparent
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            label = "beatDotColor"
                        )

                        Box(
                            modifier = Modifier
                                .size(if (isActiveBeat) 36.dp else 30.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    width = if (accentLevel == BeatAccentLevel.ACCENT) 2.dp else 1.dp,
                                    color = if (accentLevel == BeatAccentLevel.ACCENT) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .clickable { onCycleBeatAccent(beat) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (accentLevel == BeatAccentLevel.MUTE) "–" else beat.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveBeat && accentLevel != BeatAccentLevel.MUTE) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                // Large BPM Readout + Fine Controls (-5, -1, BPM, +1, +5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { onBpmChange(studioState.metronomeBpm - 5) }) {
                        Text("-5")
                    }
                    FilledTonalIconButton(onClick = { onBpmChange(studioState.metronomeBpm - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1 BPM")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${studioState.metronomeBpm}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val bestRef = currentSection?.bestBpm ?: currentPiece?.bestBpm
                        Text(
                            text = if (bestRef != null) {
                                "Best $bestRef • Target ${studioState.targetBpm} BPM"
                            } else {
                                "BPM (Target: ${studioState.targetBpm})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(onClick = { onBpmChange(studioState.metronomeBpm + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1 BPM")
                    }
                    FilledTonalButton(onClick = { onBpmChange(studioState.metronomeBpm + 5) }) {
                        Text("+5")
                    }
                }

                Slider(
                    value = studioState.metronomeBpm.toFloat(),
                    onValueChange = { onBpmChange(it.toInt()) },
                    valueRange = 30f..240f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subdivision Selector Row
                Text(
                    text = "Subdivisions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubdivisionMode.entries.forEach { sub ->
                        FilterChip(
                            selected = studioState.subdivisionMode == sub,
                            onClick = { onSubdivisionChange(sub) },
                            label = { Text(sub.label) }
                        )
                    }
                }

                // Time Signatures + Jazz 2&4 Preset
                Text(
                    text = "Meter & Feel Presets",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(2, 4, "2/4"),
                        Triple(3, 4, "3/4"),
                        Triple(4, 4, "4/4"),
                        Triple(5, 4, "5/4"),
                        Triple(6, 8, "6/8"),
                        Triple(9, 8, "9/8")
                    ).forEach { (b, nv, lbl) ->
                        FilterChip(
                            selected = studioState.beatsPerMeasure == b && studioState.noteValuePerBeat == nv,
                            onClick = { onTimeSignatureChange(b, nv) },
                            label = { Text(lbl) }
                        )
                    }
                    OutlinedButton(onClick = onApplyJazzBackbeat) {
                        Text("2 & 4 Swing")
                    }
                }

                // Tempo Mastery Bar toward Piece Target BPM
                if (studioState.targetBpm > 0) {
                    val masteryRatio = (studioState.metronomeBpm.toFloat() / studioState.targetBpm.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Tempo vs. Target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(masteryRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { masteryRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleMetronomePlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMetronomePlaying) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isMetronomePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isMetronomePlaying) {
                                if (studioState.metronomeMuted) "Stop Visual Pulse" else "Stop Click"
                            } else {
                                if (studioState.metronomeMuted) "Start Visual Pulse" else "Start Metronome"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onTapTempo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tap Tempo")
                    }
                }
            }
        }

        // Standalone save action — outside the cards so the screen ends quietly
        Button(
            onClick = onSaveSession,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            val fallbackMins = if (studioState.targetDurationMinutes > 0) studioState.targetDurationMinutes else 15
            val displayMins = if (studioState.elapsedSeconds >= 60) {
                "${(studioState.elapsedSeconds + 30) / 60}m"
            } else {
                "${fallbackMins}m Quick"
            }
            Text(
                text = if (studioState.isQueueMode) {
                    "Log Block & Advance ($displayMins @ ${studioState.metronomeBpm} BPM)"
                } else {
                    "Save Practice Session ($displayMins @ ${studioState.metronomeBpm} BPM)"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    // --- OVERLAYS & DIALOGS ---

    if (studioState.isMusicStandModeOpen) {
        MusicStandFocusOverlay(
            studioState = studioState,
            pinnedNextGoal = pinnedNextGoal,
            standingScoreNotes = standingScoreNotes,
            previousSessionSummary = previousSessionSummary,
            isMetronomePlaying = isMetronomePlaying,
            currentBeat = currentBeat,
            beatPulseId = beatPulseId,
            onToggleTimer = onToggleTimer,
            onBpmChange = onBpmChange,
            onCycleBeatAccent = onCycleBeatAccent,
            onSubdivisionChange = onSubdivisionChange,
            onToggleMetronomeMute = onToggleMetronomeMute,
            onToggleMetronomePlay = onToggleMetronomePlay,
            onTapTempo = onTapTempo,
            onNotesChange = onNotesChange,
            onSaveSession = onSaveSession,
            onDismiss = { onSetMusicStandMode(false) }
        )
    }

    if (showLogPastDialog) {
        LogPastSessionDialog(
            piecesWithSections = piecesWithSections,
            allInstruments = allInstruments,
            allFocusTags = allFocusTags,
            onDismiss = { showLogPastDialog = false },
            onConfirm = { epoch, mins, piece, sec, title, inst, cat, bpm, target, meas, tags, notes, goal, rating ->
                onLogPastSession(
                    epoch, mins, piece, sec, title, inst, cat, bpm, target, meas, tags, notes, goal, rating
                )
                showLogPastDialog = false
            }
        )
    }

    if (showRoutinesDialog) {
        AlertDialog(
            onDismissRequest = { showRoutinesDialog = false },
            title = { Text("Practice Routines & Session Queue") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Load a saved multi-item routine template or build an ad-hoc queue for today's sitting:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = {
                            showRoutinesDialog = false
                            showAddQueueBlockDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Item to Today's Session Queue")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (routines.isEmpty()) {
                        Text(
                            text = "No saved routine templates yet. Add blocks to a queue and tap 'Save Template'!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        routines.forEach { rwi ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rwi.routine.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        IconButton(
                                            onClick = { onDeleteRoutineTemplate(rwi.routine) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Routine")
                                        }
                                    }
                                    if (rwi.routine.description.isNotBlank()) {
                                        Text(
                                            text = rwi.routine.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${rwi.items.size} blocks • ${rwi.totalTargetMinutes} min target",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Button(
                                        onClick = {
                                            onLoadRoutine(rwi)
                                            showRoutinesDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Start Routine")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutinesDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAddQueueBlockDialog) {
        var pickedPiece by remember { mutableStateOf<RepertoirePieceEntity?>(null) }
        var pickedSection by remember { mutableStateOf<PieceSectionEntity?>(null) }
        var blockTitle by remember { mutableStateOf("Warmup & Scales") }
        var targetMinsText by remember { mutableStateOf("15") }
        var pickerMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddQueueBlockDialog = false },
            title = { Text("Add Block to Session Queue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { pickerMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    pickedPiece != null && pickedSection != null -> "${pickedPiece!!.title} — ${pickedSection!!.name}"
                                    pickedPiece != null -> pickedPiece!!.title
                                    else -> "Custom Block / Warmup"
                                },
                                maxLines = 1
                            )
                        }
                        DropdownMenu(expanded = pickerMenu, onDismissRequest = { pickerMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Custom Block / Warmup") },
                                onClick = {
                                    pickedPiece = null
                                    pickedSection = null
                                    pickerMenu = false
                                }
                            )
                            piecesWithSections.forEach { pws ->
                                DropdownMenuItem(
                                    text = { Text(pws.piece.title, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        pickedPiece = pws.piece
                                        pickedSection = null
                                        blockTitle = pws.piece.title
                                        pickerMenu = false
                                    }
                                )
                                pws.sortedSections.forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("   ↳ ${sec.name}") },
                                        onClick = {
                                            pickedPiece = pws.piece
                                            pickedSection = sec
                                            blockTitle = "${pws.piece.title} — ${sec.name}"
                                            pickerMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (pickedPiece == null) {
                        OutlinedTextField(
                            value = blockTitle,
                            onValueChange = { blockTitle = it },
                            label = { Text("Block Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = targetMinsText,
                        onValueChange = { targetMinsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Minutes (Silent Progress Bar)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddAdHocQueueItem(
                            pickedPiece,
                            pickedSection,
                            blockTitle,
                            targetMinsText.toIntOrNull() ?: 15
                        )
                        showAddQueueBlockDialog = false
                    }
                ) {
                    Text("Add to Queue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddQueueBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSaveRoutineDialog) {
        var rName by remember { mutableStateOf(studioState.activeRoutineName) }
        var rDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveRoutineDialog = false },
            title = { Text("Save Queue as Routine Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rName,
                        onValueChange = { rName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rDesc,
                        onValueChange = { rDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rName.isNotBlank()) {
                            onSaveQueueAsTemplate(rName, rDesc)
                            showSaveRoutineDialog = false
                        }
                    }
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRoutineDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuickTagDialog) {
        var tagInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickTagDialog = false },
            title = { Text("Create Custom Focus Tag") },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tag Name (e.g., Double Stops, Rubato, Tone)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onAddCustomFocusTag(tagInput.trim())
                            onToggleFocusTag(tagInput.trim())
                            showQuickTagDialog = false
                        }
                    }
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTagDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatSecondsToClock(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
