package com.xr.notes.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.core.content.edit

class AppPreferenceManager(context: Context) {

    companion object {
        // Constants for preference keys
        const val KEY_THEME = "theme_preference"
        const val KEY_TEXT_SIZE = "text_size_preference"
        const val KEY_SORT_ORDER = "sort_order_preference"
        const val KEY_AUTO_BACKUP = "auto_backup_preference"
        const val KEY_BACKUP_FREQUENCY = "backup_frequency_preference"

        // Theme options
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        // Text size options
        const val TEXT_SIZE_SMALL = "small"
        const val TEXT_SIZE_MEDIUM = "medium"
        const val TEXT_SIZE_LARGE = "large"

        // Sort options
        const val SORT_TITLE_ASC = "title_asc"
        const val SORT_TITLE_DESC = "title_desc" // Add this
        const val SORT_DATE_CREATED_DESC = "date_created_desc"
        const val SORT_DATE_CREATED_ASC = "date_created_asc" // Add this
        const val SORT_DATE_MODIFIED_DESC = "date_modified_desc"
        const val SORT_DATE_MODIFIED_ASC = "date_modified_asc" // Add this

        // Backup frequency options
        const val BACKUP_DAILY = "daily"
        const val BACKUP_WEEKLY = "weekly"
        const val BACKUP_MONTHLY = "monthly"
    }

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Theme preferences
    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setTheme(theme: String) {
        sharedPreferences.edit {
            putString(KEY_THEME, theme)
        }
    }

    fun applyTheme() {
        when (getTheme()) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // Text size preferences
    fun getTextSize(): String {
        return sharedPreferences.getString(KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM) ?: TEXT_SIZE_MEDIUM
    }

    fun setTextSize(textSize: String) {
        sharedPreferences.edit().putString(KEY_TEXT_SIZE, textSize).apply()
    }

    fun getTextSizeInSp(): Float {
        return when (getTextSize()) {
            TEXT_SIZE_SMALL -> 14f
            TEXT_SIZE_MEDIUM -> 16f
            TEXT_SIZE_LARGE -> 18f
            else -> 16f
        }
    }

    // Sort order preferences
    fun getSortOrder(): String {
        return sharedPreferences.getString(KEY_SORT_ORDER, SORT_DATE_MODIFIED_DESC)
            ?: SORT_DATE_MODIFIED_DESC
    }

    fun setSortOrder(sortOrder: String) {
        sharedPreferences.edit().putString(KEY_SORT_ORDER, sortOrder).apply()
    }

    // Backup preferences
    fun isAutoBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_BACKUP, false)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun getBackupFrequency(): String {
        return sharedPreferences.getString(KEY_BACKUP_FREQUENCY, BACKUP_WEEKLY) ?: BACKUP_WEEKLY
    }

    fun setBackupFrequency(frequency: String) {
        sharedPreferences.edit().putString(KEY_BACKUP_FREQUENCY, frequency).apply()
    }

    fun getBackupIntervalInDays(): Int {
        return when (getBackupFrequency()) {
            BACKUP_DAILY -> 1
            BACKUP_WEEKLY -> 7
            BACKUP_MONTHLY -> 30
            else -> 7
        }
    }
}