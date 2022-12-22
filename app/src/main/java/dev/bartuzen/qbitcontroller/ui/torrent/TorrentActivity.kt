package dev.bartuzen.qbitcontroller.ui.torrent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces.TorrentPiecesFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersFragment
import dev.bartuzen.qbitcontroller.utils.getParcelable

@AndroidEntryPoint
class TorrentActivity : AppCompatActivity() {
    object Extras {
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"

        const val TORRENT_DELETED = "dev.bartuzen.qbitcontroller.TORRENT_DELETED"
    }

    private lateinit var binding: ActivityTorrentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val serverConfig = intent.getParcelable<ServerConfig>(Extras.SERVER_CONFIG)

        if (serverConfig == null || torrentHash == null) {
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
            override fun getItemCount() = 4

            override fun createFragment(position: Int) = when (position) {
                0 -> TorrentOverviewFragment(serverConfig, torrentHash)
                1 -> TorrentFilesFragment(serverConfig, torrentHash)
                2 -> TorrentPiecesFragment(serverConfig, torrentHash)
                3 -> TorrentTrackersFragment(serverConfig, torrentHash)
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
                else -> return@TabLayoutMediator
            }

            tab.text = getString(resId)
        }.attach()
    }
}
