package com.xr.notes

import android.os.Bundle
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

        // Initialize active labels with a slight delay to ensure repository is ready
        lifecycleScope.launch {
            delay(500)  // Short delay to ensure database is initialized
            initializeActiveLabels()
        }
    }

    private fun initializeActiveLabels() {
        android.util.Log.d("MainActivity", "Initializing active labels")
        sharedLabelViewModel.initializeActiveLabels()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}