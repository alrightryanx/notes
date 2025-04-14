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

    // Keep track of sources that have been added
    private val addedSources = mutableSetOf<String>()

    init {
        // Initialize with active labels from the store
        _activeLabels.value = activeLabelsStore.getActiveLabels()

        // Check database health on initialization
        viewModelScope.launch {
            checkAndRebuildDatabase()
        }

        setupObservers()
    }

    private fun setupObservers() {
        try {
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
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error setting up observers", e)

            // Set empty items to avoid UI crashes
            _labelItems.value = listOf(LabelItem("ALL", isSpecial = true, isActive = false))
        }
    }

    // Public function to force a refresh of labels
    fun forceRefreshLabels() {
        try {
            Log.d("LabelsViewModel", "Force refreshing labels")

            // Make sure observers are set up
            setupObservers()

            // Reset and reload active labels
            viewModelScope.launch {
                try {
                    // Repopulate all labels source
                    allLabelsSource = repository.getAllLabels()

                    // Update the UI immediately
                    withContext(Dispatchers.Main) {
                        allLabelsSource?.value?.let { labels ->
                            updateLabelItems(labels)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LabelsViewModel", "Error refreshing labels", e)
                }
            }
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error in forceRefreshLabels", e)
        }
    }

    private fun updateLabelItems(labels: List<Label>) {
        try {
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
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error updating label items", e)
            // Provide safe fallback to avoid UI crashes
            _labelItems.value = listOf(LabelItem("ALL", isSpecial = true, isActive = false))
        }
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                val newLabel = Label(name = name)
                val labelId = repository.insertLabel(newLabel)

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
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error updating label", e)
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
                Log.e("LabelsViewModel", "Error deleting label", e)
            }
        }
    }

    fun toggleAllLabelsActive(isActive: Boolean) {
        try {
            val allLabels = allLabelsSource?.value ?: return

            if (isActive) {
                // Activate all labels
                val allLabelIds = allLabels.map { it.id }.toSet()
                activeLabelsStore.setActiveLabels(allLabelIds)
            } else {
                // Deactivate all labels
                activeLabelsStore.clearActiveLabels()
            }
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error toggling all labels active", e)
        }
    }

    fun toggleLabelActive(labelId: Long, isActive: Boolean) {
        try {
            activeLabelsStore.toggleActiveLabel(labelId, isActive)
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error toggling label active state", e)
        }
    }

    fun getActiveLabelsIds(): Set<Long> {
        return try {
            activeLabelsStore.getActiveLabels()
        } catch (e: Exception) {
            Log.e("LabelsViewModel", "Error getting active label IDs", e)
            setOf()
        }
    }

    private fun checkAndRebuildDatabase() {
        viewModelScope.launch {
            try {
                repository.rebuildDatabaseIfEmpty()
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Error checking and rebuilding database", e)

                // Try database recovery
                try {
                    repository.attemptDatabaseRecovery()
                } catch (e2: Exception) {
                    Log.e("LabelsViewModel", "Database recovery failed", e2)
                }
            }
        }
    }

    fun debugDatabaseState() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Check database counts and log them
                    val noteCount = repository.getNoteCount()
                    Log.d("DatabaseDebug", "Database state:")
                    Log.d("DatabaseDebug", "Note count via direct query: $noteCount")

                    // Attempt to fix
                    if (noteCount == 0) {
                        Log.w("DatabaseDebug", "Empty database detected, attempting recovery...")
                        repository.rebuildDatabaseIfEmpty()
                    }
                }
            } catch (e: Exception) {
                Log.e("DatabaseDebug", "Error checking database state", e)

                // Try recovery on exception
                viewModelScope.launch {
                    repository.attemptDatabaseRecovery()
                }
            }
        }
    }

    // Add emergency reset method
    fun resetDatabase() {
        viewModelScope.launch {
            try {
                Log.w("LabelsViewModel", "Emergency database reset requested")
                repository.resetDatabase()

                // Reset active labels
                activeLabelsStore.reset()

                // Force refresh
                forceRefreshLabels()
            } catch (e: Exception) {
                Log.e("LabelsViewModel", "Emergency database reset failed", e)
            }
        }
    }
}