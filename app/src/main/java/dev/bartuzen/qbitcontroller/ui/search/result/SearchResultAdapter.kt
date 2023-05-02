package dev.bartuzen.qbitcontroller.ui.search.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemSearchResultBinding
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.utils.formatBytes
import okhttp3.internal.Util.verifyAsIpAddress
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import java.net.URI

class SearchResultAdapter(
    private val onClick: (searchResult: Search.Result) -> Unit
) : ListAdapter<Search.Result, SearchResultAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { result ->
            holder.bind(result)
        }
    }

    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { result ->
                        onClick(result)
                    }
                }
            }
        }

        fun bind(result: Search.Result) {
            val context = binding.root.context

            binding.textTorrentName.text = result.fileName

            binding.textSize.text = context.getString(
                R.string.search_torrent_size,
                if (result.fileSize != null) {
                    formatBytes(context, result.fileSize)
                } else {
                    "-"
                }
            )

            val site = try {
                val host = URI.create(result.siteUrl).host ?: throw IllegalArgumentException()
                if (verifyAsIpAddress(host)) {
                    host
                } else {
                    PublicSuffixDatabase.get().getEffectiveTldPlusOne(host)
                }
            } catch (_: IllegalArgumentException) {
                null
            } ?: result.siteUrl
            binding.textEngine.text = context.getString(R.string.search_torrent_site, site)

            binding.textSeeders.text = context.getString(
                R.string.search_torrent_seeders,
                result.seeders?.toString() ?: "-"
            )
            binding.textLeechers.text = context.getString(
                R.string.search_torrent_leechers,
                result.leechers?.toString() ?: "-"
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Search.Result>() {
        override fun areItemsTheSame(oldItem: Search.Result, newItem: Search.Result) = oldItem.fileUrl == newItem.fileUrl

        override fun areContentsTheSame(oldItem: Search.Result, newItem: Search.Result) =
            oldItem.fileName == newItem.fileName && oldItem.siteUrl == newItem.siteUrl &&
                oldItem.fileSize == newItem.fileSize && oldItem.seeders == newItem.seeders &&
                oldItem.leechers == newItem.leechers
    }
}
