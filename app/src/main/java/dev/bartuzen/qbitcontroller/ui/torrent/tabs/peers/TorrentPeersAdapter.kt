package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ItemTorrentPeerBinding
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.ui.base.MultiSelectAdapter
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import java.util.Locale

class TorrentPeersAdapter(
    private val loadImage: (imageView: ImageView, countryCode: String) -> Unit,
) : MultiSelectAdapter<TorrentPeer, String, TorrentPeersAdapter.ViewHolder>(
    diffCallBack = DiffCallback(),
    getKey = { peer ->
        "${peer.ip}:${peer.port}"
    },
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTorrentPeerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    inner class ViewHolder(private val binding: ItemTorrentPeerBinding) :
        MultiSelectAdapter.ViewHolder<TorrentPeer, String>(binding.root, this) {

        fun bind(peer: TorrentPeer) {
            val context = binding.root.context

            binding.root.isChecked = isItemSelected(getKey(peer))

            binding.textName.text = context.getString(R.string.torrent_peers_ip_format, peer.ip, peer.port)

            val speedList = mutableListOf<String>()
            if (peer.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(context, peer.downloadSpeed.toLong())}")
            }
            if (peer.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(context, peer.uploadSpeed.toLong())}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")

            if (peer.countryCode != null) {
                val countryName = Locale("", peer.countryCode).getDisplayCountry(
                    Locale(context.getString(R.string.language_code)),
                )
                binding.textCountry.text = context.getString(R.string.torrent_peers_country, countryName)
                binding.textCountry.visibility = View.VISIBLE

                loadImage(binding.imageFlag, peer.countryCode)
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

        override fun areContentsTheSame(oldItem: TorrentPeer, newItem: TorrentPeer) = oldItem.ip == newItem.ip &&
            oldItem.port == newItem.port &&
            oldItem.countryCode == newItem.countryCode &&
            oldItem.connection == newItem.connection &&
            oldItem.flags == newItem.flags &&
            oldItem.downloadSpeed == newItem.downloadSpeed &&
            oldItem.uploadSpeed == newItem.uploadSpeed
    }
}
