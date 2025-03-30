package com.xr.notes.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.R
import com.xr.notes.models.Label
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        val view = inflater.inflate(R.layout.fragment_labels, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLabels)
        fabAddLabel = view.findViewById(R.id.fabAddLabel)

        setupRecyclerView()
        setupFab()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe the ViewModel after view is created
        observeViewModel()
    }

    private fun setupRecyclerView() {
        labelsAdapter = LabelsAdapter(this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = labelsAdapter
        }
    }

    private fun setupFab() {
        fabAddLabel.setOnClickListener {
            showAddLabelDialog()
        }
    }

    private fun observeViewModel() {
        // Force refresh labels when fragment is created
        lifecycleScope.launch {
            viewModel.refreshLabels()
        }

        viewModel.labels.observe(viewLifecycleOwner) { labels ->
            Log.d("LabelsFragment", "Received ${labels.size} labels from ViewModel")
            labelsAdapter.submitList(labels)

            // Show a message if no labels
            if (labels.isEmpty()) {
                Snackbar.make(requireView(), "No labels yet. Create your first label!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddLabelDialog() {
        val input = EditText(requireContext())

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_new_label)
            .setView(input)
            .setPositiveButton(R.string.action_create) { _, _ ->
                val labelName = input.text.toString().trim()
                if (labelName.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.createLabel(labelName)
                    }
                } else {
                    Snackbar.make(requireView(), R.string.error_empty_label, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showEditLabelDialog(label: Label) {
        val input = EditText(requireContext())
        input.setText(label.name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_label)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val labelName = input.text.toString().trim()
                if (labelName.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.updateLabel(label.id, labelName)
                    }
                } else {
                    Snackbar.make(requireView(), R.string.error_empty_label, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun confirmDeleteLabel(label: Label) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_label)
            .setMessage(R.string.confirm_delete_label_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteLabel(label)
                    Snackbar.make(requireView(), R.string.label_deleted, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_labels, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_label -> {
                showAddLabelDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLabelClicked(label: Label) {
        // Navigate to notes with this label
        val bundle = Bundle().apply {
            putLong("labelId", label.id)
            putString("labelName", label.name)
        }
        findNavController().navigate(R.id.action_labelsFragment_to_labelNotesFragment, bundle)
    }

    override fun onLabelEditClicked(label: Label) {
        showEditLabelDialog(label)
    }

    override fun onLabelDeleteClicked(label: Label) {
        confirmDeleteLabel(label)
    }
}