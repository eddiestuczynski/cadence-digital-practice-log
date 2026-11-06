package com.cadence.practicelog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    // --- Repertoire Pieces & Sub-Sections ---

    @Query(
        """
        SELECT * FROM repertoire_pieces 
        ORDER BY CASE status WHEN 'LEARNING' THEN 1 WHEN 'POLISHING' THEN 2 ELSE 3 END, title ASC
        """
    )
    fun observeAllPieces(): Flow<List<RepertoirePieceEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM repertoire_pieces 
        ORDER BY CASE status WHEN 'LEARNING' THEN 1 WHEN 'POLISHING' THEN 2 ELSE 3 END, title ASC
        """
    )
    fun observeAllPiecesWithSections(): Flow<List<PieceWithSections>>

    @Query("SELECT * FROM repertoire_pieces")
    suspend fun getAllPiecesSnapshot(): List<RepertoirePieceEntity>

    @Query("SELECT * FROM repertoire_pieces WHERE id = :pieceId LIMIT 1")
    suspend fun getPieceById(pieceId: Long): RepertoirePieceEntity?

    @Query("SELECT COUNT(*) FROM repertoire_pieces")
    suspend fun getPieceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPiece(piece: RepertoirePieceEntity): Long

    @Update
    suspend fun updatePiece(piece: RepertoirePieceEntity)

    @Delete
    suspend fun deletePiece(piece: RepertoirePieceEntity)

    @Query("SELECT * FROM piece_sections ORDER BY pieceId ASC, orderIndex ASC, id ASC")
    suspend fun getAllSectionsSnapshot(): List<PieceSectionEntity>

    @Query("SELECT * FROM piece_sections WHERE id = :sectionId LIMIT 1")
    suspend fun getSectionById(sectionId: Long): PieceSectionEntity?

    @Query("SELECT * FROM piece_sections WHERE pieceId = :pieceId ORDER BY orderIndex ASC, id ASC")
    suspend fun getSectionsForPiece(pieceId: Long): List<PieceSectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: PieceSectionEntity): Long

    @Update
    suspend fun updateSection(section: PieceSectionEntity)

    @Delete
    suspend fun deleteSection(section: PieceSectionEntity)

    // --- Practice Sessions ---

    @Query("SELECT * FROM practice_sessions ORDER BY timestampEpochMillis DESC")
    fun observeAllSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions ORDER BY timestampEpochMillis DESC")
    suspend fun getAllSessionsSnapshot(): List<PracticeSessionEntity>

    @Query("SELECT * FROM practice_sessions WHERE pieceId = :pieceId ORDER BY timestampEpochMillis ASC")
    fun observeSessionsForPiece(pieceId: Long): Flow<List<PracticeSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Update
    suspend fun updateSession(session: PracticeSessionEntity)

    @Delete
    suspend fun deleteSession(session: PracticeSessionEntity)

    // --- Routine Templates ---

    @Transaction
    @Query("SELECT * FROM routine_templates ORDER BY createdAtEpochMillis DESC")
    fun observeAllRoutinesWithItems(): Flow<List<RoutineWithItems>>

    @Query("SELECT * FROM routine_templates")
    suspend fun getAllRoutineTemplatesSnapshot(): List<RoutineTemplateEntity>

    @Query("SELECT * FROM routine_template_items ORDER BY routineId ASC, orderIndex ASC")
    suspend fun getAllRoutineItemsSnapshot(): List<RoutineTemplateItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTemplate(routine: RoutineTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineItems(items: List<RoutineTemplateItemEntity>)

    @Query("DELETE FROM routine_template_items WHERE routineId = :routineId")
    suspend fun deleteItemsForRoutine(routineId: Long)

    @Delete
    suspend fun deleteRoutineTemplate(routine: RoutineTemplateEntity)

    // --- Bulk Reset Operations ---

    @Query("DELETE FROM practice_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM piece_sections")
    suspend fun deleteAllSections()

    @Query("DELETE FROM repertoire_pieces")
    suspend fun deleteAllPieces()

    @Query("DELETE FROM routine_template_items")
    suspend fun deleteAllRoutineItems()

    @Query("DELETE FROM routine_templates")
    suspend fun deleteAllRoutineTemplates()

    @Transaction
    suspend fun clearAllData() {
        deleteAllSessions()
        deleteAllRoutineItems()
        deleteAllRoutineTemplates()
        deleteAllSections()
        deleteAllPieces()
    }
}
