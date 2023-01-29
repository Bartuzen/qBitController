package dev.bartuzen.qbitcontroller.ui.rss.articles

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemRssArticleBinding
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.getColorCompat

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

            val backgroundColor = if (article.isRead) {
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.TRANSPARENT)
            } else {
                context.getColorCompat(R.color.rss_read_article_background)
            }
            binding.root.setCardBackgroundColor(backgroundColor)

            binding.textName.text = article.title
            binding.textDate.text = context.getString(R.string.rss_date, formatDate(article.date))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Article, newItem: Article) =
            oldItem.title == newItem.title && oldItem.description == newItem.description && oldItem.date == newItem.date
    }
}
