package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagBinding
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagTitleBinding
import dev.bartuzen.qbitcontroller.utils.getColorCompat

class CategoryTagAdapter(
    private val isCategory: Boolean,
    private val onSelected: (categoryTag: CategoryTag) -> Unit,
    private val onLongClick: (name: String) -> Unit,
    private val onCreateClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<String> = emptyList()
    private var selectedItem: CategoryTag = CategoryTag.All

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_category_tag -> {
            ItemViewHolder(ItemCategoryTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        R.layout.item_category_tag_title -> {
            TitleViewHolder(
                ItemCategoryTagTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        else -> throw IllegalStateException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val categoryTag = when (position) {
                    1 -> {
                        CategoryTag.All
                    }
                    2 -> {
                        CategoryTag.Uncategorized
                    }
                    else -> {
                        CategoryTag.Item(items[position - 3])
                    }
                }

                holder.bind(categoryTag)
            }
            is TitleViewHolder -> {
                holder.bind(position == 0)
            }
        }
    }

    override fun getItemCount() = items.size + 3

    override fun getItemViewType(position: Int) = if (position == 0) {
        R.layout.item_category_tag_title
    } else {
        R.layout.item_category_tag
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<String>) {
        this.items = items

        val selectedItem = selectedItem
        if (selectedItem is CategoryTag.Item && selectedItem.name !in items) {
            this.selectedItem = CategoryTag.All
            onSelected(CategoryTag.All)
        }

        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: ItemCategoryTagBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var categoryTag: CategoryTag

        init {
            binding.root.setOnClickListener {
                val oldPosition = when (val selectedItem = selectedItem) {
                    CategoryTag.All -> 1
                    CategoryTag.Uncategorized -> 2
                    is CategoryTag.Item -> items.indexOf(selectedItem.name) + 3
                }

                this@CategoryTagAdapter.selectedItem = categoryTag

                notifyItemChanged(bindingAdapterPosition)
                notifyItemChanged(oldPosition)

                onSelected(categoryTag)
            }

            binding.root.setOnLongClickListener {
                val categoryTag = categoryTag
                if (categoryTag is CategoryTag.Item) {
                    onLongClick(categoryTag.name)
                    true
                } else {
                    false
                }
            }
        }

        fun bind(categoryTag: CategoryTag) {
            this.categoryTag = categoryTag

            val context = binding.root.context

            val backgroundColor = if (selectedItem == categoryTag) {
                context.getColorCompat(R.color.category_tag_selected_background)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setBackgroundColor(backgroundColor)

            val icon = if (isCategory) R.drawable.ic_folder else R.drawable.ic_tag

            binding.textCategoryTag.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)

            binding.textCategoryTag.text = when (categoryTag) {
                is CategoryTag.All -> {
                    context.getString(R.string.torrent_list_category_tag_all)
                }
                is CategoryTag.Uncategorized -> {
                    if (isCategory) {
                        context.getString(R.string.torrent_list_uncategorized)
                    } else {
                        context.getString(R.string.torrent_list_untagged)
                    }
                }
                is CategoryTag.Item -> {
                    categoryTag.name
                }
            }
        }
    }

    inner class TitleViewHolder(private val binding: ItemCategoryTagTitleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(isCategory: Boolean) {
            binding.textTitle.text = binding.root.context.getString(
                if (isCategory) R.string.torrent_list_categories else R.string.torrent_list_tags
            )

            binding.imageCreate.setOnClickListener {
                onCreateClick()
            }
        }
    }
}

sealed interface CategoryTag {
    object All : CategoryTag
    object Uncategorized : CategoryTag
    data class Item(val name: String) : CategoryTag
}
