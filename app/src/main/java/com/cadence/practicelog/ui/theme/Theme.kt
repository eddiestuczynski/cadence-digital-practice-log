package com.cadence.practicelog.ui.theme

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
import com.cadence.practicelog.data.preferences.DarkVariant
import com.cadence.practicelog.data.preferences.ThemeMode

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

@Composable
fun isNightModeActive(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.DAY -> false
        ThemeMode.NIGHT_STAGE, ThemeMode.NIGHT_OLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}

@Composable
fun activeThemeSubtitle(themeMode: ThemeMode, preferredDarkVariant: DarkVariant): String {
    val sysDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.DAY -> "Day • Sheet Music Mode"
        ThemeMode.NIGHT_STAGE -> "Night • Stage Amber Mode"
        ThemeMode.NIGHT_OLED -> "Night • OLED Pit Black"
        ThemeMode.SYSTEM -> if (sysDark) {
            if (preferredDarkVariant == DarkVariant.OLED_BLACK) "Auto • OLED Pit Black"
            else "Auto • Stage Amber Mode"
        } else {
            "Auto • Sheet Music Day"
        }
    }
}

@Composable
fun CadencePracticeLogTheme(
    themeMode: ThemeMode = ThemeMode.DAY,
    preferredDarkVariant: DarkVariant = DarkVariant.STAGE_AMBER,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.DAY -> DaySheetMusicScheme
        ThemeMode.NIGHT_STAGE -> NightStageAmberScheme
        ThemeMode.NIGHT_OLED -> NightOledPitScheme
        ThemeMode.SYSTEM -> {
            if (!systemDark) DaySheetMusicScheme
            else if (preferredDarkVariant == DarkVariant.OLED_BLACK) NightOledPitScheme
            else NightStageAmberScheme
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
        typography = CadenceTypography,
        content = content
    )
}
