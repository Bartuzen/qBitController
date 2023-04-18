package dev.bartuzen.qbitcontroller.ui.search.result

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemSearchResultBinding
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.utils.formatBytes

class SearchResultAdapter : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    private var results: List<Search.Result> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount() = results.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitResults(results: List<Search.Result>) {
        this.results = results
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
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
}
