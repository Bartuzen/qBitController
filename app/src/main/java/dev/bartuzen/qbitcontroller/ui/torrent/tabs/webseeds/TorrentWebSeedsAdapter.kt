package dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemWebSeedBinding

class TorrentWebSeedsAdapter : ListAdapter<String, TorrentWebSeedsAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemWebSeedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { webSeed ->
            holder.bind(webSeed)
        }
    }

    inner class ViewHolder(private val binding: ItemWebSeedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(webSeed: String) {
            binding.textWebSeed.text = webSeed
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = true
    }
}
