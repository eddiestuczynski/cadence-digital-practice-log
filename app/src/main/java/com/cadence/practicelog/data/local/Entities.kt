package com.cadence.practicelog.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class PieceStatus(val label: String) {
    LEARNING("Learning"),
    POLISHING("Polishing"),
    MASTERED("Mastered")
}

enum class PieceCategory(val label: String, val shortLabel: String) {
    REPERTOIRE("Repertoire Piece", "Repertoire"),
    ETUDE("Etude / Study", "Etude"),
    TECHNIQUE("Warmup / Scale / Technique", "Technique")
}

@Entity(tableName = "repertoire_pieces")
data class RepertoirePieceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val composer: String = "",
    val instrument: String,
    val category: String = PieceCategory.REPERTOIRE.name,
    val keySignature: String = "",
    val timeSignatureBeats: Int = 4,
    val timeSignatureNoteValue: Int = 4,
    val bestBpm: Int,
    val lastPracticedBpm: Int,
    val targetBpm: Int,
    val status: String = PieceStatus.LEARNING.name,
    val practiceNotes: String = "",
    val nextSessionGoal: String = "",
    val totalMinutesPracticed: Int = 0,
    val lastPracticedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val pieceStatus: PieceStatus
        get() = runCatching { PieceStatus.valueOf(status) }.getOrDefault(PieceStatus.LEARNING)

    val pieceCategory: PieceCategory
        get() = runCatching { PieceCategory.valueOf(category) }.getOrDefault(PieceCategory.REPERTOIRE)

    val timeSignatureLabel: String
        get() = "$timeSignatureBeats/$timeSignatureNoteValue"

    val tempoMasteryProgress: Float
        get() = if (targetBpm <= 0) 0f else (bestBpm.toFloat() / targetBpm.toFloat()).coerceIn(0f, 1f)
}

@Entity(
    tableName = "piece_sections",
    foreignKeys = [
        ForeignKey(
            entity = RepertoirePieceEntity::class,
            parentColumns = ["id"],
            childColumns = ["pieceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pieceId")]
)
data class PieceSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pieceId: Long,
    val name: String,
    val orderIndex: Int = 0,
    val bestBpm: Int,
    val lastPracticedBpm: Int,
    val targetBpm: Int,
    val timeSignatureBeats: Int = 4,
    val timeSignatureNoteValue: Int = 4,
    val sectionNotes: String = "",
    val nextSessionGoal: String = "",
    val totalMinutesPracticed: Int = 0,
    val lastPracticedAtEpochMillis: Long? = null
) {
    val tempoMasteryProgress: Float
        get() = if (targetBpm <= 0) 0f else (bestBpm.toFloat() / targetBpm.toFloat()).coerceIn(0f, 1f)

    val timeSignatureLabel: String
        get() = "$timeSignatureBeats/$timeSignatureNoteValue"
}

data class PieceWithSections(
    @Embedded val piece: RepertoirePieceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "pieceId"
    )
    val sections: List<PieceSectionEntity>
) {
    val sortedSections: List<PieceSectionEntity>
        get() = sections.sortedWith(compareBy({ it.orderIndex }, { it.id }))
}

@Entity(
    tableName = "practice_sessions",
    indices = [Index("pieceId"), Index("sectionId"), Index("timestampEpochMillis")]
)
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pieceId: Long?,
    val sectionId: Long? = null,
    val pieceTitle: String,
    val sectionName: String = "",
    val composer: String = "",
    val instrument: String,
    val category: String = PieceCategory.REPERTOIRE.name,
    val durationMinutes: Int,
    val durationSecondsTotal: Int,
    val practicedBpm: Int,
    val targetBpmSnapshot: Int,
    val measuresPracticed: String = "",
    val focusTags: String = "",
    val sessionNotes: String,
    val nextSessionGoal: String = "",
    val moodRating: Int = 4,
    val routineName: String = "",
    val timestampEpochMillis: Long = System.currentTimeMillis()
) {
    val pieceCategory: PieceCategory
        get() = runCatching { PieceCategory.valueOf(category) }.getOrDefault(PieceCategory.REPERTOIRE)
}

@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val instrument: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_template_items",
    foreignKeys = [
        ForeignKey(
            entity = RoutineTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class RoutineTemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val routineId: Long,
    val orderIndex: Int,
    val pieceId: Long? = null,
    val sectionId: Long? = null,
    val customTitle: String,
    val category: String = PieceCategory.TECHNIQUE.name,
    val targetMinutes: Int = 10,
    val suggestedBpm: Int = 92,
    val notesPrompt: String = ""
)

data class RoutineWithItems(
    @Embedded val routine: RoutineTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val items: List<RoutineTemplateItemEntity>
) {
    val sortedItems: List<RoutineTemplateItemEntity>
        get() = items.sortedWith(compareBy({ it.orderIndex }, { it.id }))

    val totalTargetMinutes: Int
        get() = items.sumOf { it.targetMinutes }
}
