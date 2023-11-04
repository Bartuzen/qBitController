package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentFilterBinding
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentStatusTitleBinding
import dev.bartuzen.qbitcontroller.model.TorrentState

class TorrentFilterAdapter(
    isCollapsed: Boolean,
    private val onClick: (filter: TorrentFilter) -> Unit,
    private val onCollapse: (isCollapsed: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var filter = TorrentFilter.ALL
    private var countMap: Map<TorrentFilter, Int> = emptyMap()

    private var isCollapsed = isCollapsed
        set(value) {
            if (field != value) {
                notifyItemChanged(0, Unit)
                if (value) {
                    notifyItemRemoved(1)
                } else {
                    notifyItemInserted(1)
                }
                onCollapse(value)
                field = value
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_torrent_filter -> {
            ViewHolder(
                ItemTorrentFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        R.layout.item_torrent_status_title -> {
            TitleViewHolder(
                ItemTorrentStatusTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        else -> {
            throw IllegalStateException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.bind()
            is TitleViewHolder -> holder.bind()
        }
    }

    override fun getItemCount() = if (!isCollapsed) 2 else 1

    override fun getItemViewType(position: Int) = if (position == 0) {
        R.layout.item_torrent_status_title
    } else {
        R.layout.item_torrent_filter
    }

    fun submitCountMap(countMap: Map<TorrentFilter, Int>) {
        this.countMap = countMap
        notifyItemChanged(1, Unit)
    }

    inner class ViewHolder(private val binding: ItemTorrentFilterBinding) : RecyclerView.ViewHolder(binding.root) {
        private val views = listOf(
            Triple(binding.textAll, TorrentFilter.ALL, R.string.torrent_list_status_all),
            Triple(binding.textDownloading, TorrentFilter.DOWNLOADING, R.string.torrent_list_status_downloading),
            Triple(binding.textSeeding, TorrentFilter.SEEDING, R.string.torrent_list_status_seeding),
            Triple(binding.textCompleted, TorrentFilter.COMPLETED, R.string.torrent_list_status_completed),
            Triple(binding.textResumed, TorrentFilter.RESUMED, R.string.torrent_list_status_resumed),
            Triple(binding.textPaused, TorrentFilter.PAUSED, R.string.torrent_list_status_paused),
            Triple(binding.textActive, TorrentFilter.ACTIVE, R.string.torrent_list_status_active),
            Triple(binding.textInactive, TorrentFilter.INACTIVE, R.string.torrent_list_status_inactive),
            Triple(binding.textStalled, TorrentFilter.STALLED, R.string.torrent_list_status_stalled),
            Triple(binding.textChecking, TorrentFilter.CHECKING, R.string.torrent_list_status_checking),
            Triple(binding.textMoving, TorrentFilter.MOVING, R.string.torrent_list_status_moving),
            Triple(binding.textError, TorrentFilter.ERROR, R.string.torrent_list_status_error)
        )

        init {
            views.forEach { (view, filter) ->
                view.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        this@TorrentFilterAdapter.filter = filter
                        notifyItemChanged(1, Unit)
                        onClick(filter)
                    }
                }
            }
        }

        fun bind() {
            views.forEach { (view, filter, stringId) ->
                if (this@TorrentFilterAdapter.filter == filter) {
                    view.setBackgroundResource(R.color.torrent_status_selected_background)
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                }

                val context = binding.root.context
                view.text = context.getString(
                    R.string.torrent_list_status_format,
                    context.getString(stringId),
                    countMap[filter] ?: 0
                )
            }
        }
    }

    inner class TitleViewHolder(private val binding: ItemTorrentStatusTitleBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                isCollapsed = !isCollapsed
            }
        }

        fun bind() {
            if (isCollapsed) {
                binding.textTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_expand, 0, 0, 0)
            } else {
                binding.textTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_collapse, 0, 0, 0)
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
            TorrentState.META_DL,
            TorrentState.FORCED_META_DL,
            TorrentState.PAUSED_DL
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
            TorrentState.FORCED_META_DL,
            TorrentState.FORCED_DL,
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.QUEUED_UP,
            TorrentState.FORCED_UP
        )
    ),
    PAUSED(listOf(TorrentState.PAUSED_DL, TorrentState.PAUSED_UP)),
    ACTIVE(null),
    INACTIVE(null),
    STALLED(listOf(TorrentState.STALLED_DL, TorrentState.STALLED_UP)),
    CHECKING(listOf(TorrentState.CHECKING_DL, TorrentState.CHECKING_UP, TorrentState.CHECKING_RESUME_DATA)),
    MOVING(listOf(TorrentState.MOVING)),
    ERROR(listOf(TorrentState.ERROR, TorrentState.MISSING_FILES))
}
