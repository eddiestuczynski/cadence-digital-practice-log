package com.cadence.practicelog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cadence.practicelog.data.preferences.DarkVariant
import com.cadence.practicelog.data.preferences.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndDataSheet(
    themeMode: ThemeMode,
    preferredDarkVariant: DarkVariant,
    keepScreenAwake: Boolean,
    globalInstrumentFilter: String,
    allInstruments: List<String>,
    customInstruments: List<String>,
    customFocusTags: List<String>,
    statusMessage: String?,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkVariant: (DarkVariant) -> Unit,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    onSelectGlobalInstrument: (String) -> Unit,
    onAddCustomInstrument: (String) -> Unit,
    onRemoveCustomInstrument: (String) -> Unit,
    onAddCustomFocusTag: (String) -> Unit,
    onRemoveCustomFocusTag: (String) -> Unit,
    onShareTeacherReport7Days: () -> Unit,
    onShareTeacherReport30Days: () -> Unit,
    onShareSessionsCsv: () -> Unit,
    onExportBackupJson: () -> Unit,
    onImportBackupJson: () -> Unit,
    onClearAllData: () -> Unit,
    onReloadDemoData: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newInstrumentText by remember { mutableStateOf("") }
    var newTagText by remember { mutableStateOf("") }
    var confirmClearDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studio Settings, Export & Backup",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Settings")
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Visual Mode & Music Stand Ergonomics
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Visual Mode & Music Stand", style = MaterialTheme.typography.titleLarge)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    if (themeMode == ThemeMode.SYSTEM) {
                        Text(
                            text = "Preferred Night Palette for System Auto:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DarkVariant.entries.forEach { v ->
                                FilterChip(
                                    selected = preferredDarkVariant == v,
                                    onClick = { onSelectDarkVariant(v) },
                                    label = { Text(v.label) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Screen Awake While Practicing",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Prevents screen dimming whenever stopwatch or metronome is running",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepScreenAwake,
                            onCheckedChange = onKeepScreenAwakeChange
                        )
                    }
                }
            }

            // 2. Instruments & Custom Focus Tags
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Instruments & Global Filter", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Filter Repertoire, Journal & Analytics by active instrument:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (listOf("All") + allInstruments).forEach { inst ->
                            FilterChip(
                                selected = globalInstrumentFilter.equals(inst, ignoreCase = true),
                                onClick = { onSelectGlobalInstrument(inst) },
                                label = { Text(inst) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newInstrumentText,
                            onValueChange = { newInstrumentText = it },
                            label = { Text("Add Custom Instrument (e.g. Viola, Harp)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                if (newInstrumentText.isNotBlank()) {
                                    onAddCustomInstrument(newInstrumentText)
                                    newInstrumentText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add")
                        }
                    }

                    if (customInstruments.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            customInstruments.forEach { custom ->
                                InputChip(
                                    selected = false,
                                    onClick = { onRemoveCustomInstrument(custom) },
                                    label = { Text("$custom ✕") }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Custom Focus Tags", style = MaterialTheme.typography.titleMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            label = { Text("Add Focus Tag (e.g. Vibrato, Pedaling)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                if (newTagText.isNotBlank()) {
                                    onAddCustomFocusTag(newTagText)
                                    newTagText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add")
                        }
                    }
                    if (customFocusTags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            customFocusTags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { onRemoveCustomFocusTag(tag) },
                                    label = { Text("#$tag ✕") }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Shareable Teacher Report & CSV Export
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Share Practice Report with Teacher", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Send a clean summary of total hours, active pieces, BPM gains, measures practiced, and notes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onShareTeacherReport7Days,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("7-Day Report")
                        }
                        FilledTonalButton(
                            onClick = onShareTeacherReport30Days,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("30-Day Report")
                        }
                    }
                    OutlinedButton(
                        onClick = onShareSessionsCsv,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Full Session Log (.CSV)")
                    }
                }
            }

            // 4. JSON Backup, Restore & Demo Data Controls
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Backup, Restore & Demo Data", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Save or restore your entire offline database (pieces, movements, routines, and session notes) as a portable JSON file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExportBackupJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        FilledTonalButton(
                            onClick = onImportBackupJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReloadDemoData,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reload Demo")
                        }
                        OutlinedButton(
                            onClick = { confirmClearDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Data")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            title = { Text("Clear All Practice & Demo Data?") },
            text = {
                Text("This removes all current pieces, movements, routines, and logged sessions so you can start with a blank slate. Consider exporting a JSON backup first if you have personal logs.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearDialog = false
                        onClearAllData()
                        onDismiss()
                    }
                ) {
                    Text("Clear Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
