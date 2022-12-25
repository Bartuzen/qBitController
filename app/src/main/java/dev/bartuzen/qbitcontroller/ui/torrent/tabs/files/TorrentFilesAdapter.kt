package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.View
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

            fileNode.file?.let { file ->
                binding.progressIndicator.visibility = View.VISIBLE
                binding.textDetails.visibility = View.VISIBLE

                val progress = file.progress * 100

                binding.progressIndicator.progress = progress.toInt()

                val context = binding.root.context
                val downloadedSize = (file.progress * file.size).toLong()
                val progressText = if (file.progress < 1) {
                    progress.floorToDecimal(1).toString()
                } else {
                    "100"
                }
                binding.textDetails.text = context.getString(
                    R.string.torrent_file_details_format,
                    formatFilePriority(context, file.priority),
                    formatBytes(context, downloadedSize),
                    formatBytes(context, file.size),
                    progressText
                )
            }
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentFileNode>() {
        override fun areItemsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name &&
            oldItem.file?.priority == newItem.file?.priority && oldItem.file?.progress == newItem.file?.progress &&
            oldItem.file?.size == newItem.file?.size
    }
}
