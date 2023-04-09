package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.color.MaterialColors
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPeerBinding
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.ui.base.MultiSelectAdapter
import dev.bartuzen.qbitcontroller.utils.getColorCompat
import java.util.Locale

class TorrentPeersAdapter : MultiSelectAdapter<TorrentPeer, String, TorrentPeersAdapter.ViewHolder>(
    diffCallBack = DiffCallback(),
    getKey = { peer ->
        "${peer.ip}:${peer.port}"
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemTorrentPeerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentPeerBinding) :
        MultiSelectAdapter.ViewHolder<TorrentPeer, String>(binding.root, this) {

        fun bind(peer: TorrentPeer) {
            val context = binding.root.context

            val backgroundColor = if (isItemSelected(getKey(peer))) {
                context.getColorCompat(R.color.selected_card_background)
            } else {
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.TRANSPARENT)
            }
            binding.root.setCardBackgroundColor(backgroundColor)

            binding.textName.text = context.getString(R.string.torrent_peers_ip_format, peer.ip, peer.port)

            if (peer.countryCode != null) {
                val countryName = Locale("", peer.countryCode).getDisplayCountry(
                    Locale(context.getString(R.string.language_code))
                )
                binding.textCountry.text = context.getString(R.string.torrent_peers_country, countryName)
                binding.textCountry.visibility = View.VISIBLE
            } else {
                binding.textCountry.visibility = View.GONE
            }

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
