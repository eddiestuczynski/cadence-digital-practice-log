package com.cadence.practicelog.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.cadence.practicelog.data.preferences.DarkVariant
import com.cadence.practicelog.data.preferences.ThemeMode
import com.cadence.practicelog.ui.theme.activeThemeSubtitle

@Composable
fun CadenceTopBar(
    currentThemeMode: ThemeMode,
    preferredDarkVariant: DarkVariant,
    weeklyStreak: Int,
    globalInstrumentFilter: String,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDarkVariant: (DarkVariant) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    val pillBg by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        label = "themePillBg"
    )

    val modeIcon = when (currentThemeMode) {
        ThemeMode.DAY -> Icons.Default.LightMode
        ThemeMode.NIGHT_STAGE -> Icons.Default.DarkMode
        ThemeMode.NIGHT_OLED -> Icons.Default.Contrast
        ThemeMode.SYSTEM -> Icons.Default.AutoAwesome
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Title + Active Readability Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cadence",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (globalInstrumentFilter != "All") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = globalInstrumentFilter,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = activeThemeSubtitle(currentThemeMode, preferredDarkVariant),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Weekly Consistency Streak Pill (Mon–Sun Goal Streak)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Weekly Consistency Streak",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${weeklyStreak}w",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // 4-Mode Day / Stage Night / OLED Night / Auto Selector Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillBg,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { showThemeDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = "Select Day or Night Visual Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentThemeMode.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Settings, Export, Backup & Instruments Button
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings, Export & Instruments",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Visual Readability Mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose high-contrast lighting tailored for bright music stands, dim stages, or pitch-black pits:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectThemeMode(mode)
                                    if (mode != ThemeMode.SYSTEM) {
                                        showThemeDialog = false
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentThemeMode == mode,
                                onClick = {
                                    onSelectThemeMode(mode)
                                    if (mode != ThemeMode.SYSTEM) {
                                        showThemeDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (currentThemeMode == ThemeMode.SYSTEM) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            text = "Dark Palette when System is in Night Mode:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DarkVariant.entries.forEach { variant ->
                                FilterChip(
                                    selected = preferredDarkVariant == variant,
                                    onClick = { onSelectDarkVariant(variant) },
                                    label = {
                                        Text(
                                            if (variant == DarkVariant.OLED_BLACK) "OLED Pit" else "Stage Amber"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
