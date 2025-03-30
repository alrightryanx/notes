package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/notes/AddEditNoteFragment.kt

import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddEditNoteFragment : Fragment() {

    private val args: AddEditNoteFragmentArgs by navArgs()
    private val viewModel: AddEditNoteViewModel by viewModels()

    @Inject
    lateinit var prefManager: AppPreferenceManager

    private lateinit var editTextNote: EditText
    private var isEncrypted = false

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

        val noteId = args.noteId
        if (noteId != -1L) {
            // Edit existing note
            viewModel.loadNote(noteId)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                editTextNote.setText(it.content)
                isEncrypted = it.isEncrypted

                if (isEncrypted) {
                    showDecryptDialog()
                }
            }
        }

        viewModel.saveComplete.observe(viewLifecycleOwner) { saved ->
            if (saved) {
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
                saveNote()
                true
            }

            R.id.action_labels -> {
                showLabelsDialog()
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

        viewModel.saveNote(content, isEncrypted)
    }

    private fun showLabelsDialog() {
        viewModel.getAllLabels()

        viewModel.labelsWithSelection.observe(viewLifecycleOwner) { labelsWithSelection ->
            val labelNames = labelsWithSelection.map { it.first.name }.toTypedArray()
            val checkedItems = labelsWithSelection.map { it.second }.toBooleanArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.select_labels)
                .setMultiChoiceItems(labelNames, checkedItems) { _, position, isChecked ->
                    val labelId = labelsWithSelection[position].first.id
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