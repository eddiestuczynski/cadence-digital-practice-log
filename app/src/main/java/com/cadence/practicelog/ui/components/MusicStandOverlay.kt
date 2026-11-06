package com.cadence.practicelog.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cadence.practicelog.ui.PracticeStudioState
import com.cadence.practicelog.util.BeatAccentLevel
import com.cadence.practicelog.util.SubdivisionMode
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MusicStandFocusOverlay(
    studioState: PracticeStudioState,
    pinnedNextGoal: String,
    standingScoreNotes: String,
    previousSessionSummary: String,
    isMetronomePlaying: Boolean,
    currentBeat: Int,
    beatPulseId: Long,
    onToggleTimer: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onCycleBeatAccent: (Int) -> Unit,
    onSubdivisionChange: (SubdivisionMode) -> Unit,
    onToggleMetronomeMute: () -> Unit,
    onToggleMetronomePlay: () -> Unit,
    onTapTempo: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveSession: () -> Unit,
    onDismiss: () -> Unit
) {
    // Visual screen-edge pulse for music-stand & loud-instrument visibility (especially when muted!)
    var pulseFlashActive by remember { mutableStateOf(false) }
    LaunchedEffect(beatPulseId, isMetronomePlaying) {
        if (isMetronomePlaying && beatPulseId > 0L) {
            pulseFlashActive = true
            delay(115L)
            pulseFlashActive = false
        } else {
            pulseFlashActive = false
        }
    }

    val isDownbeat = currentBeat == 1
    val borderPulseColor by animateColorAsState(
        targetValue = when {
            pulseFlashActive && isDownbeat -> MaterialTheme.colorScheme.primary
            pulseFlashActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 90),
        label = "standPulseBorder"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .border(width = if (studioState.metronomeMuted) 8.dp else 4.dp, color = borderPulseColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Piece Title + Exit Focus View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MUSIC STAND FOCUS MODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = buildString {
                                append(studioState.customPieceTitle.ifBlank { "Active Practice" })
                                if (studioState.sectionName.isNotBlank()) {
                                    append(" • ")
                                    append(studioState.sectionName)
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    FilledTonalButton(onClick = onDismiss) {
                        Icon(Icons.Default.CloseFullscreen, contentDescription = "Exit Stand Mode")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit")
                    }
                }

                // Giant Music-Stand Stopwatch Card (Count-up + silent target progress bar)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatStandClock(studioState.elapsedSeconds),
                            fontSize = 68.sp,
                            lineHeight = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (studioState.targetDurationMinutes > 0) {
                            val targetSecs = studioState.targetDurationMinutes * 60
                            val ratio = (studioState.elapsedSeconds.toFloat() / targetSecs.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (ratio >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "Target Block: ${studioState.targetDurationMinutes} min (Silent Count-Up)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onToggleTimer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = if (studioState.timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (studioState.timerRunning) "Pause Stopwatch" else "Start Stopwatch",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                // Giant Metronome & Visual Beat Bar
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Extra-large Beat Circles (Tap to cycle Loud Accent / Normal / Silent)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            studioState.beatPattern.forEachIndexed { index, accentLevel ->
                                val beatNum = index + 1
                                val active = isMetronomePlaying && currentBeat == beatNum
                                val bg = when {
                                    active && accentLevel == BeatAccentLevel.ACCENT -> MaterialTheme.colorScheme.primary
                                    active && accentLevel == BeatAccentLevel.NORMAL -> MaterialTheme.colorScheme.secondary
                                    active && accentLevel == BeatAccentLevel.MUTE -> MaterialTheme.colorScheme.surfaceVariant
                                    accentLevel == BeatAccentLevel.MUTE -> Color.Transparent
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 46.dp else 38.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .border(
                                            width = if (accentLevel == BeatAccentLevel.ACCENT) 2.5.dp else 1.dp,
                                            color = if (accentLevel == BeatAccentLevel.ACCENT) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { onCycleBeatAccent(beatNum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (accentLevel == BeatAccentLevel.MUTE) "–" else beatNum.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active && accentLevel != BeatAccentLevel.MUTE) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        // Giant BPM Adjusters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(onClick = { onBpmChange(studioState.metronomeBpm - 5) }) {
                                Text("-5", style = MaterialTheme.typography.titleMedium)
                            }
                            FilledTonalIconButton(onClick = { onBpmChange(studioState.metronomeBpm - 1) }) {
                                Icon(Icons.Default.Remove, contentDescription = "-1 BPM")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${studioState.metronomeBpm}",
                                    fontSize = 54.sp,
                                    lineHeight = 58.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "BPM • Target ${studioState.targetBpm}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalIconButton(onClick = { onBpmChange(studioState.metronomeBpm + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "+1 BPM")
                            }
                            FilledTonalButton(onClick = { onBpmChange(studioState.metronomeBpm + 5) }) {
                                Text("+5", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        // Subdivision Quick Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubdivisionMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = studioState.subdivisionMode == mode,
                                    onClick = { onSubdivisionChange(mode) },
                                    label = { Text(mode.shortLabel) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMetronomePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMetronomePlaying) "Stop Click" else "Start Click",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            OutlinedButton(
                                onClick = onTapTempo,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tap")
                            }

                            IconButton(onClick = onToggleMetronomeMute) {
                                Icon(
                                    imageVector = if (studioState.metronomeMuted) {
                                        Icons.Default.VolumeOff
                                    } else {
                                        Icons.Default.VolumeUp
                                    },
                                    contentDescription = "Toggle Visual-Only Mute",
                                    tint = if (studioState.metronomeMuted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                // Pinned Continuity Notes (Large Legible Music-Stand Text)
                if (pinnedNextGoal.isNotBlank() || standingScoreNotes.isNotBlank() || previousSessionSummary.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pinnedNextGoal.isNotBlank()) {
                                Text(
                                    text = "TODAY'S PINNED GOAL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = pinnedNextGoal,
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (standingScoreNotes.isNotBlank()) {
                                Text(
                                    text = "SCORE CUES: $standingScoreNotes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (previousSessionSummary.isNotBlank()) {
                                Text(
                                    text = "LAST SESSION: $previousSessionSummary",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Stand Note Jotting + Save Session
                OutlinedTextField(
                    value = studioState.sessionNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Quick Stand Notes (Measures, Fingerings, Breakthroughs)") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSaveSession()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Finish & Log Session",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatStandClock(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
