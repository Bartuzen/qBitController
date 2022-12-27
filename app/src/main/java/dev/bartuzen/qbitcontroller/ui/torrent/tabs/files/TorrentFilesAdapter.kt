package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFileBinding
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.ui.base.MultiSelectAdapter
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatFilePriority
import dev.bartuzen.qbitcontroller.utils.getColorCompat

class TorrentFilesAdapter : MultiSelectAdapter<TorrentFileNode, String, TorrentFilesAdapter.ViewHolder>(
    diffCallBack = DiffCallBack(),
    getKey = { fileNode ->
        "${if (fileNode.isFile) 1 else 0}${fileNode.name}"
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { file ->
            holder.bind(file)
        }
    }

    fun getSelectedFiles(): List<TorrentFile> {
        val selectedNodes = currentList.filter { fileNode ->
            getKey(fileNode) in selectedItems
        }
        return getSelectedFiles(selectedNodes).map { fileNode ->
            fileNode.file!!
        }
    }

    private fun getSelectedFiles(nodes: List<TorrentFileNode>): List<TorrentFileNode> {
        val files = mutableListOf<TorrentFileNode>()
        nodes.forEach { node ->
            if (node.children == null) {
                files.add(node)
            } else {
                files.addAll(getSelectedFiles(node.children))
            }
        }
        return files
    }

    inner class ViewHolder(private val binding: ItemTorrentFileBinding) :
        MultiSelectAdapter.ViewHolder<TorrentFileNode, String>(binding.root, this) {

        fun bind(fileNode: TorrentFileNode) {
            val context = binding.root.context

            val backgroundColor = if (isItemSelected(getKey(fileNode))) {
                context.getColorCompat(R.color.selected_card_background)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setBackgroundColor(backgroundColor)

            binding.textName.text = fileNode.name

            val iconId = if (fileNode.isFile) R.drawable.ic_file else R.drawable.ic_folder
            binding.imageIcon.setImageResource(iconId)

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
        override fun areItemsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) =
            oldItem.isFile == newItem.isFile && oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name &&
            oldItem.file?.priority == newItem.file?.priority && oldItem.file?.progress == newItem.file?.progress &&
            oldItem.file?.size == newItem.file?.size && oldItem.getFolderProperties() == newItem.getFolderProperties()
    }
}
