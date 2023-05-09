package dev.bartuzen.qbitcontroller.ui.rss.rules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemRssRuleBinding
import dev.bartuzen.qbitcontroller.model.RssRule

class RssRulesAdapter(
    private val onClick: (rule: RssRule) -> Unit,
    private val onLongClick: (rule: RssRule) -> Unit
) : ListAdapter<RssRule, RssRulesAdapter.ViewHolder>(DiffCallback()) {
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

        fun bind(rule: RssRule) {
            binding.textName.text = rule.name
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RssRule>() {
        override fun areItemsTheSame(oldItem: RssRule, newItem: RssRule) = oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: RssRule, newItem: RssRule) =
            oldItem.name == newItem.name && oldItem.isEnabled == newItem.isEnabled
    }
}
