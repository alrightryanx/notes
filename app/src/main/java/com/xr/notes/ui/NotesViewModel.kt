package com.xr.notes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Note
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.repo.NotesRepository
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
    private val backupManager: BackupManager
) : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")
    private val notesSource = MediatorLiveData<List<Note>>()
    private val _currentNotes = MutableLiveData<List<Note>>(listOf())

    val notes: LiveData<List<Note>> = notesSource

    init {
        // Initial sort order from preferences
        updateSortOrder(prefManager.getSortOrder())
    }

    private fun updateSortOrder(sortOrder: String) {
        notesSource.removeSource(repository.getAllNotes())
        notesSource.removeSource(repository.getAllNotesSortedByTitle())
        notesSource.removeSource(repository.getAllNotesSortedByDateCreated())
        notesSource.removeSource(repository.getAllNotesSortedByDateModified())

        val source = when (sortOrder) {
            AppPreferenceManager.SORT_TITLE_ASC -> repository.getAllNotesSortedByTitle()
            AppPreferenceManager.SORT_DATE_CREATED_DESC -> repository.getAllNotesSortedByDateCreated()
            AppPreferenceManager.SORT_DATE_MODIFIED_DESC -> repository.getAllNotesSortedByDateModified()
            else -> repository.getAllNotesSortedByDateModified()
        }

        notesSource.addSource(source) { notesList ->
            _currentNotes.value = notesList
            val query = _searchQuery.value ?: ""
            if (query.isEmpty()) {
                notesSource.value = notesList
            } else {
                notesSource.value = notesList.filter { note ->
                    note.content.contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query

        // Trigger filtering of current list
        _currentNotes.value?.let { notes ->
            notesSource.value = notes.filter { note ->
                note.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        updateSortOrder(sortOrder)
    }

    fun deleteNotes(noteIds: List<Long>) {
        // Immediately update the UI
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filter { note -> note.id !in noteIds }
            notesSource.value = updatedList
            _currentNotes.value = updatedList
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
                    // Log error if needed
                }
            }
        }
    }

    fun deleteNote(note: Note) {
        // Immediately update the UI
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filter { it.id != note.id }
            notesSource.value = updatedList
            _currentNotes.value = updatedList
        }

        // Then perform the actual database deletion
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteNote(note)
            } catch (e: Exception) {
                // Log error if needed
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                // Get all notes, labels, and their relationships
                val notes = withContext(Dispatchers.IO) {
                    repository.getAllNotes().value ?: emptyList()
                }

                val labels = withContext(Dispatchers.IO) {
                    repository.getAllLabels().value ?: emptyList()
                }

                // This would need to be expanded to get the actual cross references
                val crossRefs = mutableListOf<NoteLabelCrossRef>()

                // Create the backup
                backupManager.createBackup(notes, labels, crossRefs)
            } catch (e: Exception) {
                // Log error if needed
            }
        }
    }
}