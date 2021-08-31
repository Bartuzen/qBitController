package dev.bartuzen.qbitcontroller.ui.torrent

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.TorrentOverviewFragment
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.filelist.TorrentFileListFragment
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.showToast
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class TorrentActivity : AppCompatActivity() {
    object Extras {
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val TORRENT = "dev.bartuzen.qbitcontroller.TORRENT"
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"
    }

    private lateinit var binding: ActivityTorrentBinding

    private val viewModel: TorrentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val torrent = intent.getParcelableExtra<Torrent>(Extras.TORRENT)
        val serverConfig = intent.getParcelableExtra<ServerConfig>(Extras.SERVER_CONFIG)

        if (serverConfig == null || torrentHash == null || (torrent != null && torrent.hash != torrentHash)) {
            finish()
            showToast(R.string.an_error_occurred)
            return
        }

        viewModel.torrentHash = torrentHash
        viewModel.serverConfig = serverConfig

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = serverConfig.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int) = when (position) {
                0 -> TorrentOverviewFragment()
                1 -> TorrentFileListFragment()
                else -> Fragment()
            }
        }.apply {
            binding.viewPager.offscreenPageLimit = itemCount - 1
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.tab_torrent_overview)
                1 -> tab.text = getString(R.string.tab_torrent_files)
            }
        }.attach()

        if (torrent != null) {
            viewModel.torrent.value = torrent
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentActivityEvent.collect { event ->
                when (event) {
                    is TorrentViewModel.TorrentActivityEvent.ShowMessage -> {
                        showSnackbar(event.message)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.torrent_menu, menu)
        viewModel.torrent.observe(this) { torrent ->
            if (torrent.state == TorrentState.PAUSED_DL || torrent.state == TorrentState.PAUSED_UP) {
                menu.findItem(R.id.menu_resume).isVisible = true
                menu.findItem(R.id.menu_pause).isVisible = false
            } else {
                menu.findItem(R.id.menu_pause).isVisible = true
                menu.findItem(R.id.menu_resume).isVisible = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.menu_pause -> {
                viewModel.pauseTorrent(this)
                true
            }
            R.id.menu_resume -> {
                viewModel.resumeTorrent(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}