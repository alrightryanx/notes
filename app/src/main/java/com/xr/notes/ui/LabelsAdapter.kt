package com.xr.notes.ui

import com.xr.notes.models.Label

// File: app/src/main/java/com/example/notesapp/ui/labels/LabelsAdapter.kt

import com.xr.notes.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class LabelsAdapter(private val listener: LabelItemListener) :
    ListAdapter<Label, LabelsAdapter.LabelViewHolder>(LabelsDiffCallback()) {

    interface LabelItemListener {
        fun onLabelClicked(label: Label)
        fun onLabelEditClicked(label: Label)
        fun onLabelDeleteClicked(label: Label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_label, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = getItem(position)
        holder.bind(label)
    }

    inner class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textLabelName: TextView = itemView.findViewById(R.id.textLabelName)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEditLabel)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteLabel)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val label = getItem(position)
                    listener.onLabelClicked(label)
                }
            }

            buttonEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val label = getItem(position)
                    listener.onLabelEditClicked(label)
                }
            }

            buttonDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val label = getItem(position)
                    listener.onLabelDeleteClicked(label)
                }
            }
        }

        fun bind(label: Label) {
            textLabelName.text = label.name

            // If the label has a color, apply it
            if (label.color != 0) {
                textLabelName.setTextColor(label.color)
            }
        }
    }

    class LabelsDiffCallback : DiffUtil.ItemCallback<Label>() {
        override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem.name == newItem.name && oldItem.color == newItem.color
        }
    }
}