package com.xr.notes

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.xr.notes.ui.SharedLabelViewModel
import com.xr.notes.utils.ActiveLabelsStore
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var prefManager: AppPreferenceManager

    @Inject
    lateinit var activeLabelsStore: ActiveLabelsStore

    private lateinit var navController: NavController
    private val sharedLabelViewModel: SharedLabelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

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

        // Ensure active labels are initialized early
        initializeActiveLabels()
    }

    private fun initializeActiveLabels() {
        Log.d("MainActivity", "Initializing active labels")

        // Ensure we start with all labels active
        lifecycleScope.launch {
            // Make an immediate attempt to initialize
            sharedLabelViewModel.initializeActiveLabels()

            // Try additional times with delays to handle race conditions
            delay(500)
            sharedLabelViewModel.initializeActiveLabels()

            delay(1000)
            sharedLabelViewModel.initializeActiveLabels()

            // Debug log
            val activeLabels = activeLabelsStore.getActiveLabels()
            Log.d("MainActivity", "After initialization, active labels count: ${activeLabels.size}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}