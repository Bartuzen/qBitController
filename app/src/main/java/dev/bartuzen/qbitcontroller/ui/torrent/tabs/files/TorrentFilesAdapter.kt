package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFileBinding
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatFilePriority

class TorrentFilesAdapter(
    private val onClick: (file: TorrentFileNode) -> Unit
) : ListAdapter<TorrentFileNode, TorrentFilesAdapter.ViewHolder>(DiffCallBack()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { file ->
            holder.bind(file)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { file ->
                        onClick(file)
                    }
                }
            }
        }

        fun bind(fileNode: TorrentFileNode) {
            binding.textName.text = fileNode.name

            val iconId = if (fileNode.isFile) R.drawable.ic_file else R.drawable.ic_folder
            binding.imageIcon.setImageResource(iconId)

            val context = binding.root.context
            val file = fileNode.file
            if (file != null) {
                val progress = file.progress * 100

                binding.progressIndicator.progress = progress.toInt()

                val downloadedSize = (file.progress * file.size).toLong()
                val progressText = if (file.progress < 1) {
                    progress.floorToDecimal(1).toString()
                } else {
                    "100"
                }
                binding.textDetails.text = context.getString(
                    R.string.torrent_files_details_format,
                    formatFilePriority(context, file.priority),
                    formatBytes(context, downloadedSize),
                    formatBytes(context, file.size),
                    progressText
                )
            } else {
                val properties = fileNode.getFolderProperties()

                val progress = properties.progressSum / properties.fileCount
                val downloadedSize = (progress * properties.size).toLong()

                val progressText = if (progress < 1) {
                    (progress * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }

                val priority = properties.priority
                val priorityText = if (priority != null) {
                    formatFilePriority(context, priority)
                } else {
                    context.getString(R.string.torrent_files_priority_mixed)
                }

                binding.progressIndicator.progress = (progress * 100).toInt()
                binding.textDetails.text = context.getString(
                    R.string.torrent_files_details_format,
                    priorityText,
                    formatBytes(context, downloadedSize),
                    formatBytes(context, properties.size),
                    progressText
                )
            }
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentFileNode>() {
        override fun areItemsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name &&
            oldItem.file?.priority == newItem.file?.priority && oldItem.file?.progress == newItem.file?.progress &&
            oldItem.file?.size == newItem.file?.size && oldItem.getFolderProperties() == newItem.getFolderProperties()
    }
}
