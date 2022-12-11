package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPieceBinding
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.utils.getColorCompat

class TorrentPiecesAdapter : RecyclerView.Adapter<TorrentPiecesAdapter.ViewHolder>() {
    private var pieces: List<PieceState> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentPieceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: TorrentPiecesAdapter.ViewHolder, position: Int) {
        holder.bind(pieces[position])
    }

    override fun getItemCount() = pieces.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitPieces(pieces: List<PieceState>) {
        this.pieces = pieces
        notifyDataSetChanged()
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
}
