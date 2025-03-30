package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/settings/SettingsFragment.kt

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xr.notes.R
import com.xr.notes.utils.AppPreferenceManager
import com.xr.notes.utils.BackupWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var prefManager: AppPreferenceManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Theme preference
        val themePref = findPreference<ListPreference>(AppPreferenceManager.KEY_THEME)
        themePref?.setOnPreferenceChangeListener { _, newValue ->
            val theme = newValue.toString()
            prefManager.setTheme(theme)
            prefManager.applyTheme()
            true
        }

        // Auto-backup preference
        val autoBackupPref = findPreference<SwitchPreferenceCompat>(AppPreferenceManager.KEY_AUTO_BACKUP)
        autoBackupPref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            prefManager.setAutoBackupEnabled(enabled)

            if (enabled) {
                scheduleBackupWork()
            } else {
                cancelBackupWork()
            }
            true
        }

        // Backup frequency preference
        val backupFrequencyPref = findPreference<ListPreference>(AppPreferenceManager.KEY_BACKUP_FREQUENCY)
        backupFrequencyPref?.setOnPreferenceChangeListener { _, newValue ->
            val frequency = newValue.toString()
            prefManager.setBackupFrequency(frequency)

            if (prefManager.isAutoBackupEnabled()) {
                // Reschedule with new frequency
                scheduleBackupWork()
            }
            true
        }

        // Update summaries
        updatePreferenceSummaries()
    }

    private fun updatePreferenceSummaries() {
        val themePref = findPreference<ListPreference>(AppPreferenceManager.KEY_THEME)
        themePref?.summary = themePref?.entry

        val textSizePref = findPreference<ListPreference>(AppPreferenceManager.KEY_TEXT_SIZE)
        textSizePref?.summary = textSizePref?.entry

        val backupFrequencyPref = findPreference<ListPreference>(AppPreferenceManager.KEY_BACKUP_FREQUENCY)
        backupFrequencyPref?.summary = backupFrequencyPref?.entry
    }

    private fun scheduleBackupWork() {
        val days = prefManager.getBackupIntervalInDays()
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            days.toLong(), TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            backupRequest
        )
    }

    private fun cancelBackupWork() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(BACKUP_WORK_NAME)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Update preference summaries when they change
        when (key) {
            AppPreferenceManager.KEY_THEME,
            AppPreferenceManager.KEY_TEXT_SIZE,
            AppPreferenceManager.KEY_BACKUP_FREQUENCY -> {
                updatePreferenceSummaries()
            }
        }
    }

    companion object {
        private const val BACKUP_WORK_NAME = "notes_auto_backup"
    }
}