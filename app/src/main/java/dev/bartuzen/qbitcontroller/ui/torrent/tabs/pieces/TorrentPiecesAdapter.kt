package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPieceBinding
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPieceHeaderBinding
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.getColorCompat

class TorrentPiecesAdapter :
    ListAdapter<PieceState, RecyclerView.ViewHolder>(DiffCallBack()) {
    private var pieceCount: Int? = null
    private var pieceSize: Long? = null

    override fun getItemViewType(position: Int) = if (position == 0) 0 else 1

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == 0) {
        HeaderViewHolder(
            ItemTorrentPieceHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    } else {
        ViewHolder(
            ItemTorrentPieceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            (holder as HeaderViewHolder).bind()
        } else {
            getItem(position - 1)?.let { piece ->
                (holder as ViewHolder).bind(piece)
            }
        }
    }

    fun submitHeaderData(pieceCount: Int?, pieceSize: Long?) {
        this.pieceCount = pieceCount
        this.pieceSize = pieceSize
        notifyItemChanged(0)
    }

    inner class ViewHolder(private val binding: ItemTorrentPieceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(piece: PieceState) {
            val context = binding.root.context

            val colorId = if (piece == PieceState.DOWNLOADED) {
                R.color.piece_downloaded
            } else {
                R.color.piece_not_downloaded
            }
            binding.viewPiece.setBackgroundColor(context.getColorCompat(colorId))
        }
    }

    inner class HeaderViewHolder(private val binding: ItemTorrentPieceHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.textPieces.text = pieceCount?.toString() ?: "-"

            val pieceSize = pieceSize
            binding.textPieceSize.text = if (pieceSize != null) {
                formatBytes(binding.root.context, pieceSize)
            } else "-"

        }
    }

    class DiffCallBack : DiffUtil.ItemCallback<PieceState>() {
        override fun areItemsTheSame(oldItem: PieceState, newItem: PieceState) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: PieceState, newItem: PieceState) =
            oldItem == newItem
    }
}
