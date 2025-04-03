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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.R
import com.xr.notes.models.Label
import com.xr.notes.repo.NotesRepository
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AddEditNoteFragment : Fragment() {

    private val viewModel: AddEditNoteViewModel by viewModels()

    @Inject
    lateinit var prefManager: AppPreferenceManager

    @Inject
    lateinit var repository: NotesRepository

    private lateinit var editTextNote: EditText
    private var isEncrypted = false
    private var isSaving = false // Flag to prevent multiple saves
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_edit_note, container, false)

        editTextNote = view.findViewById(R.id.editTextNote)

        // Set text size from preferences
        editTextNote.textSize = prefManager.getTextSizeInSp()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments directly instead of using navArgs
        val noteId = arguments?.getLong("noteId", -1L) ?: -1L
        Log.d("AddEditNoteFragment", "onViewCreated with noteId: $noteId")

        if (noteId != -1L) {
            // Edit existing note
            viewModel.loadNote(noteId)
        }

        observeViewModel()
    }


    private fun observeViewModel() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                Log.d("AddEditNoteFragment", "Note data received: ID=${it.id}, content=${it.content.take(20)}...")
                editTextNote.setText(it.content)
                isEncrypted = it.isEncrypted

                if (isEncrypted) {
                    showDecryptDialog()
                }
            }
        }

        viewModel.saveComplete.observe(viewLifecycleOwner) { saved ->
            Log.d("AddEditNoteFragment", "saveComplete: $saved")
            if (saved) {
                isSaving = false
                Log.d("AddEditNoteFragment", "Navigation back after save")
                findNavController().navigateUp()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_edit_note, menu)

        // Update encrypt/decrypt menu item
        menu.findItem(R.id.action_encrypt_decrypt).setTitle(
            if (isEncrypted) R.string.action_decrypt else R.string.action_encrypt
        )

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                if (!isSaving) { // Prevent double saves
                    saveNote()
                }
                true
            }

            R.id.action_labels -> {
                if (!viewModel.hasNoteBeenSaved()) {
                    // For a new note, save it first then show labels
                    saveNoteAndShowLabels()
                } else {
                    // For an existing note, just show labels
                    showLabelsDialog()
                }
                true
            }

            R.id.action_encrypt_decrypt -> {
                if (isEncrypted) {
                    showDecryptDialog()
                } else {
                    showEncryptDialog()
                }
                true
            }

            R.id.action_delete -> {
                confirmDelete()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveNote() {
        val content = editTextNote.text.toString().trim()
        if (content.isEmpty()) {
            Snackbar.make(requireView(), R.string.error_empty_note, Snackbar.LENGTH_SHORT).show()
            return
        }

        Log.d("AddEditNoteFragment", "Saving note with content: ${content.take(20)}...")
        isSaving = true

        val job = viewModel.saveNote(content, isEncrypted)
        job.invokeOnCompletion {
            activity?.runOnUiThread {
                Log.d("AddEditNoteFragment", "Save operation completed")
                // The ViewModel's saveComplete LiveData will handle navigation
            }
        }
    }

    private fun saveNoteAndShowLabels() {
        val content = editTextNote.text.toString().trim()
        if (content.isEmpty()) {
            Snackbar.make(requireView(), R.string.error_empty_note, Snackbar.LENGTH_SHORT).show()
            return
        }

        // Show a loading indicator
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("Saving note...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        isSaving = true
        val job = viewModel.saveNote(content, isEncrypted)
        job.invokeOnCompletion {
            activity?.runOnUiThread {
                loadingDialog.dismiss()
                // Make sure the note is saved before showing the labels dialog
                if (viewModel.hasNoteBeenSaved()) {
                    showLabelsDialog()
                } else {
                    Snackbar.make(requireView(), "Failed to save note", Snackbar.LENGTH_SHORT).show()
                    isSaving = false
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Only auto-save if not already saving through the save button
        if (!isSaving && ::editTextNote.isInitialized) {
            val content = editTextNote.text.toString().trim()
            if (content.isNotEmpty()) {
                isSaving = true
                viewModel.saveNote(content, isEncrypted)
            }
        }
    }

    private fun showLabelsDialog() {
        // Get all labels from the ViewModel
        coroutineScope.launch {
            val loadingDialog = AlertDialog.Builder(requireContext())
                .setMessage("Loading labels...")
                .setCancelable(false)
                .create()

            try {
                // Safe context check
                if (!isAdded) return@launch

                loadingDialog.show()

                // Fetch all labels
                val labels = withContext(Dispatchers.IO) {
                    repository.getAllLabels().value ?: emptyList()
                }

                // Safe context check after background work
                if (!isAdded) {
                    loadingDialog.dismiss()
                    return@launch
                }

                loadingDialog.dismiss()

                if (labels.isEmpty()) {
                    // No labels available, show dialog to create one
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.select_labels)
                        .setMessage("No labels available. Create a label first.")
                        .setPositiveButton(R.string.action_done) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.action_new_label) { _, _ ->
                            showNewLabelDialog()
                        }
                        .show()
                } else {
                    displayLabelsSelectionDialog(labels)
                }
            } catch (e: Exception) {
                Log.e("AddEditNote", "Error loading labels", e)
                if (isAdded && loadingDialog.isShowing) {
                    loadingDialog.dismiss()
                    Snackbar.make(requireView(), "Failed to load labels: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayLabelsSelectionDialog(allLabels: List<Label>) {
        val noteId = viewModel.getCurrentNoteId()
        if (noteId == -1L) {
            Log.e("AddEditNote", "Cannot show labels dialog: Note ID is invalid")
            Snackbar.make(requireView(), "Error: Cannot associate labels with unsaved note", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Get any existing label associations for this note
        coroutineScope.launch {
            try {
                val noteWithLabels = withContext(Dispatchers.IO) {
                    repository.getNoteWithLabels(noteId).value
                }

                if (!isAdded) return@launch

                val associatedLabels = noteWithLabels?.labels ?: emptyList()

                // Get the IDs of labels already associated with this note
                val associatedLabelIds = associatedLabels.map { it.id }

                // Create the checkbox items
                val labelNames = allLabels.map { it.name }.toTypedArray()
                val checkedItems = allLabels.map { label ->
                    associatedLabelIds.contains(label.id)
                }.toBooleanArray()

                // Show the dialog
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.select_labels)
                    .setMultiChoiceItems(labelNames, checkedItems) { _, position, isChecked ->
                        val labelId = allLabels[position].id
                        if (isChecked) {
                            viewModel.addLabelToNote(labelId)
                        } else {
                            viewModel.removeLabelFromNote(labelId)
                        }
                    }
                    .setPositiveButton(R.string.action_done) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.action_new_label) { _, _ ->
                        showNewLabelDialog()
                    }
                    .show()
            } catch (e: Exception) {
                Log.e("AddEditNote", "Error displaying labels dialog", e)
                if (isAdded) {
                    Snackbar.make(requireView(), "Error displaying labels", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNewLabelDialog() {
        val input = EditText(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_new_label)
            .setView(input)
            .setPositiveButton(R.string.action_create) { _, _ ->
                val labelName = input.text.toString().trim()
                if (labelName.isNotEmpty()) {
                    viewModel.createLabel(labelName)
                    // Wait a moment and then show the labels dialog again
                    requireView().postDelayed({
                        showLabelsDialog()
                    }, 300)
                } else {
                    Snackbar.make(requireView(), R.string.error_empty_label, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showEncryptDialog() {
        val input = EditText(requireContext())
        input.hint = getString(R.string.password_hint)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.encrypt_note)
            .setMessage(R.string.encrypt_note_message)
            .setView(input)
            .setPositiveButton(R.string.action_encrypt) { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    viewModel.encryptNote(password)
                    isEncrypted = true
                    activity?.invalidateOptionsMenu()
                    Snackbar.make(requireView(), R.string.note_encrypted, Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.error_empty_password,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showDecryptDialog() {
        val input = EditText(requireContext())
        input.hint = getString(R.string.password_hint)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.decrypt_note)
            .setMessage(R.string.decrypt_note_message)
            .setView(input)
            .setPositiveButton(R.string.action_decrypt) { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    viewModel.decryptNote(password)
                    isEncrypted = false
                    activity?.invalidateOptionsMenu()
                    Snackbar.make(requireView(), R.string.note_decrypted, Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    Snackbar.make(
                        requireView(),
                        R.string.error_empty_password,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteNote()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}