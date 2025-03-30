package com.xr.notes.ui

import android.util.Log
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
import kotlinx.coroutines.withContext
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

    // Source for notes with labels
    private var notesWithLabelsSource: LiveData<List<NoteWithLabels>>? = null
    // To keep track of observers we've added
    private val activeObservers = mutableSetOf<Any>()

    // For selection mode
    private var inSelectionMode = false
    private val selectedNoteIds = mutableSetOf<Long>()

    init {
        Log.d("NotesViewModel", "Initializing")
        setupObservers()
    }

    private fun setupObservers() {
        Log.d("NotesViewModel", "Setting up observers")

        // First, check if we already have a source
        val existingSource = notesWithLabelsSource
        if (existingSource != null) {
            // We already have a source, don't remove it or add again
            Log.d("NotesViewModel", "Source already exists, skipping setup")
            return
        }

        // Get fresh source
        notesWithLabelsSource = repository.getAllNotesWithLabels()

        // Add new source
        _notesWithLabels.addSource(notesWithLabelsSource!!) { notesWithLabels ->
            Log.d("NotesViewModel", "Received ${notesWithLabels.size} notes from repository")
            _notesWithLabels.value = applySortOrder(notesWithLabels, prefManager.getSortOrder())
            updateFilteredNotes()
        }

        // Add active labels observer if we haven't already
        if (!activeObservers.contains("activeLabels")) {
            _filteredNotes.addSource(activeLabelsStore.activeLabelsIds) { activeLabelsIds ->
                Log.d("NotesViewModel", "Active labels changed: ${activeLabelsIds.size}")
                updateFilteredNotes()
            }
            activeObservers.add("activeLabels")
        }

        // Add search query observer if we haven't already
        if (!activeObservers.contains("searchQuery")) {
            _filteredNotes.addSource(_searchQuery) { query ->
                Log.d("NotesViewModel", "Search query changed: $query")
                updateFilteredNotes()
            }
            activeObservers.add("searchQuery")
        }
    }

    // Force a refresh of the notes list
    fun forceRefreshNotes() {
        Log.d("NotesViewModel", "Force refreshing notes")

        // Don't call setupObservers again - we just need to trigger updates

        // Manually trigger repository to update its LiveData
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // This is a workaround to force Room to re-query
                    val dummyNote = Note(id = -999L, content = "Dummy")
                    val inserted = repository.insertNote(dummyNote)
                    repository.deleteNote(dummyNote.copy(id = inserted))
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error in force refresh", e)
            }
        }

        // Also manually trigger an update if we already have notes
        _notesWithLabels.value?.let {
            updateFilteredNotes()
        }
    }

    private fun updateFilteredNotes() {
        val allNotes = _notesWithLabels.value ?: emptyList()
        val activeLabelsIds = activeLabelsStore.getActiveLabels()
        val searchQuery = _searchQuery.value ?: ""

        Log.d("NotesViewModel", "Updating filtered notes. Total notes: ${allNotes.size}, " +
                "Active labels: ${activeLabelsIds.size}, Search query: '$searchQuery'")

        // Just show all notes for now, no filtering by labels
        val filteredBySearch = if (searchQuery.isEmpty()) {
            allNotes
        } else {
            allNotes.filter { noteWithLabels ->
                noteWithLabels.note.content.contains(searchQuery, ignoreCase = true)
            }
        }

        Log.d("NotesViewModel", "After filtering, showing ${filteredBySearch.size} notes")
        _filteredNotes.value = filteredBySearch
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
                    Log.e("NotesViewModel", "Error deleting note $noteId", e)
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
                Log.e("NotesViewModel", "Error deleting note ${note.id}", e)
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
                Log.e("NotesViewModel", "Error creating backup", e)
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