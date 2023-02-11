package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import dev.bartuzen.qbitcontroller.databinding.DialogSpeedLimitDownloadBinding
import dev.bartuzen.qbitcontroller.databinding.DialogSpeedLimitUploadBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentLocationBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentRenameBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentShareLimitBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.category.TorrentCategoryDialog
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.tags.TorrentTagsDialog
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

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_menu, menu)

                    viewModel.torrent.launchAndCollectLatestIn(this@TorrentOverviewFragment) { torrent ->
                        val tags = menu.findItem(R.id.menu_tags)
                        val resume = menu.findItem(R.id.menu_resume)
                        val pause = menu.findItem(R.id.menu_pause)
                        val shareLimit = menu.findItem(R.id.menu_share_limit)
                        val reannounce = menu.findItem(R.id.menu_reannounce)
                        val sequentialDownload = menu.findItem(R.id.menu_sequential_download)
                        val prioritizeFirstLastPieces = menu.findItem(R.id.menu_prioritize_first_last_pieces)
                        val autoTmm = menu.findItem(R.id.menu_automatic_torrent_management)
                        val forceStart = menu.findItem(R.id.menu_force_start)
                        val superSeeding = menu.findItem(R.id.menu_super_seeding)
                        val copy = menu.findItem(R.id.menu_copy)
                        val copyHashV1 = menu.findItem(R.id.menu_copy_hash_v1)
                        val copyHashV2 = menu.findItem(R.id.menu_copy_hash_v2)

                        tags.isEnabled = torrent != null
                        shareLimit.isEnabled = torrent != null
                        sequentialDownload.isEnabled = torrent != null
                        prioritizeFirstLastPieces.isEnabled = torrent != null
                        autoTmm.isEnabled = torrent != null
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

                            sequentialDownload.isChecked = torrent.isSequentialDownloadEnabled
                            prioritizeFirstLastPieces.isChecked = torrent.isFirstLastPiecesPrioritized
                            autoTmm.isChecked = torrent.isAutomaticTorrentManagementEnabled
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
                        R.id.menu_category -> {
                            TorrentCategoryDialog(serverId, viewModel.torrent.value?.category)
                                .show(childFragmentManager, null)
                        }
                        R.id.menu_tags -> {
                            TorrentTagsDialog(serverId, viewModel.torrent.value?.tags ?: listOf())
                                .show(childFragmentManager, null)
                        }
                        R.id.menu_share_limit -> {
                            showShareLimitDialog()
                        }
                        R.id.menu_rename -> {
                            showRenameTorrentDialog()
                        }
                        R.id.menu_location -> {
                            showLocationDialog()
                        }
                        R.id.menu_recheck -> {
                            viewModel.recheckTorrent(serverId, torrentHash)
                        }
                        R.id.menu_reannounce -> {
                            viewModel.reannounceTorrent(serverId, torrentHash)
                        }
                        R.id.menu_dlspeed_limit -> {
                            showDownloadSpeedLimitDialog()
                        }
                        R.id.menu_upspeed_limit -> {
                            showUploadSpeedLimitDialog()
                        }
                        R.id.menu_sequential_download -> {
                            viewModel.toggleSequentialDownload(serverId, torrentHash)
                        }
                        R.id.menu_prioritize_first_last_pieces -> {
                            viewModel.togglePrioritizeFirstLastPiecesDownload(serverId, torrentHash)
                        }
                        R.id.menu_automatic_torrent_management -> {
                            val isEnabled = viewModel.torrent.value?.isAutomaticTorrentManagementEnabled ?: return true
                            viewModel.setAutomaticTorrentManagement(serverId, torrentHash, !isEnabled)
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

        viewModel.isNaturalLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isNaturalLoading ->
            val autoRefreshLoadingBar = viewModel.autoRefreshHideLoadingBar.value
            binding.progressIndicator.visibility =
                if (isNaturalLoading == true || isNaturalLoading == false && !autoRefreshLoadingBar) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
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
                val chip = Chip(context)
                chip.text = torrent.category
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_category)
                chip.ellipsize = TextUtils.TruncateAt.END
                binding.chipGroupCategoryAndTag.addView(chip)
            }

            torrent.tags.forEach { tag ->
                val chip = Chip(context)
                chip.text = tag
                chip.setEnsureMinTouchTargetSize(false)
                chip.setChipBackgroundColorResource(R.color.torrent_tag)
                chip.ellipsize = TextUtils.TruncateAt.END
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
                R.string.torrent_overview_progress,
                formatBytes(requireContext(), torrent.completed),
                formatBytes(requireContext(), torrent.size),
                progressText
            )

            binding.textEta.text = torrent.eta?.let { eta ->
                if (eta < 8640000) {
                    formatSeconds(requireContext(), eta)
                } else {
                    null
                }
            }

            binding.textState.text = formatTorrentState(requireContext(), torrent.state)

            val speedList = mutableListOf<String>()
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(requireContext(), torrent.uploadSpeed)}")
            }
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(requireContext(), torrent.downloadSpeed)}")
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

            binding.textTimeActive.text = formatSeconds(requireContext(), torrent.timeActive)
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
            binding.textRatio.text = torrent.ratio.floorToDecimal(2).toString()
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
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentOverviewViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found)
                }
                TorrentOverviewViewModel.Event.TorrentDeleted -> {
                    val intent = Intent().apply {
                        putExtra(TorrentActivity.Extras.TORRENT_DELETED, true)
                    }
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                }
                TorrentOverviewViewModel.Event.TorrentPaused -> {
                    showSnackbar(R.string.torrent_paused_success)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent pauses the torrent
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentResumed -> {
                    showSnackbar(R.string.torrent_resumed_success)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent resumes the torrent
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentRechecked -> {
                    showSnackbar(R.string.torrent_recheck_success)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent starts rechecking
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentReannounced -> {
                    showSnackbar(R.string.torrent_reannounce_success)
                }
                TorrentOverviewViewModel.Event.TorrentRenamed -> {
                    showSnackbar(R.string.torrent_rename_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.LocationUpdated -> {
                    showSnackbar(R.string.torrent_location_update_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.SequentialDownloadToggled -> {
                    showSnackbar(R.string.torrent_toggle_sequential_download_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.PrioritizeFirstLastPiecesToggled -> {
                    showSnackbar(R.string.torrent_toggle_prioritize_first_last_pieces)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                is TorrentOverviewViewModel.Event.AutomaticTorrentManagementChanged -> {
                    showSnackbar(
                        if (event.isEnabled) {
                            R.string.torrent_enable_automatic_torrent_management_success
                        } else {
                            R.string.torrent_disable_automatic_torrent_management_success
                        }
                    )

                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.DownloadSpeedLimitUpdated -> {
                    showSnackbar(R.string.torrent_dlspeed_limit_change_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.UploadSpeedLimitUpdated -> {
                    showSnackbar(R.string.torrent_upspeed_limit_change_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                is TorrentOverviewViewModel.Event.ForceStartChanged -> {
                    showSnackbar(
                        if (event.isEnabled) {
                            R.string.torrent_enable_force_start_success
                        } else {
                            R.string.torrent_disable_force_start_success
                        }
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
                        }
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent sets the value
                        viewModel.loadTorrent(serverId, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.CategoryUpdated -> {
                    showSnackbar(R.string.torrent_category_update_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.TagsUpdated -> {
                    showSnackbar(R.string.torrent_tags_update_success)
                    viewModel.loadTorrent(serverId, torrentHash)
                }
                TorrentOverviewViewModel.Event.ShareLimitUpdated -> {
                    showSnackbar(R.string.torrent_share_limit_update_success)
                    viewModel.loadTorrent(serverId, torrentHash)
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

    private fun showShareLimitDialog() {
        showDialog(DialogTorrentShareLimitBinding::inflate) { binding ->
            val torrent = viewModel.torrent.value ?: return@showDialog

            when {
                torrent.seedingTimeLimit == -2 && torrent.ratioLimit == -2.0 -> {
                    binding.radioLimitGlobal.isChecked = true

                    binding.inputLayoutRatio.isEnabled = false
                    binding.inputLayoutTime.isEnabled = false
                }
                torrent.seedingTimeLimit == -1 && torrent.ratioLimit == -1.0 -> {
                    binding.radioLimitDisable.isChecked = true

                    binding.inputLayoutRatio.isEnabled = false
                    binding.inputLayoutTime.isEnabled = false
                }
                else -> {
                    binding.radioLimitCustom.isChecked = true

                    if (torrent.ratioLimit >= 0) {
                        binding.inputLayoutRatio.setTextWithoutAnimation(torrent.ratioLimit.toString())
                    }
                    if (torrent.seedingTimeLimit >= 0) {
                        binding.inputLayoutTime.setTextWithoutAnimation(torrent.seedingTimeLimit.toString())
                    }
                }
            }

            binding.radioLimitCustom.setOnCheckedChangeListener { _, isChecked ->
                binding.inputLayoutRatio.isEnabled = isChecked
                binding.inputLayoutTime.isEnabled = isChecked
            }

            setTitle(R.string.torrent_share_limit_dialog_title)
            setPositiveButton { _, _ ->
                when (binding.radioGroupLimit.checkedRadioButtonId) {
                    R.id.radio_limit_global -> {
                        viewModel.setShareLimit(serverId, torrentHash, -2.0, -2)
                    }
                    R.id.radio_limit_disable -> {
                        viewModel.setShareLimit(serverId, torrentHash, -1.0, -1)
                    }
                    R.id.radio_limit_custom -> {
                        val ratioLimit = binding.editRatio.text.toString().toDoubleOrNull() ?: -1.0
                        val timeLimit = binding.editTime.text.toString().toIntOrNull() ?: -1

                        viewModel.setShareLimit(serverId, torrentHash, ratioLimit, timeLimit)
                    }
                }
            }
            setNegativeButton()
        }
    }

    private fun showRenameTorrentDialog() {
        lateinit var dialogBinding: DialogTorrentRenameBinding

        val dialog = showDialog(DialogTorrentRenameBinding::inflate) { binding ->
            dialogBinding = binding

            val name = viewModel.torrent.value?.name
            binding.inputLayoutName.setTextWithoutAnimation(name)

            setTitle(R.string.torrent_rename_dialog_title)
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

    private fun showLocationDialog() {
        lateinit var dialogBinding: DialogTorrentLocationBinding

        val dialog = showDialog(DialogTorrentLocationBinding::inflate) { binding ->
            dialogBinding = binding

            val name = viewModel.torrentProperties.value?.savePath
            binding.inputLayoutLocation.setTextWithoutAnimation(name)

            setTitle(R.string.torrent_location_dialog_title)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newLocation = dialogBinding.editLocation.text.toString()
            if (newLocation.isNotBlank()) {
                viewModel.setLocation(serverId, torrentHash, newLocation)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutLocation.error = getString(R.string.torrent_location_cannot_be_blank)
            }
        }
    }

    private fun showDownloadSpeedLimitDialog() {
        showDialog(DialogSpeedLimitDownloadBinding::inflate) { binding ->
            val currentLimit = viewModel.torrent.value?.downloadSpeedLimit?.let { speed ->
                speed / 1024
            }
            binding.inputLayoutLimit.setTextWithoutAnimation(currentLimit?.toString())

            setTitle(R.string.torrent_dlspeed_limit_dialog_title)
            setPositiveButton { _, _ ->
                val limit = binding.editLimit.text.toString().toIntOrNull()?.let { speed ->
                    speed * 1024
                } ?: 0

                viewModel.setDownloadSpeedLimit(serverId, torrentHash, limit)
            }
            setNegativeButton()
        }
    }

    private fun showUploadSpeedLimitDialog() {
        showDialog(DialogSpeedLimitUploadBinding::inflate) { binding ->
            val currentLimit = viewModel.torrent.value?.uploadSpeedLimit?.let { speed ->
                speed / 1024
            }
            binding.inputLayoutLimit.setTextWithoutAnimation(currentLimit?.toString())

            setTitle(R.string.torrent_upspeed_limit_dialog_title)
            setPositiveButton { _, _ ->
                val limit = binding.editLimit.text.toString().toIntOrNull()?.let { speed ->
                    speed * 1024
                } ?: 0

                viewModel.setUploadSpeedLimit(serverId, torrentHash, limit)
            }
            setNegativeButton()
        }
    }

    fun onCategoryDialogResult(selectedCategory: String?) {
        viewModel.setCategory(serverId, torrentHash, selectedCategory)
    }

    fun onCategoryDialogError(error: RequestResult.Error) {
        showSnackbar(getErrorMessage(requireContext(), error))
    }

    fun onTagsDialogResult(selectedTags: List<String>) {
        viewModel.setTags(serverId, torrentHash, selectedTags)
    }

    fun onTagsDialogError(error: RequestResult.Error) {
        showSnackbar(getErrorMessage(requireContext(), error))
    }
}
