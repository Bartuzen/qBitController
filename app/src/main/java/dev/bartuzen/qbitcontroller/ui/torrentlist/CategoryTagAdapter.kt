package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagBinding
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagTitleBinding
import dev.bartuzen.qbitcontroller.databinding.ItemDividerBinding
import kotlin.properties.Delegates

class CategoryTagAdapter(
    private val onSelected: (isCategory: Boolean, name: String?) -> Unit,
    private val onLongClick: (isCategory: Boolean, name: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var categoryList: List<String> = emptyList()
    private var tagList: List<String> = emptyList()

    private var selectedCategory: String? = null
    private var selectedTag: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_category_tag -> {
            ItemViewHolder(
                ItemCategoryTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        R.layout.item_category_tag_title -> {
            TitleViewHolder(
                ItemCategoryTagTitleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
        R.layout.item_divider -> {
            DividerViewHolder(
                ItemDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        else -> throw IllegalStateException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val (item, isCategory) = when (position) {
                    1 -> {
                        null to true
                    }
                    in 2..categoryList.size + 2 -> {
                        categoryList[position - 2] to true
                    }
                    categoryList.size + 4 -> {
                        null to false
                    }
                    in categoryList.size + 5..categoryList.size + 5 + tagList.size -> {
                        tagList[position - categoryList.size - 5] to false
                    }
                    else -> throw IllegalStateException()
                }

                holder.bind(item, isCategory)
            }
            is TitleViewHolder -> {
                holder.bind(position == 0)
            }
        }
    }

    override fun getItemCount() = categoryList.size + tagList.size + 5

    override fun getItemViewType(position: Int) = when (position) {
        0 -> {
            R.layout.item_category_tag_title
        }
        in 1..categoryList.size + 1 -> {
            R.layout.item_category_tag
        }
        categoryList.size + 2 -> {
            R.layout.item_divider
        }
        categoryList.size + 3 -> {
            R.layout.item_category_tag_title
        }
        in categoryList.size + 4..categoryList.size + 5 + tagList.size -> {
            R.layout.item_category_tag
        }
        else -> throw IllegalStateException()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitLists(categoryList: List<String>, tagList: List<String>) {
        this.categoryList = categoryList
        this.tagList = tagList

        if (selectedCategory != null && selectedCategory !in categoryList) {
            selectedCategory = null
            onSelected(true, null)
        }
        if (selectedTag != null && selectedTag !in tagList) {
            selectedTag = null
            onSelected(false, null)
        }

        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: ItemCategoryTagBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var name: String? = null
        private var isCategory by Delegates.notNull<Boolean>()

        init {
            binding.root.setOnClickListener {
                val oldPosition = if (isCategory) {
                    if (selectedCategory == null) {
                        1
                    } else {
                        categoryList.indexOf(selectedCategory) + 2
                    }
                } else {
                    if (selectedTag == null) {
                        categoryList.size + 4
                    } else {
                        tagList.indexOf(selectedTag) + categoryList.size + 5
                    }
                }

                if (isCategory) {
                    selectedCategory = name
                } else {
                    selectedTag = name
                }

                notifyItemChanged(bindingAdapterPosition)
                notifyItemChanged(oldPosition)

                onSelected(isCategory, name)
            }

            binding.root.setOnLongClickListener {
                name?.let { name ->
                    onLongClick(isCategory, name)
                    true
                } ?: false
            }
        }

        fun bind(name: String?, isCategory: Boolean) {
            this.name = name
            this.isCategory = isCategory

            binding.root.isSelected = (if (isCategory) selectedCategory else selectedTag) == name

            binding.root.text =
                name ?: binding.root.context.getString(R.string.torrent_list_category_tag_all)
        }
    }

    inner class TitleViewHolder(private val binding: ItemCategoryTagTitleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(isCategory: Boolean) {
            binding.root.text = binding.root.context.getString(
                if (isCategory) R.string.torrent_list_categories else R.string.torrent_list_tags
            )
        }
    }

    inner class DividerViewHolder(binding: ItemDividerBinding) :
        RecyclerView.ViewHolder(binding.root)
}
