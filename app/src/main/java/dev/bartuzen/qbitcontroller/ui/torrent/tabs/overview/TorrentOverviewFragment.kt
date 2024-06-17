package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentOptionsBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentRenameBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.category.TorrentCategoryDialog
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.tags.TorrentTagsDialog
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.copyToClipboard
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setColor
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorrentOverviewFragment() : Fragment(R.layout.fragment_torrent_overview) {
    private val binding by viewBinding(FragmentTorrentOverviewBinding::bind)

    private val viewModel: TorrentOverviewViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    val exportActivity =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-bittorrent")) { uri ->
            if (uri != null) {
                viewModel.exportTorrent(serverId, torrentHash, uri)
            }
        }

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.scrollView.applyNavigationBarInsets()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent, menu)

                    viewModel.torrent.launchAndCollectLatestIn(this@TorrentOverviewFragment) { torrent ->
                        val tags = menu.findItem(R.id.menu_tags)
                        val resume = menu.findItem(R.id.menu_resume)
                        val pause = menu.findItem(R.id.menu_pause)
                        val torrentOptions = menu.findItem(R.id.menu_options)
                        val reannounce = menu.findItem(R.id.menu_reannounce)
                        val forceStart = menu.findItem(R.id.menu_force_start)
                        val superSeeding = menu.findItem(R.id.menu_super_seeding)
                        val copy = menu.findItem(R.id.menu_copy)
                        val copyHashV1 = menu.findItem(R.id.menu_copy_hash_v1)
                        val copyHashV2 = menu.findItem(R.id.menu_copy_hash_v2)

                        tags.isEnabled = torrent != null
                        torrentOptions.isEnabled = torrent != null
                        forceStart.isEnabled = torrent != null
                        superSeeding.isEnabled = torrent != null
                        copy.isEnabled = torrent != null
                        copyHashV1.isEnabled = torrent?.hashV1 != null
                        copyHashV2.isEnabled = torrent?.hashV2 != null

                        reannounce.isEnabled = torrent != null && when (torrent.state) {
                            TorrentState.PAUSED_UP, TorrentState.PAUSED_DL, TorrentState.QUEUED_UP, TorrentState.QUEUED_DL,
                            TorrentState.ERROR, TorrentState.CHECKING_UP, TorrentState.CHECKING_DL -> false

                            else -> true
                        }

                        if (torrent != null) {
                            val isPaused = when (torrent.state) {
                                TorrentState.PAUSED_DL, TorrentState.PAUSED_UP,
                                TorrentState.MISSING_FILES, TorrentState.ERROR -> true

                                else -> false
                            }
                            resume.isVisible = isPaused
                            pause.isVisible = !isPaused

                            forceStart.isChecked = torrent.isForceStartEnabled
                            superSeeding.isChecked = torrent.isSuperSeedingEnabled
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_pause -> {
                            viewModel.pauseTorrent(serverId, torrentHash)
                        }
                        R.id.menu_resume -> {
                            viewModel.resumeTorrent(serverId, torrentHash)
                        }
                        R.id.menu_delete -> {
                            showDeleteTorrentDialog()
                        }
                        R.id.menu_options -> {
                            showTorrentOptionsDialog()
                        }
                        R.id.menu_category -> {
                            TorrentCategoryDialog(serverId, viewModel.torrent.value?.category)
                                .show(childFragmentManager, null)
                        }
                        R.id.menu_tags -> {
                            TorrentTagsDialog(serverId, viewModel.torrent.value?.tags ?: listOf())
                                .show(childFragmentManager, null)
                        }
                        R.id.menu_rename -> {
                            showRenameTorrentDialog()
                        }
                        R.id.menu_recheck -> {
                            showRecheckDialog()
                        }
                        R.id.menu_reannounce -> {
                            viewModel.reannounceTorrent(serverId, torrentHash)
                        }
                        R.id.menu_force_start -> {
                            val isEnabled = viewModel.torrent.value?.isForceStartEnabled ?: return true
                            viewModel.setForceStart(serverId, torrentHash, !isEnabled)
                        }
                        R.id.menu_super_seeding -> {
                            val isEnabled = viewModel.torrent.value?.isSuperSeedingEnabled ?: return true
                            viewModel.setSuperSeeding(serverId, torrentHash, !isEnabled)
                        }
                        R.id.menu_copy_name -> {
                            val torrent = viewModel.torrent.value
                            if (torrent != null) {
                                requireContext().copyToClipboard(torrent.name)
                            }
                        }
                        R.id.menu_copy_hash_v1 -> {
                            val torrent = viewModel.torrent.value
                            if (torrent?.hashV1 != null) {
                                requireContext().copyToClipboard(torrent.hashV1)
                            }
                        }
                        R.id.menu_copy_hash_v2 -> {
                            val torrent = viewModel.torrent.value
                            if (torrent?.hashV2 != null) {
                                requireContext().copyToClipboard(torrent.hashV2)
                            }
                        }
                        R.id.menu_copy_magnet -> {
                            val torrent = viewModel.torrent.value
                            if (torrent != null) {
                                requireContext().copyToClipboard(torrent.magnetUri)
                            }
                        }
                        R.id.menu_export -> {
                            val torrentName = viewModel.torrent.value?.name ?: return true
                            exportActivity.launch("$torrentName.torrent")
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner
        )

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTorrent(serverId, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTorrent(serverId, torrentHash)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.INVISIBLE)
        viewModel.isNaturalLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isNaturalLoading ->
            if (isNaturalLoading == true) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.torrent, viewModel.torrentProperties) { torrent, properties ->
            torrent != null && properties != null
        }.launchAndCollectLatestIn(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.layoutContent.alpha = 0f
                binding.layoutContent.visibility = View.VISIBLE
                binding.layoutContent.animate().apply {
                    alpha(1f)
                    duration = 120
                }
                cancel()
            }
        }

        combine(viewModel.torrent, viewModel.torrentProperties) { torrent, properties ->
            if (torrent != null && properties != null) {
                torrent to properties
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (torrent, properties) ->
            binding.progressTorrent.setColor(getTorrentStateColor(requireContext(), torrent.state))

            binding.textName.text = torrent.name

            binding.chipGroupCategoryAndTag.removeAllViews()

            if (torrent.category != null) {
                val chip = layoutInflater.inflate(R.layout.chip_category, binding.chipGroupCategoryAndTag, false) as Chip
                chip.text = torrent.category
                chip.isFocusable = false
                binding.chipGroupCategoryAndTag.addView(chip)
            }

            torrent.tags.forEach { tag ->
                val chip = layoutInflater.inflate(R.layout.chip_tag, binding.chipGroupCategoryAndTag, false) as Chip
                chip.text = tag
                chip.isFocusable = false
                binding.chipGroupCategoryAndTag.addView(chip)
            }

            if (torrent.category == null && torrent.tags.isEmpty()) {
                binding.textProgress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = ConstraintLayout.LayoutParams.UNSET
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                }
                binding.textEta.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = ConstraintLayout.LayoutParams.UNSET
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                }
            } else {
                binding.textProgress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.UNSET
                    topToBottom = binding.chipGroupCategoryAndTag.id
                }
                binding.textEta.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = ConstraintLayout.LayoutParams.UNSET
                    topToBottom = binding.chipGroupCategoryAndTag.id
                }
            }

            val progress = torrent.progress * 100
            binding.progressTorrent.progress = progress.toInt()

            val progressText = if (torrent.progress < 1) {
                progress.floorToDecimal(1).toString()
            } else {
                "100"
            }
            binding.textProgress.text = requireContext().getString(
                R.string.torrent_item_progress_format,
                formatBytes(requireContext(), torrent.completed),
                formatBytes(requireContext(), torrent.size),
                progressText,
                torrent.ratio.floorToDecimal(2).toString()
            )

            binding.textEta.text = torrent.eta?.let { eta ->
                formatSeconds(requireContext(), eta)
            }

            binding.textState.text = formatTorrentState(requireContext(), torrent.state)

            val speedList = mutableListOf<String>()
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(requireContext(), torrent.downloadSpeed)}")
            }
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(requireContext(), torrent.uploadSpeed)}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")

            binding.textTotalSize.text = if (properties.totalSize != null) {
                formatBytes(requireContext(), properties.totalSize)
            } else {
                "-"
            }
            binding.textAddedOn.text = formatDate(properties.additionDate)
            binding.textHashV1.text = torrent.hashV1 ?: "-"
            binding.textHashV2.text = torrent.hashV2 ?: "-"
            binding.textSavePath.text = properties.savePath
            binding.textComment.text = properties.comment ?: "-"
            binding.textPieces.text = if (properties.piecesCount != null && properties.pieceSize != null) {
                getString(
                    R.string.torrent_overview_pieces_format,
                    properties.piecesCount,
                    formatBytes(requireContext(), properties.pieceSize),
                    properties.piecesHave
                )
            } else {
                "-"
            }
            binding.textCompletedOn.text = if (properties.completionDate != null) {
                formatDate(properties.completionDate)
            } else {
                "-"
            }
            binding.textCreatedBy.text = properties.createdBy ?: "-"
            binding.textCreatedOn.text = if (properties.creationDate != null) {
                formatDate(properties.creationDate)
            } else {
                "-"
            }

            binding.textTimeActive.text = if (torrent.seedingTime > 0) {
                getString(
                    R.string.torrent_overview_time_active_seeding_time_format,
                    formatSeconds(requireContext(), torrent.timeActive),
                    formatSeconds(requireContext(), torrent.seedingTime)
                )
            } else {
                formatSeconds(requireContext(), torrent.timeActive)
            }
            binding.textDownloaded.text = getString(
                R.string.torrent_overview_downloaded_format,
                formatBytes(requireContext(), torrent.downloaded),
                formatBytes(requireContext(), torrent.downloadedSession)
            )
            binding.textUploaded.text = getString(
                R.string.torrent_overview_uploaded_format,
                formatBytes(requireContext(), torrent.uploaded),
                formatBytes(requireContext(), torrent.uploadedSession)
            )
            binding.textReannounceIn.text = formatSeconds(requireContext(), properties.nextReannounce)
            binding.textLastActivity.text = formatDate(torrent.lastActivity)
            binding.textLastSeenComplete.text = if (torrent.lastSeenComplete != null) {
                formatDate(torrent.lastSeenComplete)
            } else {
                "-"
            }
            binding.textConnections.text = getString(
                R.string.torrent_overview_connections_format,
                properties.connections,
                properties.connectionsLimit
            )
            binding.textSeeds.text = getString(
                R.string.torrent_overview_seeds_format,
                properties.seeds,
                properties.seedsTotal
            )
            binding.textPeers.text = getString(
                R.string.torrent_overview_peers_format,
                properties.peers,
                properties.peersTotal
            )
            binding.textWasted.text = formatBytes(requireContext(), properties.wasted)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive) {
                        viewModel.loadTorrent(serverId, torrentHash, autoRefresh = true)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentOverviewViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error), view = requireActivity().view)
                }
                TorrentOverviewViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found, view = requireActivity().view)
                }
                TorrentOverviewViewModel.Event.TorrentDeleted -> {
                    val intent = Intent().apply {
                        putExtra(TorrentActivity.Extras.TORRENT_DELETED, true)
                    }
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                }
                TorrentOverviewViewModel.Event.TorrentPaused -> {
                    showSnackbar(R.string.torrent_paused_success, view = requireActivity().view)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent pauses the torrent
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentResumed -> {
                    showSnackbar(R.string.torrent_resumed_success, view = requireActivity().view)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent resumes the torrent
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.OptionsUpdated -> {
                    showSnackbar(R.string.torrent_option_update_success, view = requireActivity().view)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.TorrentRechecked -> {
                    showSnackbar(R.string.torrent_recheck_success, view = requireActivity().view)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent starts rechecking
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentReannounced -> {
                    showSnackbar(R.string.torrent_reannounce_success, view = requireActivity().view)
                }
                TorrentOverviewViewModel.Event.TorrentRenamed -> {
                    showSnackbar(R.string.torrent_rename_success, view = requireActivity().view)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                is TorrentOverviewViewModel.Event.ForceStartChanged -> {
                    showSnackbar(
                        if (event.isEnabled) {
                            R.string.torrent_enable_force_start_success
                        } else {
                            R.string.torrent_disable_force_start_success
                        },
                        view = requireActivity().view
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent sets the value
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                is TorrentOverviewViewModel.Event.SuperSeedingChanged -> {
                    showSnackbar(
                        if (event.isEnabled) {
                            R.string.torrent_enable_super_seeding_success
                        } else {
                            R.string.torrent_disable_super_seeding_success
                        },
                        view = requireActivity().view
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent sets the value
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.CategoryUpdated -> {
                    showSnackbar(R.string.torrent_category_update_success, view = requireActivity().view)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.TagsUpdated -> {
                    showSnackbar(R.string.torrent_tags_update_success, view = requireActivity().view)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.TorrentExported -> {
                    showSnackbar(R.string.torrent_export_success, view = requireActivity().view)
                }
                TorrentOverviewViewModel.Event.TorrentExportError -> {
                    showSnackbar(R.string.torrent_export_error, view = requireActivity().view)
                }
            }
        }
    }

    private fun showDeleteTorrentDialog() {
        showDialog(DialogTorrentDeleteBinding::inflate) { binding ->
            setTitle(R.string.torrent_delete)
            setPositiveButton { _, _ ->
                viewModel.deleteTorrent(serverId, torrentHash, binding.checkDeleteFiles.isChecked)
            }
            setNegativeButton()
        }
    }

    private fun showTorrentOptionsDialog() {
        val dialog = showDialog(DialogTorrentOptionsBinding::inflate) { binding ->
            val torrent = viewModel.torrent.value ?: return@showDialog

            binding.checkboxAutoTmm.isChecked = torrent.isAutomaticTorrentManagementEnabled
            binding.checkboxDownloadPath.isChecked = torrent.downloadPath != null
            binding.checkboxSequentialDownload.isChecked = torrent.isSequentialDownloadEnabled
            binding.checkboxPrioritizeFirstLastPieces.isChecked = torrent.isFirstLastPiecesPrioritized
            binding.inputLayoutSavePath.setTextWithoutAnimation(torrent.savePath)
            binding.inputLayoutDownloadPath.setTextWithoutAnimation(torrent.downloadPath)
            binding.inputLayoutUpSpeedLimit.setTextWithoutAnimation((torrent.uploadSpeedLimit / 1024).toString())
            binding.inputLayoutDlSpeedLimit.setTextWithoutAnimation((torrent.downloadSpeedLimit / 1024).toString())

            when {
                torrent.seedingTimeLimit == -2 && torrent.ratioLimit == -2.0 -> {
                    binding.radioLimitGlobal.isChecked = true

                    binding.inputLayoutRatio.isEnabled = false
                    binding.inputLayoutTotalMinutes.isEnabled = false
                    binding.inputLayoutInactiveMinutes.isEnabled = false
                }
                torrent.seedingTimeLimit == -1 && torrent.ratioLimit == -1.0 -> {
                    binding.radioLimitDisable.isChecked = true

                    binding.inputLayoutRatio.isEnabled = false
                    binding.inputLayoutTotalMinutes.isEnabled = false
                    binding.inputLayoutInactiveMinutes.isEnabled = false
                }
                else -> {
                    binding.radioLimitCustom.isChecked = true

                    if (torrent.ratioLimit >= 0) {
                        binding.inputLayoutRatio.setTextWithoutAnimation(torrent.ratioLimit.toString())
                    }
                    if (torrent.seedingTimeLimit >= 0) {
                        binding.inputLayoutTotalMinutes.setTextWithoutAnimation(torrent.seedingTimeLimit.toString())
                    }
                    if (torrent.inactiveSeedingTimeLimit >= 0) {
                        binding.inputLayoutInactiveMinutes.setTextWithoutAnimation(
                            torrent.inactiveSeedingTimeLimit.toString()
                        )
                    }
                }
            }

            binding.inputLayoutSavePath.isEnabled = !torrent.isAutomaticTorrentManagementEnabled
            binding.checkboxDownloadPath.isEnabled = !torrent.isAutomaticTorrentManagementEnabled
            binding.checkboxAutoTmm.setOnCheckedChangeListener { _, isChecked ->
                binding.checkboxDownloadPath.isEnabled = !isChecked
                binding.inputLayoutSavePath.isEnabled = !isChecked
                binding.inputLayoutDownloadPath.isEnabled = !isChecked && binding.checkboxDownloadPath.isChecked
            }

            binding.inputLayoutDownloadPath.isEnabled = torrent.downloadPath != null
            binding.checkboxDownloadPath.setOnCheckedChangeListener { _, isChecked ->
                binding.inputLayoutDownloadPath.isEnabled = isChecked
            }

            binding.radioLimitCustom.setOnCheckedChangeListener { _, isChecked ->
                binding.inputLayoutRatio.isEnabled = isChecked
                binding.inputLayoutTotalMinutes.isEnabled = isChecked
                binding.inputLayoutInactiveMinutes.isEnabled = isChecked
            }

            setTitle(R.string.torrent_action_options)
            setPositiveButton { _, _ ->
                val autoTmm = if (torrent.isAutomaticTorrentManagementEnabled != binding.checkboxAutoTmm.isChecked) {
                    binding.checkboxAutoTmm.isChecked
                } else {
                    null
                }
                val savePath = binding.inputLayoutSavePath.text.let { savePath ->
                    if (!binding.checkboxAutoTmm.isChecked && savePath.isNotBlank() && torrent.savePath != savePath) {
                        savePath
                    } else {
                        null
                    }
                }
                val downloadPath = binding.inputLayoutDownloadPath.text.let { downloadPath ->
                    val oldPath = torrent.downloadPath ?: ""
                    val newPath = if (binding.checkboxDownloadPath.isChecked) downloadPath else ""
                    if (!binding.checkboxAutoTmm.isChecked && oldPath != newPath) {
                        newPath
                    } else {
                        null
                    }
                }
                val toggleSequentialDownload =
                    torrent.isSequentialDownloadEnabled != binding.checkboxSequentialDownload.isChecked
                val togglePrioritizeFirstLastPiece =
                    torrent.isFirstLastPiecesPrioritized != binding.checkboxPrioritizeFirstLastPieces.isChecked
                val uploadSpeedLimit = binding.inputLayoutUpSpeedLimit.text.toIntOrNull().let { limit ->
                    val uploadSpeedLimit = limit ?: 0
                    if (uploadSpeedLimit != torrent.uploadSpeedLimit) {
                        uploadSpeedLimit * 1024
                    } else {
                        null
                    }
                }
                val downloadSpeedLimit = binding.inputLayoutDlSpeedLimit.text.toIntOrNull().let { limit ->
                    val downloadSpeedLimit = limit ?: 0
                    if (downloadSpeedLimit != torrent.downloadSpeedLimit) {
                        downloadSpeedLimit * 1024
                    } else {
                        null
                    }
                }
                val (ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit) =
                    when (binding.radioGroupLimit.checkedRadioButtonId) {
                        R.id.radio_limit_global -> {
                            Triple(-2.0, -2, -2)
                        }
                        R.id.radio_limit_disable -> {
                            Triple(-1.0, -1, -1)
                        }
                        R.id.radio_limit_custom -> {
                            val ratioLimit = binding.inputLayoutRatio.text.toDoubleOrNull() ?: -1.0
                            val seedingTimeLimit = binding.inputLayoutTotalMinutes.text.toIntOrNull() ?: -1
                            val inactiveSeedingTimeLimit = binding.inputLayoutInactiveMinutes.text.toIntOrNull() ?: -1

                            if (ratioLimit != -1.0 || seedingTimeLimit != -1 || inactiveSeedingTimeLimit != -1) {
                                Triple(ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit)
                            } else {
                                Triple(null, null, null)
                            }
                        }
                        else -> {
                            Triple(null, null, null)
                        }
                    }.let { (ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit) ->
                        if (ratioLimit == null || seedingTimeLimit == null || inactiveSeedingTimeLimit == null) {
                            Triple(null, null, null)
                        } else if (ratioLimit != torrent.ratioLimit || seedingTimeLimit != torrent.seedingTimeLimit ||
                            inactiveSeedingTimeLimit != torrent.inactiveSeedingTimeLimit
                        ) {
                            Triple(ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit)
                        } else {
                            Triple(null, null, null)
                        }
                    }

                viewModel.setTorrentOptions(
                    serverId = serverId,
                    torrentHash = torrentHash,
                    autoTmm = autoTmm,
                    savePath = savePath,
                    downloadPath = downloadPath,
                    toggleSequentialDownload = toggleSequentialDownload,
                    togglePrioritizeFirstLastPiece = togglePrioritizeFirstLastPiece,
                    uploadSpeedLimit = uploadSpeedLimit,
                    downloadSpeedLimit = downloadSpeedLimit,
                    ratioLimit = ratioLimit,
                    seedingTimeLimit = seedingTimeLimit,
                    inactiveSeedingTimeLimit = inactiveSeedingTimeLimit
                )
            }
            setNegativeButton()
        }

        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun showRenameTorrentDialog() {
        lateinit var dialogBinding: DialogTorrentRenameBinding

        val dialog = showDialog(DialogTorrentRenameBinding::inflate) { binding ->
            dialogBinding = binding

            val name = viewModel.torrent.value?.name
            binding.inputLayoutName.setTextWithoutAnimation(name)

            setTitle(R.string.torrent_action_rename_torrent)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newName = dialogBinding.editName.text.toString()
            if (newName.isNotBlank()) {
                viewModel.renameTorrent(serverId, torrentHash, newName)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.torrent_rename_name_cannot_be_blank)
            }
        }
    }

    private fun showRecheckDialog() {
        showDialog {
            setTitle(R.string.torrent_action_force_recheck)
            setMessage(R.string.torrent_force_recheck_confirm)
            setPositiveButton { _, _ ->
                viewModel.recheckTorrent(serverId, torrentHash)
            }
            setNegativeButton()
        }
    }

    fun onCategoryDialogResult(selectedCategory: String?) {
        viewModel.setCategory(serverId, torrentHash, selectedCategory)
    }

    fun onCategoryDialogError(error: RequestResult.Error) {
        showSnackbar(getErrorMessage(requireContext(), error), view = requireActivity().view)
    }

    fun onTagsDialogResult(selectedTags: List<String>) {
        viewModel.setTags(serverId, torrentHash, selectedTags)
    }

    fun onTagsDialogError(error: RequestResult.Error) {
        showSnackbar(getErrorMessage(requireContext(), error), view = requireActivity().view)
    }
}
