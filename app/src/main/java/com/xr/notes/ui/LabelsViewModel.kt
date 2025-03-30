package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/labels/LabelsViewModel.kt

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Label
import com.xr.notes.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelsViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    val labels: LiveData<List<Label>> = repository.getAllLabels()

    fun createLabel(name: String) {
        viewModelScope.launch {
            val newLabel = Label(name = name)
            repository.insertLabel(newLabel)
        }
    }

    fun updateLabel(labelId: Long, name: String) {
        viewModelScope.launch {
            val label = repository.getLabelById(labelId).value ?: return@launch
            val updatedLabel = label.copy(name = name)
            repository.updateLabel(updatedLabel)
        }
    }

    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            repository.deleteLabel(label)
        }
    }
}