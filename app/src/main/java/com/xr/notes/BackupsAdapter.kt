package com.xr.notes

// File: app/src/main/java/com/example/notesapp/ui/settings/BackupsAdapter.kt

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class BackupsAdapter(
    private val onBackupSelected: (String, Uri) -> Unit
) : ListAdapter<Pair<String, Uri>, BackupsAdapter.BackupViewHolder>(BackupsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_backup, parent, false)
        return BackupViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupViewHolder, position: Int) {
        val (backupName, backupUri) = getItem(position)
        holder.bind(backupName, backupUri)
    }

    inner class BackupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textBackupName: TextView = itemView.findViewById(R.id.textBackupName)
        private val buttonRestore: MaterialButton = itemView.findViewById(R.id.buttonRestore)

        fun bind(backupName: String, backupUri: Uri) {
            textBackupName.text = backupName

            buttonRestore.setOnClickListener {
                onBackupSelected(backupName, backupUri)
            }

            // Show the whole card is clickable
            itemView.setOnClickListener {
                onBackupSelected(backupName, backupUri)
            }
        }
    }

    class BackupsDiffCallback : DiffUtil.ItemCallback<Pair<String, Uri>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, Uri>,
            newItem: Pair<String, Uri>
        ): Boolean {
            return oldItem.second == newItem.second
        }

        override fun areContentsTheSame(
            oldItem: Pair<String, Uri>,
            newItem: Pair<String, Uri>
        ): Boolean {
            return oldItem == newItem
        }
    }
}