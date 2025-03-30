package com.xr.notes.database

// File: app/src/main/java/com/example/notesapp/data/database/NoteDao.kt

import androidx.lifecycle.LiveData
import androidx.room.*
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.models.NoteWithLabels

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY modifiedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes ORDER BY title ASC")
    fun getAllNotesSortedByTitle(): LiveData<List<Note>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesSortedByDateCreated(): LiveData<List<Note>>

    @Query("SELECT * FROM notes ORDER BY modifiedAt DESC")
    fun getAllNotesSortedByDateModified(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Long): LiveData<Note>

    @Query("SELECT * FROM notes WHERE content LIKE '%' || :searchQuery || '%'")
    fun searchNotes(searchQuery: String): LiveData<List<Note>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteWithLabels(noteId: Long): LiveData<NoteWithLabels>

    @Transaction
    @Query("SELECT * FROM notes")
    fun getAllNotesWithLabels(): LiveData<List<NoteWithLabels>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteLabelCrossRef(crossRef: NoteLabelCrossRef)

    @Delete
    suspend fun deleteNoteLabelCrossRef(crossRef: NoteLabelCrossRef)

    @Query("DELETE FROM note_label_cross_ref WHERE noteId = :noteId")
    suspend fun deleteAllLabelsForNote(noteId: Long)
}
