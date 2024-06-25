package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemRssFeedBinding
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.utils.toPx

class RssFeedsAdapter(
    private val collapsedNodes: MutableSet<String>,
    private val onClick: (feed: RssFeedNode) -> Unit,
    private val onLongClick: (feed: RssFeedNode, view: View) -> Unit
) : ListAdapter<RssFeedNode, RssFeedsAdapter.ViewHolder>(DiffCallback()) {

    private var rootNode: RssFeedNode? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRssFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { node ->
            holder.bind(node)
        }
    }

    fun setNode(rootNode: RssFeedNode) {
        this.rootNode = rootNode
        submitNodes()
    }

    private fun submitNodes() {
        val result = mutableListOf<RssFeedNode>()
        rootNode?.let { node ->
            processNodes(node, result)
        }
        submitList(result)
    }

    private fun processNodes(node: RssFeedNode, result: MutableList<RssFeedNode>) {
        result.add(node)
        if (node.uniqueId !in collapsedNodes && node.children != null) {
            for (child in node.children) {
                processNodes(child, result)
            }
        }
    }

    private fun toggleNodeExpansion(node: RssFeedNode) {
        if (node.uniqueId in collapsedNodes) {
            collapsedNodes -= node.uniqueId
        } else {
            collapsedNodes += node.uniqueId
        }

        notifyItemChanged(currentList.indexOf(node), Unit)

        submitNodes()
    }

    inner class ViewHolder(private val binding: ItemRssFeedBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { node ->
                        onClick(node)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { node ->
                        onLongClick(node, binding.root)
                    }
                }
                true
            }

            binding.imageExpand.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { node ->
                        toggleNodeExpansion(node)
                    }
                }
            }
        }

        fun bind(node: RssFeedNode) {
            binding.textName.text = node.name
            val iconId = if (node.isFeed) R.drawable.ic_rss else R.drawable.ic_folder
            binding.imageIcon.setImageResource(iconId)

            val paddingStart = (node.level * 16).toPx(binding.root.context)
            binding.root.updatePaddingRelative(start = paddingStart)

            if (node.isFolder) {
                binding.imageExpand.visibility = View.VISIBLE
                val expandIconId = if (node.uniqueId in collapsedNodes) {
                    R.drawable.ic_expand
                } else {
                    R.drawable.ic_collapse
                }
                binding.imageExpand.setImageResource(expandIconId)
            } else {
                binding.imageExpand.visibility = View.INVISIBLE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RssFeedNode>() {
        override fun areItemsTheSame(oldItem: RssFeedNode, newItem: RssFeedNode) = if (oldItem.isFeed == newItem.isFeed) {
            if (oldItem.isFeed) {
                oldItem.feed?.uid == newItem.feed?.uid
            } else {
                oldItem.level == newItem.level && oldItem.name == newItem.name
            }
        } else {
            false
        }

        override fun areContentsTheSame(oldItem: RssFeedNode, newItem: RssFeedNode) =
            oldItem.level == newItem.level && oldItem.isFeed == newItem.isFeed && oldItem.name == newItem.name
    }
}
