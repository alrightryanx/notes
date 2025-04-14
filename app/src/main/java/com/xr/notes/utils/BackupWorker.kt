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
            // Get all notes with their labels directly from suspend DAO method
            val notesWithLabels = repository.getAllNotesWithLabels()
            val labels = repository.getAllLabelsDirect()

            // Extract notes and cross-references
            val notes = notesWithLabels.map { it.note }
            val crossRefs = notesWithLabels.flatMap { noteWithLabels ->
                noteWithLabels.labels.map { label ->
                    NoteLabelCrossRef(noteWithLabels.note.id, label.id)
                }
            }

            // Get all labels (LiveData-less, you may want to create a suspend method)
            //val labels = repository.getAllLabels().value ?: emptyList()

            // Create the backup
            backupManager.createBackup(notes, labels, crossRefs)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
