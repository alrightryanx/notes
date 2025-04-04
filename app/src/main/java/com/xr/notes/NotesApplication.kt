package com.xr.notes

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xr.notes.utils.AppPreferenceManager
import com.xr.notes.utils.BackupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NotesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prefManager: AppPreferenceManager

    // Implement as a property instead of a method
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Apply theme from preferences
        prefManager.applyTheme()

        // Initialize WorkManager explicitly to avoid initialization issues
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )

        // Setup auto-backup if enabled
        if (prefManager.isAutoBackupEnabled()) {
            scheduleBackupWork()
        }
    }

    private fun scheduleBackupWork() {
        val days = prefManager.getBackupIntervalInDays()
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            days.toLong(), TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            backupRequest
        )
    }

    companion object {
        private const val BACKUP_WORK_NAME = "notes_auto_backup"
    }
}