package com.xr.notes.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton store to maintain active labels state across the application.
 * This allows consistent label selection states between different screens.
 */
@Singleton
class ActiveLabelsStore @Inject constructor() {

    private val _activeLabelsIds = MutableLiveData<Set<Long>>(setOf())
    val activeLabelsIds: LiveData<Set<Long>> = _activeLabelsIds

    /**
     * Set the entire active labels set
     */
    fun setActiveLabels(labelIds: Set<Long>) {
        _activeLabelsIds.value = labelIds
    }

    /**
     * Add a label to the active set
     */
    fun addActiveLabel(labelId: Long) {
        val currentSet = _activeLabelsIds.value?.toMutableSet() ?: mutableSetOf()
        currentSet.add(labelId)
        _activeLabelsIds.value = currentSet
    }

    /**
     * Remove a label from the active set
     */
    fun removeActiveLabel(labelId: Long) {
        val currentSet = _activeLabelsIds.value?.toMutableSet() ?: return
        currentSet.remove(labelId)
        _activeLabelsIds.value = currentSet
    }

    /**
     * Toggle a label in the active set
     */
    fun toggleActiveLabel(labelId: Long, isActive: Boolean) {
        if (isActive) {
            addActiveLabel(labelId)
        } else {
            removeActiveLabel(labelId)
        }
    }

    /**
     * Check if a label is active
     */
    fun isLabelActive(labelId: Long): Boolean {
        return _activeLabelsIds.value?.contains(labelId) ?: false
    }

    /**
     * Get the current active labels
     */
    fun getActiveLabels(): Set<Long> {
        return _activeLabelsIds.value ?: setOf()
    }

    /**
     * Clear all active labels
     */
    fun clearActiveLabels() {
        _activeLabelsIds.value = setOf()
    }
}