package com.ostinato.practicelog.data.repository

import com.ostinato.practicelog.data.local.OstinatoDatabase
import com.ostinato.practicelog.data.local.PieceCategory
import com.ostinato.practicelog.data.local.PieceSectionEntity
import com.ostinato.practicelog.data.local.PieceStatus
import com.ostinato.practicelog.data.local.PieceWithSections
import com.ostinato.practicelog.data.local.PracticeDao
import com.ostinato.practicelog.data.local.PracticeSessionEntity
import com.ostinato.practicelog.data.local.RepertoirePieceEntity
import com.ostinato.practicelog.data.local.RoutineTemplateEntity
import com.ostinato.practicelog.data.local.RoutineTemplateItemEntity
import com.ostinato.practicelog.data.local.RoutineWithItems
import com.ostinato.practicelog.util.BackupPayload
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class PracticeRepository(private val dao: PracticeDao) {

    val allPieces: Flow<List<RepertoirePieceEntity>> = dao.observeAllPieces()
    val allPiecesWithSections: Flow<List<PieceWithSections>> = dao.observeAllPiecesWithSections()
    val allSessions: Flow<List<PracticeSessionEntity>> = dao.observeAllSessions()
    val allRoutines: Flow<List<RoutineWithItems>> = dao.observeAllRoutinesWithItems()

    fun observeSessionsForPiece(pieceId: Long): Flow<List<PracticeSessionEntity>> =
        dao.observeSessionsForPiece(pieceId)

    // --- Repertoire Pieces ---

    suspend fun addPiece(
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
        practiceNotes: String,
        nextSessionGoal: String
    ): Long {
        val clampedInitial = initialBpm.coerceIn(20, 320)
        val piece = RepertoirePieceEntity(
            title = title.trim(),
            composer = composer.trim(),
            instrument = instrument.trim().ifEmpty { "Piano" },
            category = category.name,
            keySignature = keySignature.trim(),
            timeSignatureBeats = timeSignatureBeats.coerceIn(1, 16),
            timeSignatureNoteValue = timeSignatureNoteValue.let { if (it in listOf(2, 4, 8, 16)) it else 4 },
            bestBpm = clampedInitial,
            lastPracticedBpm = clampedInitial,
            targetBpm = targetBpm.coerceIn(20, 320),
            status = status.name,
            practiceNotes = practiceNotes.trim(),
            nextSessionGoal = nextSessionGoal.trim()
        )
        return dao.insertPiece(piece)
    }

    suspend fun updatePiece(piece: RepertoirePieceEntity) {
        dao.updatePiece(piece)
    }

    suspend fun deletePiece(piece: RepertoirePieceEntity) {
        dao.deletePiece(piece)
    }

    // --- Sub-Sections / Movements ---

    suspend fun addSection(
        pieceId: Long,
        name: String,
        orderIndex: Int,
        initialBpm: Int,
        targetBpm: Int,
        timeSignatureBeats: Int,
        timeSignatureNoteValue: Int,
        sectionNotes: String,
        nextSessionGoal: String
    ): Long {
        val clampedBpm = initialBpm.coerceIn(20, 320)
        val section = PieceSectionEntity(
            pieceId = pieceId,
            name = name.trim(),
            orderIndex = orderIndex,
            bestBpm = clampedBpm,
            lastPracticedBpm = clampedBpm,
            targetBpm = targetBpm.coerceIn(20, 320),
            timeSignatureBeats = timeSignatureBeats.coerceIn(1, 16),
            timeSignatureNoteValue = timeSignatureNoteValue.let { if (it in listOf(2, 4, 8, 16)) it else 4 },
            sectionNotes = sectionNotes.trim(),
            nextSessionGoal = nextSessionGoal.trim()
        )
        return dao.insertSection(section)
    }

    suspend fun updateSection(section: PieceSectionEntity) {
        dao.updateSection(section)
    }

    suspend fun deleteSection(section: PieceSectionEntity) {
        dao.deleteSection(section)
    }

    // --- Practice Session Logging (Live & Retroactive) ---

    suspend fun logPracticeSession(
        pieceId: Long?,
        sectionId: Long?,
        pieceTitle: String,
        sectionName: String,
        composer: String,
        instrument: String,
        category: PieceCategory,
        durationMinutes: Int,
        durationSecondsTotal: Int,
        practicedBpm: Int,
        targetBpmSnapshot: Int,
        measuresPracticed: String,
        focusTags: List<String>,
        sessionNotes: String,
        nextSessionGoal: String,
        moodRating: Int,
        routineName: String = "",
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): Long {
        val cleanMins = durationMinutes.coerceAtLeast(1)
        val cleanSecs = durationSecondsTotal.coerceAtLeast(cleanMins * 60)
        val cleanBpm = practicedBpm.coerceIn(20, 320)
        val cleanGoal = nextSessionGoal.trim()

        val session = PracticeSessionEntity(
            pieceId = pieceId,
            sectionId = sectionId,
            pieceTitle = pieceTitle.trim().ifEmpty { "General Technique & Warmup" },
            sectionName = sectionName.trim(),
            composer = composer.trim(),
            instrument = instrument.trim().ifEmpty { "Piano" },
            category = category.name,
            durationMinutes = cleanMins,
            durationSecondsTotal = cleanSecs,
            practicedBpm = cleanBpm,
            targetBpmSnapshot = targetBpmSnapshot.coerceIn(20, 320),
            measuresPracticed = measuresPracticed.trim(),
            focusTags = focusTags.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","),
            sessionNotes = sessionNotes.trim(),
            nextSessionGoal = cleanGoal,
            moodRating = moodRating.coerceIn(1, 5),
            routineName = routineName.trim(),
            timestampEpochMillis = timestampEpochMillis
        )
        val sessionId = dao.insertSession(session)

        // Update Section Best BPM & Last Practiced BPM (without overriding manual PieceStatus)
        if (sectionId != null) {
            val existingSection = dao.getSectionById(sectionId)
            if (existingSection != null) {
                val isNewerOrSame = existingSection.lastPracticedAtEpochMillis == null ||
                    timestampEpochMillis >= existingSection.lastPracticedAtEpochMillis
                dao.updateSection(
                    existingSection.copy(
                        bestBpm = max(existingSection.bestBpm, cleanBpm),
                        lastPracticedBpm = if (isNewerOrSame) cleanBpm else existingSection.lastPracticedBpm,
                        nextSessionGoal = if (cleanGoal.isNotEmpty() && isNewerOrSame) cleanGoal else existingSection.nextSessionGoal,
                        totalMinutesPracticed = existingSection.totalMinutesPracticed + cleanMins,
                        lastPracticedAtEpochMillis = max(
                            existingSection.lastPracticedAtEpochMillis ?: 0L,
                            timestampEpochMillis
                        )
                    )
                )
            }
        }

        // Roll up practice minutes, Best BPM, Last Practiced BPM, and Next Session Goal to parent Piece
        if (pieceId != null) {
            val existingPiece = dao.getPieceById(pieceId)
            if (existingPiece != null) {
                val isNewerOrSame = existingPiece.lastPracticedAtEpochMillis == null ||
                    timestampEpochMillis >= existingPiece.lastPracticedAtEpochMillis

                // If practicing a sub-section, only bump piece bestBpm when no sub-section target conflict,
                // or track piece-level bestBpm cleanly when practicing the piece directly; if sub-sections exist,
                // roll up total minutes & lastPracticedAt while updating piece BPM if sectionId == null (or if higher).
                val updatedBestBpm = if (sectionId == null) {
                    max(existingPiece.bestBpm, cleanBpm)
                } else {
                    existingPiece.bestBpm
                }
                val updatedLastBpm = if (sectionId == null && isNewerOrSame) {
                    cleanBpm
                } else {
                    existingPiece.lastPracticedBpm
                }

                dao.updatePiece(
                    existingPiece.copy(
                        bestBpm = updatedBestBpm,
                        lastPracticedBpm = updatedLastBpm,
                        nextSessionGoal = if (cleanGoal.isNotEmpty() && isNewerOrSame) cleanGoal else existingPiece.nextSessionGoal,
                        totalMinutesPracticed = existingPiece.totalMinutesPracticed + cleanMins,
                        lastPracticedAtEpochMillis = max(
                            existingPiece.lastPracticedAtEpochMillis ?: 0L,
                            timestampEpochMillis
                        )
                        // Note: PieceStatus remains strictly manual per musician preference!
                    )
                )
            }
        }
        return sessionId
    }

    suspend fun updateSession(
        session: PracticeSessionEntity,
        newNotes: String,
        newMeasures: String,
        newNextGoal: String,
        newTags: String,
        newBpm: Int
    ) {
        dao.updateSession(
            session.copy(
                sessionNotes = newNotes.trim(),
                measuresPracticed = newMeasures.trim(),
                nextSessionGoal = newNextGoal.trim(),
                focusTags = newTags.trim(),
                practicedBpm = newBpm.coerceIn(20, 320)
            )
        )
    }

    suspend fun deleteSession(session: PracticeSessionEntity) {
        dao.deleteSession(session)
    }

    // --- Routine Templates ---

    suspend fun saveRoutineTemplate(
        existingId: Long?,
        name: String,
        description: String,
        instrument: String,
        items: List<RoutineTemplateItemEntity>
    ): Long {
        val routineId = if (existingId != null && existingId > 0L) {
            dao.insertRoutineTemplate(
                RoutineTemplateEntity(
                    id = existingId,
                    name = name.trim(),
                    description = description.trim(),
                    instrument = instrument.trim()
                )
            )
            dao.deleteItemsForRoutine(existingId)
            existingId
        } else {
            dao.insertRoutineTemplate(
                RoutineTemplateEntity(
                    name = name.trim(),
                    description = description.trim(),
                    instrument = instrument.trim()
                )
            )
        }

        val normalizedItems = items.mapIndexed { idx, item ->
            item.copy(
                id = 0L,
                routineId = routineId,
                orderIndex = idx,
                customTitle = item.customTitle.trim().ifEmpty { "Practice Block ${idx + 1}" },
                targetMinutes = item.targetMinutes.coerceIn(1, 240),
                suggestedBpm = item.suggestedBpm.coerceIn(20, 320)
            )
        }
        dao.insertRoutineItems(normalizedItems)
        return routineId
    }

    suspend fun deleteRoutineTemplate(routine: RoutineTemplateEntity) {
        dao.deleteRoutineTemplate(routine)
    }

    // --- Demo Data & Backup / Restore ---

    suspend fun clearAllData() {
        dao.clearAllData()
    }

    suspend fun reseedDemoData() {
        OstinatoDatabase.seedSampleData(dao, forceReseed = true)
    }

    suspend fun captureDatabaseSnapshot(
        weeklyGoalMinutes: Int,
        customInstruments: List<String>,
        customFocusTags: List<String>
    ): BackupPayload {
        return BackupPayload(
            pieces = dao.getAllPiecesSnapshot(),
            sections = dao.getAllSectionsSnapshot(),
            sessions = dao.getAllSessionsSnapshot(),
            routines = dao.getAllRoutineTemplatesSnapshot(),
            routineItems = dao.getAllRoutineItemsSnapshot(),
            weeklyGoalMinutes = weeklyGoalMinutes,
            customInstruments = customInstruments,
            customFocusTags = customFocusTags
        )
    }

    suspend fun restoreDatabaseSnapshot(payload: BackupPayload) {
        dao.clearAllData()
        payload.pieces.forEach { dao.insertPiece(it) }
        payload.sections.forEach { dao.insertSection(it) }
        payload.sessions.forEach { dao.insertSession(it) }
        payload.routines.forEach { dao.insertRoutineTemplate(it) }
        if (payload.routineItems.isNotEmpty()) {
            dao.insertRoutineItems(payload.routineItems)
        }
    }
}
