package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemRssFeedBinding

class RssFeedsCurrentDirectoryAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<RssFeedsCurrentDirectoryAdapter.ViewHolder>() {

    var currentDirectory: String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemChanged(0)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemRssFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount() = 1

    inner class ViewHolder(private val binding: ItemRssFeedBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClick()
                }
            }
        }

        fun bind() {
            val context = binding.root.context

            binding.textName.text = if (currentDirectory != null) {
                context.getString(R.string.rss_view_all_articles_in, currentDirectory)
            } else {
                context.getString(R.string.rss_view_all_articles)
            }

            binding.imageIcon.setImageResource(R.drawable.ic_all_folders)
        }
    }
}
