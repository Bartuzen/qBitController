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
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesFragmentBuilder
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewFragmentBuilder
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces.TorrentPiecesFragmentBuilder
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersFragmentBuilder
import dev.bartuzen.qbitcontroller.utils.getParcelable
import dev.bartuzen.qbitcontroller.utils.showToast

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
            showToast(R.string.an_error_occurred)
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = serverConfig.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4

            override fun createFragment(position: Int) = when (position) {
                0 -> TorrentOverviewFragmentBuilder(serverConfig, torrentHash).build()
                1 -> TorrentFilesFragmentBuilder(serverConfig, torrentHash).build()
                2 -> TorrentPiecesFragmentBuilder(serverConfig, torrentHash).build()
                3 -> TorrentTrackersFragmentBuilder(serverConfig, torrentHash).build()
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