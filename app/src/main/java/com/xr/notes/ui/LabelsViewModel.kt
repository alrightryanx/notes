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

        // Subscribe to changes from repository - make sure this is working
        _labelItems.addSource(repository.getAllLabels()) { labels ->
            // Add logging to debug
            Log.d("LabelsViewModel", "Retrieved ${labels.size} labels from repository")
            updateLabelItems(labels)
        }

        // Subscribe to changes from active labels store
        _labelItems.addSource(activeLabelsStore.activeLabelsIds) { activeLabelsIds ->
            _activeLabels.value = activeLabelsIds
            updateLabelItems(repository.getAllLabels().value ?: emptyList())
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
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            val newLabel = Label(name = name)
            val labelId = repository.insertLabel(newLabel)

            // Automatically add new label to active set
            activeLabelsStore.addActiveLabel(labelId)
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

            // Remove from active labels if present
            activeLabelsStore.removeActiveLabel(label.id)
        }
    }

    fun toggleAllLabelsActive(isActive: Boolean) {
        val allLabels = repository.getAllLabels().value ?: return

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