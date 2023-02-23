package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
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
    }

    private lateinit var binding: ActivityAddTorrentBinding

    private val viewModel: AddTorrentViewModel by viewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTorrentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverId = MutableStateFlow(
            intent.getIntExtra(Extras.SERVER_ID, -1).takeIf { it != -1 }
        )

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
                    servers.map { server -> server.name ?: server.visibleUrl }
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
        val fileUri = if (torrentUrl != null) {
            binding.inputLayoutTorrentLink.setTextWithoutAnimation(torrentUrl)
            null
        } else if (uri != null) {
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
                menuInflater.inflate(R.menu.add_torrent, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_add -> {
                        serverId.value?.let { id ->
                            tryAddTorrent(id, fileUri)
                        }
                    }

                    else -> return false
                }
                return true
            }
        })

        binding.editTorrentLink.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                if (binding.inputLayoutTorrentLink.isErrorEnabled && text?.isNotBlank() == true) {
                    binding.inputLayoutTorrentLink.isErrorEnabled = false
                }
            }
        )

        binding.dropdownDlspeedLimitUnit.setItems(
            listOf(
                getString(R.string.speed_bytes_per_second),
                getString(R.string.speed_kibibytes_per_second),
                getString(R.string.speed_mebibytes_per_second)
            )
        )
        binding.dropdownDlspeedLimitUnit.setPosition(2)

        binding.dropdownUpspeedLimitUnit.setItems(
            listOf(
                getString(R.string.speed_bytes_per_second),
                getString(R.string.speed_kibibytes_per_second),
                getString(R.string.speed_mebibytes_per_second)
            )
        )
        binding.dropdownUpspeedLimitUnit.setPosition(2)

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

        viewModel.isCreating.launchAndCollectLatestIn(this) { isCreating ->
            binding.progressIndicator.visibility = if (isCreating) View.VISIBLE else View.GONE
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

            if (categoryList.isEmpty()) {
                binding.chipGroupCategory.visibility = View.GONE
                binding.textNoCategories.visibility = View.VISIBLE
            } else {
                binding.chipGroupCategory.visibility = View.VISIBLE
                binding.textNoCategories.visibility = View.GONE
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

            if (tagList.isEmpty()) {
                binding.chipGroupTag.visibility = View.GONE
                binding.textNoTags.visibility = View.VISIBLE
            } else {
                binding.chipGroupTag.visibility = View.VISIBLE
                binding.textNoTags.visibility = View.GONE
            }
        }

        binding.dropdownAutoTmm.setItems(
            listOf(
                R.string.torrent_add_default,
                R.string.torrent_add_tmm_manual,
                R.string.torrent_add_tmm_auto
            )
        )
        binding.dropdownAutoTmm.onItemChangeListener = { position ->
            binding.inputLayoutSavePath.isEnabled = position != 2
        }

        binding.dropdownStopCondition.setItems(
            listOf(
                R.string.torrent_add_default,
                R.string.torrent_add_stop_condition_none,
                R.string.torrent_add_stop_condition_metadata_received,
                R.string.torrent_add_stop_condition_files_checked
            )
        )

        binding.dropdownContentLayout.setItems(
            listOf(
                R.string.torrent_add_default,
                R.string.torrent_add_content_layout_original,
                R.string.torrent_add_content_layout_subfolder,
                R.string.torrent_add_content_layout_no_subfolder
            )
        )

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is AddTorrentViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(this@AddTorrentActivity, event.error), view = binding.layoutCoordinator)
                }
                AddTorrentViewModel.Event.FileNotFound -> {
                    showSnackbar(R.string.torrent_add_file_not_found, view = binding.layoutCoordinator)
                }
                AddTorrentViewModel.Event.TorrentAddError -> {
                    showSnackbar(R.string.torrent_add_error, view = binding.layoutCoordinator)
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

    private fun convertSpeedToBytes(speed: String, unit: Int): Pair<Int?, Boolean> {
        if (speed.length > 10) {
            return null to false
        }

        val limit = speed.toLongOrNull() ?: return null to true
        return when (unit) {
            0 -> {
                // Limit is 2,000,000 KiB
                if (limit > 2_000_000 * 1024) {
                    null to false
                } else {
                    limit.toInt() to true
                }
            }
            1 -> {
                if (limit > 2_000_000) {
                    null to false
                } else {
                    limit.toInt() * 1024 to true
                }
            }
            2 -> {
                if (limit > 2_000_000 / 1024) {
                    null to false
                } else {
                    limit.toInt() * 1024 * 1024 to true
                }
            }
            else -> null to true
        }
    }

    private fun tryAddTorrent(serverId: Int, fileUri: Uri?) {
        var isValid = true

        val links = binding.editTorrentLink.text.toString()

        if (links.isBlank() && fileUri == null) {
            isValid = false
            binding.inputLayoutTorrentLink.error = getString(R.string.torrent_add_link_cannot_be_empty)
        } else {
            binding.inputLayoutTorrentLink.isErrorEnabled = false
        }

        val (downloadSpeedLimit, isDownloadSpeedValid) =
            convertSpeedToBytes(binding.editDlspeedLimit.text.toString(), binding.dropdownDlspeedLimitUnit.position)
        if (!isDownloadSpeedValid) {
            isValid = false
            binding.textInputDlspeedLimit.error = getString(R.string.torrent_add_speed_limit_too_big)
        } else {
            binding.textInputDlspeedLimit.isErrorEnabled = false
        }

        val (uploadSpeedLimit, isUploadSpeedValid) =
            convertSpeedToBytes(binding.editUpspeedLimit.text.toString(), binding.dropdownUpspeedLimitUnit.position)
        if (!isUploadSpeedValid) {
            isValid = false
            binding.textInputUpspeedLimit.error = getString(R.string.torrent_add_speed_limit_too_big)
        } else {
            binding.textInputUpspeedLimit.isErrorEnabled = false
        }

        if (!isValid) {
            return
        }

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

        viewModel.createTorrent(
            serverId = serverId,
            links = if (fileUri == null) links.split("\n") else null,
            fileUri = fileUri,
            savePath = binding.editSavePath.text.toString().ifBlank { null },
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
            isFirstLastPiecePrioritized = binding.checkPrioritizeFirstLastPiece.isChecked
        )
    }
}
