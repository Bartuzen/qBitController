package dev.bartuzen.qbitcontroller.ui.torrent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers.TorrentPeersFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces.TorrentPiecesFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds.TorrentWebSeedsFragment
import javax.inject.Inject

@AndroidEntryPoint
class TorrentActivity : AppCompatActivity() {
    object Extras {
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"

        const val TORRENT_DELETED = "dev.bartuzen.qbitcontroller.TORRENT_DELETED"
    }

    private lateinit var binding: ActivityTorrentBinding

    @Inject
    lateinit var serverManager: ServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)

        if (serverId == -1 || torrentHash == null) {
            finish()
            return
        }

        val serverConfig = serverManager.getServerOrNull(serverId)
        if (serverConfig == null) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = serverConfig.name ?: getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 6

            override fun createFragment(position: Int) = when (position) {
                0 -> TorrentOverviewFragment(serverId, torrentHash)
                1 -> TorrentFilesFragment(serverId, torrentHash)
                2 -> TorrentPiecesFragment(serverId, torrentHash)
                3 -> TorrentTrackersFragment(serverId, torrentHash)
                4 -> TorrentPeersFragment(serverId, torrentHash)
                5 -> TorrentWebSeedsFragment(serverId, torrentHash)
                else -> Fragment()
            }
        }.apply {
            binding.viewPager.offscreenPageLimit = itemCount - 1
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val resId = when (position) {
                0 -> R.string.tab_torrent_overview
                1 -> R.string.tab_torrent_files
                2 -> R.string.tab_torrent_pieces
                3 -> R.string.tab_torrent_trackers
                4 -> R.string.tab_torrent_peers
                5 -> R.string.tab_torrent_web_seeds
                else -> return@TabLayoutMediator
            }

            tab.text = getString(resId)
        }.attach()
    }
}
