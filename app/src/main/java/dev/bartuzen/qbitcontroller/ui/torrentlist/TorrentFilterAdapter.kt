package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFilterBinding
import dev.bartuzen.qbitcontroller.model.TorrentState

class TorrentFilterAdapter(
    private val onClick: (filter: TorrentFilter) -> Unit
) : RecyclerView.Adapter<TorrentFilterAdapter.ViewHolder>() {
    private var filter = TorrentFilter.ALL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount() = 1

    inner class ViewHolder(binding: ItemTorrentFilterBinding) : RecyclerView.ViewHolder(binding.root) {
        private val views = mapOf(
            binding.textAll to TorrentFilter.ALL,
            binding.textDownloading to TorrentFilter.DOWNLOADING,
            binding.textSeeding to TorrentFilter.SEEDING,
            binding.textCompleted to TorrentFilter.COMPLETED,
            binding.textResumed to TorrentFilter.RESUMED,
            binding.textPaused to TorrentFilter.PAUSED,
            binding.textStalled to TorrentFilter.STALLED,
            binding.textChecking to TorrentFilter.CHECKING,
            binding.textMoving to TorrentFilter.MOVING,
            binding.textError to TorrentFilter.ERROR
        )

        init {
            views.forEach { (view, filter) ->
                view.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        this@TorrentFilterAdapter.filter = filter
                        notifyItemChanged(0, Unit)
                        onClick(filter)
                    }
                }
            }
        }

        fun bind() {
            views.forEach { (view, filter) ->
                if (this@TorrentFilterAdapter.filter == filter) {
                    view.setBackgroundResource(R.color.torrent_status_selected_background)
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
}

enum class TorrentFilter(val states: List<TorrentState>?) {
    ALL(null),
    DOWNLOADING(
        listOf(
            TorrentState.DOWNLOADING,
            TorrentState.CHECKING_DL,
            TorrentState.STALLED_DL,
            TorrentState.FORCED_DL,
            TorrentState.QUEUED_DL,
            TorrentState.META_DL
        )
    ),
    SEEDING(
        listOf(
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.FORCED_UP,
            TorrentState.QUEUED_UP
        )
    ),
    COMPLETED(
        listOf(
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.FORCED_UP,
            TorrentState.QUEUED_UP,
            TorrentState.PAUSED_UP
        )
    ),
    RESUMED(
        listOf(
            TorrentState.DOWNLOADING,
            TorrentState.CHECKING_DL,
            TorrentState.STALLED_DL,
            TorrentState.QUEUED_DL,
            TorrentState.META_DL,
            TorrentState.FORCED_DL,
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.QUEUED_UP,
            TorrentState.FORCED_UP
        )
    ),
    PAUSED(listOf(TorrentState.PAUSED_DL, TorrentState.PAUSED_UP)),
    STALLED(listOf(TorrentState.STALLED_DL, TorrentState.STALLED_UP)),
    CHECKING(listOf(TorrentState.CHECKING_DL, TorrentState.CHECKING_UP, TorrentState.CHECKING_RESUME_DATA)),
    MOVING(listOf(TorrentState.MOVING)),
    ERROR(listOf(TorrentState.ERROR, TorrentState.MISSING_FILES))
}
