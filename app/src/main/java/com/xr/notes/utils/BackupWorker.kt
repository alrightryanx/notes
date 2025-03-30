package com.xr.notes.utils

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xr.notes.models.NoteLabelCrossRef
import com.xr.notes.repo.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: NotesRepository,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get all notes and labels
            val notesLiveData = repository.getAllNotes()
            val labelsLiveData = repository.getAllLabels()

            // Wait for the live data to emit values
            var notes = notesLiveData.value ?: emptyList()
            var labels = labelsLiveData.value ?: emptyList()

            // If the live data hasn't emitted values yet, try to get them manually
            if (notes.isEmpty()) {
                val notesWithLabels = repository.getAllNotesWithLabels().value ?: emptyList()
                notes = notesWithLabels.map { it.note }
            }

            if (labels.isEmpty()) {
                // This would need a method to get all labels with their notes
                // For now, we'll use an empty list if we can't get labels
            }

            // This would need to be expanded to get the actual cross references
            val crossRefs = mutableListOf<NoteLabelCrossRef>()

            // Create automatic backup
            backupManager.createBackup(notes, labels, crossRefs)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}