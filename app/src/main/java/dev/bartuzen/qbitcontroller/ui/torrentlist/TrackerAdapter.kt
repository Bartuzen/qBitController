package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTrackerBinding
import dev.bartuzen.qbitcontroller.databinding.ItemTrackerTitleBinding
import dev.bartuzen.qbitcontroller.utils.getThemeColor
import dev.bartuzen.qbitcontroller.utils.themeColors

class TrackerAdapter(
    isCollapsed: Boolean,
    private val onSelected: (tracker: Tracker) -> Unit,
    private val onCollapse: (isCollapsed: Boolean) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var trackers: List<Tracker> = emptyList()

    private var selectedTracker: Tracker = Tracker.All

    private var allCount = 0
    private var trackerlessCount = 0

    private var isCollapsed = isCollapsed
        set(value) {
            if (field != value) {
                notifyItemChanged(0, Unit)
                if (value) {
                    notifyItemRangeRemoved(1, trackers.size + 2)
                } else {
                    notifyItemRangeInserted(1, trackers.size + 2)
                }
                onCollapse(value)
                field = value
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_tracker_title -> {
            TitleViewHolder(ItemTrackerTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        R.layout.item_tracker -> {
            ViewHolder(ItemTrackerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        else -> {
            throw IllegalStateException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val tracker = when (position) {
                    1 -> Tracker.All
                    2 -> Tracker.Trackerless
                    else -> trackers[position - 3]
                }

                holder.bind(tracker)
            }
            is TitleViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun getItemCount() = if (!isCollapsed) trackers.size + 3 else 1

    override fun getItemViewType(position: Int) = if (position == 0) R.layout.item_tracker_title else R.layout.item_tracker

    @SuppressLint("NotifyDataSetChanged")
    fun submitTrackers(trackers: Map<String, List<String>>, allCount: Int, trackerlessCount: Int) {
        this.allCount = allCount
        this.trackerlessCount = trackerlessCount

        this.trackers = trackers.map { (tracker, torrentHashes) ->
            Tracker.Named(tracker, torrentHashes)
        }

        val selectedTracker = selectedTracker
        if (selectedTracker is Tracker.Named && selectedTracker.name !in trackers) {
            this.selectedTracker = Tracker.All
            onSelected(Tracker.All)
        }

        notifyDataSetChanged()
    }

    inner class TitleViewHolder(private val binding: ItemTrackerTitleBinding) : RecyclerView.ViewHolder(binding.root) {
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

    inner class ViewHolder(private val binding: ItemTrackerBinding) : RecyclerView.ViewHolder(binding.root) {
        var tracker: Tracker? = null

        init {
            binding.root.setOnClickListener {
                val tracker = tracker ?: return@setOnClickListener

                val oldPosition = when (selectedTracker) {
                    Tracker.All -> 1
                    Tracker.Trackerless -> 2
                    is Tracker.Named -> trackers.indexOf(selectedTracker) + 3
                }
                val newPosition = when (tracker) {
                    Tracker.All -> 1
                    Tracker.Trackerless -> 2
                    is Tracker.Named -> trackers.indexOf(tracker) + 3
                }

                selectedTracker = tracker

                notifyItemChanged(oldPosition)
                notifyItemChanged(newPosition)

                onSelected(tracker)
            }
        }

        fun bind(tracker: Tracker) {
            this.tracker = tracker
            val context = binding.root.context

            val backgroundColor = if (selectedTracker == tracker) {
                context.getThemeColor(themeColors.colorPrimaryVariant, alpha = 35)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setBackgroundColor(backgroundColor)

            val (name, count) = when (tracker) {
                Tracker.All -> context.getString(R.string.torrent_list_trackers_all) to allCount
                Tracker.Trackerless -> context.getString(R.string.torrent_list_trackers_trackerless) to trackerlessCount
                is Tracker.Named -> tracker.name to tracker.torrentHashes.size
            }

            binding.textTracker.text = context.getString(R.string.torrent_list_trackers_format, name, count)
        }
    }
}

sealed interface Tracker {
    data object All : Tracker
    data object Trackerless : Tracker
    data class Named(val name: String, val torrentHashes: List<String>) : Tracker
}
