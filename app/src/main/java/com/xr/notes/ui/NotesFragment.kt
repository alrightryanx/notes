package com.xr.notes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private val sharedLabelViewModel: SharedLabelViewModel by activityViewModels()

    private lateinit var notesAdapter: NotesAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var emptyView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d("NotesFragment", "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("NotesFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_notes, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewNotes)
        fabAddNote = view.findViewById(R.id.fabAddNote)
        emptyView = view.findViewById(R.id.emptyView)

        setupRecyclerView()
        setupFab()

        // Force refresh notes
        //viewModel.forceRefreshNotes()

        observeViewModel()

        return view
    }

    private fun setupRecyclerView() {
        Log.d("NotesFragment", "Setting up RecyclerView")
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
        Log.d("NotesFragment", "Observing ViewModel")

        viewModel.notesWithLabels.observe(viewLifecycleOwner) { notesWithLabels ->
            Log.d("NotesFragment", "Received ${notesWithLabels.size} notes from ViewModel")
            notesAdapter.submitList(notesWithLabels)
            updateEmptyStateVisibility(notesWithLabels)
        }
    }

    private fun updateEmptyStateVisibility(notesWithLabels: List<Any>) {
        if (notesWithLabels.isEmpty()) {
            Log.d("NotesFragment", "No notes to display, showing empty state")
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            Log.d("NotesFragment", "Showing ${notesWithLabels.size} notes")
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
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

    override fun onResume() {
        super.onResume()
        Log.d("NotesFragment", "onResume called")
        // Force refresh notes when returning from add/edit
        viewModel.forceRefreshNotes()

        // Also try a delayed refresh
        view?.postDelayed({
            if (isAdded) {
                Log.d("NotesFragment", "Delayed refresh - requesting data refresh")
                viewModel.forceRefreshNotes()
            }
        }, 500)
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_title_az -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_TITLE_ASC)
                true
            }
            R.id.action_sort_title_za -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_TITLE_DESC)
                true
            }
            R.id.action_sort_date_created_newest -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_CREATED_DESC)
                true
            }
            R.id.action_sort_date_created_oldest -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_CREATED_ASC)
                true
            }
            R.id.action_sort_date_modified_newest -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_MODIFIED_DESC)
                true
            }
            R.id.action_sort_date_modified_oldest -> {
                viewModel.setSortOrder(AppPreferenceManager.SORT_DATE_MODIFIED_ASC)
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

    @SuppressLint("StringFormatInvalid")
    private fun confirmDeleteNotes(noteIds: List<Long>) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (noteIds.size > 1) getString(R.string.confirm_delete, noteIds.size) else getString(R.string.confirm_delete))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // First delete the notes in the ViewModel which will update the UI immediately
                viewModel.deleteNotes(noteIds)

                if (notesAdapter.isInSelectionMode()) {
                    notesAdapter.toggleSelectionMode() // This will call notifyDataSetChanged()
                    activity?.invalidateOptionsMenu()
                }

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
        val bundle = Bundle().apply {
            putLong("noteId", noteId)
        }
        findNavController().navigate(R.id.action_notesFragment_to_addEditNoteFragment, bundle)
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