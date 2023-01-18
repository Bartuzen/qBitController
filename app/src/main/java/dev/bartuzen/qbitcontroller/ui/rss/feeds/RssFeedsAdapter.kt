package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemFeedBinding
import dev.bartuzen.qbitcontroller.model.RssFeed

class RssFeedsAdapter(
    private val onClick: (feed: RssFeed) -> Unit
) : ListAdapter<RssFeed, RssFeedsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { feed ->
            holder.bind(feed)
        }
    }

    inner class ViewHolder(private val binding: ItemFeedBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { feed ->
                        onClick(feed)
                    }
                }
            }
        }

        fun bind(feed: RssFeed) {
            binding.textName.text = feed.name
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RssFeed>() {
        override fun areItemsTheSame(oldItem: RssFeed, newItem: RssFeed) = oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: RssFeed, newItem: RssFeed) =
            oldItem.name == newItem.name && oldItem.path == newItem.path
    }
}
