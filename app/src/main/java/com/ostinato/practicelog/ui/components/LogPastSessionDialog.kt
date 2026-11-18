package com.ostinato.practicelog.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ostinato.practicelog.data.local.PieceCategory
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceWithSections
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogPastSessionDialog(
    piecesWithSections: List<PieceWithSections>,
    allInstruments: List<String>,
    allFocusTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        timestampEpochMillis: Long,
        durationMinutes: Int,
        piece: RepertoirePieceEntity?,
        section: PieceSectionEntity?,
        customTitle: String,
        instrument: String,
        category: PieceCategory,
        practicedBpm: Int,
        targetBpm: Int,
        measuresPracticed: String,
        focusTags: List<String>,
        sessionNotes: String,
        nextSessionGoal: String,
        moodRating: Int
    ) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.US) }

    var dateStr by remember { mutableStateOf(dateFmt.format(Date())) }
    var timeStr by remember { mutableStateOf(timeFmt.format(Date())) }
    var durationText by remember { mutableStateOf("30") }

    val initialPair = piecesWithSections.firstOrNull()
    var selectedPiece by remember { mutableStateOf(initialPair?.piece) }
    var selectedSection by remember { mutableStateOf(initialPair?.sortedSections?.firstOrNull()) }
    var customTitle by remember { mutableStateOf(initialPair?.piece?.title ?: "General Practice") }
    var instrument by remember { mutableStateOf(initialPair?.piece?.instrument ?: allInstruments.firstOrNull() ?: "Piano") }
    var category by remember { mutableStateOf(initialPair?.piece?.pieceCategory ?: PieceCategory.REPERTOIRE) }
    var bpmText by remember {
        mutableStateOf((selectedSection?.lastPracticedBpm ?: selectedPiece?.lastPracticedBpm ?: 92).toString())
    }
    var measures by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf("Technique")) }
    var notes by remember { mutableStateOf("") }
    var nextGoal by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(4) }

    var showPieceMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past Practice Session") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Record a session practiced away from your phone:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Date Presets
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Today", 1 to "Yesterday", 2 to "2 Days Ago").forEach { (daysAgo, label) ->
                        val targetKey = remember(daysAgo) {
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                                .let { dateFmt.format(it.time) }
                        }
                        FilterChip(
                            selected = dateStr == targetKey,
                            onClick = { dateStr = targetKey },
                            label = { Text(label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Piece & Section Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPieceMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = when {
                            selectedPiece != null && selectedSection != null ->
                                "${selectedPiece!!.title} — ${selectedSection!!.name}"
                            selectedPiece != null -> selectedPiece!!.title
                            else -> "Custom / General Practice"
                        }
                        Text(label, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showPieceMenu,
                        onDismissRequest = { showPieceMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Custom / Unlisted Exercise") },
                            onClick = {
                                selectedPiece = null
                                selectedSection = null
                                showPieceMenu = false
                            }
                        )
                        piecesWithSections.forEach { pws ->
                            DropdownMenuItem(
                                text = { Text(pws.piece.title, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedPiece = pws.piece
                                    selectedSection = null
                                    customTitle = pws.piece.title
                                    instrument = pws.piece.instrument
                                    category = pws.piece.pieceCategory
                                    bpmText = pws.piece.lastPracticedBpm.toString()
                                    showPieceMenu = false
                                }
                            )
                            pws.sortedSections.forEach { sec ->
                                DropdownMenuItem(
                                    text = { Text("   ↳ ${sec.name} (${sec.lastPracticedBpm}/${sec.targetBpm} BPM)") },
                                    onClick = {
                                        selectedPiece = pws.piece
                                        selectedSection = sec
                                        customTitle = pws.piece.title
                                        instrument = pws.piece.instrument
                                        category = pws.piece.pieceCategory
                                        bpmText = sec.lastPracticedBpm.toString()
                                        showPieceMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedPiece == null) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Piece / Exercise Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Duration & BPM
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Duration (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bpmText,
                        onValueChange = { bpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Tempo (BPM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Instrument chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allInstruments.forEach { inst ->
                        FilterChip(
                            selected = instrument.equals(inst, ignoreCase = true),
                            onClick = { instrument = inst },
                            label = { Text(inst) }
                        )
                    }
                }

                OutlinedTextField(
                    value = measures,
                    onValueChange = { measures = it },
                    label = { Text("Measures / Section (optional, e.g. m. 16–32)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nextGoal,
                    onValueChange = { nextGoal = it },
                    label = { Text("Next Session Goal (pins to Studio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allFocusTags.forEach { tag ->
                        val active = selectedTags.contains(tag)
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedTags = if (active) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Focus Rating:", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        for (s in 1..5) {
                            IconButton(onClick = { rating = s }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$s stars",
                                    tint = if (s <= rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedEpoch = runCatching {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .parse("${dateStr.trim()} ${timeStr.trim()}")?.time
                    }.getOrNull() ?: System.currentTimeMillis()

                    onConfirm(
                        parsedEpoch,
                        durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                        selectedPiece,
                        selectedSection,
                        customTitle,
                        instrument,
                        category,
                        bpmText.toIntOrNull() ?: 92,
                        selectedSection?.targetBpm ?: selectedPiece?.targetBpm ?: 120,
                        measures,
                        selectedTags.toList(),
                        notes.ifBlank { "Logged past session." },
                        nextGoal,
                        rating
                    )
                }
            ) {
                Text("Log Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
