package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityAddTorrentBinding
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AddTorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
        const val TORRENT_URL = "dev.bartuzen.qbitcontroller.TORRENT_URL"

        const val IS_ADDED = "dev.bartuzen.qbitcontroller.IS_ADDED"

        const val FILE_URI = "dev.bartuzen.qbitcontroller.FILE_URI"
        const val SELECTED_CATEGORY = "dev.bartuzen.qbitcontroller.SELECTED_CATEGORY"
        const val SELECTED_TAGS = "dev.bartuzen.qbitcontroller.SELECTED_TAGS"
    }

    private lateinit var binding: ActivityAddTorrentBinding

    private val viewModel: AddTorrentViewModel by viewModels()

    private val startFileActivity = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            torrentFileUri = uri
            binding.textFileName.error = null
        }
    }

    private var torrentFileUri: Uri? = null
        set(value) {
            field = value
            updateFileName()
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.layoutAppBar.applySystemBarInsets(bottom = false)
        binding.scrollView.applySystemBarInsets(top = false)

        torrentFileUri = savedInstanceState?.getParcelableCompat(Extras.FILE_URI)

        val serverId = MutableStateFlow(intent.getIntExtra(Extras.SERVER_ID, -1).takeIf { it != -1 })

        if (serverId.value == null) {
            val servers = viewModel.getServers()

            if (servers.isEmpty()) {
                showToast(R.string.torrent_add_no_server)
                finish()
                return
            }

            if (servers.size == 1) {
                serverId.value = servers.first().id
            } else {
                binding.spinnerServers.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    servers.map { server -> server.name ?: server.visibleUrl },
                )
                binding.spinnerServers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        serverId.value = servers[position].id
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                binding.layoutServerSelector.visibility = View.VISIBLE
            }
        }

        val torrentUrl = intent.getStringExtra(Extras.TORRENT_URL)
        val uri = intent.data

        if (torrentUrl != null) {
            binding.inputLayoutTorrentLink.setTextWithoutAnimation(torrentUrl)
        } else if (uri != null) {
            when (uri.scheme) {
                "http", "https", "magnet" -> {
                    binding.inputLayoutTorrentLink.setTextWithoutAnimation(uri.toString())
                }
                "content", "file" -> {
                    torrentFileUri = uri
                    binding.toggleButtonMode.check(R.id.button_file)
                    binding.inputLayoutTorrentLink.visibility = View.GONE
                    binding.textFileName.visibility = View.VISIBLE
                }
            }
        } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            binding.inputLayoutTorrentLink.setTextWithoutAnimation(text)
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
                menuInflater.inflate(R.menu.add_torrent, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_add -> {
                        serverId.value?.let { id ->
                            tryAddTorrent(id)
                        }
                    }

                    else -> return false
                }
                return true
            }
        })

        binding.toggleButtonMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.button_url) {
                    binding.inputLayoutTorrentLink.visibility = View.VISIBLE
                    binding.textFileName.visibility = View.GONE
                } else if (checkedId == R.id.button_file) {
                    binding.inputLayoutTorrentLink.visibility = View.GONE
                    binding.textFileName.visibility = View.VISIBLE
                }
            }
        }

        binding.textFileName.setOnClickListener {
            startFileActivity.launch(arrayOf("application/x-bittorrent"))
        }

        binding.editTorrentLink.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                if (binding.inputLayoutTorrentLink.isErrorEnabled && text?.isNotBlank() == true) {
                    binding.inputLayoutTorrentLink.isErrorEnabled = false
                }
            },
        )

        binding.dropdownDlspeedLimitUnit.setItems(
            R.string.speed_kibibytes_per_second,
            R.string.speed_mebibytes_per_second,
        )

        binding.dropdownUpspeedLimitUnit.setItems(
            R.string.speed_kibibytes_per_second,
            R.string.speed_mebibytes_per_second,
        )

        binding.swipeRefresh.setOnRefreshListener {
            serverId.value?.let { id ->
                viewModel.refreshCategoryAndTags(id)
            }
        }

        serverId.filterNotNull().launchAndCollectLatestIn(this) { id ->
            binding.chipGroupCategory.removeAllViews()
            binding.chipGroupTag.removeAllViews()
            viewModel.removeCategoriesAndTags()

            viewModel.loadCategoryAndTags(id)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isCreating.launchAndCollectLatestIn(this) { isCreating ->
            if (isCreating) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
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
            val selectedCategory = if (savedInstanceState?.containsKey(Extras.SELECTED_CATEGORY) == true) {
                val category = savedInstanceState.getString(Extras.SELECTED_CATEGORY)
                savedInstanceState.remove(Extras.SELECTED_CATEGORY)
                category
            } else {
                getSelectedCategory()
            }

            binding.chipGroupCategory.removeAllViews()

            categoryList.forEach { category ->
                val chip = layoutInflater.inflate(R.layout.chip_category, binding.chipGroupCategory, false) as Chip
                chip.text = category
                chip.isClickable = true

                if (category == selectedCategory) {
                    chip.isChecked = true
                }

                binding.chipGroupCategory.addView(chip)
            }

            if (categoryList.isEmpty()) {
                binding.chipGroupCategory.visibility = View.GONE
                binding.textNoCategories.visibility = View.VISIBLE
            } else {
                binding.chipGroupCategory.visibility = View.VISIBLE
                binding.textNoCategories.visibility = View.GONE
            }
        }

        viewModel.tagList.filterNotNull().launchAndCollectLatestIn(this) { tagList ->
            val selectedTags = if (savedInstanceState?.containsKey(Extras.SELECTED_TAGS) == true) {
                val tags = savedInstanceState.getStringArrayList(Extras.SELECTED_TAGS) ?: emptyList<String>()
                savedInstanceState.remove(Extras.SELECTED_TAGS)
                tags
            } else {
                getSelectedTags()
            }

            binding.chipGroupTag.removeAllViews()

            tagList.forEach { tag ->
                val chip = layoutInflater.inflate(R.layout.chip_tag, binding.chipGroupTag, false) as Chip
                chip.text = tag
                chip.isClickable = true

                if (selectedTags.contains(tag)) {
                    chip.isChecked = true
                }

                binding.chipGroupTag.addView(chip)
            }

            if (tagList.isEmpty()) {
                binding.chipGroupTag.visibility = View.GONE
                binding.textNoTags.visibility = View.VISIBLE
            } else {
                binding.chipGroupTag.visibility = View.VISIBLE
                binding.textNoTags.visibility = View.GONE
            }
        }

        viewModel.defaultSavePath.filterNotNull().launchAndCollectLatestIn(this) { defaultSavePath ->
            if (binding.editSavePath.text.isNullOrBlank()) {
                binding.inputLayoutSavePath.setTextWithoutAnimation(defaultSavePath)
            }
        }

        binding.dropdownAutoTmm.setItems(
            R.string.torrent_add_default,
            R.string.torrent_add_torrent_management_mode_manual,
            R.string.torrent_add_torrent_management_mode_auto,
        )
        binding.dropdownAutoTmm.onItemChangeListener = { position ->
            binding.inputLayoutSavePath.isEnabled = position != 2
        }

        binding.dropdownStopCondition.setItems(
            R.string.torrent_add_default,
            R.string.torrent_add_stop_condition_none,
            R.string.torrent_add_stop_condition_metadata_received,
            R.string.torrent_add_stop_condition_files_checked,
        )

        binding.dropdownContentLayout.setItems(
            R.string.torrent_add_default,
            R.string.torrent_add_content_layout_original,
            R.string.torrent_add_content_layout_subfolder,
            R.string.torrent_add_content_layout_no_subfolder,
        )

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is AddTorrentViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@AddTorrentActivity, event.error), view = binding.layoutCoordinator)
                }
                AddTorrentViewModel.Event.FileNotFound -> {
                    showSnackbar(R.string.torrent_add_file_not_found, view = binding.layoutCoordinator)
                }
                is AddTorrentViewModel.Event.FileReadError -> {
                    showSnackbar(getString(R.string.error_unknown, event.error))
                }
                AddTorrentViewModel.Event.TorrentAddError -> {
                    showSnackbar(R.string.torrent_add_error, view = binding.layoutCoordinator)
                }
                AddTorrentViewModel.Event.InvalidTorrentFile -> {
                    showSnackbar(R.string.torrent_add_invalid_file, view = binding.layoutCoordinator)
                }
                AddTorrentViewModel.Event.TorrentAdded -> {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Extras.FILE_URI, torrentFileUri)
        outState.putString(Extras.SELECTED_CATEGORY, getSelectedCategory())
        outState.putStringArrayList(Extras.SELECTED_TAGS, ArrayList(getSelectedTags()))
    }

    private fun getSelectedCategory() = binding.chipGroupCategory.checkedChipId.let { id ->
        if (id != View.NO_ID) {
            binding.chipGroupCategory.findViewById<Chip>(id).text.toString()
        } else {
            null
        }
    }

    private fun getSelectedTags() = binding.chipGroupTag.checkedChipIds.map { id ->
        binding.chipGroupTag.findViewById<Chip>(id).text.toString()
    }

    private fun updateFileName() {
        val uri = torrentFileUri

        if (uri != null) {
            lifecycleScope.launch {
                val fileName = withContext(Dispatchers.IO) {
                    val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                    contentResolver.query(uri, projection, null, null, null)?.use { metaCursor ->
                        if (metaCursor.moveToFirst()) {
                            metaCursor.getString(0)
                        } else {
                            null
                        }
                    }
                }
                binding.textFileName.text = fileName
            }
        } else {
            binding.textFileName.setText(R.string.torrent_add_click_to_select_file)
        }
    }

    private fun convertSpeedToBytes(speed: String, unit: Int): Int? {
        if (speed.isEmpty()) {
            return 0
        }

        val limit = speed.toIntOrNull() ?: return null
        return when (unit) {
            0 -> {
                if (limit > 2_000_000) {
                    null
                } else {
                    limit * 1024
                }
            }
            1 -> {
                if (limit > 2_000_000 / 1024) {
                    null
                } else {
                    limit * 1024 * 1024
                }
            }
            else -> null
        }
    }

    private fun tryAddTorrent(serverId: Int) {
        val isUrlMode = binding.toggleButtonMode.checkedButtonId == R.id.button_url
        var isValid = true

        val links = binding.editTorrentLink.text.toString()
        val torrentFileUri = torrentFileUri

        if (isUrlMode && links.isBlank()) {
            isValid = false
            binding.inputLayoutTorrentLink.error = getString(R.string.torrent_add_link_cannot_be_empty)
        } else {
            binding.inputLayoutTorrentLink.isErrorEnabled = false
        }

        if (!isUrlMode && torrentFileUri == null) {
            isValid = false
            binding.textFileName.error = ""
        } else {
            binding.textFileName.error = null
        }

        val downloadSpeedLimit =
            convertSpeedToBytes(binding.editDlspeedLimit.text.toString(), binding.dropdownDlspeedLimitUnit.position)
        if (downloadSpeedLimit == null) {
            isValid = false
            binding.inputLayoutDlspeedLimit.error = getString(R.string.torrent_add_speed_limit_too_big)
        } else {
            binding.inputLayoutDlspeedLimit.isErrorEnabled = false
        }

        val uploadSpeedLimit =
            convertSpeedToBytes(binding.editUpspeedLimit.text.toString(), binding.dropdownUpspeedLimitUnit.position)
        if (uploadSpeedLimit == null) {
            isValid = false
            binding.inputLayoutUpspeedLimit.error = getString(R.string.torrent_add_speed_limit_too_big)
        } else {
            binding.inputLayoutUpspeedLimit.isErrorEnabled = false
        }

        if (!isValid) {
            return
        }

        val category = getSelectedCategory()
        val tags = getSelectedTags()

        val autoTmm = when (binding.dropdownAutoTmm.position) {
            1 -> false
            2 -> true
            else -> null
        }

        val stopCondition = when (binding.dropdownStopCondition.position) {
            1 -> "None"
            2 -> "MetadataReceived"
            3 -> "FilesChecked"
            else -> null
        }

        val contentLayout = when (binding.dropdownContentLayout.position) {
            1 -> "Original"
            2 -> "Subfolder"
            3 -> "NoSubfolder"
            else -> null
        }

        val savePathText = binding.editSavePath.text.toString().ifBlank { null }
        val savePath = when (autoTmm) {
            null -> if (savePathText != viewModel.defaultSavePath.value) savePathText else null
            true -> null
            false -> savePathText
        }

        viewModel.createTorrent(
            serverId = serverId,
            links = if (isUrlMode) links.split("\n") else null,
            fileUri = if (!isUrlMode) torrentFileUri else null,
            savePath = savePath,
            category = category,
            tags = tags,
            stopCondition = stopCondition,
            contentLayout = contentLayout,
            torrentName = binding.editTorrentName.text.toString().ifBlank { null },
            downloadSpeedLimit = downloadSpeedLimit,
            uploadSpeedLimit = uploadSpeedLimit,
            ratioLimit = binding.editRatioLimit.text.toString().toDoubleOrNull(),
            seedingTimeLimit = binding.editSeedingTimeLimit.text.toString().toIntOrNull(),
            isPaused = !binding.checkStartTorrent.isChecked,
            skipHashChecking = binding.checkSkipChecking.isChecked,
            isAutoTorrentManagementEnabled = autoTmm,
            isSequentialDownloadEnabled = binding.checkSequentialDownload.isChecked,
            isFirstLastPiecePrioritized = binding.checkPrioritizeFirstLastPiece.isChecked,
        )
    }
}
