package com.xr.notes.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Note
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelNotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val prefManager: AppPreferenceManager
) : ViewModel() {

    private var labelId: Long = -1
    private val _notesWithLabel = MutableLiveData<List<Note>>()
    val notesWithLabel: LiveData<List<Note>> = _notesWithLabel

    private val _searchQuery = MutableLiveData<String>("")

    fun setLabelId(id: Long) {
        labelId = id
        loadNotesForLabel()
    }

    private fun loadNotesForLabel() {
        viewModelScope.launch {
            repository.getLabelWithNotes(labelId).observeForever { labelWithNotes ->
                val notes = labelWithNotes?.notes ?: emptyList()
                updateNotesList(notes)
            }
        }
    }

    private fun updateNotesList(notes: List<Note>) {
        val query = _searchQuery.value ?: ""
        if (query.isEmpty()) {
            _notesWithLabel.value = notes
        } else {
            _notesWithLabel.value = notes.filter { note ->
                note.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query

        // Trigger filtering of current list
        _notesWithLabel.value?.let { notes ->
            _notesWithLabel.value = notes.filter { note ->
                note.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun setSortOrder(sortOrder: String) {
        prefManager.setSortOrder(sortOrder)
        // Re-sort the current list based on sort order
        _notesWithLabel.value?.let { notes ->
            _notesWithLabel.value = when (sortOrder) {
                AppPreferenceManager.SORT_TITLE_ASC -> notes.sortedBy { it.title }
                AppPreferenceManager.SORT_DATE_CREATED_DESC -> notes.sortedByDescending { it.createdAt }
                AppPreferenceManager.SORT_DATE_MODIFIED_DESC -> notes.sortedByDescending { it.modifiedAt }
                else -> notes.sortedByDescending { it.modifiedAt }
            }
        }
    }
}