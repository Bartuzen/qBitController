package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentBinding
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.ui.base.MultiSelectAdapter
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getColorCompat
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.setColor

class TorrentListAdapter : MultiSelectAdapter<Torrent, String, TorrentListAdapter.ViewHolder>(
    diffCallBack = DiffCallBack(),
    getKey = { torrent ->
        torrent.hash
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { torrent ->
            holder.bind(torrent)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentBinding) :
        MultiSelectAdapter.ViewHolder<Torrent, String>(binding.root, this) {

        fun bind(torrent: Torrent) {
            val context = binding.root.context

            val backgroundColor = if (isItemSelected(getKey(torrent))) {
                context.getColorCompat(R.color.selected_card_background)
            } else {
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.TRANSPARENT)
            }
            binding.root.setCardBackgroundColor(backgroundColor)

            binding.textName.text = torrent.name

            val progress = torrent.progress * 100
            binding.progressTorrent.progress = progress.toInt()

            val progressText = if (torrent.progress < 1) {
                progress.floorToDecimal(1).toString()
            } else {
                "100"
            }
            binding.textProgress.text = context.getString(
                R.string.torrent_item_progress_format,
                formatBytes(context, torrent.completed),
                formatBytes(context, torrent.size),
                progressText,
                torrent.ratio.floorToDecimal(2).toString()
            )

            binding.textEta.text = torrent.eta?.let { eta ->
                formatSeconds(context, eta)
            }

            binding.textState.text = formatTorrentState(context, torrent.state)

            val speedList = mutableListOf<String>()
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(context, torrent.downloadSpeed)}")
            }
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(context, torrent.uploadSpeed)}")
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

            if (torrent.category == null && torrent.tags.isEmpty()) {
                binding.textProgress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = binding.textName.id
                }
                binding.textEta.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = binding.textName.id
                }
            } else {
                binding.textProgress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = binding.chipGroupCategoryAndTag.id
                }
                binding.textEta.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = binding.chipGroupCategoryAndTag.id
                }
            }

            binding.progressTorrent.setColor(getTorrentStateColor(context, torrent.state))
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<Torrent>() {
        override fun areItemsTheSame(oldItem: Torrent, newItem: Torrent) = oldItem.hash == newItem.hash

        override fun areContentsTheSame(oldItem: Torrent, newItem: Torrent) =
            oldItem.name == newItem.name && oldItem.progress.floorToDecimal(1) == newItem.progress.floorToDecimal(1) &&
                oldItem.completed == newItem.completed && oldItem.size == newItem.size && oldItem.eta == newItem.eta &&
                oldItem.state == newItem.state && oldItem.uploadSpeed == newItem.uploadSpeed &&
                oldItem.downloadSpeed == newItem.downloadSpeed && oldItem.category == newItem.category &&
                oldItem.tags == newItem.tags && oldItem.ratio.floorToDecimal(2) == newItem.ratio.floorToDecimal(2)
    }
}
