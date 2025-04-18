package dev.bartuzen.qbitcontroller.ui.torrent

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers.TorrentPeersFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds.TorrentWebSeedsFragment
import dev.bartuzen.qbitcontroller.utils.applySafeDrawingInsets

@AndroidEntryPoint
class TorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val TORRENT_NAME = "dev.bartuzen.qbitcontroller.TORRENT_NAME"

        const val TORRENT_DELETED = "dev.bartuzen.qbitcontroller.TORRENT_DELETED"
    }

    private lateinit var binding: ActivityTorrentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.layoutAppBar.applySafeDrawingInsets(bottom = false)

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val torrentName = intent.getStringExtra(Extras.TORRENT_NAME)

        if (serverId == -1 || torrentHash == null) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 5

            override fun createFragment(position: Int) = when (position) {
                0 -> TorrentOverviewFragment(serverId, torrentHash, torrentName)
                1 -> TorrentFilesFragment(serverId, torrentHash)
                2 -> TorrentTrackersFragment(serverId, torrentHash)
                3 -> TorrentPeersFragment(serverId, torrentHash)
                4 -> TorrentWebSeedsFragment(serverId, torrentHash)
                else -> Fragment()
            }
        }.apply {
            binding.viewPager.offscreenPageLimit = itemCount - 1
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val resId = when (position) {
                0 -> R.string.torrent_tab_overview
                1 -> R.string.torrent_tab_files
                2 -> R.string.torrent_tab_trackers
                3 -> R.string.torrent_tab_peers
                4 -> R.string.torrent_tab_web_seeds
                else -> return@TabLayoutMediator
            }

            tab.text = getString(resId)
        }.attach()
    }
}
