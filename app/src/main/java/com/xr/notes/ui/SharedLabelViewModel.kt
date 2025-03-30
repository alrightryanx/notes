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
    internal fun initializeActiveLabels() {
        // Add logging to verify this is working
        Log.d("MainActivity", "Initializing active labels")
        viewModelScope.launch {
            val labels = repository.getAllLabels().value ?: emptyList()

            // Set all labels as active, even if the list is empty
            val allLabelIds = labels.map { it.id }.toSet()
            activeLabelsStore.setActiveLabels(allLabelIds)
            Log.d("MainActivity", "Found ${labels.size} labels to initialize as active")
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