package dev.bartuzen.qbitcontroller.ui.rss.articles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemRssArticleBinding
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.getThemeColor
import dev.bartuzen.qbitcontroller.utils.themeColors

class RssArticlesAdapter(
    private val onClick: (article: Article) -> Unit
) : ListAdapter<Article, RssArticlesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRssArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { article ->
            holder.bind(article)
        }
    }

    inner class ViewHolder(private val binding: ItemRssArticleBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { article ->
                        onClick(article)
                    }
                }
            }
        }

        fun bind(article: Article) {
            val context = binding.root.context

            val backgroundColor = context.getThemeColor(
                if (article.isRead) themeColors.colorSurfaceContainerLow else themeColors.colorSurfaceContainerHigh
            )
            binding.root.setCardBackgroundColor(backgroundColor)

            binding.textName.text = article.title
            binding.textDate.text = context.getString(R.string.rss_date, formatDate(article.date))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Article, newItem: Article) =
            oldItem.title == newItem.title && oldItem.description == newItem.description &&
                oldItem.isRead == newItem.isRead && oldItem.date == newItem.date
    }
}
