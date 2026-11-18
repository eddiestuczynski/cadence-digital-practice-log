package com.ostinato.practicelog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ostinato.practicelog.data.preferences.DarkVariant
import com.ostinato.practicelog.data.preferences.ThemeMode

// =========================================================================
// Full-colour schemes (opt-in). These keep the original mahogany/brass/sage,
// amber/coral/mint and gold accents for users who want more colour.
// =========================================================================

private val DaySheetMusicScheme = lightColorScheme(
    primary = DayPrimaryMahogany,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DaySecondaryBrass,
    onSecondary = DayOnPrimary,
    secondaryContainer = DaySecondaryContainer,
    onSecondaryContainer = DayOnSecondaryContainer,
    tertiary = DayTertiarySage,
    onTertiary = DayOnPrimary,
    tertiaryContainer = DayTertiaryContainer,
    onTertiaryContainer = DayOnTertiaryContainer,
    background = DayBackgroundIvory,
    onBackground = DayInkPrimary,
    surface = DaySurfaceCream,
    onSurface = DayInkPrimary,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayInkSecondary,
    outline = DayOutline
)

private val NightStageAmberScheme = darkColorScheme(
    primary = NightPrimaryAmber,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondaryCoral,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    tertiary = NightTertiaryMint,
    onTertiary = NightOnPrimary,
    tertiaryContainer = NightTertiaryContainer,
    onTertiaryContainer = NightOnTertiaryContainer,
    background = NightBackgroundObsidian,
    onBackground = NightTextParchment,
    surface = NightSurfaceSlate,
    onSurface = NightTextParchment,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline
)

private val NightOledPitScheme = darkColorScheme(
    primary = OledPrimaryGold,
    onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer,
    onPrimaryContainer = OledOnPrimaryContainer,
    secondary = NightSecondaryCoral,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    tertiary = NightTertiaryMint,
    onTertiary = NightOnPrimary,
    tertiaryContainer = NightTertiaryContainer,
    onTertiaryContainer = NightOnTertiaryContainer,
    background = OledBackgroundPureBlack,
    onBackground = OledTextCrispParchment,
    surface = OledSurfaceCharcoal,
    onSurface = OledTextCrispParchment,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledTextSecondary,
    outline = OledOutline
)

// =========================================================================
// Calm schemes (default). Neutral ink for every supporting role, with a
// single accent per theme: mahogany, amber or gold. Containers collapse to
// the neutral surface variant so the screen reads as one quiet field.
// =========================================================================

private val DayCalmScheme = lightColorScheme(
    primary = DayPrimaryMahogany,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DayInkSecondary,
    onSecondary = DayOnPrimary,
    secondaryContainer = DaySurfaceVariant,
    onSecondaryContainer = DayInkPrimary,
    tertiary = DayInkSecondary,
    onTertiary = DayOnPrimary,
    tertiaryContainer = DaySurfaceVariant,
    onTertiaryContainer = DayInkPrimary,
    background = DayBackgroundIvory,
    onBackground = DayInkPrimary,
    surface = DaySurfaceCream,
    onSurface = DayInkPrimary,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayInkSecondary,
    outline = DayOutline
)

private val NightStageCalmScheme = darkColorScheme(
    primary = NightPrimaryAmber,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightTextSecondary,
    onSecondary = NightBackgroundObsidian,
    secondaryContainer = NightSurfaceVariant,
    onSecondaryContainer = NightTextParchment,
    tertiary = NightTextSecondary,
    onTertiary = NightBackgroundObsidian,
    tertiaryContainer = NightSurfaceVariant,
    onTertiaryContainer = NightTextParchment,
    background = NightBackgroundObsidian,
    onBackground = NightTextParchment,
    surface = NightSurfaceSlate,
    onSurface = NightTextParchment,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline
)

private val NightOledCalmScheme = darkColorScheme(
    primary = OledPrimaryGold,
    onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer,
    onPrimaryContainer = OledOnPrimaryContainer,
    secondary = OledTextSecondary,
    onSecondary = OledBackgroundPureBlack,
    secondaryContainer = OledSurfaceVariant,
    onSecondaryContainer = OledTextCrispParchment,
    tertiary = OledTextSecondary,
    onTertiary = OledBackgroundPureBlack,
    tertiaryContainer = OledSurfaceVariant,
    onTertiaryContainer = OledTextCrispParchment,
    background = OledBackgroundPureBlack,
    onBackground = OledTextCrispParchment,
    surface = OledSurfaceCharcoal,
    onSurface = OledTextCrispParchment,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledTextSecondary,
    outline = OledOutline
)

@Composable
fun isNightModeActive(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.DAY -> false
        ThemeMode.NIGHT_STAGE, ThemeMode.NIGHT_OLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}

@Composable
fun OstinatoTheme(
    themeMode: ThemeMode = ThemeMode.DAY,
    preferredDarkVariant: DarkVariant = DarkVariant.STAGE_AMBER,
    reducedAccents: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.DAY ->
            if (reducedAccents) DayCalmScheme else DaySheetMusicScheme
        ThemeMode.NIGHT_STAGE ->
            if (reducedAccents) NightStageCalmScheme else NightStageAmberScheme
        ThemeMode.NIGHT_OLED ->
            if (reducedAccents) NightOledCalmScheme else NightOledPitScheme
        ThemeMode.SYSTEM -> {
            if (!systemDark) {
                if (reducedAccents) DayCalmScheme else DaySheetMusicScheme
            } else if (preferredDarkVariant == DarkVariant.OLED_BLACK) {
                if (reducedAccents) NightOledCalmScheme else NightOledPitScheme
            } else {
                if (reducedAccents) NightStageCalmScheme else NightStageAmberScheme
            }
        }
    }
    val isDark = themeMode == ThemeMode.NIGHT_STAGE ||
        themeMode == ThemeMode.NIGHT_OLED ||
        (themeMode == ThemeMode.SYSTEM && systemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OstinatoTypography,
        content = content
    )
}
