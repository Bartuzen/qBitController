package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val serverConfig = MutableStateFlow<ServerConfig?>(
            intent.getParcelable(Extras.SERVER_CONFIG)
        )

        if (serverConfig.value == null) {
            val servers = viewModel.getServers()

            if (servers.isEmpty()) {
                showToast(R.string.torrent_add_no_server)
                finish()
                return
            }

            if (servers.size == 1) {
                serverConfig.value = servers.first()
            } else {
                binding.spinnerServers.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    servers.map { server -> server.name ?: server.host }
                )
                binding.spinnerServers.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            serverConfig.value = servers[position]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                binding.layoutServerSelector.visibility = View.VISIBLE
            }
        }

        val uri = intent.data
        val fileUri = if (uri != null) {
            when (uri.scheme) {
                "http", "https", "magnet" -> {
                    binding.editTorrentLink.setText(uri.toString())
                    null
                }
                "content", "file" -> {
                    binding.textFileName.visibility = View.VISIBLE
                    binding.inputLayoutTorrentLink.visibility = View.GONE

                    lifecycleScope.launch {
                        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                        val fileName = withContext(Dispatchers.IO) {
                            contentResolver.query(uri, projection, null, null, null)
                                ?.use { metaCursor ->
                                    if (metaCursor.moveToFirst()) {
                                        metaCursor.getString(0)
                                    } else {
                                        null
                                    }
                                }
                        }

                        binding.textFileName.text = fileName
                    }
                    uri
                }
                else -> null
            }
        } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            binding.editTorrentLink.setText(text)
            null
        } else {
            null
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
                    val downloadSpeedLimit = binding.editDlspeedLimit.text.toString().toDoubleOrNull().let { limit ->
                        if (limit != null) {
                            (limit * 1024).roundToInt()
                        } else {
                            null
                        }
                    }
                    val uploadSpeedLimit = binding.editUpspeedLimit.text.toString().toDoubleOrNull().let { limit ->
                        if (limit != null) {
                            (limit * 1024).roundToInt()
                        } else {
                            null
                        }
                    }

                    if (links.isNotBlank() || fileUri != null) {
                        val config = serverConfig.value

                        val category = binding.chipGroupCategory.checkedChipId.let { id ->
                            if (id != View.NO_ID) {
                                binding.chipGroupCategory.findViewById<Chip>(id).text.toString()
                            } else {
                                null
                            }
                        }

                        val tags = binding.chipGroupTag.checkedChipIds.map { id ->
                            binding.chipGroupTag.findViewById<Chip>(id).text.toString()
                        }

                        if (config != null) {
                            viewModel.createTorrent(
                                serverConfig = config,
                                links = if (fileUri == null) links.split("\n") else null,
                                fileUri = fileUri,
                                savePath = binding.editSavePath.text.toString().ifBlank { null },
                                category = category,
                                tags = tags,
                                torrentName = binding.editTorrentName.text.toString().ifBlank { null },
                                downloadSpeedLimit = downloadSpeedLimit,
                                uploadSpeedLimit = uploadSpeedLimit,
                                ratioLimit = binding.editRatioLimit.text.toString().toDoubleOrNull(),
                                seedingTimeLimit = binding.editSeedingTimeLimit.text.toString().toIntOrNull(),
                                isPaused = !binding.checkStartTorrent.isChecked,
                                skipHashChecking = binding.checkSkipChecking.isChecked,
                                isAutoTorrentManagementEnabled = binding.spinnerTmm.selectedItemPosition == 1,
                                isSequentialDownloadEnabled = binding.checkSequentialDownload.isChecked,
                                isFirstLastPiecePrioritized = binding.checkPrioritizeFirstLastPiece.isChecked
                            )
                        }
                    } else {
                        binding.inputLayoutTorrentLink.error = getString(R.string.torrent_add_link_cannot_be_empty)
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

        binding.swipeRefresh.setOnRefreshListener {
            serverConfig.value?.let { config ->
                viewModel.refreshCategoryAndTags(config)
            }
        }

        serverConfig.filterNotNull().launchAndCollectLatestIn(this) { config ->
            binding.chipGroupCategory.removeAllViews()
            binding.chipGroupTag.removeAllViews()
            viewModel.removeCategoriesAndTags()

            viewModel.loadCategoryAndTags(config)
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

        viewModel.isRefreshing.launchAndCollectLatestIn(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.categoryList.filterNotNull().launchAndCollectLatestIn(this) { categoryList ->
            val selectedCategory = binding.chipGroupCategory.checkedChipId.let { id ->
                if (id != View.NO_ID) {
                    binding.chipGroupCategory.findViewById<Chip>(id).text.toString()
                } else {
                    null
                }
            }

            binding.chipGroupCategory.removeAllViews()

            categoryList.forEach { category ->
                val chip = Chip(this@AddTorrentActivity)
                chip.text = category
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_category)
                chip.ellipsize = TextUtils.TruncateAt.END
                chip.isCheckable = true

                if (category == selectedCategory) {
                    chip.isChecked = true
                }

                binding.chipGroupCategory.addView(chip)
            }
        }

        viewModel.tagList.filterNotNull().launchAndCollectLatestIn(this) { tagList ->
            val selectedTags = binding.chipGroupTag.checkedChipIds.map { id ->
                binding.chipGroupTag.findViewById<Chip>(id).text.toString()
            }

            binding.chipGroupTag.removeAllViews()

            tagList.forEach { tag ->
                val chip = Chip(this@AddTorrentActivity)
                chip.text = tag
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_tag)
                chip.isCheckable = true
                chip.ellipsize = TextUtils.TruncateAt.END

                if (selectedTags.contains(tag)) {
                    chip.isChecked = true
                }

                binding.chipGroupTag.addView(chip)
            }
        }

        binding.spinnerTmm.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.torrent_add_tmm_manual), getString(R.string.torrent_add_tmm_auto))
        )

        binding.spinnerTmm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.inputLayoutSavePath.isEnabled = position == 0
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is AddTorrentViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@AddTorrentActivity, event.error))
                }
                AddTorrentViewModel.Event.FileNotFound -> {
                    showSnackbar(R.string.torrent_add_file_not_found)
                }
                AddTorrentViewModel.Event.TorrentCreated -> {
                    if (intent.data == null) {
                        val intent = Intent().apply {
                            putExtra(Extras.IS_ADDED, true)
                        }
                        setResult(Activity.RESULT_OK, intent)
                    } else {
                        showToast(R.string.torrent_add_success)
                    }
                    finish()
                }
            }
        }
    }
}
