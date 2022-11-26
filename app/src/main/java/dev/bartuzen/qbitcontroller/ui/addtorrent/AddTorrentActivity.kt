package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
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
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityAddTorrentBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelable
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.showToast
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

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

            if (servers.size == 1) {
                serverConfig = servers.first()
            } else {
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
            }
        }

        val uri = intent.data
        val fileUri = when (uri?.scheme) {
            "http", "https", "magnet" -> {
                binding.editTorrentLink.setText(uri.toString())
                null
            }
            "content", "file" -> {
                binding.layoutFileName.visibility = View.VISIBLE
                binding.inputLayoutTorrentLink.visibility = View.GONE
                binding.textFileName.text = Uri.decode(uri.path)?.split("/")?.last()
                uri
            }
            else -> null
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setTitle(R.string.torrent_add_title)
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
                    val downloadSpeedLimit =
                        binding.editDlspeedLimit.text.toString().toDoubleOrNull().let { limit ->
                            if (limit != null) {
                                (limit * 1024).roundToInt()
                            } else {
                                null
                            }
                        }
                    val uploadSpeedLimit =
                        binding.editUpspeedLimit.text.toString().toDoubleOrNull().let { limit ->
                            if (limit != null) {
                                (limit * 1024).roundToInt()
                            } else {
                                null
                            }
                        }

                    if (links.isNotBlank() || fileUri != null) {
                        val category = binding.chipGroupCategory.checkedChipId.let { id ->
                            if (id != View.NO_ID) {
                                binding.chipGroupCategory.findViewById<Chip>(id).text.toString()
                            } else null
                        }

                        val tags = mutableListOf<String>()
                        binding.chipGroupTag.checkedChipIds.forEach { id ->
                            tags.add(binding.chipGroupTag.findViewById<Chip>(id).text.toString())
                        }

                        viewModel.createTorrent(
                            serverConfig = serverConfig,
                            links = if (fileUri == null) links.split("\n") else null,
                            fileUri = fileUri,
                            category = category,
                            tags = tags,
                            torrentName = binding.editTorrentName.text.toString().ifBlank { null },
                            downloadSpeedLimit = downloadSpeedLimit,
                            uploadSpeedLimit = uploadSpeedLimit,
                            ratioLimit = binding.editRatioLimit.text.toString().toDoubleOrNull(),
                            seedingTimeLimit = binding.editSeedingTimeLimit.text.toString()
                                .toIntOrNull(),
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

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadCategoryAndTags(serverConfig)
        }

        viewModel.isCreating.launchAndCollectLatestIn(this) { isCreating ->
            binding.progressIndicator.visibility = if (isCreating) View.VISIBLE else View.INVISIBLE
        }

        viewModel.isLoading.launchAndCollectLatestIn(this) { isLoading ->
            if (isLoading) {
                binding.progressCategory.visibility = View.VISIBLE
                binding.progressTag.visibility = View.VISIBLE
            } else {
                binding.progressCategory.visibility = View.GONE
                binding.progressTag.visibility = View.GONE
            }
        }

        viewModel.categoryList.filterNotNull().launchAndCollectLatestIn(this) { categoryList ->
            binding.chipGroupCategory.removeAllViews()

            categoryList.forEach { category ->
                val chip = Chip(this@AddTorrentActivity)
                chip.text = category
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_category)
                chip.ellipsize = TextUtils.TruncateAt.END
                chip.isCheckable = true
                binding.chipGroupCategory.addView(chip)
            }
        }

        viewModel.tagList.filterNotNull().launchAndCollectLatestIn(this) { tagList ->
            binding.chipGroupTag.removeAllViews()

            tagList.forEach { tag ->
                val chip = Chip(this@AddTorrentActivity)
                chip.text = tag
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_tag)
                chip.isCheckable = true
                chip.ellipsize = TextUtils.TruncateAt.END
                binding.chipGroupTag.addView(chip)
            }
        }

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
