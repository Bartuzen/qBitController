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
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState

class TorrentListAdapter(
    private val onClick: (torrent: Torrent) -> Unit,
    private val onSelectionModeStart: () -> Unit,
    private val onSelectionModeEnd: () -> Unit,
    private val onUpdateSelection: (torrentCount: Int) -> Unit
) : ListAdapter<Torrent, TorrentListAdapter.ViewHolder>(DiffCallBack()) {
    private val _selectedTorrentHashes = mutableListOf<String>()
    val selectedTorrentHashes: List<String> get() = _selectedTorrentHashes
    val isInSelectionMode get() = selectedTorrentHashes.isNotEmpty()

    fun finishSelection() {
        val selectedHashesCopy = selectedTorrentHashes.toList()
        _selectedTorrentHashes.clear()
        currentList.forEachIndexed { index, torrent ->
            if (torrent.hash in selectedHashesCopy) {
                notifyItemChanged(index)
            }
        }
    }

    fun selectAll() {
        currentList.forEachIndexed { index, torrent ->
            if (torrent.hash !in selectedTorrentHashes) {
                _selectedTorrentHashes.add(torrent.hash)
                notifyItemChanged(index)
            }
        }
        onUpdateSelection(selectedTorrentHashes.size)
    }

    fun selectInverse() {
        val inverseHashList = mutableListOf<String>()

        currentList.forEachIndexed { index, torrent ->
            if (torrent.hash !in selectedTorrentHashes) {
                inverseHashList.add(torrent.hash)
            }
            notifyItemChanged(index)
        }

        _selectedTorrentHashes.clear()
        _selectedTorrentHashes.addAll(inverseHashList)

        if (inverseHashList.isEmpty()) {
            onSelectionModeEnd()
        } else {
            onUpdateSelection(inverseHashList.size)
        }
    }

    // If a torrent is removed after list change, remove it from hash list
    override fun onCurrentListChanged(
        previousList: MutableList<Torrent>,
        currentList: MutableList<Torrent>
    ) {
        val updatedList = selectedTorrentHashes.filter { hash ->
            currentList.find { torrent ->
                torrent.hash == hash
            } != null
        }

        if (updatedList.size != selectedTorrentHashes.size) {
            onUpdateSelection(updatedList.size)
        }

        if (selectedTorrentHashes.isNotEmpty() && updatedList.isEmpty()) {
            onSelectionModeEnd()
        }

        _selectedTorrentHashes.clear()
        _selectedTorrentHashes.addAll(updatedList)
    }

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
        private var isSelected: Boolean
            get() = binding.root.isSelected
            set(value) {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { torrent ->
                        if (value) {
                            _selectedTorrentHashes.add(torrent.hash)
                        } else {
                            _selectedTorrentHashes.remove(torrent.hash)
                        }
                        notifyItemChanged(bindingAdapterPosition, Unit)
                    }
                }
            }

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (isInSelectionMode) {
                        if (isSelected) {
                            isSelected = false
                            onUpdateSelection(selectedTorrentHashes.size)
                            if (!isInSelectionMode) {
                                onSelectionModeEnd()
                            }
                        } else {
                            isSelected = true
                            onUpdateSelection(selectedTorrentHashes.size)
                        }
                    } else {
                        getItem(bindingAdapterPosition)?.let { torrent ->
                            onClick(torrent)
                        }
                    }
                }
            }

            binding.root.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && !isInSelectionMode) {
                    isSelected = true
                    onSelectionModeStart()
                    onUpdateSelection(selectedTorrentHashes.size)
                }
                true
            }
        }

        fun bind(torrent: Torrent) {
            val context = binding.root.context

            binding.root.isSelected = selectedTorrentHashes.contains(torrent.hash)

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
}