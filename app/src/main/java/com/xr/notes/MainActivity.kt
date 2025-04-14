package com.xr.notes

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var prefManager: AppPreferenceManager

    private lateinit var navController: NavController

    // Variable to track the last opened note
    private var lastOpenedNoteId: Long = -1L

    companion object {
        // Shared preference key to store last opened note ID
        const val PREF_LAST_OPENED_NOTE = "last_opened_note_id"
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme from preferences before setting content view
        prefManager.applyTheme()

        // Handle window insets properly
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup ActionBar with NavController
        setupActionBarWithNavController(navController)

        // Try to restore last opened note from preferences
        val sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        lastOpenedNoteId = sharedPrefs.getLong(PREF_LAST_OPENED_NOTE, -1L)
        Log.d(TAG, "Restored last opened note ID: $lastOpenedNoteId")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Function to save the last opened note ID
    fun setLastOpenedNote(noteId: Long) {
        Log.d(TAG, "Setting last opened note ID: $noteId")
        lastOpenedNoteId = noteId
        // Also save to SharedPreferences for persistence across app restart
        val sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong(PREF_LAST_OPENED_NOTE, noteId).apply()
    }

    // Function to get the last opened note ID
    fun getLastOpenedNote(): Long {
        Log.d(TAG, "Getting last opened note ID: $lastOpenedNoteId")
        return lastOpenedNoteId
    }

    // Clear the last opened note (e.g., when deleting a note)
    fun clearLastOpenedNote() {
        Log.d(TAG, "Clearing last opened note ID")
        lastOpenedNoteId = -1L
        val sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(PREF_LAST_OPENED_NOTE).apply()
    }
}