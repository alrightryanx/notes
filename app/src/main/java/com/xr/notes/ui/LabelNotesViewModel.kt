package com.xr.notes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Note
import com.xr.notes.models.NoteWithLabels
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LabelNotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val prefManager: AppPreferenceManager
) : ViewModel() {

    private var labelId: Long = -1L
    private val _notesWithLabel = MutableLiveData<List<NoteWithLabels>>()
    val notesWithLabel: LiveData<List<NoteWithLabels>> = _notesWithLabel

    private val _searchQuery = MutableLiveData<String>("")
    private val _currentNotes = MutableLiveData<List<Note>>(listOf())

    fun setLabelId(id: Long) {
        labelId = id
        loadNotesForLabel()
    }

    private fun loadNotesForLabel() {
        viewModelScope.launch {
            repository.getLabelWithNotes(labelId).observeForever { labelWithNotes ->
                val notes = labelWithNotes?.notes ?: emptyList()
                _currentNotes.value = notes
                updateNotesList(notes)
            }
        }
    }

    private fun updateNotesList(notes: List<Note>) {
        val query = _searchQuery.value ?: ""

        // Apply sorting first
        val sortedNotes = applySortOrder(notes, prefManager.getSortOrder())

        if (query.isEmpty()) {
            _notesWithLabel.value = sortedNotes.map { note ->
                NoteWithLabels(note, emptyList())
            }
        } else {
            val filteredNotes = sortedNotes.filter { note ->
                note.content.contains(query, ignoreCase = true)
            }
            _notesWithLabel.value = filteredNotes.map { note ->
                NoteWithLabels(note, emptyList())
            }
        }
    }

    private fun applySortOrder(notes: List<Note>, sortOrder: String): List<Note> {
        return when (sortOrder) {
            AppPreferenceManager.SORT_TITLE_ASC -> notes.sortedBy { it.title }
            AppPreferenceManager.SORT_TITLE_DESC -> notes.sortedByDescending { it.title }
            AppPreferenceManager.SORT_DATE_CREATED_DESC -> notes.sortedByDescending { it.createdAt }
            AppPreferenceManager.SORT_DATE_CREATED_ASC -> notes.sortedBy { it.createdAt }
            AppPreferenceManager.SORT_DATE_MODIFIED_DESC -> notes.sortedByDescending { it.modifiedAt }
            AppPreferenceManager.SORT_DATE_MODIFIED_ASC -> notes.sortedBy { it.modifiedAt }
            else -> notes.sortedByDescending { it.modifiedAt }
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query

        // Trigger filtering of current list
        _currentNotes.value?.let { notes ->
            updateNotesList(notes)
        }
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        // Re-sort the current list based on sort order
        _currentNotes.value?.let { notes ->
            updateNotesList(notes)
        }
    }

    fun deleteNote(note: Note) {
        // Immediately update the UI
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filter { it.id != note.id }
            _currentNotes.value = updatedList
            updateNotesList(updatedList)
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

    fun deleteNotes(noteIds: List<Long>) {
        // Immediately update the UI
        _currentNotes.value?.let { currentList ->
            val updatedList = currentList.filter { note -> note.id !in noteIds }
            _currentNotes.value = updatedList
            updateNotesList(updatedList)
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
}