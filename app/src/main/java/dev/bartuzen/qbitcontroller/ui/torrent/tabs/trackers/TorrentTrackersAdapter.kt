package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentTrackerBinding
import dev.bartuzen.qbitcontroller.model.TorrentTracker

class TorrentTrackersAdapter :
    ListAdapter<TorrentTracker, TorrentTrackersAdapter.ViewHolder>(DiffCallBack()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentTrackerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { tracker ->
            holder.bind(tracker)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentTrackerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tracker: TorrentTracker) {
            binding.textUrl.text = tracker.url

            binding.textPeers.text = (tracker.peers ?: "-").toString()
            binding.textSeeds.text = (tracker.seeds ?: "-").toString()
            binding.textLeeches.text = (tracker.leeches ?: "-").toString()
            binding.textDownloaded.text = (tracker.downloaded ?: "-").toString()

            if (tracker.message != null) {
                binding.textMessage.text = binding.root.context
                    .getString(R.string.torrent_trackers_message, tracker.message)
            } else {
                binding.textMessage.visibility = View.GONE
            }
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentTracker>() {
        override fun areItemsTheSame(oldItem: TorrentTracker, newItem: TorrentTracker) =
            oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: TorrentTracker, newItem: TorrentTracker) =
            oldItem == newItem
    }
}