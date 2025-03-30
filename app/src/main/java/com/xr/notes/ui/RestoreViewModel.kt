package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/settings/RestoreViewModel.kt

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestoreViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backups = MutableLiveData<List<Pair<String, Uri>>>()
    val backups: LiveData<List<Pair<String, Uri>>> = _backups

    private val _restoreResult = MutableLiveData<RestoreResult>()
    val restoreResult: LiveData<RestoreResult> = _restoreResult

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch {
            val backupsList = backupManager.listBackups()
            _backups.value = backupsList
        }
    }

    fun restoreBackup(backupUri: Uri) {
        viewModelScope.launch {
            try {
                val backupData = backupManager.restoreBackup(backupUri)

                // Clear existing data
                repository.deleteAllNotes()
                repository.deleteAllLabels()

                // Insert restored labels first
                backupData.labels.forEach { label ->
                    repository.insertLabel(label)
                }

                // Insert restored notes
                backupData.notes.forEach { note ->
                    repository.insertNote(note)
                }

                // Restore note-label relationships
                backupData.noteLabelCrossRefs.forEach { crossRef ->
                    repository.addLabelToNote(crossRef.noteId, crossRef.labelId)
                }

                _restoreResult.value = RestoreResult.Success
            } catch (e: Exception) {
                _restoreResult.value = RestoreResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class RestoreResult {
        object Success : RestoreResult()
        data class Error(val error: String) : RestoreResult()
    }
}