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

    private val _searchQuery = MutableLiveData("")
    private val notesSource = MediatorLiveData<List<Note>>()
    private val _currentNotes = MutableLiveData<List<Note>>(listOf())

    val notes: LiveData<List<Note>> = notesSource

    init {
        updateSortOrder(prefManager.getSortOrder())
    }

    private fun updateSortOrder(sortOrder: String) {
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
            applySearchFilter(notesList)
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query
        _currentNotes.value?.let { applySearchFilter(it) }
    }

    private fun applySearchFilter(notes: List<Note>) {
        val query = _searchQuery.value ?: ""
        notesSource.value = if (query.isBlank()) {
            notes
        } else {
            notes.filter { it.content.contains(query, ignoreCase = true) }
        }
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        updateSortOrder(sortOrder)
    }

    fun deleteNotes(noteIds: List<Long>) {
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filterNot { note -> note.id in noteIds }
            _currentNotes.value = updatedList
            notesSource.value = updatedList
        }

        viewModelScope.launch(Dispatchers.IO) {
            for (noteId in noteIds) {
                repository.getNoteById(noteId).value?.let {
                    repository.deleteNote(it)
                }
            }
        }
    }

    fun deleteNote(note: Note) {
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filter { it.id != note.id }
            _currentNotes.value = updatedList
            notesSource.value = updatedList
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                val notes = withContext(Dispatchers.IO) {
                    repository.getAllNotes().value ?: emptyList()
                }

                val labels = withContext(Dispatchers.IO) {
                    repository.getAllLabels().value ?: emptyList()
                }

                val crossRefs = mutableListOf<NoteLabelCrossRef>()

                backupManager.createBackup(notes, labels, crossRefs)
            } catch (_: Exception) {
            }
        }
    }
}
