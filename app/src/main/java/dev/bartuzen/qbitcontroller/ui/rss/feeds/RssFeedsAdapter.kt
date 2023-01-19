package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemRssFeedBinding
import dev.bartuzen.qbitcontroller.model.RssFeedNode

class RssFeedsAdapter(
    private val onClick: (feed: RssFeedNode) -> Unit
) : ListAdapter<RssFeedNode, RssFeedsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRssFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { feed ->
            holder.bind(feed)
        }
    }

    inner class ViewHolder(private val binding: ItemRssFeedBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { feed ->
                        onClick(feed)
                    }
                }
            }
        }

        fun bind(feed: RssFeedNode) {
            binding.textName.text = feed.name
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RssFeedNode>() {
        override fun areItemsTheSame(oldItem: RssFeedNode, newItem: RssFeedNode) =
            oldItem.isFeed == newItem.isFeed && oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: RssFeedNode, newItem: RssFeedNode) =
            oldItem.name == newItem.name && oldItem.feed?.uid == newItem.feed?.uid
    }
}
