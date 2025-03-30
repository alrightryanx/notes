package com.xr.notes.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.xr.notes.database.LabelDao
import com.xr.notes.database.NoteDao
import com.xr.notes.models.Label
import com.xr.notes.models.LabelWithNotes
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.models.NoteWithLabels
import com.xr.notes.utils.Encryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun getAllNotesWithLabels(): LiveData<List<NoteWithLabels>> = noteDao.getAllNotesWithLabels()

    fun getNoteWithLabels(noteId: Long): LiveData<NoteWithLabels> = noteDao.getNoteWithLabels(noteId)

    suspend fun insertNote(note: Note): Long = withContext(Dispatchers.IO) {
        try {
            val content = if (note.isEncrypted) {
                encryption.encrypt(note.content)
            } else {
                note.content
            }

            val noteToInsert = note.copy(content = content)
            val id = noteDao.insert(noteToInsert)
            Log.d("NotesRepository", "Inserted note with ID: $id, content: ${note.content.take(20)}...")
            id
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error inserting note", e)
            throw e
        }
    }

    suspend fun insertLabel(label: Label): Long = withContext(Dispatchers.IO) {
        try {
            val id = labelDao.insert(label)
            Log.d("NotesRepository", "Inserted label with ID: $id, name: ${label.name}")
            id
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error inserting label", e)
            throw e
        }
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        try {
            val content = if (note.isEncrypted) {
                encryption.encrypt(note.content)
            } else {
                note.content
            }

            val noteToUpdate = note.copy(content = content)
            noteDao.update(noteToUpdate)
            Log.d("NotesRepository", "Updated note with ID: ${note.id}")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error updating note", e)
            throw e
        }
    }

    suspend fun encryptNote(note: Note, password: String) = withContext(Dispatchers.IO) {
        try {
            val encryptedContent = encryption.encrypt(note.content, password)
            val updatedNote = note.copy(content = encryptedContent, isEncrypted = true)
            noteDao.update(updatedNote)
            Log.d("NotesRepository", "Encrypted note with ID: ${note.id}")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error encrypting note", e)
            throw e
        }
    }

    suspend fun decryptNote(note: Note, password: String): Note = withContext(Dispatchers.IO) {
        if (!note.isEncrypted) return@withContext note

        try {
            val decryptedContent = encryption.decrypt(note.content, password)
            val decryptedNote = note.copy(content = decryptedContent, isEncrypted = false)
            noteDao.update(decryptedNote)
            Log.d("NotesRepository", "Decrypted note with ID: ${note.id}")
            decryptedNote
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error decrypting note", e)
            throw e
        }
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        try {
            noteDao.delete(note)
            Log.d("NotesRepository", "Deleted note with ID: ${note.id}")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting note", e)
            throw e
        }
    }

    suspend fun deleteAllNotes() = withContext(Dispatchers.IO) {
        try {
            noteDao.deleteAllNotes()
            Log.d("NotesRepository", "Deleted all notes")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting all notes", e)
            throw e
        }
    }

    fun getAllNotesSortedByTitleDesc(): LiveData<List<Note>> = noteDao.getAllNotesSortedByTitleDesc()

    fun getAllNotesSortedByDateCreatedAsc(): LiveData<List<Note>> = noteDao.getAllNotesSortedByDateCreatedAsc()

    fun getAllNotesSortedByDateModifiedAsc(): LiveData<List<Note>> = noteDao.getAllNotesSortedByDateModifiedAsc()

    // Label operations
    fun getAllLabels(): LiveData<List<Label>> = labelDao.getAllLabels()

    fun getLabelById(labelId: Long): LiveData<Label> = labelDao.getLabelById(labelId)

    fun getLabelWithNotes(labelId: Long): LiveData<LabelWithNotes> = labelDao.getLabelWithNotes(labelId)

    suspend fun updateLabel(label: Label) = withContext(Dispatchers.IO) {
        try {
            labelDao.update(label)
            Log.d("NotesRepository", "Updated label with ID: ${label.id}")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error updating label", e)
            throw e
        }
    }

    suspend fun deleteLabel(label: Label) = withContext(Dispatchers.IO) {
        try {
            labelDao.delete(label)
            Log.d("NotesRepository", "Deleted label with ID: ${label.id}")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting label", e)
            throw e
        }
    }

    suspend fun deleteAllLabels() = withContext(Dispatchers.IO) {
        try {
            labelDao.deleteAllLabels()
            Log.d("NotesRepository", "Deleted all labels")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error deleting all labels", e)
            throw e
        }
    }

    // Note-Label relationship operations
    suspend fun addLabelToNote(noteId: Long, labelId: Long) = withContext(Dispatchers.IO) {
        try {
            val crossRef = NoteLabelCrossRef(noteId, labelId)
            noteDao.insertNoteLabelCrossRef(crossRef)
            Log.d("NotesRepository", "Added label $labelId to note $noteId")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error adding label to note", e)
            throw e
        }
    }

    suspend fun removeLabelFromNote(noteId: Long, labelId: Long) = withContext(Dispatchers.IO) {
        try {
            val crossRef = NoteLabelCrossRef(noteId, labelId)
            noteDao.deleteNoteLabelCrossRef(crossRef)
            Log.d("NotesRepository", "Removed label $labelId from note $noteId")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error removing label from note", e)
            throw e
        }
    }

    suspend fun removeAllLabelsFromNote(noteId: Long) = withContext(Dispatchers.IO) {
        try {
            noteDao.deleteAllLabelsForNote(noteId)
            Log.d("NotesRepository", "Removed all labels from note $noteId")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error removing all labels from note", e)
            throw e
        }
    }
}