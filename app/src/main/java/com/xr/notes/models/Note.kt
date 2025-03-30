package com.xr.notes.models

// File: app/src/main/java/com/example/notesapp/data/models/Note.kt

import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var content: String = "",
    var isEncrypted: Boolean = false,
    var createdAt: Date = Date(),
    var modifiedAt: Date = Date()
) {
    // Get title from the first line of the note
    val title: String
        get() = content.lines().firstOrNull()?.takeIf { it.isNotEmpty() } ?: "Untitled"

    // Get summary from the second line of the note
    val summary: String
        get() {
            val lines = content.lines()
            return if (lines.size > 1) lines[1].take(50) else ""
        }
}

// Junction table for many-to-many relationship between notes and labels
@Entity(tableName = "note_label_cross_ref", primaryKeys = ["noteId", "labelId"])
data class NoteLabelCrossRef(
    val noteId: Long,
    val labelId: Long
)

// Class to represent a note with its labels
data class NoteWithLabels(
    @androidx.room.Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteLabelCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "labelId"
        )
    )
    val labels: List<Label>
)