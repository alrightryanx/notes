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

            // Get the values
            val notes = notesLiveData.value ?: emptyList()
            val labels = labelsLiveData.value ?: emptyList()

            // Get note-label relationships
            val crossRefs = mutableListOf<NoteLabelCrossRef>()
            val notesWithLabels = repository.getAllNotesWithLabels().value ?: emptyList()

            for (noteWithLabels in notesWithLabels) {
                val noteId = noteWithLabels.note.id
                for (label in noteWithLabels.labels) {
                    crossRefs.add(NoteLabelCrossRef(noteId, label.id))
                }
            }

            // Create automatic backup
            backupManager.createBackup(notes, labels, crossRefs)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}