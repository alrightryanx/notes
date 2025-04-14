package com.xr.notes.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.R
import com.xr.notes.models.Label
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LabelsFragment : Fragment(), LabelsAdapter.LabelItemListener {

    private val viewModel: LabelsViewModel by viewModels()

    private lateinit var labelsAdapter: LabelsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddLabel: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            val view = inflater.inflate(R.layout.fragment_labels, container, false)

            recyclerView = view.findViewById(R.id.recyclerViewLabels)
            fabAddLabel = view.findViewById(R.id.fabAddLabel)

            try {
                setupRecyclerView()
                setupFab()
                observeViewModel()

                // Force refresh the labels
                viewModel.forceRefreshLabels()
            } catch (e: Exception) {
                Log.e("LabelsFragment", "Error in onCreateView setup", e)
                showDatabaseErrorAndRecoveryDialog()
            }

            return view
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Critical error in onCreateView", e)

            // Return an empty view to avoid crashing
            return View(context)
        }
    }

    private fun setupRecyclerView() {
        try {
            labelsAdapter = LabelsAdapter(this)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = labelsAdapter
            }
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun setupFab() {
        try {
            fabAddLabel.setOnClickListener {
                showAddLabelDialog()
            }
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error setting up FAB", e)
            throw e
        }
    }

    private fun observeViewModel() {
        try {
            viewModel.labelItems.observe(viewLifecycleOwner) { labelItems ->
                try {
                    labelsAdapter.submitList(labelItems)
                } catch (e: Exception) {
                    Log.e("LabelsFragment", "Error submitting list to adapter", e)
                }
            }

            // Observe active labels for UI updates
            viewModel.activeLabels.observe(viewLifecycleOwner) { activeLabels ->
                try {
                    viewModel.forceRefreshLabels()
                } catch (e: Exception) {
                    Log.e("LabelsFragment", "Error in activeLabels observer", e)
                }
            }
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error setting up observers", e)
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.forceRefreshLabels()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error refreshing labels in onResume", e)
        }
    }

    private fun showAddLabelDialog() {
        try {
            val input = EditText(requireContext())

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.create_new_label)
                .setView(input)
                .setPositiveButton(R.string.action_create) { _, _ ->
                    val labelName = input.text.toString().trim()
                    if (labelName.isNotEmpty()) {
                        viewModel.createLabel(labelName)
                        // Show success message
                        Snackbar.make(requireView(), "Label created", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(requireView(), R.string.error_empty_label, Snackbar.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error showing add label dialog", e)
            Toast.makeText(context, "Couldn't create label", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditLabelDialog(label: Label) {
        try {
            val input = EditText(requireContext())
            input.setText(label.name)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_label)
                .setView(input)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val labelName = input.text.toString().trim()
                    if (labelName.isNotEmpty()) {
                        viewModel.updateLabel(label.id, labelName)
                        // Show success message
                        Snackbar.make(requireView(), "Label updated", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(requireView(), R.string.error_empty_label, Snackbar.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error showing edit label dialog", e)
        }
    }

    private fun confirmDeleteLabel(label: Label) {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_label)
                .setMessage(R.string.confirm_delete_label_message)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    viewModel.deleteLabel(label)
                    Snackbar.make(requireView(), R.string.label_deleted, Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error showing delete label confirmation", e)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        try {
            inflater.inflate(R.menu.menu_labels, menu)

            // Add a database recovery option if needed
            menu.add(Menu.NONE, R.id.action_database_recovery, Menu.NONE, "Database Recovery")

            super.onCreateOptionsMenu(menu, inflater)
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error creating options menu", e)
        }
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_database_recovery -> {
                    showDatabaseErrorAndRecoveryDialog()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error handling options item selection", e)
            false
        }
    }

    override fun onLabelClicked(labelItem: LabelItem) {
        try {
            if (labelItem.isSpecial) {
                // For "ALL" label, just navigate back to the main screen
                findNavController().navigateUp()
            } else {
                // Navigate to notes with this label
                labelItem.label?.let { label ->
                    val bundle = Bundle().apply {
                        putLong("labelId", label.id)
                        putString("labelName", label.name)
                    }
                    findNavController().navigate(R.id.action_labelsFragment_to_labelNotesFragment, bundle)
                }
            }
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error handling label click", e)
            Toast.makeText(context, "Couldn't open label", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLabelEditClicked(label: Label) {
        try {
            showEditLabelDialog(label)
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error handling label edit click", e)
        }
    }

    override fun onLabelDeleteClicked(label: Label) {
        try {
            confirmDeleteLabel(label)
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error handling label delete click", e)
        }
    }

    override fun onLabelActiveChanged(labelItem: LabelItem, isActive: Boolean) {
        try {
            if (labelItem.isSpecial) {
                // Handle ALL label
                viewModel.toggleAllLabelsActive(isActive)
            } else {
                // Handle regular label
                labelItem.label?.let { label ->
                    viewModel.toggleLabelActive(label.id, isActive)
                }
            }

            // Show a feedback message that active labels have been updated
            val message = if (isActive) R.string.label_activated else R.string.label_deactivated
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error handling label active state change", e)
        }
    }

    // Add a method to show database error and recovery dialog
    private fun showDatabaseErrorAndRecoveryDialog() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Database Error")
                .setMessage("The app has detected potential database issues. Would you like to attempt recovery? This may reset your database if serious problems are found.")
                .setPositiveButton("Attempt Recovery") { _, _ ->
                    viewModel.debugDatabaseState()
                    Toast.makeText(context, "Recovery process started", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Reset Database") { _, _ ->
                    // Confirm reset with another dialog
                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Reset")
                        .setMessage("WARNING: This will delete all notes and labels. This action cannot be undone. Are you sure?")
                        .setPositiveButton("Reset") { _, _ ->
                            viewModel.resetDatabase()
                            Toast.makeText(context, "Database reset complete", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e("LabelsFragment", "Error showing database recovery dialog", e)
            Toast.makeText(context, "Database error occurred", Toast.LENGTH_SHORT).show()
        }
    }
}