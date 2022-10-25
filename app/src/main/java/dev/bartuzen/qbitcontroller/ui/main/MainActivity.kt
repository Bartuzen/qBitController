package dev.bartuzen.qbitcontroller.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragment
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragmentBuilder
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val onDrawerBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.layoutDrawer.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_settings -> {
                        startActivity(
                            Intent(this@MainActivity, SettingsActivity::class.java)
                        )
                    }
                    else -> return false
                }
                return true
            }
        })

        onBackPressedDispatcher.addCallback(this, onDrawerBackPressedCallback)
        binding.layoutDrawer.addDrawerListener(object : DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                onDrawerBackPressedCallback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                onDrawerBackPressedCallback.isEnabled = false
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerStateChanged(newState: Int) {}
        })

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, binding.layoutDrawer, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.layoutDrawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        val adapter = ServerListAdapter(object : ServerListAdapter.OnItemClickListener {
            override fun onClick(serverConfig: ServerConfig) {
                binding.layoutDrawer.closeDrawer(GravityCompat.START)
                viewModel.setCurrentServer(serverConfig)
            }
        })
        binding.recyclerServerList.adapter = adapter
        binding.recyclerServerList.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        viewModel.serverList.launchAndCollectLatestIn(this) { serverList ->
            adapter.submitList(serverList.values.toList())
        }

        viewModel.currentServer.launchAndCollectLatestIn(this) { serverConfig ->
            adapter.selectedServerId = serverConfig?.id ?: -1
            val currentFragment =
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (binding.layoutDrawer.isOpen) {
            onDrawerBackPressedCallback.isEnabled = true
        }
    }
}