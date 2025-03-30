package com.xr.notes.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.models.Label
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.ActiveLabelsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelsViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val activeLabelsStore: ActiveLabelsStore
) : ViewModel() {

    private val _labelItems = MediatorLiveData<List<LabelItem>>()
    val labelItems: LiveData<List<LabelItem>> = _labelItems

    private val _activeLabels = MutableLiveData<Set<Long>>(setOf())
    val activeLabels: LiveData<Set<Long>> = _activeLabels

    init {
        // Initialize with active labels from the store
        _activeLabels.value = activeLabelsStore.getActiveLabels()

        // Add direct source to observe all labels
        val allLabelsSource = repository.getAllLabels()

        _labelItems.addSource(allLabelsSource) { labels ->
            Log.d("LabelsViewModel", "Retrieved ${labels.size} labels from repository")
            updateLabelItems(labels)
        }

        // Subscribe to changes from active labels store
        _labelItems.addSource(activeLabelsStore.activeLabelsIds) { activeLabelsIds ->
            Log.d("LabelsViewModel", "Active labels changed: ${activeLabelsIds.size}")
            _activeLabels.value = activeLabelsIds
            // Only update if we have labels data
            repository.getAllLabels().value?.let { labels ->
                updateLabelItems(labels)
            }
        }

        // Initial load attempt - sometimes repository isn't ready in init
        viewModelScope.launch {
            try {
                val labels = repository.getAllLabels().value ?: emptyList()
                if (labels.isNotEmpty()) {
                    Log.d("LabelsViewModel", "Initial load found ${labels.size} labels")
                    updateLabelItems(labels)
                }
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error in initial labels load", e)
            }
        }
    }

    private fun updateLabelItems(labels: List<Label>) {
        val items = mutableListOf<LabelItem>()
        val activeLabelsSet = _activeLabels.value ?: setOf()

        // Add "ALL" label first
        val allActive = labels.isNotEmpty() && labels.all { activeLabelsSet.contains(it.id) }
        items.add(LabelItem("ALL", isSpecial = true, isActive = allActive))

        // Add regular labels
        items.addAll(labels.map { label ->
            LabelItem(
                name = label.name,
                label = label,
                isActive = activeLabelsSet.contains(label.id)
            )
        })

        _labelItems.value = items
        Log.d("LabelsViewModel", "Updated label items: ${items.size} items")
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val labelId = repository.insertLabel(newLabel)
                Log.d("LabelsViewModel", "Created new label with ID: $labelId")

                // Automatically add new label to active set
                activeLabelsStore.addActiveLabel(labelId)
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error creating label", e)
            }
        }
    }

    fun updateLabel(labelId: Long, name: String) {
        viewModelScope.launch {
            try {
                val label = repository.getLabelById(labelId).value ?: return@launch
                val updatedLabel = label.copy(name = name)
                repository.updateLabel(updatedLabel)
                Log.d("LabelsViewModel", "Updated label $labelId")
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error updating label", e)
            }
        }
    }

    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            try {
                repository.deleteLabel(label)
                Log.d("LabelsViewModel", "Deleted label ${label.id}")

                // Remove from active labels if present
                activeLabelsStore.removeActiveLabel(label.id)
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error deleting label", e)
            }
        }
    }

    fun toggleAllLabelsActive(isActive: Boolean) {
        val allLabels = repository.getAllLabels().value ?: return
        Log.d("LabelsViewModel", "Toggling all ${allLabels.size} labels active: $isActive")

        if (isActive) {
            // Activate all labels
            val allLabelIds = allLabels.map { it.id }.toSet()
            activeLabelsStore.setActiveLabels(allLabelIds)
        } else {
            // Deactivate all labels
            activeLabelsStore.clearActiveLabels()
        }
    }

    fun toggleLabelActive(labelId: Long, isActive: Boolean) {
        Log.d("LabelsViewModel", "Toggling label $labelId active: $isActive")
        activeLabelsStore.toggleActiveLabel(labelId, isActive)
    }

    fun getActiveLabelsIds(): Set<Long> {
        return activeLabelsStore.getActiveLabels()
    }
}