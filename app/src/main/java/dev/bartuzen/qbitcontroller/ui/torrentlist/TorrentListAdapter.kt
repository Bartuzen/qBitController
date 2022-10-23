package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentBinding
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.utils.*

class TorrentListAdapter(private val listener: OnItemClickListener? = null) :
    ListAdapter<Torrent, TorrentListAdapter.ViewHolder>(DiffCallBack()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        currentItem?.let { torrent ->
            holder.bind(torrent)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { torrent ->
                        listener?.onClick(torrent)
                    }
                }
            }
        }

        fun bind(torrent: Torrent) {
            val context = binding.root.context

            binding.textName.text = torrent.name

            val progress = torrent.progress * 100
            binding.progressTorrent.progress = progress.toInt()

            val progressText = if (torrent.progress < 1) {
                progress.floorToDecimal(1)
            } else {
                "100"
            }
            binding.textProgress.text =
                context.getString(
                    R.string.torrent_item_progress,
                    formatBytes(context, torrent.completed),
                    formatBytes(context, torrent.size),
                    progressText
                )

            val eta = formatSeconds(context, torrent.eta)
            if (eta != "inf") {
                binding.textEta.text = eta
            }
            binding.textState.text = formatTorrentState(context, torrent.state)


            val speedList = mutableListOf<String>()
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(context, torrent.uploadSpeed)}")
            }
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(context, torrent.downloadSpeed)}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")

            binding.chipGroupCategoryAndTag.removeAllViews()

            if (torrent.category != null) {
                val chip = Chip(context)
                chip.text = torrent.category
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_category)
                chip.ellipsize = TextUtils.TruncateAt.END
                binding.chipGroupCategoryAndTag.addView(chip)
            }

            torrent.tags.forEach { tag ->
                val chip = Chip(context)
                chip.text = tag
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_tag)
                chip.ellipsize = TextUtils.TruncateAt.END
                binding.chipGroupCategoryAndTag.addView(chip)
            }
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<Torrent>() {
        override fun areItemsTheSame(oldItem: Torrent, newItem: Torrent) =
            oldItem.hash == newItem.hash

        override fun areContentsTheSame(oldItem: Torrent, newItem: Torrent) =
            oldItem == newItem
    }

    interface OnItemClickListener {
        fun onClick(torrent: Torrent)
    }
}