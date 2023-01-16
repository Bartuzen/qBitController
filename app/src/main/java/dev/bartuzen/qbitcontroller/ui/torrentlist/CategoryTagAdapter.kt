package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagBinding
import dev.bartuzen.qbitcontroller.databinding.ItemCategoryTagTitleBinding
import dev.bartuzen.qbitcontroller.databinding.ItemDividerBinding
import dev.bartuzen.qbitcontroller.utils.getColorCompat

class CategoryTagAdapter(
    private val onSelected: (categoryTag: CategoryTag) -> Unit,
    private val onLongClick: (isCategory: Boolean, name: String) -> Unit,
    private val onCreateClick: (isCategory: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var categoryList: List<String> = emptyList()
    private var tagList: List<String> = emptyList()

    private var selectedCategory: CategoryTag.ICategory = CategoryTag.AllCategories
    private var selectedTag: CategoryTag.ITag = CategoryTag.AllTags

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        R.layout.item_category_tag -> {
            ItemViewHolder(ItemCategoryTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        R.layout.item_category_tag_title -> {
            TitleViewHolder(
                ItemCategoryTagTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
        R.layout.item_divider -> {
            DividerViewHolder(ItemDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
        else -> throw IllegalStateException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val categoryTag = when (position) {
                    1 -> {
                        CategoryTag.AllCategories
                    }
                    2 -> {
                        CategoryTag.Uncategorized
                    }
                    in 3..categoryList.size + 3 -> {
                        CategoryTag.Category(categoryList[position - 3])
                    }
                    categoryList.size + 5 -> {
                        CategoryTag.AllTags
                    }
                    categoryList.size + 6 -> {
                        CategoryTag.Untagged
                    }
                    in categoryList.size + 7..categoryList.size + 7 + tagList.size -> {
                        CategoryTag.Tag(tagList[position - categoryList.size - 7])
                    }
                    else -> throw IllegalStateException()
                }

                holder.bind(categoryTag)
            }
            is TitleViewHolder -> {
                holder.bind(position == 0)
            }
        }
    }

    override fun getItemCount() = categoryList.size + tagList.size + 7

    override fun getItemViewType(position: Int) = when (position) {
        0 -> {
            R.layout.item_category_tag_title
        }
        in 1..categoryList.size + 2 -> {
            R.layout.item_category_tag
        }
        categoryList.size + 3 -> {
            R.layout.item_divider
        }
        categoryList.size + 4 -> {
            R.layout.item_category_tag_title
        }
        in categoryList.size + 5..categoryList.size + 6 + tagList.size -> {
            R.layout.item_category_tag
        }
        else -> throw IllegalStateException()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitLists(categoryList: List<String>, tagList: List<String>) {
        this.categoryList = categoryList
        this.tagList = tagList

        val selectedCategory = selectedCategory
        if (selectedCategory is CategoryTag.Category && selectedCategory.name !in categoryList) {
            this.selectedCategory = CategoryTag.AllCategories
            onSelected(selectedCategory)
        }

        val selectedTag = selectedTag
        if (selectedTag is CategoryTag.Tag && selectedTag.name !in tagList) {
            this.selectedTag = CategoryTag.AllTags
            onSelected(selectedTag)
        }

        notifyDataSetChanged()
    }

    inner class ItemViewHolder(private val binding: ItemCategoryTagBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var categoryTag: CategoryTag

        init {
            binding.root.setOnClickListener {
                val categoryTag = categoryTag
                val selectedCategory = selectedCategory
                val selectedTag = selectedTag

                val oldPosition = when (categoryTag) {
                    is CategoryTag.ICategory -> {
                        when (selectedCategory) {
                            CategoryTag.AllCategories -> {
                                1
                            }
                            CategoryTag.Uncategorized -> {
                                2
                            }
                            is CategoryTag.Category -> {
                                categoryList.indexOf(selectedCategory.name) + 3
                            }
                        }
                    }
                    is CategoryTag.ITag -> {
                        when (selectedTag) {
                            CategoryTag.AllTags -> {
                                categoryList.size + 5
                            }
                            CategoryTag.Untagged -> {
                                categoryList.size + 6
                            }
                            is CategoryTag.Tag -> {
                                tagList.indexOf(selectedTag.name) + categoryList.size + 7
                            }
                        }
                    }
                }

                when (categoryTag) {
                    is CategoryTag.ICategory -> {
                        this@CategoryTagAdapter.selectedCategory = categoryTag
                    }
                    is CategoryTag.ITag -> {
                        this@CategoryTagAdapter.selectedTag = categoryTag
                    }
                }

                notifyItemChanged(bindingAdapterPosition)
                notifyItemChanged(oldPosition)

                onSelected(categoryTag)
            }

            binding.root.setOnLongClickListener {
                val categoryTag = categoryTag
                if (categoryTag is CategoryTag.Item) {
                    onLongClick(categoryTag is CategoryTag.Category, categoryTag.name)
                    true
                } else {
                    false
                }
            }
        }

        fun bind(categoryTag: CategoryTag) {
            this.categoryTag = categoryTag

            val context = binding.root.context

            val isSelected = when (categoryTag) {
                is CategoryTag.ICategory -> {
                    selectedCategory == categoryTag
                }
                is CategoryTag.ITag -> {
                    selectedTag == categoryTag
                }
            }

            val backgroundColor = if (isSelected) {
                context.getColorCompat(R.color.category_tag_selected_background)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setBackgroundColor(backgroundColor)

            val icon = when (categoryTag) {
                is CategoryTag.ICategory -> R.drawable.ic_folder
                is CategoryTag.ITag -> R.drawable.ic_tag
            }
            binding.textCategoryTag.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)

            binding.textCategoryTag.text = when (categoryTag) {
                is CategoryTag.IAll -> context.getString(R.string.torrent_list_category_tag_all)
                is CategoryTag.Uncategorized -> context.getString(R.string.torrent_list_uncategorized)
                is CategoryTag.Untagged -> context.getString(R.string.torrent_list_untagged)
                is CategoryTag.Item -> categoryTag.name
            }
        }
    }

    inner class TitleViewHolder(private val binding: ItemCategoryTagTitleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(isCategory: Boolean) {
            binding.textTitle.text = binding.root.context.getString(
                if (isCategory) R.string.torrent_list_categories else R.string.torrent_list_tags
            )

            binding.imageCreate.setOnClickListener {
                onCreateClick(isCategory)
            }
        }
    }

    inner class DividerViewHolder(binding: ItemDividerBinding) :
        RecyclerView.ViewHolder(binding.root)
}

sealed interface CategoryTag {
    object AllCategories : ICategory, IAll
    object Uncategorized : ICategory, IUncategorized
    class Category(name: String) : ICategory, Item(name) {
        override fun equals(other: Any?) = other is Category && name == other.name
        override fun hashCode() = name.hashCode()
    }

    object AllTags : ITag, IAll
    object Untagged : ITag, IUncategorized
    class Tag(name: String) : ITag, Item(name) {
        override fun equals(other: Any?) = other is Tag && name == other.name
        override fun hashCode() = name.hashCode()
    }

    sealed interface ITag : CategoryTag
    sealed interface ICategory : CategoryTag

    sealed interface IAll : CategoryTag
    sealed interface IUncategorized : CategoryTag

    sealed class Item(val name: String) : CategoryTag
}
