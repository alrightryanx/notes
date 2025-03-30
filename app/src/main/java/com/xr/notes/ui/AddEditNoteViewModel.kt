package com.xr.notes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Label
import com.xr.notes.models.Note
import com.xr.notes.models.NoteWithLabels
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
                }

                _saveComplete.value = true
            } finally {
                isSaving = false
            }
        }
    }

    fun getAllLabels() {
        viewModelScope.launch {
            // Get all labels
            val labels = repository.getAllLabels().value ?: emptyList()

            if (currentNoteId != -1L) {
                // For existing note, get associated labels
                val noteWithLabels = repository.getNoteWithLabels(currentNoteId).value
                val noteLabels = noteWithLabels?.labels ?: emptyList()

                // Create pairs of (label, isSelected)
                _labelsWithSelection.value = labels.map { label ->
                    label to noteLabels.any { it.id == label.id }
                }
            } else {
                // For a new note, all labels are unselected
                _labelsWithSelection.value = labels.map { label ->
                    label to false
                }
            }
        }
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            val newLabel = Label(name = name)
            val labelId = repository.insertLabel(newLabel)

            // Refresh labels list
            getAllLabels()
        }
    }

    fun addLabelToNote(labelId: Long) {
        if (currentNoteId == -1L) return

        viewModelScope.launch {
            repository.addLabelToNote(currentNoteId, labelId)
        }
    }

    fun removeLabelFromNote(labelId: Long) {
        if (currentNoteId == -1L) return

        viewModelScope.launch {
            repository.removeLabelFromNote(currentNoteId, labelId)
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