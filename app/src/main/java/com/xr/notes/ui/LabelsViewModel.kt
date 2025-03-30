package com.xr.notes.ui

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

    // Source for all labels
    private var allLabelsSource: LiveData<List<Label>>? = null

    // Keep track of sources that have been added
    private val addedSources = mutableSetOf<String>()

    init {
        // Initialize with active labels from the store
        _activeLabels.value = activeLabelsStore.getActiveLabels()
        setupObservers()
    }

    private fun setupObservers() {
        // 1. Setup repository labels source if not already set up
        if ("allLabels" !in addedSources) {
            // Get labels source
            allLabelsSource = repository.getAllLabels()

            // Add new source
            _labelItems.addSource(allLabelsSource!!) { labels ->
                updateLabelItems(labels)
            }

            addedSources.add("allLabels")
        }

        // 2. Setup active labels observer if not already set up
        if ("activeLabels" !in addedSources) {
            // Subscribe to changes from active labels store
            _labelItems.addSource(activeLabelsStore.activeLabelsIds) { activeLabelsIds ->
                _activeLabels.value = activeLabelsIds

                // Only update if we have labels data
                allLabelsSource?.value?.let { labels ->
                    updateLabelItems(labels)
                }
            }

            addedSources.add("activeLabels")
        }
    }

    // Public function to force a refresh of labels
    fun forceRefreshLabels() {
        // Make sure observers are set up
        setupObservers()
    }

    private fun updateLabelItems(labels: List<Label>) {
        val items = mutableListOf<LabelItem>()
        val activeLabelsSet = _activeLabels.value ?: setOf()

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

        _labelItems.value = items
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val labelId = repository.insertLabel(newLabel)

                // Automatically add new label to active set
                activeLabelsStore.addActiveLabel(labelId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateLabel(labelId: Long, name: String) {
        viewModelScope.launch {
            try {
                val label = repository.getLabelById(labelId).value ?: return@launch
                val updatedLabel = label.copy(name = name)
                repository.updateLabel(updatedLabel)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            try {
                repository.deleteLabel(label)

                // Remove from active labels if present
                activeLabelsStore.removeActiveLabel(label.id)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleAllLabelsActive(isActive: Boolean) {
        val allLabels = allLabelsSource?.value ?: return

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
        activeLabelsStore.toggleActiveLabel(labelId, isActive)
    }

    fun getActiveLabelsIds(): Set<Long> {
        return activeLabelsStore.getActiveLabels()
    }
}