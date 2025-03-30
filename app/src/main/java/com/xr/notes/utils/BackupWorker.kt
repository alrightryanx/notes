package com.xr.notes.utils

// File: app/src/main/java/com/example/notesapp/utils/BackupWorker.kt

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            // Get all notes, labels, and their relationships
            val notes = repository.getAllNotes().value ?: emptyList()
            val labels = repository.getAllLabels().value ?: emptyList()

            // This would need to be expanded to get the actual cross references
            val crossRefs = emptyList() // Placeholder

            // Create automatic backup
            backupManager.createBackup(notes, labels, crossRefs)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}