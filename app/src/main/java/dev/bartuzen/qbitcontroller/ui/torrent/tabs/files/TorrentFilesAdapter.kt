package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFileBinding
import dev.bartuzen.qbitcontroller.model.TorrentFileNode

class TorrentFilesAdapter(
    private val onClick: (file: TorrentFileNode) -> Unit
) :
    ListAdapter<TorrentFileNode, TorrentFilesAdapter.ViewHolder>(DiffCallBack()) {

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

        fun bind(file: TorrentFileNode) {
            binding.textName.text = file.name

            val iconId = if (file.isFile) R.drawable.ic_file else R.drawable.ic_folder
            binding.imageIcon.setImageDrawable(
                AppCompatResources.getDrawable(binding.root.context, iconId)
            )
        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<TorrentFileNode>() {
        override fun areItemsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TorrentFileNode, newItem: TorrentFileNode) = oldItem.name == newItem.name
    }
}
