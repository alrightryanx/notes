package com.xr.notes.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Label
import com.xr.notes.models.Note
import com.xr.notes.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _saveComplete = MutableLiveData<Boolean>()
    val saveComplete: LiveData<Boolean> = _saveComplete

    private val _labelsWithSelection = MutableLiveData<List<Pair<Label, Boolean>>>()
    val labelsWithSelection: LiveData<List<Pair<Label, Boolean>>> = _labelsWithSelection

    private var currentNoteId: Long = -1L
    private var isSaving = false // Flag to prevent concurrent save operations

    // Expose the current note ID
    fun getCurrentNoteId(): Long = currentNoteId

    fun loadNote(noteId: Long) {
        if (noteId != -1L) {
            currentNoteId = noteId
            viewModelScope.launch {
                repository.getNoteById(noteId).observeForever { loadedNote ->
                    loadedNote?.let {
                        _note.value = it
                    }
                }
            }
        }
    }

    fun hasNoteBeenSaved(): Boolean = currentNoteId != -1L

    fun saveNote(content: String, isEncrypted: Boolean): Job {
        // Prevent multiple concurrent save operations
        if (isSaving) return viewModelScope.launch {}
        isSaving = true

        return viewModelScope.launch {
            try {
                val currentNote = _note.value

                if (currentNote != null) {
                    // Update existing note
                    val updatedNote = currentNote.copy(
                        content = content,
                        isEncrypted = isEncrypted,
                        modifiedAt = Date()
                    )
                    repository.updateNote(updatedNote)
                } else {
                    // Create new note
                    val newNote = Note(
                        content = content,
                        isEncrypted = isEncrypted,
                        createdAt = Date(),
                        modifiedAt = Date()
                    )
                    currentNoteId = repository.insertNote(newNote)
                    Log.d("AddEditNoteVM", "New note created with ID: $currentNoteId")
                }

                _saveComplete.value = true
            } catch (e: Exception) {
                Log.e("AddEditNoteVM", "Error saving note", e)
            } finally {
                isSaving = false
            }
        }
    }

    fun getAllLabels() {
        viewModelScope.launch {
            try {
                val allLabels = repository.getAllLabels().value ?: emptyList()
                Log.d("AddEditNoteVM", "getAllLabels found ${allLabels.size} labels")

                if (currentNoteId != -1L) {
                    val noteWithLabels = repository.getNoteWithLabels(currentNoteId).value
                    val associatedLabelIds = noteWithLabels?.labels?.map { it.id } ?: emptyList()

                    val result = allLabels.map { label ->
                        val isSelected = associatedLabelIds.contains(label.id)
                        label to isSelected
                    }

                    _labelsWithSelection.postValue(result)
                } else {
                    // For a new note, all labels are unselected
                    _labelsWithSelection.postValue(allLabels.map { it to false })
                }
            } catch (e: Exception) {
                Log.e("AddEditNoteVM", "Error loading labels", e)
                _labelsWithSelection.postValue(emptyList())
            }
        }
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val labelId = repository.insertLabel(newLabel)
                Log.d("AddEditNoteVM", "Created new label with ID: $labelId")
            } catch (e: Exception) {
                Log.e("AddEditNoteVM", "Error creating label", e)
            }
        }
    }

    fun addLabelToNote(labelId: Long) {
        if (currentNoteId == -1L) {
            Log.e("AddEditNoteVM", "Cannot add label to unsaved note")
            return
        }

        viewModelScope.launch {
            try {
                repository.addLabelToNote(currentNoteId, labelId)
                Log.d("AddEditNoteVM", "Added label $labelId to note $currentNoteId")
            } catch (e: Exception) {
                Log.e("AddEditNoteVM", "Error adding label to note", e)
            }
        }
    }

    fun removeLabelFromNote(labelId: Long) {
        if (currentNoteId == -1L) {
            Log.e("AddEditNoteVM", "Cannot remove label from unsaved note")
            return
        }

        viewModelScope.launch {
            try {
                repository.removeLabelFromNote(currentNoteId, labelId)
                Log.d("AddEditNoteVM", "Removed label $labelId from note $currentNoteId")
            } catch (e: Exception) {
                Log.e("AddEditNoteVM", "Error removing label from note", e)
            }
        }
    }

    fun encryptNote(password: String) {
        viewModelScope.launch {
            val currentNote = _note.value ?: return@launch
            repository.encryptNote(currentNote, password)

            // Refresh note
            _note.value = repository.getNoteById(currentNoteId).value
        }
    }

    fun decryptNote(password: String) {
        viewModelScope.launch {
            val currentNote = _note.value ?: return@launch
            val decryptedNote = repository.decryptNote(currentNote, password)

            // Update UI with decrypted content
            _note.value = decryptedNote
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            val currentNote = _note.value ?: return@launch
            repository.deleteNote(currentNote)
        }
    }
}