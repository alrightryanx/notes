package com.xr.notes.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Label
import com.xr.notes.repo.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LabelsViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    // Get labels from repository
    val labels: LiveData<List<Label>> = repository.getAllLabels()

    // Creates a test label - use this for debugging only
    fun createTestLabels() {
        viewModelScope.launch {
            val testLabels = listOf(
                Label(name = "Work", color = -0xb350b0),
                Label(name = "Personal", color = -0xde690d),
                Label(name = "Important", color = -0xbbcca)
            )

            for (label in testLabels) {
                try {
                    val id = repository.insertLabel(label)
                    Log.d("LabelsViewModel", "Created test label: ${label.name} with ID: $id")
                } catch (e: Exception) {
                    Log.e("LabelsViewModel", "Error creating test label", e)
                }
            }
        }
    }

    // Function to refresh labels - call this when needed
    suspend fun refreshLabels() {
        withContext(Dispatchers.IO) {
            val allLabels = repository.getAllLabels().value
            Log.d("LabelsViewModel", "refreshLabels found ${allLabels?.size ?: 0} labels")
        }
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val id = repository.insertLabel(newLabel)
                Log.d("LabelsViewModel", "Created label: $name with ID: $id")
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error creating label", e)
            }
        }
    }

    fun updateLabel(labelId: Long, name: String) {
        viewModelScope.launch {
            try {
                val label = repository.getLabelById(labelId).value
                if (label != null) {
                    val updatedLabel = label.copy(name = name)
                    repository.updateLabel(updatedLabel)
                    Log.d("LabelsViewModel", "Updated label ID $labelId to: $name")
                } else {
                    Log.e("LabelsViewModel", "Label not found with ID: $labelId")
                }
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error updating label", e)
            }
        }
    }

    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            try {
                repository.deleteLabel(label)
                Log.d("LabelsViewModel", "Deleted label: ${label.name}")
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error deleting label", e)
            }
        }
    }
}