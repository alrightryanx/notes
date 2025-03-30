package com.xr.notes.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.xr.notes.models.Note
import com.xr.notes.models.NoteWithLabels
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesFilterManager @Inject constructor() {

    // Set of active label IDs
    private var activeLabelsIds: Set<Long> = setOf()

    // Flag to indicate if we're filtering by active labels
    private var isFilteringByActive = false

    // Combined source for notes filtering
    private val filteredNotes = MediatorLiveData<List<Note>>()

    /**
     * Configure the filter with notes source and return filtered result
     */
    fun setupFilter(
        notesWithLabelsSource: LiveData<List<NoteWithLabels>>,
        searchQuery: String = ""
    ): LiveData<List<Note>> {
        // Reset mediator sources
        filteredNotes.value = emptyList()

        // Add the source and apply filtering
        filteredNotes.addSource(notesWithLabelsSource) { notesWithLabels ->
            if (isFilteringByActive && activeLabelsIds.isNotEmpty()) {
                // Filter notes by active labels
                val filtered = notesWithLabels.filter { noteWithLabels ->
                    // A note is included if it has at least one of the active labels
                    noteWithLabels.labels.any { label ->
                        activeLabelsIds.contains(label.id)
                    }
                }.map { it.note }

                // Apply search filter if needed
                val searchFiltered = if (searchQuery.isNotEmpty()) {
                    filtered.filter { note ->
                        note.content.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    filtered
                }

                filteredNotes.value = searchFiltered
            } else {
                // No filtering by active labels, just apply search filter if needed
                val allNotes = notesWithLabels.map { it.note }
                val searchFiltered = if (searchQuery.isNotEmpty()) {
                    allNotes.filter { note ->
                        note.content.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    allNotes
                }

                filteredNotes.value = searchFiltered
            }
        }

        return filteredNotes
    }

    /**
     * Set the active labels
     */
    fun setActiveLabels(labelIds: Set<Long>) {
        activeLabelsIds = labelIds
    }

    /**
     * Set whether we're filtering by active labels
     */
    fun setFilteringByActive(filtering: Boolean) {
        isFilteringByActive = filtering
    }

    /**
     * Check if we're currently filtering by active labels
     */
    fun isFilteringByActive(): Boolean = isFilteringByActive

    /**
     * Get the current active label IDs
     */
    fun getActiveLabels(): Set<Long> = activeLabelsIds
}