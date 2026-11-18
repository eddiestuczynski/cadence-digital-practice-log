package com.ostinato.practicelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.ui.components.LogPastSessionDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionJournalScreen(
    sessions: List<PracticeSessionEntity>,
    piecesWithSections: List<PieceWithSections>,
    allInstruments: List<String>,
    allFocusTags: List<String>,
    onLogPastSession: (
        Long, Int, RepertoirePieceEntity?, PieceSectionEntity?, String, String,
        PieceCategory, Int, Int, String, List<String>, String, String, Int
    ) -> Unit,
    onUpdateSession: (PracticeSessionEntity, String, String, String, String, Int) -> Unit,
    onDeleteSession: (PracticeSessionEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PieceCategory?>(null) }
    var selectedInstrumentFilter by remember { mutableStateOf("All") }
    var editingSession by remember { mutableStateOf<PracticeSessionEntity?>(null) }
    var showLogPastDialog by remember { mutableStateOf(false) }

    val availableInstruments = remember(sessions, allInstruments) {
        (listOf("All") + (sessions.map { it.instrument } + allInstruments).distinct().sorted())
    }

    val filteredSessions = remember(sessions, searchQuery, selectedCategory, selectedInstrumentFilter) {
        sessions.filter { session ->
            val matchesCat = selectedCategory == null || session.pieceCategory == selectedCategory
            val matchesInstrument = selectedInstrumentFilter == "All" ||
                session.instrument.equals(selectedInstrumentFilter, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                session.pieceTitle.contains(q, ignoreCase = true) ||
                session.sectionName.contains(q, ignoreCase = true) ||
                session.composer.contains(q, ignoreCase = true) ||
                session.measuresPracticed.contains(q, ignoreCase = true) ||
                session.sessionNotes.contains(q, ignoreCase = true) ||
                session.nextSessionGoal.contains(q, ignoreCase = true) ||
                session.focusTags.contains(q, ignoreCase = true)
            matchesCat && matchesInstrument && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Practice Journal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredSessions.size} sessions • Notes, measures & goals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showLogPastDialog = true },
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Past")
            }
        }

        // Search input across piece titles, measures, notes, next goals, and focus tags
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search notes, measures, pieces, or tags...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Category + Instrument Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All Types") }
            )
            PieceCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.shortLabel) }
                )
            }
            availableInstruments.filter { it != "All" }.forEach { inst ->
                FilterChip(
                    selected = selectedInstrumentFilter == inst,
                    onClick = {
                        selectedInstrumentFilter = if (selectedInstrumentFilter == inst) "All" else inst
                    },
                    label = { Text(inst) }
                )
            }
        }

        if (filteredSessions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No matching practice sessions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Log a live session in Studio, tap 'Log Past' above, or clear your search filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    SessionLogCard(
                        session = session,
                        onEditClick = { editingSession = session },
                        onDeleteClick = { onDeleteSession(session) }
                    )
                }
            }
        }
    }

    editingSession?.let { session ->
        EditSessionNotesDialog(
            session = session,
            onDismiss = { editingSession = null },
            onConfirm = { newNotes, newMeasures, newNextGoal, newTags, newBpm ->
                onUpdateSession(session, newNotes, newMeasures, newNextGoal, newTags, newBpm)
                editingSession = null
            }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionLogCard(
    session: PracticeSessionEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Date + Instrument + Category + Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFmt.format(Date(session.timestampEpochMillis)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${session.instrument} • ${session.pieceCategory.shortLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Session Notes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Piece Title, Movement, Composer & Measures Practiced
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (session.sectionName.isNotBlank()) {
                        "${session.pieceTitle} — ${session.sectionName}"
                    } else {
                        session.pieceTitle
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.composer.isNotBlank()) {
                        Text(
                            text = session.composer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (session.measuresPracticed.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = session.measuresPracticed,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (session.routineName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = session.routineName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Metrics Bar: Duration, Practiced BPM vs Target, Star Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.durationMinutes} min",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val targetSuffix = if (session.targetBpmSnapshot > 0) " / ${session.targetBpmSnapshot}" else ""
                    Text(
                        text = "${session.practicedBpm}$targetSuffix BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row {
                    repeat(session.moodRating.coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // High-Contrast Practice Notes Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = session.sessionNotes.ifBlank { "No written notes for this session." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (session.nextSessionGoal.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next Goal: ${session.nextSessionGoal}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Focus Tags
            val tags = session.focusTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSessionNotesDialog(
    session: PracticeSessionEntity,
    onDismiss: () -> Unit,
    onConfirm: (newNotes: String, newMeasures: String, newNextGoal: String, newTags: String, newBpm: Int) -> Unit
) {
    var notes by remember { mutableStateOf(session.sessionNotes) }
    var measures by remember { mutableStateOf(session.measuresPracticed) }
    var nextGoal by remember { mutableStateOf(session.nextSessionGoal) }
    var tags by remember { mutableStateOf(session.focusTags) }
    var bpmText by remember { mutableStateOf(session.practicedBpm.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = session.pieceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = measures,
                    onValueChange = { measures = it },
                    label = { Text("Measures / Section Practiced") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Practice Session Notes") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nextGoal,
                    onValueChange = { nextGoal = it },
                    label = { Text("Next Session Goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Focus Tags (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Practiced Tempo (BPM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedBpm = bpmText.toIntOrNull() ?: session.practicedBpm
                    onConfirm(notes, measures, nextGoal, tags, parsedBpm)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
