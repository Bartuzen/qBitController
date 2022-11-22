package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityAddTorrentBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelable
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.showToast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class AddTorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"

        const val IS_ADDED = "dev.bartuzen.qbitcontroller.IS_ADDED"
    }

    private lateinit var binding: ActivityAddTorrentBinding

    private val viewModel: AddTorrentViewModel by viewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverConfigFromIntent = intent.getParcelable<ServerConfig>(Extras.SERVER_CONFIG)
        lateinit var serverConfig: ServerConfig
        if (serverConfigFromIntent != null) {
            serverConfig = serverConfigFromIntent
        } else {
            val servers = runBlocking { viewModel.serversFlow.first().values.toList() }

            if (servers.isEmpty()) {
                showToast(R.string.torrent_add_no_server)
                finish()
                return
            }

            binding.spinnerServers.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                servers.map { server -> server.name ?: server.host }
            )
            binding.spinnerServers.setSelection(0)
            binding.spinnerServers.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        serverConfig = servers[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            binding.layoutServerSelector.visibility = View.VISIBLE

            binding.editTorrentLink.setText(intent.data.toString())
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.editTorrentLink.setOnTouchListener { _, _ ->
            binding.scrollView.requestDisallowInterceptTouchEvent(true)
            false
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.add_torrent_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                R.id.menu_add -> {
                    val links = binding.editTorrentLink.text.toString()
                    if (links.isNotBlank()) {
                        viewModel.createTorrent(
                            serverConfig = serverConfig,
                            links = links.split("\n"),
                            ratioLimit = binding.editRatioLimit.text.toString().toDoubleOrNull(),
                            isPaused = !binding.checkStartTorrent.isChecked,
                            skipHashChecking = binding.checkSkipChecking.isChecked,
                            isAutoTorrentManagementEnabled = binding.checkAutoTmm.isChecked,
                            isSequentialDownloadEnabled = binding.checkSequentialDownload.isChecked,
                            isFirstLastPiecePrioritized = binding.checkPrioritizeFirstLastPiece.isChecked
                        )
                    } else {
                        binding.inputLayoutTorrentLink.error =
                            getString(R.string.torrent_add_link_cannot_be_empty)
                    }
                    true
                }
                else -> false
            }
        })

        binding.editTorrentLink.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                if (binding.inputLayoutTorrentLink.isErrorEnabled && text?.isNotBlank() == true) {
                    binding.inputLayoutTorrentLink.isErrorEnabled = false
                }
            }
        )

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is AddTorrentViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@AddTorrentActivity, event.error))
                }
                AddTorrentViewModel.Event.TorrentCreated -> {
                    if (intent.data == null) {
                        val intent = Intent().apply {
                            putExtra(Extras.IS_ADDED, true)
                        }
                        setResult(Activity.RESULT_OK, intent)
                    } else {
                        Toast.makeText(
                            this@AddTorrentActivity, R.string.torrent_add_success, Toast.LENGTH_LONG
                        ).show()
                    }
                    finish()
                }
            }
        }
    }
}
