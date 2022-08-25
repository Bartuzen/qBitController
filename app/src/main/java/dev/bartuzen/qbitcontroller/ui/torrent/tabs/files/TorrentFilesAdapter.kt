package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemFileBinding
import dev.bartuzen.qbitcontroller.model.TorrentFileNode

class TorrentFilesAdapter(private val listener: OnItemClickListener? = null) :
    ListAdapter<TorrentFileNode, TorrentFilesAdapter.ViewHolder>(DiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { file ->
            holder.bind(file)
        }
    }

    inner class ViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { file ->
                        listener?.onClick(file)
                    }
                }
            }
        }

        fun bind(file: TorrentFileNode) {
            binding.textName.text = file.name

            val iconId = if (file.isFile) R.drawable.ic_file else R.drawable.ic_folder
            binding.imageIcon.setImageDrawable(
                AppCompatResources.getDrawable(binding.root.context, iconId)
            )
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentFileNode>() {
        override fun areItemsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) =
            oldItem.name == newItem.name
    }

    interface OnItemClickListener {
        fun onClick(file: TorrentFileNode)
    }
}