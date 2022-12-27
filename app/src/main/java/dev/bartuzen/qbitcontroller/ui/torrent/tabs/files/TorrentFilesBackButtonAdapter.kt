package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFilesBackButtonBinding

class TorrentFilesBackButtonAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<TorrentFilesBackButtonAdapter.ViewHolder>() {
    var currentDirectory: String? = null
        set(value) {
            if (field == null && value != null) {
                field = value
                notifyItemInserted(0)
            } else if (field != null && value == null) {
                field = value
                notifyItemRemoved(0)
            } else if (field != value) {
                field = value
                notifyItemChanged(0)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentFilesBackButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    inner class ViewHolder(private val binding: ItemTorrentFilesBackButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.layoutBackButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClick()
                }
            }
        }

        fun bind() {
            val context = binding.root.context
            binding.textDirectory.text =
                context.getString(R.string.torrent_files_directory_format, currentDirectory).parseAsHtml()
        }
    }

    override fun getItemCount() = if (currentDirectory != null) 1 else 0
}
