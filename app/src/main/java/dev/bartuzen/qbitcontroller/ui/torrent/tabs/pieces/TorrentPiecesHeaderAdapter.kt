package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPieceHeaderBinding
import dev.bartuzen.qbitcontroller.utils.formatBytes

class TorrentPiecesHeaderAdapter : RecyclerView.Adapter<TorrentPiecesHeaderAdapter.ViewHolder>() {
    private var pieceCount: Int? = null
    private var pieceSize: Long? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentPieceHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount() = 1

    fun submitHeaderData(pieceCount: Int?, pieceSize: Long?) {
        val isChanged = this.pieceCount != pieceCount || this.pieceSize != pieceSize
        if (isChanged) {
            this.pieceCount = pieceCount
            this.pieceSize = pieceSize
            notifyItemChanged(0)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentPieceHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.textPieces.text = pieceCount?.toString() ?: "-"

            val pieceSize = pieceSize
            binding.textPieceSize.text = if (pieceSize != null) {
                formatBytes(binding.root.context, pieceSize)
            } else {
                "-"
            }
        }
    }
}
