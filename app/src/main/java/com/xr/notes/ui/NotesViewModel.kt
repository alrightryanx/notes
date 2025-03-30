package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/notes/NotesViewModel.kt

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
        notesSource.value = notesSource.value?.filter { note ->
            note.content.contains(query, ignoreCase = true)
        }
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        updateSortOrder(sortOrder)
    }

    fun deleteNotes(noteIds: List<Long>) {
        viewModelScope.launch {
            noteIds.forEach { noteId ->
                val note = repository.getNoteById(noteId).value ?: return@forEach
                repository.deleteNote(note)
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
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
        }
    }
}