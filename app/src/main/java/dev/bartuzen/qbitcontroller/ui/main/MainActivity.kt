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
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.commit
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.BuildConfig
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogAboutBinding
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragment
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private val drawerAdapter = ConcatAdapter()

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
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                    R.id.menu_about -> {
                        showAboutDialog()
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
            this,
            binding.layoutDrawer,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.layoutDrawer.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        val serverListAdapter = ServerListAdapter(
            onClick = { serverConfig ->
                binding.layoutDrawer.close()
                viewModel.setCurrentServer(serverConfig)
            },
            onLongClick = { serverConfig ->
                binding.layoutDrawer.close()
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(SettingsActivity.Extras.EDIT_SERVER_CONFIG, serverConfig)
                }
                startActivity(intent)
            }
        )

        val addServerAdapter = AddServerAdapter(
            onClick = {
                binding.layoutDrawer.close()
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(SettingsActivity.Extras.MOVE_TO_ADD_SERVER, true)
                }
                startActivity(intent)
            }
        )

        drawerAdapter.addAdapter(serverListAdapter)
        drawerAdapter.addAdapter(addServerAdapter)

        binding.recyclerDrawer.adapter = drawerAdapter

        binding.textClickToAddServer.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SettingsActivity.Extras.MOVE_TO_ADD_SERVER, true)
            }
            startActivity(intent)
        }

        viewModel.serversFlow.launchAndCollectLatestIn(this) { serverList ->
            serverListAdapter.submitList(serverList.values.toList())

            binding.textClickToAddServer.visibility = if (serverList.isEmpty()) View.VISIBLE else View.GONE
            addServerAdapter.isVisible = serverList.isEmpty()
        }

        viewModel.currentServer.launchAndCollectLatestIn(this) { serverConfig ->
            serverListAdapter.selectedServerId = serverConfig?.id ?: -1
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container) as? TorrentListFragment?

            supportActionBar?.title = serverConfig?.name ?: getString(R.string.app_name)

            if (serverConfig == null) {
                if (currentFragment != null) {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        remove(currentFragment)
                    }
                }
            } else if (currentFragment?.serverConfig != serverConfig) {
                val fragment = TorrentListFragment(serverConfig)
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.container, fragment)
                }
            }
        }
    }

    private fun showAboutDialog() {
        showDialog(DialogAboutBinding::inflate) { binding ->
            binding.textVersion.text = BuildConfig.VERSION_NAME

            setTitle(R.string.about_dialog_title)
            setPositiveButton()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (binding.layoutDrawer.isOpen) {
            onDrawerBackPressedCallback.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.recyclerDrawer.adapter = null
    }

    fun submitAdapter(adapter: RecyclerView.Adapter<*>) {
        drawerAdapter.addAdapter(adapter)
    }

    fun removeAdapter(adapter: RecyclerView.Adapter<*>) {
        drawerAdapter.removeAdapter(adapter)
        binding.recyclerDrawer.adapter = null
        binding.recyclerDrawer.adapter = drawerAdapter
    }
}
