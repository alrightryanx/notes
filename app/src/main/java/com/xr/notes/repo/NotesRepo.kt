package com.xr.notes.repo

// File: app/src/main/java/com/example/notesapp/data/repository/NotesRepository.kt

import androidx.lifecycle.LiveData
import com.xr.notes.database.LabelDao
import com.xr.notes.database.NoteDao
import com.xr.notes.models.Label
import com.xr.notes.models.LabelWithNotes
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.models.NoteWithLabels
import com.xr.notes.utils.Encryption

class NotesRepository(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val encryption: Encryption
) {
    // Note operations
    fun getAllNotes(): LiveData<List<Note>> = noteDao.getAllNotes()

    fun getAllNotesSortedByTitle(): LiveData<List<Note>> = noteDao.getAllNotesSortedByTitle()

    fun getAllNotesSortedByDateCreated(): LiveData<List<Note>> = noteDao.getAllNotesSortedByDateCreated()

    fun getAllNotesSortedByDateModified(): LiveData<List<Note>> = noteDao.getAllNotesSortedByDateModified()

    fun getNoteById(noteId: Long): LiveData<Note> = noteDao.getNoteById(noteId)

    fun searchNotes(query: String): LiveData<List<Note>> = noteDao.searchNotes(query)

    fun getNoteWithLabels(noteId: Long): LiveData<NoteWithLabels> = noteDao.getNoteWithLabels(noteId)

    fun getAllNotesWithLabels(): LiveData<List<NoteWithLabels>> = noteDao.getAllNotesWithLabels()

    suspend fun insertNote(note: Note): Long {
        return if (note.isEncrypted) {
            val encryptedContent = encryption.encrypt(note.content)
            noteDao.insert(note.copy(content = encryptedContent))
        } else {
            noteDao.insert(note)
        }
    }

    suspend fun updateNote(note: Note) {
        if (note.isEncrypted) {
            val encryptedContent = encryption.encrypt(note.content)
            noteDao.update(note.copy(content = encryptedContent))
        } else {
            noteDao.update(note)
        }
    }

    suspend fun encryptNote(note: Note, password: String) {
        val encryptedContent = encryption.encrypt(note.content, password)
        noteDao.update(note.copy(content = encryptedContent, isEncrypted = true))
    }

    suspend fun decryptNote(note: Note, password: String): Note {
        if (!note.isEncrypted) return note

        val decryptedContent = encryption.decrypt(note.content, password)
        val decryptedNote = note.copy(content = decryptedContent, isEncrypted = false)
        noteDao.update(decryptedNote)
        return decryptedNote
    }

    suspend fun deleteNote(note: Note) {
        noteDao.delete(note)
    }

    suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    // Label operations
    fun getAllLabels(): LiveData<List<Label>> = labelDao.getAllLabels()

    fun getLabelById(labelId: Long): LiveData<Label> = labelDao.getLabelById(labelId)

    fun getLabelWithNotes(labelId: Long): LiveData<LabelWithNotes> = labelDao.getLabelWithNotes(labelId)

    suspend fun insertLabel(label: Label): Long = labelDao.insert(label)

    suspend fun updateLabel(label: Label) = labelDao.update(label)

    suspend fun deleteLabel(label: Label) = labelDao.delete(label)

    suspend fun deleteAllLabels() = labelDao.deleteAllLabels()

    // Note-Label relationship operations
    suspend fun addLabelToNote(noteId: Long, labelId: Long) {
        noteDao.insertNoteLabelCrossRef(NoteLabelCrossRef(noteId, labelId))
    }

    suspend fun removeLabelFromNote(noteId: Long, labelId: Long) {
        noteDao.deleteNoteLabelCrossRef(NoteLabelCrossRef(noteId, labelId))
    }

    suspend fun removeAllLabelsFromNote(noteId: Long) {
        noteDao.deleteAllLabelsForNote(noteId)
    }
}