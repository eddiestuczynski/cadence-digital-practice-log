package com.cadence.practicelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cadence.practicelog.data.local.PieceCategory
import com.cadence.practicelog.data.local.PieceSectionEntity
import com.cadence.practicelog.data.local.PieceStatus
import com.cadence.practicelog.data.local.PieceWithSections
import com.cadence.practicelog.data.local.RepertoirePieceEntity

@Composable
fun RepertoireScreen(
    piecesWithSections: List<PieceWithSections>,
    allInstruments: List<String>,
    onPracticeNow: (RepertoirePieceEntity, PieceSectionEntity?) -> Unit,
    onAddPiece: (
        title: String,
        composer: String,
        instrument: String,
        category: PieceCategory,
        keySignature: String,
        timeSignatureBeats: Int,
        timeSignatureNoteValue: Int,
        initialBpm: Int,
        targetBpm: Int,
        status: PieceStatus,
        notes: String,
        nextGoal: String
    ) -> Unit,
    onUpdatePiece: (RepertoirePieceEntity) -> Unit,
    onDeletePiece: (RepertoirePieceEntity) -> Unit,
    onAddSection: (
        pieceId: Long,
        name: String,
        orderIndex: Int,
        initialBpm: Int,
        targetBpm: Int,
        beats: Int,
        noteValue: Int,
        notes: String,
        nextGoal: String
    ) -> Unit,
    onDeleteSection: (PieceSectionEntity) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf<PieceCategory?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<PieceStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPiece by remember { mutableStateOf<RepertoirePieceEntity?>(null) }
    var addingSectionForPiece by remember { mutableStateOf<PieceWithSections?>(null) }

    val filteredPieces = remember(piecesWithSections, selectedCategoryFilter, selectedStatusFilter) {
        piecesWithSections.filter { pws ->
            val catOk = selectedCategoryFilter == null || pws.piece.pieceCategory == selectedCategoryFilter
            val statusOk = selectedStatusFilter == null || pws.piece.pieceStatus == selectedStatusFilter
            catOk && statusOk
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
                    text = "Repertoire & Studies",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${piecesWithSections.size} items • Best vs. Last BPM & Movements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item")
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = { Text("All Categories") }
            )
            PieceCategory.entries.forEach { cat ->
                val count = piecesWithSections.count { it.piece.pieceCategory == cat }
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text("${cat.shortLabel} ($count)") }
                )
            }
        }

        // Manual Mastery Status Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == null,
                onClick = { selectedStatusFilter = null },
                label = { Text("All Statuses") }
            )
            PieceStatus.entries.forEach { status ->
                val count = piecesWithSections.count { it.piece.pieceStatus == status }
                FilterChip(
                    selected = selectedStatusFilter == status,
                    onClick = { selectedStatusFilter = status },
                    label = { Text("${status.label} ($count)") }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredPieces, key = { it.piece.id }) { pws ->
                RepertoirePieceCard(
                    pieceWithSections = pws,
                    onPracticePieceNow = { sec -> onPracticeNow(pws.piece, sec) },
                    onCycleStatus = {
                        val nextStatus = when (pws.piece.pieceStatus) {
                            PieceStatus.LEARNING -> PieceStatus.POLISHING
                            PieceStatus.POLISHING -> PieceStatus.MASTERED
                            PieceStatus.MASTERED -> PieceStatus.LEARNING
                        }
                        onUpdatePiece(pws.piece.copy(status = nextStatus.name))
                    },
                    onAddMovement = { addingSectionForPiece = pws },
                    onDeleteMovement = onDeleteSection,
                    onEdit = { editingPiece = pws.piece },
                    onDelete = { onDeletePiece(pws.piece) }
                )
            }
        }
    }

    if (showAddDialog) {
        PieceEditorDialog(
            initialPiece = null,
            allInstruments = allInstruments,
            onDismiss = { showAddDialog = false },
            onSave = { title, composer, inst, cat, keySig, beats, noteVal, initBpm, targetBpm, status, notes, nextGoal ->
                onAddPiece(
                    title, composer, inst, cat, keySig, beats, noteVal, initBpm, targetBpm, status, notes, nextGoal
                )
                showAddDialog = false
            }
        )
    }

    editingPiece?.let { piece ->
        PieceEditorDialog(
            initialPiece = piece,
            allInstruments = allInstruments,
            onDismiss = { editingPiece = null },
            onSave = { title, composer, inst, cat, keySig, beats, noteVal, initBpm, targetBpm, status, notes, nextGoal ->
                onUpdatePiece(
                    piece.copy(
                        title = title,
                        composer = composer,
                        instrument = inst,
                        category = cat.name,
                        keySignature = keySig,
                        timeSignatureBeats = beats,
                        timeSignatureNoteValue = noteVal,
                        bestBpm = initBpm.coerceAtLeast(piece.bestBpm),
                        lastPracticedBpm = initBpm,
                        targetBpm = targetBpm,
                        status = status.name,
                        practiceNotes = notes,
                        nextSessionGoal = nextGoal
                    )
                )
                editingPiece = null
            }
        )
    }

    addingSectionForPiece?.let { pws ->
        AddMovementDialog(
            parentPiece = pws.piece,
            nextOrderIndex = pws.sections.size,
            onDismiss = { addingSectionForPiece = null },
            onSave = { name, initBpm, targetBpm, beats, noteVal, notes, nextGoal ->
                onAddSection(
                    pws.piece.id,
                    name,
                    pws.sections.size,
                    initBpm,
                    targetBpm,
                    beats,
                    noteVal,
                    notes,
                    nextGoal
                )
                addingSectionForPiece = null
            }
        )
    }
}

@Composable
private fun RepertoirePieceCard(
    pieceWithSections: PieceWithSections,
    onPracticePieceNow: (PieceSectionEntity?) -> Unit,
    onCycleStatus: () -> Unit,
    onAddMovement: () -> Unit,
    onDeleteMovement: (PieceSectionEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val piece = pieceWithSections.piece
    val sections = pieceWithSections.sortedSections

    val statusBadgeColor = when (piece.pieceStatus) {
        PieceStatus.LEARNING -> MaterialTheme.colorScheme.secondaryContainer
        PieceStatus.POLISHING -> MaterialTheme.colorScheme.primaryContainer
        PieceStatus.MASTERED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusTextColor = when (piece.pieceStatus) {
        PieceStatus.LEARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        PieceStatus.POLISHING -> MaterialTheme.colorScheme.onPrimaryContainer
        PieceStatus.MASTERED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = piece.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val metaParts = buildList {
                        if (piece.composer.isNotBlank()) add(piece.composer)
                        add(piece.instrument)
                        add(piece.pieceCategory.shortLabel)
                        if (piece.keySignature.isNotBlank()) add(piece.keySignature)
                        add(piece.timeSignatureLabel)
                        add("${piece.totalMinutesPracticed}m total")
                    }
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Manual Mastery Status Badge (Tap to cycle Learning -> Polishing -> Mastered)
                Surface(
                    onClick = onCycleStatus,
                    shape = RoundedCornerShape(50),
                    color = statusBadgeColor
                ) {
                    Text(
                        text = piece.pieceStatus.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Dual BPM Tracking Bar: Best Achieved BPM + Last Practiced BPM vs Target BPM
            val progress = piece.tempoMasteryProgress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Best ${piece.bestBpm} BPM • Last ${piece.lastPracticedBpm} BPM → Target ${piece.targetBpm}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (piece.pieceStatus == PieceStatus.MASTERED) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (piece.nextSessionGoal.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next Goal: ${piece.nextSessionGoal}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (piece.practiceNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = piece.practiceNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Optional Sub-Sections / Movements List
            if (sections.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text(
                    text = "Movements / Sub-Sections (${sections.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sections.forEach { sec ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${sec.name} (${sec.timeSignatureLabel})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Best ${sec.bestBpm} • Last ${sec.lastPracticedBpm} / Target ${sec.targetBpm} BPM • ${sec.totalMinutesPracticed}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (sec.nextSessionGoal.isNotBlank()) {
                                    Text(
                                        text = "Goal: ${sec.nextSessionGoal}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onPracticePieceNow(sec) }) {
                                    Text("Practice")
                                }
                                IconButton(
                                    onClick = { onDeleteMovement(sec) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Movement",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Actions Row: Practice in Studio, + Movement, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = { onPracticePieceNow(null) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Practice")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onAddMovement) {
                        Text("+ Movement")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Piece")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Piece")
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceEditorDialog(
    initialPiece: RepertoirePieceEntity?,
    allInstruments: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        composer: String,
        instrument: String,
        category: PieceCategory,
        keySignature: String,
        beats: Int,
        noteValue: Int,
        initialBpm: Int,
        targetBpm: Int,
        status: PieceStatus,
        notes: String,
        nextGoal: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialPiece?.title ?: "") }
    var composer by remember { mutableStateOf(initialPiece?.composer ?: "") }
    var instrument by remember { mutableStateOf(initialPiece?.instrument ?: allInstruments.firstOrNull() ?: "Piano") }
    var category by remember { mutableStateOf(initialPiece?.pieceCategory ?: PieceCategory.REPERTOIRE) }
    var keySignature by remember { mutableStateOf(initialPiece?.keySignature ?: "") }
    var beatsText by remember { mutableStateOf((initialPiece?.timeSignatureBeats ?: 4).toString()) }
    var noteValText by remember { mutableStateOf((initialPiece?.timeSignatureNoteValue ?: 4).toString()) }
    var currentBpmText by remember { mutableStateOf((initialPiece?.lastPracticedBpm ?: 72).toString()) }
    var targetBpmText by remember { mutableStateOf((initialPiece?.targetBpm ?: 120).toString()) }
    var status by remember { mutableStateOf(initialPiece?.pieceStatus ?: PieceStatus.LEARNING) }
    var notes by remember { mutableStateOf(initialPiece?.practiceNotes ?: "") }
    var nextGoal by remember { mutableStateOf(initialPiece?.nextSessionGoal ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPiece == null) "Add Library Item" else "Edit Library Item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    label = { Text("Composer / Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PieceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortLabel) }
                        )
                    }
                }

                // Instrument Selector + Custom Input
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

                // Key Signature & Time Signature
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keySignature,
                        onValueChange = { keySignature = it },
                        label = { Text("Key (e.g. D Minor)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = beatsText,
                        onValueChange = { beatsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Beats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = noteValText,
                        onValueChange = { noteValText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Note") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentBpmText,
                        onValueChange = { currentBpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Starting BPM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetBpmText,
                        onValueChange = { targetBpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target BPM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PieceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = nextGoal,
                    onValueChange = { nextGoal = it },
                    label = { Text("Pinned Next Session Goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Standing Score Notes (Fingerings, Bowings, Cues)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            composer,
                            instrument.ifBlank { "Piano" },
                            category,
                            keySignature,
                            beatsText.toIntOrNull() ?: 4,
                            noteValText.toIntOrNull() ?: 4,
                            currentBpmText.toIntOrNull() ?: 80,
                            targetBpmText.toIntOrNull() ?: 120,
                            status,
                            notes,
                            nextGoal
                        )
                    }
                }
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddMovementDialog(
    parentPiece: RepertoirePieceEntity,
    nextOrderIndex: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        initBpm: Int,
        targetBpm: Int,
        beats: Int,
        noteValue: Int,
        notes: String,
        nextGoal: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("Mov. ${nextOrderIndex + 1}") }
    var initBpmText by remember { mutableStateOf(parentPiece.lastPracticedBpm.toString()) }
    var targetBpmText by remember { mutableStateOf(parentPiece.targetBpm.toString()) }
    var beatsText by remember { mutableStateOf(parentPiece.timeSignatureBeats.toString()) }
    var noteValText by remember { mutableStateOf(parentPiece.timeSignatureNoteValue.toString()) }
    var notes by remember { mutableStateOf("") }
    var nextGoal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Movement / Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = parentPiece.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Movement / Section Name *") },
                    placeholder = { Text("e.g., I. Allegro, Cadenza, Bridge Solo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initBpmText,
                        onValueChange = { initBpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Current BPM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetBpmText,
                        onValueChange = { targetBpmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target BPM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = beatsText,
                        onValueChange = { beatsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Meter Beats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = noteValText,
                        onValueChange = { noteValText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Note Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = nextGoal,
                    onValueChange = { nextGoal = it },
                    label = { Text("Next Session Goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Movement Score Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            initBpmText.toIntOrNull() ?: 80,
                            targetBpmText.toIntOrNull() ?: 120,
                            beatsText.toIntOrNull() ?: 4,
                            noteValText.toIntOrNull() ?: 4,
                            notes,
                            nextGoal
                        )
                    }
                }
            ) {
                Text("Add Movement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
