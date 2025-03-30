package com.xr.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xr.notes.models.Note
import com.xr.notes.utils.AppPreferenceManager
import java.text.SimpleDateFormat
import java.util.Locale

class NotesAdapter(
    private val prefManager: AppPreferenceManager,
    private val listener: NoteItemListener
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NotesDiffCallback()) {

    private var selectedNotes = mutableSetOf<Long>()
    private var selectionMode = false

    interface NoteItemListener {
        fun onNoteClicked(note: Note)
        fun onNoteSelected(note: Note, isSelected: Boolean)
        fun onSelectionChanged(count: Int)
        fun onRequestDeleteNote(note: Note) // Added method for direct delete
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    fun toggleSelectionMode(): Boolean {
        selectionMode = !selectionMode
        if (!selectionMode) {
            selectedNotes.clear()
            listener.onSelectionChanged(0)
        }
        notifyDataSetChanged()
        return selectionMode
    }

    fun isInSelectionMode() = selectionMode

    fun getSelectedNoteIds(): List<Long> = selectedNotes.toList()

    fun getSelectedCount() = selectedNotes.size

    fun selectAllNotes() {
        currentList.forEach { note ->
            selectedNotes.add(note.id)
        }
        listener.onSelectionChanged(selectedNotes.size)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedNotes.clear()
        listener.onSelectionChanged(0)
        notifyDataSetChanged()
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkboxNote)
        private val titleTextView: TextView = itemView.findViewById(R.id.textNoteTitle)
        private val summaryTextView: TextView = itemView.findViewById(R.id.textNoteSummary)
        private val dateTextView: TextView = itemView.findViewById(R.id.textNoteDate)

        init {
            // Set text size from preferences
            val textSize = prefManager.getTextSizeInSp()
            titleTextView.textSize = textSize
            summaryTextView.textSize = textSize - 2
            dateTextView.textSize = textSize - 4

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val note = getItem(position)
                    if (selectionMode) {
                        toggleNoteSelection(note)
                    } else {
                        listener.onNoteClicked(note)
                    }
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val note = getItem(position)
                    if (!selectionMode) {
                        // Start selection mode and select this note
                        selectionMode = true
                        toggleNoteSelection(note)
                        return@setOnLongClickListener true
                    } else {
                        // If already in selection mode, request to delete this note directly
                        listener.onRequestDeleteNote(note)
                        return@setOnLongClickListener true
                    }
                }
                return@setOnLongClickListener false
            }

            checkBox.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val note = getItem(position)
                    toggleNoteSelection(note)
                }
            }
        }

        fun bind(note: Note) {
            titleTextView.text = note.title
            summaryTextView.text = note.summary

            // Format the date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(note.modifiedAt)

            // Show checkbox in selection mode
            checkBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedNotes.contains(note.id)

            // Encrypted note indicator
            if (note.isEncrypted) {
                // Add a lock icon or indicator
                // This would be implemented with a separate view in the layout
            }
        }

        private fun toggleNoteSelection(note: Note) {
            val isSelected = if (selectedNotes.contains(note.id)) {
                selectedNotes.remove(note.id)
                false
            } else {
                selectedNotes.add(note.id)
                true
            }

            checkBox.isChecked = isSelected
            listener.onNoteSelected(note, isSelected)
            listener.onSelectionChanged(selectedNotes.size)
        }
    }

    class NotesDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.content == newItem.content &&
                    oldItem.modifiedAt == newItem.modifiedAt &&
                    oldItem.isEncrypted == newItem.isEncrypted
        }
    }
}