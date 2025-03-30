package com.xr.notes.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xr.notes.R
import com.xr.notes.models.Label

class LabelsAdapter(private val listener: LabelItemListener) :
    ListAdapter<LabelItem, RecyclerView.ViewHolder>(LabelsDiffCallback()) {

    interface LabelItemListener {
        fun onLabelClicked(labelItem: LabelItem)
        fun onLabelEditClicked(label: Label)
        fun onLabelDeleteClicked(label: Label)
        fun onLabelActiveChanged(labelItem: LabelItem, isActive: Boolean)
    }

    companion object {
        private const val VIEW_TYPE_ALL = 0
        private const val VIEW_TYPE_NORMAL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ALL else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ALL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_special_label, parent, false)
                AllLabelViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_label_with_checkbox, parent, false)
                NormalLabelViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val labelItem = getItem(position)
        when (holder) {
            is AllLabelViewHolder -> holder.bind(labelItem)
            is NormalLabelViewHolder -> holder.bind(labelItem)
        }
    }

    inner class AllLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textLabelName: TextView = itemView.findViewById(R.id.textSpecialLabelName)
        private val checkboxLabel: CheckBox = itemView.findViewById(R.id.checkboxSpecialLabel)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    listener.onLabelClicked(labelItem)
                }
            }

            checkboxLabel.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    labelItem.isActive = checkboxLabel.isChecked
                    listener.onLabelActiveChanged(labelItem, checkboxLabel.isChecked)

                    // When ALL is checked or unchecked, update all other items
                    if (position == 0) {
                        val itemCount = currentList.size
                        for (i in 1 until itemCount) {
                            currentList[i].isActive = checkboxLabel.isChecked
                        }
                        notifyItemRangeChanged(1, itemCount - 1)
                    }
                }
            }
        }

        fun bind(labelItem: LabelItem) {
            textLabelName.text = labelItem.name
            checkboxLabel.isChecked = labelItem.isActive
        }
    }

    inner class NormalLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textLabelName: TextView = itemView.findViewById(R.id.textLabelName)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEditLabel)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDeleteLabel)
        private val checkboxLabel: CheckBox = itemView.findViewById(R.id.checkboxLabel)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    listener.onLabelClicked(labelItem)
                }
            }

            buttonEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    labelItem.label?.let { label ->
                        listener.onLabelEditClicked(label)
                    }
                }
            }

            buttonDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    labelItem.label?.let { label ->
                        listener.onLabelDeleteClicked(label)
                    }
                }
            }

            checkboxLabel.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val labelItem = getItem(position)
                    labelItem.isActive = checkboxLabel.isChecked
                    listener.onLabelActiveChanged(labelItem, checkboxLabel.isChecked)

                    // Update ALL checkbox based on whether all labels are active
                    val allActive = currentList.drop(1).all { it.isActive }
                    if (currentList.isNotEmpty() && currentList[0].isActive != allActive) {
                        currentList[0].isActive = allActive
                        notifyItemChanged(0)
                    }
                }
            }
        }

        fun bind(labelItem: LabelItem) {
            textLabelName.text = labelItem.name
            checkboxLabel.isChecked = labelItem.isActive

            // If the label has a color, apply it
            labelItem.label?.let { label ->
                if (label.color != 0) {
                    textLabelName.setTextColor(label.color)
                }
            }
        }
    }

    class LabelsDiffCallback : DiffUtil.ItemCallback<LabelItem>() {
        override fun areItemsTheSame(oldItem: LabelItem, newItem: LabelItem): Boolean {
            // For "ALL" label (at index 0)
            if (oldItem.isSpecial && newItem.isSpecial) {
                return true
            }
            // For regular labels
            return oldItem.label?.id == newItem.label?.id
        }

        override fun areContentsTheSame(oldItem: LabelItem, newItem: LabelItem): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.isActive == newItem.isActive &&
                    oldItem.label?.color == newItem.label?.color
        }
    }
}

// Data class to represent items in the labels list
data class LabelItem(
    val name: String,
    val label: Label? = null,
    val isSpecial: Boolean = false,
    var isActive: Boolean = false
)