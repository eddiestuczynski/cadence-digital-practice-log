package com.cadence.practicelog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadence.practicelog.ui.MainViewModel
import com.cadence.practicelog.ui.MainViewModelFactory
import com.cadence.practicelog.ui.components.CadenceTopBar
import com.cadence.practicelog.ui.components.SettingsAndDataSheet
import com.cadence.practicelog.ui.screens.AnalyticsScreen
import com.cadence.practicelog.ui.screens.PracticeStudioScreen
import com.cadence.practicelog.ui.screens.RepertoireScreen
import com.cadence.practicelog.ui.screens.SessionJournalScreen
import com.cadence.practicelog.ui.theme.CadencePracticeLogTheme
import com.cadence.practicelog.util.AnalyticsCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CadenceTab(val label: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.PlayCircleFilled),
    JOURNAL("Journal", Icons.Default.HistoryEdu),
    REPERTOIRE("Repertoire", Icons.Default.LibraryMusic),
    ANALYTICS("Stats & BPM", Icons.Default.Analytics)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CadenceApplication

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(
                    repository = app.practiceRepository,
                    preferences = app.userPreferencesRepository
                )
            )
            CadencePracticeLogApp(viewModel = viewModel)
        }
    }
}

@Composable
fun CadencePracticeLogApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentView = LocalView.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val preferredDarkVariant by viewModel.preferredDarkVariant.collectAsStateWithLifecycle()
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsStateWithLifecycle()
    val weeklyGoalMinutes by viewModel.weeklyGoalMinutes.collectAsStateWithLifecycle()
    val globalInstrumentFilter by viewModel.globalInstrumentFilter.collectAsStateWithLifecycle()
    val allInstruments by viewModel.allInstruments.collectAsStateWithLifecycle()
    val customInstruments by viewModel.customInstruments.collectAsStateWithLifecycle()
    val allFocusTags by viewModel.allFocusTags.collectAsStateWithLifecycle()
    val customFocusTags by viewModel.customFocusTags.collectAsStateWithLifecycle()

    val piecesWithSections by viewModel.piecesWithSections.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val isMetronomePlaying by viewModel.isMetronomePlaying.collectAsStateWithLifecycle()
    val currentBeat by viewModel.currentMetronomeBeat.collectAsStateWithLifecycle()
    val beatPulseId by viewModel.beatPulseId.collectAsStateWithLifecycle()

    // Automatically keep music-stand screen awake whenever stopwatch or metronome is running
    val shouldStayAwake = keepScreenAwake &&
        (studioState.timerRunning || isMetronomePlaying || studioState.isMusicStandModeOpen)
    DisposableEffect(shouldStayAwake) {
        currentView.keepScreenOn = shouldStayAwake
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // Apply optional Global Instrument Filter across views
    val filteredPiecesWithSections = remember(piecesWithSections, globalInstrumentFilter) {
        if (globalInstrumentFilter == "All") piecesWithSections
        else piecesWithSections.filter { it.piece.instrument.equals(globalInstrumentFilter, ignoreCase = true) }
    }
    val filteredSessions = remember(sessions, globalInstrumentFilter) {
        if (globalInstrumentFilter == "All") sessions
        else sessions.filter { it.instrument.equals(globalInstrumentFilter, ignoreCase = true) }
    }

    val weeklyStreak = remember(filteredSessions, weeklyGoalMinutes) {
        AnalyticsCalculator.calculateWeeklyConsistencyStreak(filteredSessions, weeklyGoalMinutes)
    }

    var activeTab by rememberSaveable { mutableStateOf(CadenceTab.STUDIO) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var settingsStatusMessage by remember { mutableStateOf<String?>(null) }

    // Android Storage Access Framework Launchers for JSON Backup & Restore
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.generateBackupJson { jsonContent ->
                val result = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                }
                settingsStatusMessage = if (result.isSuccess) {
                    "Backup exported successfully!"
                } else {
                    "Export failed: ${result.exceptionOrNull()?.localizedMessage}"
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val readResult = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Unable to read selected file.")
            }
            readResult.onSuccess { jsonText ->
                viewModel.restoreFromBackupJson(jsonText) { outcome ->
                    settingsStatusMessage = outcome.fold(
                        onSuccess = { it },
                        onFailure = { err -> "Invalid backup JSON: ${err.localizedMessage}" }
                    )
                }
            }.onFailure { err ->
                settingsStatusMessage = "Could not open file: ${err.localizedMessage}"
            }
        }
    }

    CadencePracticeLogTheme(
        themeMode = themeMode,
        preferredDarkVariant = preferredDarkVariant
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CadenceTopBar(
                    currentThemeMode = themeMode,
                    preferredDarkVariant = preferredDarkVariant,
                    weeklyStreak = weeklyStreak,
                    globalInstrumentFilter = globalInstrumentFilter,
                    onSelectThemeMode = viewModel::setThemeMode,
                    onSelectDarkVariant = viewModel::setPreferredDarkVariant,
                    onOpenSettings = {
                        settingsStatusMessage = null
                        showSettingsSheet = true
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    CadenceTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    CadenceTab.STUDIO -> PracticeStudioScreen(
                        studioState = studioState,
                        piecesWithSections = filteredPiecesWithSections.ifEmpty { piecesWithSections },
                        sessions = sessions,
                        routines = routines,
                        allInstruments = allInstruments,
                        allFocusTags = allFocusTags,
                        keepScreenAwakeEnabled = keepScreenAwake,
                        isMetronomePlaying = isMetronomePlaying,
                        currentBeat = currentBeat,
                        beatPulseId = beatPulseId,
                        onToggleTimer = viewModel::toggleTimer,
                        onResetTimer = viewModel::resetTimer,
                        onAdjustMinutes = viewModel::adjustTimerMinutes,
                        onTargetDurationChange = viewModel::setTargetDurationMinutes,
                        onSelectPieceAndSection = viewModel::selectPieceForPractice,
                        onSelectInstrument = viewModel::setSelectedInstrument,
                        onSelectCategory = viewModel::setSelectedCategory,
                        onCustomTitleChange = viewModel::setCustomPieceTitle,
                        onBpmChange = viewModel::setMetronomeBpm,
                        onTimeSignatureChange = viewModel::setTimeSignature,
                        onCycleBeatAccent = viewModel::cycleBeatAccent,
                        onApplyJazzBackbeat = viewModel::applyJazzBackbeatPattern,
                        onSubdivisionChange = viewModel::setSubdivisionMode,
                        onToggleMetronomeMute = viewModel::toggleMetronomeMute,
                        onToggleMetronomePlay = viewModel::toggleMetronomePlayback,
                        onTapTempo = viewModel::registerTapTempo,
                        onMeasuresChange = viewModel::updateMeasuresPracticed,
                        onToggleFocusTag = viewModel::toggleFocusTag,
                        onAddCustomFocusTag = viewModel::addCustomFocusTag,
                        onNotesChange = viewModel::updateSessionNotes,
                        onNextGoalChange = viewModel::updateNextSessionGoal,
                        onAppendNoteTemplate = viewModel::appendNoteTemplate,
                        onMoodRatingChange = viewModel::setMoodRating,
                        onSaveSession = viewModel::saveCurrentPracticeSession,
                        onLogPastSession = viewModel::logPastPracticeSession,
                        onLoadRoutine = viewModel::loadRoutineIntoQueue,
                        onAddAdHocQueueItem = viewModel::addAdHocItemToQueue,
                        onSelectQueueIndex = viewModel::selectQueueItemIndex,
                        onExitQueueMode = viewModel::exitQueueMode,
                        onSaveQueueAsTemplate = viewModel::saveActiveQueueAsRoutineTemplate,
                        onDeleteRoutineTemplate = viewModel::deleteRoutineTemplate,
                        onSetMusicStandMode = viewModel::setMusicStandModeOpen,
                        onDismissBanner = viewModel::dismissSavedBanner
                    )

                    CadenceTab.JOURNAL -> SessionJournalScreen(
                        sessions = filteredSessions,
                        piecesWithSections = piecesWithSections,
                        allInstruments = allInstruments,
                        allFocusTags = allFocusTags,
                        onLogPastSession = viewModel::logPastPracticeSession,
                        onUpdateSession = viewModel::updateExistingSession,
                        onDeleteSession = viewModel::deleteSession
                    )

                    CadenceTab.REPERTOIRE -> RepertoireScreen(
                        piecesWithSections = filteredPiecesWithSections,
                        allInstruments = allInstruments,
                        onPracticeNow = { piece, section ->
                            viewModel.selectPieceForPractice(piece, section)
                            activeTab = CadenceTab.STUDIO
                        },
                        onAddPiece = viewModel::addRepertoirePiece,
                        onUpdatePiece = viewModel::updateRepertoirePiece,
                        onDeletePiece = viewModel::deleteRepertoirePiece,
                        onAddSection = viewModel::addPieceSection,
                        onDeleteSection = viewModel::deletePieceSection
                    )

                    CadenceTab.ANALYTICS -> AnalyticsScreen(
                        sessions = filteredSessions,
                        piecesWithSections = filteredPiecesWithSections,
                        weeklyGoalMinutes = weeklyGoalMinutes,
                        onUpdateWeeklyGoal = viewModel::updateWeeklyGoalMinutes,
                        onPracticePieceNow = { piece, section ->
                            viewModel.selectPieceForPractice(piece, section)
                            activeTab = CadenceTab.STUDIO
                        }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            SettingsAndDataSheet(
                themeMode = themeMode,
                preferredDarkVariant = preferredDarkVariant,
                keepScreenAwake = keepScreenAwake,
                globalInstrumentFilter = globalInstrumentFilter,
                allInstruments = allInstruments,
                customInstruments = customInstruments,
                customFocusTags = customFocusTags,
                statusMessage = settingsStatusMessage,
                onSelectThemeMode = viewModel::setThemeMode,
                onSelectDarkVariant = viewModel::setPreferredDarkVariant,
                onKeepScreenAwakeChange = viewModel::setKeepScreenAwake,
                onSelectGlobalInstrument = viewModel::setGlobalInstrumentFilter,
                onAddCustomInstrument = viewModel::addCustomInstrument,
                onRemoveCustomInstrument = viewModel::removeCustomInstrument,
                onAddCustomFocusTag = viewModel::addCustomFocusTag,
                onRemoveCustomFocusTag = viewModel::removeCustomFocusTag,
                onShareTeacherReport7Days = {
                    sharePlainText(
                        context = context,
                        subject = "Cadence Practice Report (Last 7 Days)",
                        body = viewModel.buildTeacherReportMarkdown(daysWindow = 7)
                    )
                },
                onShareTeacherReport30Days = {
                    sharePlainText(
                        context = context,
                        subject = "Cadence Practice Report (Last 30 Days)",
                        body = viewModel.buildTeacherReportMarkdown(daysWindow = 30)
                    )
                },
                onShareSessionsCsv = {
                    sharePlainText(
                        context = context,
                        subject = "Cadence Practice Sessions CSV",
                        body = viewModel.buildSessionsCsv()
                    )
                },
                onExportBackupJson = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    exportJsonLauncher.launch("cadence_practice_backup_$stamp.json")
                },
                onImportBackupJson = {
                    importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onClearAllData = viewModel::clearAllData,
                onReloadDemoData = {
                    viewModel.reloadDemoData()
                    settingsStatusMessage = "Musician demo repertoire, routines & history reloaded!"
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

private fun sharePlainText(context: Context, subject: String, body: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(sendIntent, subject))
}
