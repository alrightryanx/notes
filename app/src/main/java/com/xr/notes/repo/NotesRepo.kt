package com.xr.notes.repo

import androidx.lifecycle.LiveData
import com.xr.notes.database.LabelDao
import com.xr.notes.database.NoteDao
import com.xr.notes.models.*
import com.xr.notes.utils.Encryption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val encryption: Encryption
) {
    // Notes
    fun getAllNotes(): LiveData<List<Note>> = noteDao.getAllNotes()

    fun getAllNotesSortedByDateModified(): LiveData<List<Note>> =
        noteDao.getAllNotesSortedByDateModified()

    fun getAllNotesSortedByDateCreated(): LiveData<List<Note>> =
        noteDao.getAllNotesSortedByDateCreated()

    fun getAllNotesSortedByTitle(): LiveData<List<Note>> =
        noteDao.getAllNotesSortedByTitle()

    fun getNoteById(noteId: Long): LiveData<Note> = noteDao.getNoteById(noteId)

    fun getNoteWithLabels(noteId: Long): LiveData<NoteWithLabels> =
        noteDao.getNoteWithLabels(noteId)

    suspend fun getAllNotesDirect(): List<Note> = noteDao.getAllNotesDirect()

    suspend fun getAllNotesWithLabelsDirect(): List<NoteWithLabels> = noteDao.getAllNotesWithLabelsDirect()

    suspend fun insertNote(note: Note): Long = noteDao.insert(note)

    suspend fun updateNote(note: Note) = noteDao.update(note)

    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    suspend fun deleteAllNotes() = noteDao.deleteAllNotes()

    // Labels
    fun getAllLabels(): LiveData<List<Label>> = labelDao.getAllLabels()

    fun getLabelById(labelId: Long): LiveData<Label> = labelDao.getLabelById(labelId)

    fun getLabelWithNotes(labelId: Long): LiveData<LabelWithNotes> = labelDao.getLabelWithNotes(labelId)

    suspend fun insertLabel(label: Label): Long = labelDao.insert(label)

    suspend fun updateLabel(label: Label) = labelDao.update(label)

    suspend fun deleteLabel(label: Label) = labelDao.delete(label)

    suspend fun deleteAllLabels() = labelDao.deleteAllLabels()

    // CrossRefs
    suspend fun addLabelToNote(noteId: Long, labelId: Long) {
        noteDao.addLabelToNote(NoteLabelCrossRef(noteId, labelId))
    }

    suspend fun removeLabelFromNote(noteId: Long, labelId: Long) {
        noteDao.removeLabelFromNote(noteId, labelId)
    }

    // Encryption support
    suspend fun encryptNote(note: Note, password: String) {
        val encrypted = encryption.encrypt(note.content, password)
        val updated = note.copy(content = encrypted, isEncrypted = true)
        noteDao.update(updated)
    }

    suspend fun decryptNote(note: Note, password: String): Note {
        val decrypted = encryption.decrypt(note.content, password)
        return note.copy(content = decrypted, isEncrypted = false)
    }

    // Optional for backup
    suspend fun getAllNotesWithLabels(): List<NoteWithLabels> {
        return noteDao.getAllNotesWithLabels() as List<NoteWithLabels>
    }

    suspend fun getAllLabelsDirect(): List<Label> = labelDao.getAllLabelsDirect()

}
