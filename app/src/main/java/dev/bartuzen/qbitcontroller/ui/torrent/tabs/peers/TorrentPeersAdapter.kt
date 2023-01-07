package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPeerBinding
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import java.util.Locale

class TorrentPeersAdapter : ListAdapter<TorrentPeer, TorrentPeersAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentPeerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    class ViewHolder(private val binding: ItemTorrentPeerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(peer: TorrentPeer) {
            val context = binding.root.context

            binding.textName.text = context.getString(R.string.torrent_peers_ip_format, peer.ip, peer.port)

            val countryName = Locale("", peer.countryCode).getDisplayCountry(
                Locale(context.getString(R.string.language_code))
            )
            binding.textCountry.text = context.getString(R.string.torrent_peers_country, countryName)

            binding.textConnection.text = context.getString(R.string.torrent_peers_connection, peer.connection)

            val flags = peer.flags.joinToString(" ", transform = { it.flag }).ifEmpty { "-" }
            binding.textFlags.text = context.getString(R.string.torrent_peers_flags, flags)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TorrentPeer>() {
        override fun areItemsTheSame(oldItem: TorrentPeer, newItem: TorrentPeer) =
            oldItem.ip == newItem.ip && oldItem.port == newItem.port

        override fun areContentsTheSame(oldItem: TorrentPeer, newItem: TorrentPeer) =
            oldItem.ip == newItem.ip && oldItem.port == newItem.port && oldItem.countryCode == newItem.countryCode &&
                oldItem.connection == newItem.connection && oldItem.flags == newItem.flags
    }
}
