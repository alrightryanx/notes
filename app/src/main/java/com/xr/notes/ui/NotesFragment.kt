package com.xr.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.NotesAdapter
import com.xr.notes.R
import com.xr.notes.models.Note
import com.xr.notes.utils.AppPreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotesFragment : Fragment(), NotesAdapter.NoteItemListener {

    @Inject
    lateinit var prefManager: AppPreferenceManager

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewNotes)
        fabAddNote = view.findViewById(R.id.fabAddNote)

        setupRecyclerView()
        setupFab()
        observeViewModel()

        return view
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(prefManager, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
        }
    }

    private fun setupFab() {
        fabAddNote.setOnClickListener {
            navigateToAddEditNote(-1L)
        }
    }

    private fun observeViewModel() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            notesAdapter.submitList(notes)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notes, menu)

        // Setup search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchNotes(newText ?: "")
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    @Suppress("DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        val inSelectionMode = notesAdapter.isInSelectionMode()
        menu.findItem(R.id.action_select_all)?.isVisible = inSelectionMode
        menu.findItem(R.id.action_delete_selected)?.isVisible = inSelectionMode && notesAdapter.getSelectedCount() > 0

        super.onPrepareOptionsMenu(menu)
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_title -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_TITLE_ASC)
                true
            }
            R.id.action_sort_date_created -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_CREATED_DESC)
                true
            }
            R.id.action_sort_date_modified -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_MODIFIED_DESC)
                true
            }
            R.id.action_labels -> {
                findNavController().navigate(R.id.action_notesFragment_to_labelsFragment)
                true
            }
            R.id.action_backup -> {
                viewModel.createBackup()
                Snackbar.make(requireView(), R.string.backup_created_successfully, Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_restore -> {
                findNavController().navigate(R.id.action_notesFragment_to_restoreFragment)
                true
            }
            R.id.action_settings -> {
                findNavController().navigate(R.id.action_notesFragment_to_settingsFragment)
                true
            }
            R.id.action_select_all -> {
                if (!notesAdapter.isInSelectionMode()) {
                    notesAdapter.toggleSelectionMode()
                }
                notesAdapter.selectAllNotes()
                activity?.invalidateOptionsMenu()
                true
            }
            R.id.action_delete_selected -> {
                deleteSelectedNotes()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteSelectedNotes() {
        val selectedIds = notesAdapter.getSelectedNoteIds()
        if (selectedIds.isNotEmpty()) {
            confirmDeleteNotes(selectedIds)
        }
    }

    private fun confirmDeleteNotes(noteIds: List<Long>) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (noteIds.size > 1) getString(R.string.confirm_delete, noteIds.size) else getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // First delete the notes in the ViewModel which will update the UI immediately
                viewModel.deleteNotes(noteIds)

                // Show a Snackbar
                Snackbar.make(
                    requireView(),
                    getString(R.string.notes_deleted, noteIds.size),
                    Snackbar.LENGTH_SHORT
                ).show()

                // Exit selection mode
                if (notesAdapter.isInSelectionMode()) {
                    notesAdapter.toggleSelectionMode()
                }

                activity?.invalidateOptionsMenu()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun navigateToAddEditNote(noteId: Long) {
        val action = NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(noteId)
        findNavController().navigate(action)
    }

    // NotesAdapter.NoteItemListener implementation
    override fun onNoteClicked(note: Note) {
        navigateToAddEditNote(note.id)
    }

    override fun onNoteSelected(note: Note, isSelected: Boolean) {
        // This is handled by the adapter
    }

    override fun onSelectionChanged(count: Int) {
        activity?.invalidateOptionsMenu()
    }

    override fun onRequestDeleteNote(note: Note) {
        // Single note deletion request (from long-press)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.confirm_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // Delete the note using the direct delete method
                viewModel.deleteNote(note)

                // Show confirmation
                Snackbar.make(
                    requireView(),
                    getString(R.string.notes_deleted, 1),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}