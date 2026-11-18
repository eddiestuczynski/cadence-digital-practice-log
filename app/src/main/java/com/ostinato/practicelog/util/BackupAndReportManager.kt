package com.ostinato.practicelog.util

import com.ostinato.practicelog.data.local.PieceCategory
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceStatus
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.data.local.RoutineTemplateEntity
import com.ostinato.practicelog.data.local.RoutineTemplateItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BackupPayload(
    val pieces: List<RepertoirePieceEntity>,
    val sections: List<PieceSectionEntity>,
    val sessions: List<PracticeSessionEntity>,
    val routines: List<RoutineTemplateEntity>,
    val routineItems: List<RoutineTemplateItemEntity>,
    val weeklyGoalMinutes: Int,
    val customInstruments: List<String>,
    val customFocusTags: List<String>
)

object BackupAndReportManager {

    private const val BACKUP_SCHEMA_VERSION = 2

    fun serializeBackupToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", BACKUP_SCHEMA_VERSION)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())
        root.put("weeklyGoalMinutes", payload.weeklyGoalMinutes)
        root.put("customInstruments", JSONArray(payload.customInstruments))
        root.put("customFocusTags", payload.customFocusTags.let { JSONArray(it) })

        val piecesArr = JSONArray()
        payload.pieces.forEach { p ->
            piecesArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("composer", p.composer)
                    put("instrument", p.instrument)
                    put("category", p.category)
                    put("keySignature", p.keySignature)
                    put("timeSignatureBeats", p.timeSignatureBeats)
                    put("timeSignatureNoteValue", p.timeSignatureNoteValue)
                    put("bestBpm", p.bestBpm)
                    put("lastPracticedBpm", p.lastPracticedBpm)
                    put("targetBpm", p.targetBpm)
                    put("status", p.status)
                    put("practiceNotes", p.practiceNotes)
                    put("nextSessionGoal", p.nextSessionGoal)
                    put("totalMinutesPracticed", p.totalMinutesPracticed)
                    if (p.lastPracticedAtEpochMillis != null) {
                        put("lastPracticedAtEpochMillis", p.lastPracticedAtEpochMillis)
                    }
                    put("createdAtEpochMillis", p.createdAtEpochMillis)
                }
            )
        }
        root.put("pieces", piecesArr)

        val sectionsArr = JSONArray()
        payload.sections.forEach { s ->
            sectionsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("pieceId", s.pieceId)
                    put("name", s.name)
                    put("orderIndex", s.orderIndex)
                    put("bestBpm", s.bestBpm)
                    put("lastPracticedBpm", s.lastPracticedBpm)
                    put("targetBpm", s.targetBpm)
                    put("timeSignatureBeats", s.timeSignatureBeats)
                    put("timeSignatureNoteValue", s.timeSignatureNoteValue)
                    put("sectionNotes", s.sectionNotes)
                    put("nextSessionGoal", s.nextSessionGoal)
                    put("totalMinutesPracticed", s.totalMinutesPracticed)
                    if (s.lastPracticedAtEpochMillis != null) {
                        put("lastPracticedAtEpochMillis", s.lastPracticedAtEpochMillis)
                    }
                }
            )
        }
        root.put("sections", sectionsArr)

        val sessionsArr = JSONArray()
        payload.sessions.forEach { sess ->
            sessionsArr.put(
                JSONObject().apply {
                    put("id", sess.id)
                    if (sess.pieceId != null) put("pieceId", sess.pieceId)
                    if (sess.sectionId != null) put("sectionId", sess.sectionId)
                    put("pieceTitle", sess.pieceTitle)
                    put("sectionName", sess.sectionName)
                    put("composer", sess.composer)
                    put("instrument", sess.instrument)
                    put("category", sess.category)
                    put("durationMinutes", sess.durationMinutes)
                    put("durationSecondsTotal", sess.durationSecondsTotal)
                    put("practicedBpm", sess.practicedBpm)
                    put("targetBpmSnapshot", sess.targetBpmSnapshot)
                    put("measuresPracticed", sess.measuresPracticed)
                    put("focusTags", sess.focusTags)
                    put("sessionNotes", sess.sessionNotes)
                    put("nextSessionGoal", sess.nextSessionGoal)
                    put("moodRating", sess.moodRating)
                    put("routineName", sess.routineName)
                    put("timestampEpochMillis", sess.timestampEpochMillis)
                }
            )
        }
        root.put("sessions", sessionsArr)

        val routinesArr = JSONArray()
        payload.routines.forEach { r ->
            routinesArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("description", r.description)
                    put("instrument", r.instrument)
                    put("createdAtEpochMillis", r.createdAtEpochMillis)
                }
            )
        }
        root.put("routines", routinesArr)

        val routineItemsArr = JSONArray()
        payload.routineItems.forEach { item ->
            routineItemsArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("routineId", item.routineId)
                    put("orderIndex", item.orderIndex)
                    if (item.pieceId != null) put("pieceId", item.pieceId)
                    if (item.sectionId != null) put("sectionId", item.sectionId)
                    put("customTitle", item.customTitle)
                    put("category", item.category)
                    put("targetMinutes", item.targetMinutes)
                    put("suggestedBpm", item.suggestedBpm)
                    put("notesPrompt", item.notesPrompt)
                }
            )
        }
        root.put("routineItems", routineItemsArr)

        return root.toString(2)
    }

    fun parseBackupFromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val weeklyGoal = root.optInt("weeklyGoalMinutes", 180)
        val customInst = root.optJSONArray("customInstruments").toStringList()
        val customTags = root.optJSONArray("customFocusTags").toStringList()

        val pieces = mutableListOf<RepertoirePieceEntity>()
        val piecesArr = root.optJSONArray("pieces") ?: JSONArray()
        for (i in 0 until piecesArr.length()) {
            val o = piecesArr.getJSONObject(i)
            val fallbackBpm = o.optInt("currentBpm", 80)
            pieces.add(
                RepertoirePieceEntity(
                    id = o.getLong("id"),
                    title = o.optString("title", "Untitled Piece"),
                    composer = o.optString("composer", ""),
                    instrument = o.optString("instrument", "Piano"),
                    category = o.optString("category", PieceCategory.REPERTOIRE.name),
                    keySignature = o.optString("keySignature", ""),
                    timeSignatureBeats = o.optInt("timeSignatureBeats", 4),
                    timeSignatureNoteValue = o.optInt("timeSignatureNoteValue", 4),
                    bestBpm = o.optInt("bestBpm", fallbackBpm),
                    lastPracticedBpm = o.optInt("lastPracticedBpm", fallbackBpm),
                    targetBpm = o.optInt("targetBpm", 120),
                    status = o.optString("status", PieceStatus.LEARNING.name),
                    practiceNotes = o.optString("practiceNotes", ""),
                    nextSessionGoal = o.optString("nextSessionGoal", ""),
                    totalMinutesPracticed = o.optInt("totalMinutesPracticed", 0),
                    lastPracticedAtEpochMillis = o.optNullableLong("lastPracticedAtEpochMillis"),
                    createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis())
                )
            )
        }

        val sections = mutableListOf<PieceSectionEntity>()
        val sectionsArr = root.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until sectionsArr.length()) {
            val o = sectionsArr.getJSONObject(i)
            sections.add(
                PieceSectionEntity(
                    id = o.getLong("id"),
                    pieceId = o.getLong("pieceId"),
                    name = o.optString("name", "Movement"),
                    orderIndex = o.optInt("orderIndex", i),
                    bestBpm = o.optInt("bestBpm", 80),
                    lastPracticedBpm = o.optInt("lastPracticedBpm", 80),
                    targetBpm = o.optInt("targetBpm", 120),
                    timeSignatureBeats = o.optInt("timeSignatureBeats", 4),
                    timeSignatureNoteValue = o.optInt("timeSignatureNoteValue", 4),
                    sectionNotes = o.optString("sectionNotes", ""),
                    nextSessionGoal = o.optString("nextSessionGoal", ""),
                    totalMinutesPracticed = o.optInt("totalMinutesPracticed", 0),
                    lastPracticedAtEpochMillis = o.optNullableLong("lastPracticedAtEpochMillis")
                )
            )
        }

        val sessions = mutableListOf<PracticeSessionEntity>()
        val sessionsArr = root.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sessionsArr.length()) {
            val o = sessionsArr.getJSONObject(i)
            val mins = o.optInt("durationMinutes", 15)
            sessions.add(
                PracticeSessionEntity(
                    id = o.getLong("id"),
                    pieceId = o.optNullableLong("pieceId"),
                    sectionId = o.optNullableLong("sectionId"),
                    pieceTitle = o.optString("pieceTitle", "Practice Session"),
                    sectionName = o.optString("sectionName", ""),
                    composer = o.optString("composer", ""),
                    instrument = o.optString("instrument", "Piano"),
                    category = o.optString("category", PieceCategory.REPERTOIRE.name),
                    durationMinutes = mins,
                    durationSecondsTotal = o.optInt("durationSecondsTotal", mins * 60),
                    practicedBpm = o.optInt("practicedBpm", 90),
                    targetBpmSnapshot = o.optInt("targetBpmSnapshot", 120),
                    measuresPracticed = o.optString("measuresPracticed", ""),
                    focusTags = o.optString("focusTags", ""),
                    sessionNotes = o.optString("sessionNotes", ""),
                    nextSessionGoal = o.optString("nextSessionGoal", ""),
                    moodRating = o.optInt("moodRating", 4),
                    routineName = o.optString("routineName", ""),
                    timestampEpochMillis = o.optLong("timestampEpochMillis", System.currentTimeMillis())
                )
            )
        }

        val routines = mutableListOf<RoutineTemplateEntity>()
        val routinesArr = root.optJSONArray("routines") ?: JSONArray()
        for (i in 0 until routinesArr.length()) {
            val o = routinesArr.getJSONObject(i)
            routines.add(
                RoutineTemplateEntity(
                    id = o.getLong("id"),
                    name = o.optString("name", "Practice Routine"),
                    description = o.optString("description", ""),
                    instrument = o.optString("instrument", ""),
                    createdAtEpochMillis = o.optLong("createdAtEpochMillis", System.currentTimeMillis())
                )
            )
        }

        val routineItems = mutableListOf<RoutineTemplateItemEntity>()
        val itemsArr = root.optJSONArray("routineItems") ?: JSONArray()
        for (i in 0 until itemsArr.length()) {
            val o = itemsArr.getJSONObject(i)
            routineItems.add(
                RoutineTemplateItemEntity(
                    id = o.getLong("id"),
                    routineId = o.getLong("routineId"),
                    orderIndex = o.optInt("orderIndex", i),
                    pieceId = o.optNullableLong("pieceId"),
                    sectionId = o.optNullableLong("sectionId"),
                    customTitle = o.optString("customTitle", "Block"),
                    category = o.optString("category", PieceCategory.TECHNIQUE.name),
                    targetMinutes = o.optInt("targetMinutes", 10),
                    suggestedBpm = o.optInt("suggestedBpm", 90),
                    notesPrompt = o.optString("notesPrompt", "")
                )
            )
        }

        return BackupPayload(
            pieces = pieces,
            sections = sections,
            sessions = sessions,
            routines = routines,
            routineItems = routineItems,
            weeklyGoalMinutes = weeklyGoal,
            customInstruments = customInst,
            customFocusTags = customTags
        )
    }

    /**
     * Generates a clean, teacher-friendly Markdown practice report summarizing recent sessions,
     * pieces practiced, BPM progress, measures worked on, and session reflections.
     */
    fun generateTeacherReportMarkdown(
        sessions: List<PracticeSessionEntity>,
        pieces: List<RepertoirePieceEntity>,
        daysWindow: Int = 7,
        weeklyStreak: Int = 0
    ): String {
        val now = System.currentTimeMillis()
        val cutoff = now - TimeUnit.DAYS.toMillis(daysWindow.toLong())
        val recent = sessions.filter { it.timestampEpochMillis >= cutoff }
            .sortedByDescending { it.timestampEpochMillis }

        val dateHeaderFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val sessionFmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US)

        val totalMins = recent.sumOf { it.durationMinutes }
        val hours = totalMins / 60
        val mins = totalMins % 60

        val sb = StringBuilder()
        sb.appendLine("# Ostinato Practice Report (Last $daysWindow Days)")
        sb.appendLine("Generated: ${dateHeaderFmt.format(Date(now))}")
        sb.appendLine("Total Practice Time: **${hours}h ${mins}m** across **${recent.size} sessions**")
        sb.appendLine("Weekly Consistency Streak: **$weeklyStreak week(s)**")
        sb.appendLine()

        val activePieces = pieces.filter { it.pieceStatus != PieceStatus.MASTERED }
        if (activePieces.isNotEmpty()) {
            sb.appendLine("## Current Repertoire & Tempo Progress")
            activePieces.forEach { p ->
                sb.appendLine("- **${p.title}** (${p.instrument}, ${p.pieceStatus.label}): Best ${p.bestBpm} BPM / Last ${p.lastPracticedBpm} BPM → Target ${p.targetBpm} BPM")
                if (p.nextSessionGoal.isNotBlank()) {
                    sb.appendLine("  - Next Focus: ${p.nextSessionGoal}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("## Detailed Session Log")
        if (recent.isEmpty()) {
            sb.appendLine("_No sessions logged in the last $daysWindow days._")
        } else {
            recent.forEach { s ->
                val secSuffix = if (s.sectionName.isNotBlank()) " — ${s.sectionName}" else ""
                val measuresSuffix = if (s.measuresPracticed.isNotBlank()) " [${s.measuresPracticed}]" else ""
                sb.appendLine("### ${sessionFmt.format(Date(s.timestampEpochMillis))}")
                sb.appendLine("- **Piece**: ${s.pieceTitle}$secSuffix$measuresSuffix (${s.instrument})")
                sb.appendLine("- **Duration & Tempo**: ${s.durationMinutes} min @ ${s.practicedBpm} BPM (Target ${s.targetBpmSnapshot} BPM)")
                if (s.focusTags.isNotBlank()) {
                    sb.appendLine("- **Focus**: ${s.focusTags}")
                }
                if (s.sessionNotes.isNotBlank()) {
                    sb.appendLine("- **Notes**: ${s.sessionNotes}")
                }
                if (s.nextSessionGoal.isNotBlank()) {
                    sb.appendLine("- **Next Session Goal**: ${s.nextSessionGoal}")
                }
                sb.appendLine()
            }
        }
        return sb.toString().trim()
    }

    /**
     * Generates a standard CSV export of all logged practice sessions for spreadsheets.
     */
    fun generateSessionsCsv(sessions: List<PracticeSessionEntity>): String {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("Date,Instrument,Category,Piece,Movement_Section,Measures,Duration_Min,Practiced_BPM,Target_BPM,Rating,Focus_Tags,Session_Notes,Next_Goal")
        sessions.sortedByDescending { it.timestampEpochMillis }.forEach { s ->
            val row = listOf(
                isoFmt.format(Date(s.timestampEpochMillis)),
                s.instrument,
                s.pieceCategory.shortLabel,
                s.pieceTitle,
                s.sectionName,
                s.measuresPracticed,
                s.durationMinutes.toString(),
                s.practicedBpm.toString(),
                s.targetBpmSnapshot.toString(),
                s.moodRating.toString(),
                s.focusTags,
                s.sessionNotes,
                s.nextSessionGoal
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val normalized = value.replace("\r\n", " ").replace("\n", " ")
        return if (normalized.contains(",") || normalized.contains("\"")) {
            "\"${normalized.replace("\"", "\"\"")}\""
        } else {
            normalized
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until length()) {
            val s = optString(i, "").trim()
            if (s.isNotEmpty()) out.add(s)
        }
        return out
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        return if (has(key) && !isNull(key)) optLong(key) else null
    }
}
