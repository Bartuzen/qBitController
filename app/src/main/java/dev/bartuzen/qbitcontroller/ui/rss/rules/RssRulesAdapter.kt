package dev.bartuzen.qbitcontroller.ui.rss.rules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemRssRuleBinding

class RssRulesAdapter(
    private val onClick: (rule: String) -> Unit,
    private val onLongClick: (rule: String) -> Unit
) : ListAdapter<String, RssRulesAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRssRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { rule ->
            holder.bind(rule)
        }
    }

    inner class ViewHolder(private val binding: ItemRssRuleBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { rule ->
                        onClick(rule)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { rule ->
                        onLongClick(rule)
                    }
                }
                true
            }
        }

        fun bind(rule: String) {
            binding.textName.text = rule
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = true
    }
}
