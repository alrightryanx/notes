package com.xr.notes.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.util.Log
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
        try {
            if (labelIds != _activeLabelsIds.value) {
                Log.d("ActiveLabelsStore", "Setting active labels: $labelIds")
                _activeLabelsIds.value = labelIds
            }
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error setting active labels", e)
            // In case of error, set to empty to avoid crashes
            _activeLabelsIds.value = setOf()
        }
    }

    /**
     * Add a label to the active set
     */
    fun addActiveLabel(labelId: Long) {
        try {
            val currentSet = _activeLabelsIds.value?.toMutableSet() ?: mutableSetOf()
            if (currentSet.add(labelId)) {
                Log.d("ActiveLabelsStore", "Added label $labelId to active set")
                _activeLabelsIds.value = currentSet
            }
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error adding active label", e)
        }
    }

    /**
     * Remove a label from the active set
     */
    fun removeActiveLabel(labelId: Long) {
        try {
            val currentSet = _activeLabelsIds.value?.toMutableSet() ?: return
            if (currentSet.remove(labelId)) {
                Log.d("ActiveLabelsStore", "Removed label $labelId from active set")
                _activeLabelsIds.value = currentSet
            }
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error removing active label", e)
        }
    }

    /**
     * Toggle a label in the active set
     */
    fun toggleActiveLabel(labelId: Long, isActive: Boolean) {
        try {
            if (isActive) {
                addActiveLabel(labelId)
            } else {
                removeActiveLabel(labelId)
            }
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error toggling active label", e)
        }
    }

    /**
     * Check if a label is active
     */
    fun isLabelActive(labelId: Long): Boolean {
        return try {
            _activeLabelsIds.value?.contains(labelId) ?: false
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error checking if label is active", e)
            false
        }
    }

    /**
     * Get the current active labels
     */
    fun getActiveLabels(): Set<Long> {
        return try {
            _activeLabelsIds.value ?: setOf()
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error getting active labels", e)
            setOf()
        }
    }

    /**
     * Clear all active labels
     */
    fun clearActiveLabels() {
        try {
            if (_activeLabelsIds.value?.isNotEmpty() == true) {
                Log.d("ActiveLabelsStore", "Clearing all active labels")
                _activeLabelsIds.value = setOf()
            }
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error clearing active labels", e)
            // In case of error, force set to empty
            _activeLabelsIds.postValue(setOf())
        }
    }

    /**
     * Reset to initial state (useful for recovery)
     */
    fun reset() {
        try {
            Log.d("ActiveLabelsStore", "Resetting active labels store")
            _activeLabelsIds.value = setOf()
        } catch (e: Exception) {
            Log.e("ActiveLabelsStore", "Error resetting active labels store", e)
            _activeLabelsIds.postValue(setOf())
        }
    }
}