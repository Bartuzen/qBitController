package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentTrackerBinding
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.ui.base.MultiSelectAdapter

class TorrentTrackersAdapter :
    MultiSelectAdapter<TorrentTracker, String, TorrentTrackersAdapter.ViewHolder>(
        diffCallBack = DiffCallBack(),
        getKey = { tracker ->
            tracker.url
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentTrackerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { tracker ->
            holder.bind(tracker)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentTrackerBinding) :
        MultiSelectAdapter.ViewHolder<TorrentTracker, String>(binding.root, this) {

        fun bind(tracker: TorrentTracker) {
            binding.root.isSelected = isItemSelected(tracker.url)

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
