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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Source for all labels
    private var allLabelsSource: LiveData<List<Label>>? = null

    init {
        Log.d("LabelsViewModel", "Initializing")

        // Initialize with active labels from the store
        _activeLabels.value = activeLabelsStore.getActiveLabels()

        setupObservers()

        // Initial manual refresh to ensure we have data
        viewModelScope.launch {
            forceRefreshLabels()
        }
    }

    private fun setupObservers() {
        Log.d("LabelsViewModel", "Setting up observers")

        // Remove previous source if it exists
        if (allLabelsSource != null) {
            _labelItems.removeSource(allLabelsSource!!)
        }

        // Get fresh labels source
        allLabelsSource = repository.getAllLabels()

        // Add new source
        _labelItems.addSource(allLabelsSource!!) { labels ->
            Log.d("LabelsViewModel", "Repository returned ${labels.size} labels")
            updateLabelItems(labels)
        }

        // Subscribe to changes from active labels store
        _labelItems.addSource(activeLabelsStore.activeLabelsIds) { activeLabelsIds ->
            Log.d("LabelsViewModel", "Active labels changed: ${activeLabelsIds.size}")
            _activeLabels.value = activeLabelsIds

            // Only update if we have labels data
            allLabelsSource?.value?.let { labels ->
                updateLabelItems(labels)
            }
        }
    }

    // Public function to force a refresh of labels
    fun forceRefreshLabels() {
        Log.d("LabelsViewModel", "Force refreshing labels")

        // Refresh observers
        setupObservers()

        // Manually query if we have a source
        allLabelsSource?.value?.let { labels ->
            updateLabelItems(labels)
        }

        // Workaround to force Room to re-query
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // This creates a temporary label to force a refresh
                    val dummyLabel = Label(id = -999L, name = "Dummy")
                    val insertedId = repository.insertLabel(dummyLabel)
                    repository.deleteLabel(dummyLabel.copy(id = insertedId))
                }
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error in force refresh", e)
            }
        }
    }

    private fun updateLabelItems(labels: List<Label>) {
        val items = mutableListOf<LabelItem>()
        val activeLabelsSet = _activeLabels.value ?: setOf()

        Log.d("LabelsViewModel", "Updating label items with ${labels.size} labels and ${activeLabelsSet.size} active labels")

        // Add "ALL" label first
        val allActive = labels.isNotEmpty() && labels.all { activeLabelsSet.contains(it.id) }
        items.add(LabelItem("ALL", isSpecial = true, isActive = allActive))

        // Add regular labels, sorted alphabetically by name
        items.addAll(labels.sortedBy { it.name }.map { label ->
            LabelItem(
                name = label.name,
                label = label,
                isActive = activeLabelsSet.contains(label.id)
            )
        })

        Log.d("LabelsViewModel", "Updated label items: ${items.size} items")
        _labelItems.value = items
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val labelId = repository.insertLabel(newLabel)
                Log.d("LabelsViewModel", "Created new label with ID: $labelId")

                // Automatically add new label to active set
                activeLabelsStore.addActiveLabel(labelId)

                // Force refresh to show the new label immediately
                forceRefreshLabels()
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

                // Force refresh to show the updated label immediately
                forceRefreshLabels()
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

                // Force refresh to update the list immediately
                forceRefreshLabels()
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error deleting label", e)
            }
        }
    }

    fun toggleAllLabelsActive(isActive: Boolean) {
        val allLabels = allLabelsSource?.value ?: return
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