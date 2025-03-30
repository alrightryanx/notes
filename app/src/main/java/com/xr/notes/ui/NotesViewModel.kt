package com.xr.notes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.models.NoteWithLabels
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.ActiveLabelsStore
import com.xr.notes.utils.AppPreferenceManager
import com.xr.notes.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val prefManager: AppPreferenceManager,
    private val backupManager: BackupManager,
    private val activeLabelsStore: ActiveLabelsStore
) : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")
    private val _notesWithLabels = MediatorLiveData<List<NoteWithLabels>>()
    private val _filteredNotes = MediatorLiveData<List<NoteWithLabels>>()

    // Exposed LiveData for the UI
    val notesWithLabels: LiveData<List<NoteWithLabels>> = _filteredNotes

    // For selection mode
    private var inSelectionMode = false
    private val selectedNoteIds = mutableSetOf<Long>()

    init {
        // Load notes with their labels
        val notesWithLabelsSource = repository.getAllNotesWithLabels()
        _notesWithLabels.addSource(notesWithLabelsSource) { notesWithLabels ->
            // Debug log
            android.util.Log.d("NotesViewModel", "Received ${notesWithLabels.size} notes from repository")
            _notesWithLabels.value = applySortOrder(notesWithLabels, prefManager.getSortOrder())
            updateFilteredNotes()
        }

        // Listen for active labels changes
        _filteredNotes.addSource(activeLabelsStore.activeLabelsIds) { _ ->
            updateFilteredNotes()
        }

        // Listen for search query changes
        _filteredNotes.addSource(_searchQuery) { _ ->
            updateFilteredNotes()
        }
    }

    private fun updateFilteredNotes() {
        val allNotes = _notesWithLabels.value ?: emptyList()
        val activeLabelsIds = activeLabelsStore.getActiveLabels()
        val searchQuery = _searchQuery.value ?: ""

        // Debug
        android.util.Log.d("NotesViewModel", "Updating filtered notes. Total notes: ${allNotes.size}, Active labels: ${activeLabelsIds.size}")

        // Always show all notes initially, no filtering by labels until explicitly requested
        val notesToShow = if (searchQuery.isEmpty()) {
            allNotes
        } else {
            allNotes.filter { noteWithLabels ->
                noteWithLabels.note.content.contains(searchQuery, ignoreCase = true)
            }
        }

        _filteredNotes.value = notesToShow
    }

    private fun applySortOrder(notes: List<NoteWithLabels>, sortOrder: String): List<NoteWithLabels> {
        return when (sortOrder) {
            AppPreferenceManager.SORT_TITLE_ASC -> notes.sortedBy { it.note.title }
            AppPreferenceManager.SORT_TITLE_DESC -> notes.sortedByDescending { it.note.title }
            AppPreferenceManager.SORT_DATE_CREATED_DESC -> notes.sortedByDescending { it.note.createdAt }
            AppPreferenceManager.SORT_DATE_CREATED_ASC -> notes.sortedBy { it.note.createdAt }
            AppPreferenceManager.SORT_DATE_MODIFIED_DESC -> notes.sortedByDescending { it.note.modifiedAt }
            AppPreferenceManager.SORT_DATE_MODIFIED_ASC -> notes.sortedBy { it.note.modifiedAt }
            else -> notes.sortedByDescending { it.note.modifiedAt }
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        _notesWithLabels.value?.let { notes ->
            _notesWithLabels.value = applySortOrder(notes, sortOrder)
            updateFilteredNotes()
        }
    }

    fun deleteNotes(noteIds: List<Long>) {
        // Immediately update the UI
        _notesWithLabels.value?.let { currentList ->
            val updatedList = currentList.filter { it.note.id !in noteIds }
            _notesWithLabels.value = updatedList
            updateFilteredNotes()
        }

        // Then perform the actual database deletion
        viewModelScope.launch(Dispatchers.IO) {
            for (noteId in noteIds) {
                try {
                    val note = repository.getNoteById(noteId).value
                    if (note != null) {
                        repository.deleteNote(note)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotesViewModel", "Error deleting note $noteId", e)
                }
            }
        }
    }

    fun deleteNote(note: Note) {
        // Immediately update the UI
        _notesWithLabels.value?.let { currentList ->
            val updatedList = currentList.filter { it.note.id != note.id }
            _notesWithLabels.value = updatedList
            updateFilteredNotes()
        }

        // Then perform the actual database deletion
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteNote(note)
            } catch (e: Exception) {
                android.util.Log.e("NotesViewModel", "Error deleting note ${note.id}", e)
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                // Get all notes, labels, and their relationships
                val notes = repository.getAllNotes().value ?: emptyList()
                val labels = repository.getAllLabels().value ?: emptyList()

                // This would need to be expanded to get the actual cross references
                val crossRefs = mutableListOf<NoteLabelCrossRef>()

                // Create the backup
                backupManager.createBackup(notes, labels, crossRefs)
            } catch (e: Exception) {
                android.util.Log.e("NotesViewModel", "Error creating backup", e)
            }
        }
    }

    fun isLabelActive(labelId: Long): Boolean {
        return activeLabelsStore.isLabelActive(labelId)
    }

    fun getActiveLabelsIds(): Set<Long> {
        return activeLabelsStore.getActiveLabels()
    }
}