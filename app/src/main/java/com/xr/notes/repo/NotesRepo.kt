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
import java.util.Date

class NotesRepository(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
    private val encryption: Encryption
) {
    // Note operations
    suspend fun getNoteCount(): Int = withContext(Dispatchers.IO) {
        return@withContext noteDao.getNoteCount()
    }

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
            // Make sure content is properly handled
            val content = if (note.isEncrypted) {
                encryption.encrypt(note.content)
            } else {
                note.content
            }

            val noteToInsert = note.copy(content = content)
            val id = noteDao.insert(noteToInsert)

            // Add explicit logging
            Log.d("NotesRepository", "Database INSERT returned ID: $id for note content: ${content.take(20)}...")

            // Verify the note was actually saved by querying it back
            val savedNote = noteDao.getNoteById(id).value
            if (savedNote != null) {
                Log.d("NotesRepository", "Successfully verified note with ID $id exists in database")
            } else {
                Log.w("NotesRepository", "WARNING: Note with ID $id couldn't be verified in database!")
            }

            return@withContext id
        } catch (e: Exception) {
            Log.e("NotesRepository", "ERROR in insertNote: ${e.message}", e)
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

    // NEW METHODS FOR DATABASE RECOVERY

    /**
     * Checks if the database is empty and creates a sample note if needed
     */
    suspend fun rebuildDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        try {
            // Check if the database is empty
            val noteCount = noteDao.getNoteCount()
            Log.d("NotesRepository", "Database check - current note count: $noteCount")

            if (noteCount == 0) {
                Log.w("NotesRepository", "Database appears to be empty! Creating a recovery note...")

                // Create a sample note to ensure database is working
                val sampleNote = Note(
                    content = "Recovery Note\nThis note was created automatically because your database appeared to be empty. Previous data may have been lost due to a database error.",
                    createdAt = Date(),
                    modifiedAt = Date()
                )
                val newId = insertNote(sampleNote)
                Log.d("NotesRepository", "Created recovery note with ID: $newId")
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error checking and rebuilding database", e)

            // Try to create a new note even if there was an error checking
            try {
                Log.w("NotesRepository", "Attempting to create emergency recovery note after error...")
                val emergencyNote = Note(
                    content = "Emergency Recovery Note\nThis note was created after a database error occurred. The app will attempt to continue functioning normally.",
                    createdAt = Date(),
                    modifiedAt = Date()
                )
                val newId = insertNote(emergencyNote)
                Log.d("NotesRepository", "Created emergency recovery note with ID: $newId")
            } catch (e2: Exception) {
                Log.e("NotesRepository", "Critical error: Failed to create emergency recovery note", e2)
            }
        }
    }

    /**
     * Attempts to recover database integrity
     */
    suspend fun attemptDatabaseRecovery() = withContext(Dispatchers.IO) {
        try {
            Log.d("NotesRepository", "Attempting database recovery")

            // Check note count
            val noteCount = try {
                noteDao.getNoteCount()
            } catch (e: Exception) {
                Log.e("NotesRepository", "Error getting note count during recovery", e)
                0
            }

            // Create recovery note
            val recoveryNote = Note(
                content = "Database Recovery Attempt\nTime: ${Date()}\nStatus: Recovery process initiated\nNote count: $noteCount",
                createdAt = Date(),
                modifiedAt = Date()
            )

            try {
                val newId = insertNote(recoveryNote)
                Log.d("NotesRepository", "Recovery note created with ID: $newId")
            } catch (e: Exception) {
                Log.e("NotesRepository", "Failed to create recovery note", e)
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Critical error during database recovery", e)
        }
    }

    /**
     * Completely resets the database while preserving schema
     */
    suspend fun resetDatabase() = withContext(Dispatchers.IO) {
        try {
            Log.w("NotesRepository", "Initiating database reset")

            // Create a recovery note first (in case deletion fails)
            val recoveryNote = Note(
                content = "Database Reset\nThis note was created during a database reset operation.\nTime: ${Date()}\n\nIf you're seeing this note, the reset was successful.",
                createdAt = Date(),
                modifiedAt = Date()
            )

            try {
                // Delete everything
                deleteAllNotes()
                deleteAllLabels()
                Log.d("NotesRepository", "Database contents deleted during reset")

                // Add the recovery note
                val newId = insertNote(recoveryNote)
                Log.d("NotesRepository", "Reset recovery note created with ID: $newId")
            } catch (e: Exception) {
                Log.e("NotesRepository", "Error during database reset operations", e)

                // Try one more time to add a recovery note
                try {
                    val emergencyNote = Note(
                        content = "Emergency Note\nA database reset was attempted but encountered errors.\nTime: ${Date()}",
                        createdAt = Date(),
                        modifiedAt = Date()
                    )
                    insertNote(emergencyNote)
                } catch (e2: Exception) {
                    Log.e("NotesRepository", "Failed to create emergency note after reset error", e2)
                }
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Critical error during database reset", e)
            throw e
        }
    }
}