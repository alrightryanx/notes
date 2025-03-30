package com.xr.notes.models

// File: app/src/main/java/com/example/notesapp/data/models/Label.kt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String = "",
    var color: Int = 0
)

// Class to represent a label with its notes
data class LabelWithNotes(
    @androidx.room.Embedded val label: Label,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = NoteLabelCrossRef::class,
            parentColumn = "labelId",
            entityColumn = "noteId"
        )
    )
    val notes: List<Note>
)