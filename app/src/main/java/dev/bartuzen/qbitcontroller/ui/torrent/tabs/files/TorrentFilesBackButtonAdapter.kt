package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFileBinding

class TorrentFilesBackButtonAdapter(
    private val onClick: () -> Unit
) :
    RecyclerView.Adapter<TorrentFilesBackButtonAdapter.ViewHolder>() {
    var isVisible = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    notifyItemInserted(0)
                } else {
                    notifyItemRemoved(0)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    inner class ViewHolder(private val binding: ItemTorrentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClick.invoke()
                }
            }
        }

        fun bind() {
            binding.textName.text = "..."
        }
    }

    override fun getItemCount() = if (isVisible) 1 else 0
}
