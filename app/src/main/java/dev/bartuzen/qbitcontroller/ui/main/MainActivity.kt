package dev.bartuzen.qbitcontroller.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogAboutBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListFragment
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var notificationManager: AppNotificationManager

    private val drawerAdapter = ConcatAdapter()

    private val onDrawerBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.layoutDrawer.close()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private var currentServerConfig: ServerConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.navView.applySystemBarInsets(top = true, bottom = true, start = true, end = false)
        binding.layoutAppBar.applySystemBarInsets()

        setSupportActionBar(binding.toolbar)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main, menu)
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
            R.string.accessibility_open_navigation_drawer,
            R.string.accessibility_close_navigation_drawer
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
                    putExtra(SettingsActivity.Extras.EDIT_SERVER_ID, serverConfig.id)
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
            if (serverList.isNotEmpty()) {
                requestNotificationPermission()
            }
        }

        if (savedInstanceState == null) {
            val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
            if (serverId != -1) {
                viewModel.setCurrentServer(serverId)
            }
        }

        currentServerConfig = savedInstanceState?.getParcelableCompat("currentServerConfig")
        viewModel.currentServer.launchAndCollectLatestIn(this) { serverConfig ->
            serverListAdapter.selectedServerId = serverConfig?.id ?: -1

            supportActionBar?.title = serverConfig?.name ?: getString(R.string.app_name)

            if (currentServerConfig != serverConfig) {
                if (serverConfig != null) {
                    supportActionBar?.subtitle = null

                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        val fragment = TorrentListFragment(serverConfig.id)
                        replace(R.id.container, fragment)
                    }
                } else {
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.container) as? TorrentListFragment?
                    if (currentFragment != null) {
                        supportActionBar?.subtitle = null

                        supportFragmentManager.commit {
                            setReorderingAllowed(true)
                            remove(currentFragment)
                        }
                    }
                }
                currentServerConfig = serverConfig
            }
        }
    }

    private fun showAboutDialog() {
        showDialog(DialogAboutBinding::inflate) { binding ->
            binding.textVersion.text = BuildConfig.VERSION_NAME

            setTitle(R.string.main_action_about)
            setPositiveButton()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable("currentServerConfig", currentServerConfig)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (binding.layoutDrawer.isOpen) {
            onDrawerBackPressedCallback.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        notificationManager.startWorker()
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.recyclerDrawer.adapter = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        if (serverId != -1) {
            viewModel.setCurrentServer(serverId)
        }
    }

    fun submitAdapter(adapter: RecyclerView.Adapter<*>) {
        drawerAdapter.addAdapter(adapter)
    }

    fun removeAdapter(adapter: RecyclerView.Adapter<*>) {
        drawerAdapter.removeAdapter(adapter)
        binding.recyclerDrawer.adapter = null
        binding.recyclerDrawer.adapter = drawerAdapter
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
