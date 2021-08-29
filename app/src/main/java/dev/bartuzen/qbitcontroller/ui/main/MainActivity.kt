package dev.bartuzen.qbitcontroller.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragment
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragmentBuilder

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, binding.layoutDrawer, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.layoutDrawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        val adapter = ServerListAdapter(object : ServerListAdapter.OnItemClickListener {
            override fun onClick(serverConfig: ServerConfig) {
                binding.layoutDrawer.closeDrawer(GravityCompat.START)
                viewModel.currentServer.value = serverConfig
            }
        })
        binding.recyclerServerList.adapter = adapter
        binding.recyclerServerList.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        viewModel.serverList.observe(this) { serverList ->
            viewModel.onServerListChanged(serverList)
            adapter.submitList(serverList.values.toList())
        }

        viewModel.currentServer.observe(this) { serverConfig ->
            adapter.selectedServerId = serverConfig?.id ?: -1
            val currentFragment: TorrentListFragment? =
                supportFragmentManager.findFragmentById(R.id.container) as? TorrentListFragment?

            supportActionBar?.title = serverConfig?.name ?: getString(R.string.app_name)

            if (serverConfig == null) {
                if (currentFragment != null) {
                    supportFragmentManager.beginTransaction()
                        .remove(currentFragment)
                        .commit()
                }
            } else if (currentFragment?.serverConfig != serverConfig) {
                val fragment = TorrentListFragmentBuilder(serverConfig)
                    .build()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onBackPressed() {
        if (binding.layoutDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.layoutDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}