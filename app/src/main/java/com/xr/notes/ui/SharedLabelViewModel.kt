package com.xr.notes.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.ActiveLabelsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A shared ViewModel that helps coordinate label activities across fragments
 */
@HiltViewModel
class SharedLabelViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val activeLabelsStore: ActiveLabelsStore
) : ViewModel() {

    private val _filteringByActiveEnabled = MutableLiveData<Boolean>(false)
    val filteringByActiveEnabled: LiveData<Boolean> = _filteringByActiveEnabled

    /**
     * Initialize active labels - at the start we want all labels to be active
     */
    fun initializeActiveLabels() {
        Log.d("SharedLabelViewModel", "Initializing active labels")
        viewModelScope.launch {
            try {
                // Wait for a moment to ensure repository is initialized
                kotlinx.coroutines.delay(100)

                // Get all labels
                val labelsLiveData = repository.getAllLabels()
                val labels = labelsLiveData.value

                if (labels != null && labels.isNotEmpty()) {
                    // Set all labels as active
                    val allLabelIds = labels.map { it.id }.toSet()
                    activeLabelsStore.setActiveLabels(allLabelIds)
                    Log.d("SharedLabelViewModel", "Found and initialized ${labels.size} labels as active")
                } else {
                    // If no labels yet, ensure we're not filtering
                    activeLabelsStore.clearActiveLabels()
                    Log.d("SharedLabelViewModel", "No labels found to initialize")
                }
            } catch (e: Exception) {
                Log.e("SharedLabelViewModel", "Error initializing labels", e)
                // Ensure we're not filtering if there's an error
                activeLabelsStore.clearActiveLabels()
            }
        }
    }

    /**
     * Toggle active label filtering
     */
    fun toggleActiveFiltering(enabled: Boolean) {
        _filteringByActiveEnabled.value = enabled
    }

    /**
     * Check if we're currently filtering by active labels
     */
    fun isFilteringByActive(): Boolean {
        return _filteringByActiveEnabled.value ?: false
    }

    /**
     * Get the currently active label IDs
     */
    fun getActiveLabelsIds(): Set<Long> {
        return activeLabelsStore.getActiveLabels()
    }
}