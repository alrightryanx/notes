package com.xr.notes.ui

// File: app/src/main/java/com/example/notesapp/ui/settings/RestoreFragment.kt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.xr.notes.BackupsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RestoreFragment : Fragment() {

    private val viewModel: RestoreViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonImportBackup: Button
    private lateinit var textEmptyBackups: TextView
    private lateinit var backupsAdapter: BackupsAdapter

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // URI of the selected backup file
                context?.contentResolver?.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                restoreBackup(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restore, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewBackups)
        buttonImportBackup = view.findViewById(R.id.buttonImportBackup)
        textEmptyBackups = view.findViewById(R.id.textEmptyBackups)

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        return view
    }

    private fun setupRecyclerView() {
        backupsAdapter = BackupsAdapter { backupName, backupUri ->
            // Restore the selected backup
            restoreBackup(backupUri)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = backupsAdapter
        }
    }

    private fun setupButtons() {
        buttonImportBackup.setOnClickListener {
            openBackupFilePicker()
        }
    }

    private fun observeViewModel() {
        viewModel.backups.observe(viewLifecycleOwner) { backups ->
            backupsAdapter.submitList(backups)
            textEmptyBackups.visibility = if (backups.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.restoreResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RestoreViewModel.RestoreResult.Success -> {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.backup_restored_successfully),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is RestoreViewModel.RestoreResult.Error -> {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.backup_restore_failed, result.error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openBackupFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip"))
        }

        openDocumentLauncher.launch(intent)
    }

    private fun restoreBackup(backupUri: android.net.Uri) {
        viewModel.restoreBackup(backupUri)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBackups()
    }
}