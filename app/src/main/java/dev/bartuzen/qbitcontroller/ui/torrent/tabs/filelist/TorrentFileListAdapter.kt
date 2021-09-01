package dev.bartuzen.qbitcontroller.ui.torrent.tabs.filelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemFileBinding
import dev.bartuzen.qbitcontroller.model.TorrentFile

class TorrentFileListAdapter(private val listener: OnItemClickListener? = null) :
    ListAdapter<TorrentFile, TorrentFileListAdapter.ViewHolder>(DiffCallBack()) {
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
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition)?.let { file ->
                        listener?.onClick(file)
                    }
                }
            }
        }

        fun bind(file: TorrentFile) {
            binding.textName.text = file.name
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentFile>() {
        override fun areItemsTheSame(oldItem: TorrentFile, newItem: TorrentFile) =
            oldItem.index == newItem.index

        override fun areContentsTheSame(oldItem: TorrentFile, newItem: TorrentFile) =
            oldItem.name == newItem.name
    }

    interface OnItemClickListener {
        fun onClick(file: TorrentFile)
    }
}