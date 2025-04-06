package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.RadioButtonWithLabel
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.copyToClipboard
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.DecimalFormatSymbols

@AndroidEntryPoint
class TorrentOverviewFragment() : Fragment() {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!
    private var torrentName
        get() = arguments?.getString("torrentName")
        set(value) {
            arguments = bundleOf(
                "serverId" to serverId,
                "torrentHash" to torrentHash,
                "torrentName" to value,
            )
        }

    constructor(serverId: Int, torrentHash: String, torrentName: String?) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash,
            "torrentName" to torrentName,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    var currentLifecycle by remember { mutableStateOf(lifecycle.currentState) }
                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event ->
                            currentLifecycle = event.targetState
                        }
                        lifecycle.addObserver(observer)

                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    TorrentOverviewTab(
                        fragment = this@TorrentOverviewFragment,
                        serverId = serverId,
                        torrentHash = torrentHash,
                        initialTorrentName = torrentName,
                        isScreenActive = currentLifecycle.isAtLeast(Lifecycle.State.RESUMED),
                        onDeleteTorrent = {
                            val intent = Intent().apply {
                                putExtra(TorrentActivity.Extras.TORRENT_DELETED, true)
                            }
                            requireActivity().setResult(Activity.RESULT_OK, intent)
                            requireActivity().finish()
                        },
                    )
                }
            }
        }
}

@Composable
private fun TorrentOverviewTab(
    fragment: TorrentOverviewFragment,
    serverId: Int,
    torrentHash: String,
    initialTorrentName: String?,
    isScreenActive: Boolean,
    onDeleteTorrent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentOverviewViewModel = hiltViewModel(
        creationCallback = { factory: TorrentOverviewViewModel.Factory ->
            factory.create(serverId, torrentHash)
        },
    ),
) {
    val activity = fragment.requireActivity()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val torrent by viewModel.torrent.collectAsStateWithLifecycle()
    val torrentProperties by viewModel.torrentProperties.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val torrentName by rememberSaveable(torrent?.name) { mutableStateOf(torrent?.name ?: initialTorrentName) }
    LaunchedEffect(torrentName) {
        torrentName?.let { torrentName ->
            (activity as AppCompatActivity).supportActionBar?.title = torrentName
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    val exportActivity =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-bittorrent")) { uri ->
            if (uri != null) {
                viewModel.exportTorrent(uri)
            }
        }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    LaunchedEffect(Unit) {
        activity.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent, menu)

                    scope.launch {
                        viewModel.torrent.collectLatest { torrent ->
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

                            reannounce.isEnabled = torrent != null &&
                                when (torrent.state) {
                                    TorrentState.PAUSED_UP, TorrentState.PAUSED_DL, TorrentState.QUEUED_UP,
                                    TorrentState.QUEUED_DL, TorrentState.ERROR, TorrentState.CHECKING_UP,
                                    TorrentState.CHECKING_DL,
                                    -> false

                                    else -> true
                                }

                            if (torrent != null) {
                                val isPaused = when (torrent.state) {
                                    TorrentState.PAUSED_DL, TorrentState.PAUSED_UP,
                                    TorrentState.MISSING_FILES, TorrentState.ERROR,
                                    -> true

                                    else -> false
                                }
                                resume.isVisible = isPaused
                                pause.isVisible = !isPaused

                                forceStart.isChecked = torrent.isForceStartEnabled
                                superSeeding.isChecked = torrent.isSuperSeedingEnabled
                            }
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_pause -> {
                            viewModel.pauseTorrent()
                        }
                        R.id.menu_resume -> {
                            viewModel.resumeTorrent()
                        }
                        R.id.menu_delete -> {
                            currentDialog = Dialog.Delete
                        }
                        R.id.menu_options -> {
                            currentDialog = Dialog.Options
                        }
                        R.id.menu_category -> {
                            currentDialog = Dialog.SetCategory
                        }
                        R.id.menu_tags -> {
                            currentDialog = Dialog.SetTags
                        }
                        R.id.menu_rename -> {
                            currentDialog = Dialog.Rename
                        }
                        R.id.menu_recheck -> {
                            currentDialog = Dialog.Recheck
                        }
                        R.id.menu_reannounce -> {
                            viewModel.reannounceTorrent()
                        }
                        R.id.menu_force_start -> {
                            val isEnabled = viewModel.torrent.value?.isForceStartEnabled ?: return true
                            viewModel.setForceStart(!isEnabled)
                        }
                        R.id.menu_super_seeding -> {
                            val isEnabled = viewModel.torrent.value?.isSuperSeedingEnabled ?: return true
                            viewModel.setSuperSeeding(!isEnabled)
                        }
                        R.id.menu_copy_name -> {
                            val torrent = viewModel.torrent.value
                            if (torrent != null) {
                                context.copyToClipboard(torrent.name)
                            }
                        }
                        R.id.menu_copy_hash_v1 -> {
                            val torrent = viewModel.torrent.value
                            if (torrent?.hashV1 != null) {
                                context.copyToClipboard(torrent.hashV1)
                            }
                        }
                        R.id.menu_copy_hash_v2 -> {
                            val torrent = viewModel.torrent.value
                            if (torrent?.hashV2 != null) {
                                context.copyToClipboard(torrent.hashV2)
                            }
                        }
                        R.id.menu_copy_magnet -> {
                            val torrent = viewModel.torrent.value
                            if (torrent != null) {
                                context.copyToClipboard(torrent.magnetUri)
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
            fragment.viewLifecycleOwner,
        )
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentOverviewViewModel.Event.Error -> {
                fragment.showSnackbar(getErrorMessage(context, event.error), view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentNotFound -> {
                fragment.showSnackbar(R.string.torrent_error_not_found, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentDeleted -> {
                onDeleteTorrent()
            }
            TorrentOverviewViewModel.Event.TorrentPaused -> {
                fragment.showSnackbar(R.string.torrent_paused_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentResumed -> {
                fragment.showSnackbar(R.string.torrent_resumed_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.OptionsUpdated -> {
                fragment.showSnackbar(R.string.torrent_option_update_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentRechecked -> {
                fragment.showSnackbar(R.string.torrent_recheck_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentReannounced -> {
                fragment.showSnackbar(R.string.torrent_reannounce_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentRenamed -> {
                fragment.showSnackbar(R.string.torrent_rename_success, view = activity.view)
                viewModel.loadTorrent()
            }
            is TorrentOverviewViewModel.Event.ForceStartChanged -> {
                fragment.showSnackbar(
                    if (event.isEnabled) {
                        R.string.torrent_enable_force_start_success
                    } else {
                        R.string.torrent_disable_force_start_success
                    },
                    view = activity.view,
                )
            }
            is TorrentOverviewViewModel.Event.SuperSeedingChanged -> {
                fragment.showSnackbar(
                    if (event.isEnabled) {
                        R.string.torrent_enable_super_seeding_success
                    } else {
                        R.string.torrent_disable_super_seeding_success
                    },
                    view = activity.view,
                )
            }
            TorrentOverviewViewModel.Event.CategoryUpdated -> {
                fragment.showSnackbar(R.string.torrent_category_update_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TagsUpdated -> {
                fragment.showSnackbar(R.string.torrent_tags_update_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentExported -> {
                fragment.showSnackbar(R.string.torrent_export_success, view = activity.view)
            }
            TorrentOverviewViewModel.Event.TorrentExportError -> {
                fragment.showSnackbar(R.string.torrent_export_error, view = activity.view)
            }
        }
    }

    when (currentDialog) {
        Dialog.Delete -> {
            DeleteDialog(
                onDismiss = { currentDialog = null },
                onDelete = { deleteFiles ->
                    viewModel.deleteTorrent(deleteFiles)
                    currentDialog = null
                },
            )
        }
        Dialog.Recheck -> {
            RecheckDialog(
                onDismiss = { currentDialog = null },
                onRecheck = {
                    viewModel.recheckTorrent()
                    currentDialog = null
                },
            )
        }
        Dialog.Rename -> {
            RenameDialog(
                initialName = torrentName,
                onDismiss = { currentDialog = null },
                onRename = { newName ->
                    viewModel.renameTorrent(newName)
                    currentDialog = null
                },
            )
        }
        Dialog.SetCategory -> {
            SetCategoryDialog(
                initialSelectedCategory = torrent?.category,
                categories = categories,
                onDismiss = { currentDialog = null },
                onConfirm = {
                    viewModel.setCategory(it)
                    currentDialog = null
                },
            )
        }
        Dialog.SetTags -> {
            SetTagsDialog(
                initialSelectedTags = torrent?.tags ?: emptyList(),
                tags = tags,
                onDismiss = { currentDialog = null },
                onConfirm = {
                    viewModel.setTags(it)
                    currentDialog = null
                },
            )
        }
        Dialog.Options -> {
            val currentTorrent = torrent
            if (currentTorrent != null) {
                TorrentOptionsDialog(
                    torrent = currentTorrent,
                    onDismiss = { currentDialog = null },
                    onConfirm = {
                            autoTmm,
                            savePath,
                            downloadPath,
                            toggleSequentialDownload,
                            togglePrioritizeFirstLastPiece,
                            uploadSpeedLimit,
                            downloadSpeedLimit,
                            ratioLimit,
                            seedingTimeLimit,
                            inactiveSeedingTimeLimit,
                        ->
                        viewModel.setTorrentOptions(
                            autoTmm = autoTmm,
                            savePath = savePath,
                            downloadPath = downloadPath,
                            toggleSequentialDownload = toggleSequentialDownload,
                            togglePrioritizeFirstLastPiece = togglePrioritizeFirstLastPiece,
                            uploadSpeedLimit = uploadSpeedLimit,
                            downloadSpeedLimit = downloadSpeedLimit,
                            ratioLimit = ratioLimit,
                            seedingTimeLimit = seedingTimeLimit,
                            inactiveSeedingTimeLimit = inactiveSeedingTimeLimit,
                        )
                        currentDialog = null
                    },
                )
            } else {
                currentDialog = null
            }
        }
        null -> {}
    }

    PersistentLaunchedEffect(currentDialog == Dialog.SetCategory) {
        if (currentDialog == Dialog.SetCategory) {
            viewModel.loadCategories()
        } else {
            currentDialog = null
            viewModel.resetCategories()
        }
    }

    PersistentLaunchedEffect(currentDialog == Dialog.SetTags) {
        if (currentDialog == Dialog.SetTags) {
            viewModel.loadTags()
        } else {
            currentDialog = null
            viewModel.resetTags()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshTorrent() },
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .imePadding(),
    ) {
        AnimatedNullableVisibility(
            values = listOf(torrent, torrentProperties, torrentName),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) { _, value ->
            val torrent = value[0] as Torrent
            val properties = value[1] as TorrentProperties
            val torrentName = value[2] as String

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp),
            ) {
                Text(
                    text = torrentName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Progress(
                    torrent = torrent,
                    modifier = Modifier.fillMaxWidth(),
                )

                val labelWidth = listOfNotNull(
                    R.string.torrent_overview_total_size,
                    R.string.torrent_overview_added_on,
                    if (torrent.isPrivate != null) R.string.torrent_overview_private else null,
                    R.string.torrent_overview_hash_v1,
                    R.string.torrent_overview_hash_v2,
                    R.string.torrent_option_save_path,
                    R.string.torrent_overview_comment,
                    R.string.torrent_overview_pieces,
                    R.string.torrent_overview_completed_on,
                    R.string.torrent_overview_created_by,
                    R.string.torrent_overview_created_on,

                    R.string.torrent_overview_time_active,
                    R.string.torrent_overview_downloaded,
                    R.string.torrent_overview_uploaded,
                    R.string.torrent_overview_reannounce_in,
                    R.string.torrent_overview_last_activity,
                    R.string.torrent_overview_last_seen_complete,
                    R.string.torrent_overview_connections,
                    R.string.torrent_overview_seeds,
                    R.string.torrent_overview_peers,
                    R.string.torrent_overview_wasted,
                    R.string.torrent_overview_availability,
                    if (torrent.popularity != null) R.string.torrent_overview_popularity else null,
                ).maxOf { measureTextWidth(stringResource(it)) }

                CompositionLocalProvider(LocalLabelWidth provides labelWidth) {
                    ElevatedCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.torrent_overview_information),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_total_size),
                                value = properties.totalSize?.let { formatBytes(it) },
                            )
                            InfoRow(
                                label = stringResource(R.string.torrent_overview_added_on),
                                value = formatDate(properties.additionDate),
                            )

                            if (torrent.isPrivate != null) {
                                InfoRow(
                                    label = stringResource(R.string.torrent_overview_private),
                                    value = if (torrent.isPrivate) {
                                        stringResource(R.string.torrent_overview_private_yes)
                                    } else {
                                        stringResource(R.string.torrent_overview_private_no)
                                    },
                                )
                            }

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_hash_v1),
                                value = torrent.hashV1,
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_hash_v2),
                                value = torrent.hashV2,
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_option_save_path),
                                value = properties.savePath,
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_comment),
                                value = properties.comment ?: "-",
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_pieces),
                                value = if (properties.piecesCount != null && properties.pieceSize != null) {
                                    stringResource(
                                        R.string.torrent_overview_pieces_format,
                                        properties.piecesCount,
                                        formatBytes(properties.pieceSize),
                                        properties.piecesHave,
                                    )
                                } else {
                                    null
                                },
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_completed_on),
                                value = properties.completionDate?.let { formatDate(it) },
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_created_by),
                                value = properties.createdBy,
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_created_on),
                                value = properties.creationDate?.let { formatDate(it) },
                            )
                        }
                    }

                    ElevatedCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.torrent_overview_transfer),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_time_active),
                                value = if (torrent.seedingTime > 0) {
                                    stringResource(
                                        R.string.torrent_overview_time_active_seeding_time_format,
                                        formatSeconds(torrent.timeActive),
                                        formatSeconds(torrent.seedingTime),
                                    )
                                } else {
                                    formatSeconds(torrent.timeActive)
                                },
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_downloaded),
                                value = stringResource(
                                    R.string.torrent_overview_downloaded_format,
                                    formatBytes(torrent.downloaded),
                                    formatBytes(torrent.downloadedSession),
                                ),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_uploaded),
                                value = stringResource(
                                    R.string.torrent_overview_uploaded_format,
                                    formatBytes(torrent.uploaded),
                                    formatBytes(torrent.uploadedSession),
                                ),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_reannounce_in),
                                value = formatSeconds(properties.nextReannounce),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_last_activity),
                                value = formatDate(torrent.lastActivity),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_last_seen_complete),
                                value = torrent.lastSeenComplete?.let { formatDate(it) },
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_connections),
                                value = stringResource(
                                    R.string.torrent_overview_connections_format,
                                    properties.connections,
                                    properties.connectionsLimit,
                                ),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_seeds),
                                value = stringResource(
                                    R.string.torrent_overview_seeds_format,
                                    properties.seeds,
                                    properties.seedsTotal,
                                ),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_peers),
                                value = stringResource(
                                    R.string.torrent_overview_peers_format,
                                    properties.peers,
                                    properties.peersTotal,
                                ),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_wasted),
                                value = formatBytes(properties.wasted),
                            )

                            InfoRow(
                                label = stringResource(R.string.torrent_overview_availability),
                                value = torrent.availability?.floorToDecimal(3)?.toString(),
                            )
                            if (torrent.popularity != null) {
                                InfoRow(
                                    label = stringResource(R.string.torrent_overview_popularity),
                                    value = torrent.popularity.floorToDecimal(2).toString(),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }

        AnimatedVisibility(
            visible = isNaturalLoading == true,
            enter = expandVertically(tween(durationMillis = 500)),
            exit = shrinkVertically(tween(durationMillis = 500)),
        ) {
            LinearProgressIndicator(
                strokeCap = StrokeCap.Butt,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun Progress(torrent: Torrent, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val categoryTagList = remember(torrent.category, torrent.tags) {
                if (torrent.category == null) {
                    torrent.tags
                } else {
                    listOf(torrent.category) + torrent.tags
                }
            }

            AnimatedContent(
                targetState = (torrent.category != null) to categoryTagList,
                modifier = Modifier.fillMaxWidth(),
            ) { (hasCategory, categoryTag) ->
                FlowRow(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasCategory) {
                        CategoryChip(category = categoryTag.first())

                        categoryTag.drop(1).forEach { tag ->
                            TagChip(tag = tag)
                        }
                    } else {
                        categoryTag.forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.torrent_item_progress_format,
                        formatBytes(torrent.completed),
                        formatBytes(torrent.size),
                        if (torrent.progress < 1) {
                            (torrent.progress * 100).floorToDecimal(1).toString()
                        } else {
                            "100"
                        },
                        torrent.ratio.floorToDecimal(2).toString(),
                    ),
                )

                if (torrent.eta != null) {
                    Text(text = formatSeconds(torrent.eta))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val progressColor by animateColorAsState(
                targetValue = harmonizeWithPrimary(getTorrentStateColor(torrent.state)),
                animationSpec = tween(),
            )
            val trackColor = progressColor.copy(alpha = MaterialColors.ALPHA_DISABLED)

            val progressAnimated by animateFloatAsState(
                targetValue = torrent.progress.toFloat(),
                animationSpec = tween(),
            )

            LinearProgressIndicator(
                progress = { progressAnimated },
                color = progressColor,
                trackColor = trackColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = formatTorrentState(torrent.state))

                val speedText = buildList {
                    if (torrent.downloadSpeed > 0) {
                        add("↓ ${formatBytesPerSecond(torrent.downloadSpeed)}")
                    }
                    if (torrent.uploadSpeed > 0) {
                        add("↑ ${formatBytesPerSecond(torrent.uploadSpeed)}")
                    }
                }.joinToString(" ")
                Text(text = speedText)
            }
        }
    }
}

private val LocalLabelWidth = staticCompositionLocalOf { 0.dp }

@Composable
private fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val insetsWidth = WindowInsets.safeDrawing.asPaddingValues().let {
            it.calculateLeftPadding(LayoutDirection.Ltr) + it.calculateRightPadding(LayoutDirection.Ltr)
        }
        val paddingWidth = 40.dp
        val maxWidth = (screenWidth - insetsWidth - paddingWidth) * 0.4f
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(LocalLabelWidth.current.coerceAtMost(maxWidth)),
        )

        if (value != null) {
            SelectionContainer {
                Text(text = value)
            }
        } else {
            Text(text = "-")
        }
    }
}

@Serializable
private sealed class Dialog {
    @Serializable
    data object Delete : Dialog()

    @Serializable
    data object Recheck : Dialog()

    @Serializable
    data object Rename : Dialog()

    @Serializable
    data object SetCategory : Dialog()

    @Serializable
    data object SetTags : Dialog()

    @Serializable
    data object Options : Dialog()
}

@Composable
private fun DeleteDialog(onDismiss: () -> Unit, onDelete: (deleteFiles: Boolean) -> Unit, modifier: Modifier = Modifier) {
    var deleteFiles by rememberSaveable { mutableStateOf(false) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 16.dp,
        title = { Text(text = stringResource(R.string.torrent_delete)) },
        text = {
            CheckboxWithLabel(
                checked = deleteFiles,
                onCheckedChange = { deleteFiles = it },
                label = stringResource(R.string.torrent_delete_files),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onDelete(deleteFiles) },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun RecheckDialog(onDismiss: () -> Unit, onRecheck: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_action_force_recheck)) },
        text = { Text(text = stringResource(R.string.torrent_force_recheck_confirm)) },
        confirmButton = {
            TextButton(onClick = onRecheck) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun RenameDialog(
    initialName: String?,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName ?: "", TextRange(Int.MAX_VALUE)))
    }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_action_rename_torrent)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = {
                    Text(
                        text = stringResource(R.string.torrent_rename_torrent_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { error -> { Text(text = stringResource(error)) } },
                trailingIcon = error?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.text.isNotBlank()) {
                            onRename(name.text)
                        } else {
                            error = R.string.torrent_rename_name_cannot_be_blank
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onRename(name.text)
                    } else {
                        error = R.string.torrent_rename_name_cannot_be_blank
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun SetCategoryDialog(
    initialSelectedCategory: String?,
    categories: List<String>?,
    onDismiss: () -> Unit,
    onConfirm: (category: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(initialSelectedCategory) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_action_category)) },
        text = {
            if (categories == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (categories.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory != category) category else null
                            },
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.torrent_no_categories),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categories != null) {
                        onConfirm(selectedCategory)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun SetTagsDialog(
    initialSelectedTags: List<String>,
    tags: List<String>?,
    onDismiss: () -> Unit,
    onConfirm: (tags: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTags = rememberSaveable(saver = stateListSaver()) { initialSelectedTags.toMutableStateList() }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_action_tags)) },
        text = {
            if (tags == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = selectedTags.contains(tag),
                            onClick = {
                                if (selectedTags.contains(tag)) {
                                    selectedTags.remove(tag)
                                } else {
                                    selectedTags.add(tag)
                                }
                            },
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.torrent_no_tags),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tags != null) {
                        onConfirm(selectedTags)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun TorrentOptionsDialog(
    torrent: Torrent,
    onDismiss: () -> Unit,
    onConfirm: (
        autoTmm: Boolean?,
        savePath: String?,
        downloadPath: String?,
        toggleSequentialDownload: Boolean,
        togglePrioritizeFirstLastPiece: Boolean,
        uploadSpeedLimit: Int?,
        downloadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        inactiveSeedingTimeLimit: Int?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAutoTmmEnabled by rememberSaveable {
        mutableStateOf(torrent.isAutomaticTorrentManagementEnabled)
    }
    var isDownloadPathEnabled by rememberSaveable {
        mutableStateOf(torrent.downloadPath != null)
    }
    var savePath by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(torrent.savePath ?: "", TextRange(Int.MAX_VALUE)))
    }
    var downloadPath by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(torrent.downloadPath ?: "", TextRange(Int.MAX_VALUE)))
    }
    var uploadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue((torrent.uploadSpeedLimit / 1024).toString(), TextRange(Int.MAX_VALUE)))
    }
    var downloadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue((torrent.downloadSpeedLimit / 1024).toString(), TextRange(Int.MAX_VALUE)))
    }
    var isSequentialDownloadEnabled by rememberSaveable {
        mutableStateOf(torrent.isSequentialDownloadEnabled)
    }
    var isPrioritizeFirstLastPiecesEnabled by rememberSaveable {
        mutableStateOf(torrent.isFirstLastPiecesPrioritized)
    }

    var selectedShareLimitOption by rememberSaveable {
        mutableIntStateOf(
            when {
                torrent.seedingTimeLimit == -2 && torrent.ratioLimit == -2.0 -> 0
                torrent.seedingTimeLimit == -1 && torrent.ratioLimit == -1.0 -> 1
                else -> 2
            },
        )
    }
    var ratioLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(if (torrent.ratioLimit >= 0) torrent.ratioLimit.toString() else ""))
    }
    var seedingTimeLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(if (torrent.seedingTimeLimit >= 0) torrent.seedingTimeLimit.toString() else ""))
    }
    var inactiveSeedingTimeLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                if (torrent.inactiveSeedingTimeLimit >= 0) torrent.inactiveSeedingTimeLimit.toString() else "",
            ),
        )
    }

    LaunchedEffect(Unit) {
        val separatorIndex = ratioLimit.text.indexOfFirst { !it.isDigit() }
        if (separatorIndex != -1) {
            val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
            val stringBuilder = StringBuilder(ratioLimit.text)
            stringBuilder[separatorIndex] = decimalSeparator
            ratioLimit = ratioLimit.copy(stringBuilder.toString())
        }
    }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 16.dp,
        title = { Text(text = stringResource(R.string.torrent_action_options)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CheckboxWithLabel(
                    checked = isAutoTmmEnabled,
                    onCheckedChange = {
                        isAutoTmmEnabled = it
                    },
                    label = stringResource(R.string.torrent_option_automatic_torrent_management),
                )

                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = savePath,
                            onValueChange = { savePath = it },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_add_save_path),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAutoTmmEnabled,
                        )

                        CheckboxWithLabel(
                            checked = isDownloadPathEnabled,
                            onCheckedChange = { isDownloadPathEnabled = it },
                            label = stringResource(R.string.torrent_option_download_path_enable),
                            enabled = !isAutoTmmEnabled,
                        )

                        OutlinedTextField(
                            value = downloadPath,
                            onValueChange = { downloadPath = it },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_download_path),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAutoTmmEnabled && isDownloadPathEnabled,
                        )
                    }
                }

                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.torrent_option_speed_limit),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        OutlinedTextField(
                            value = uploadSpeedLimit,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    uploadSpeedLimit = it
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_upload_speed_limit),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = downloadSpeedLimit,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    downloadSpeedLimit = it
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_download_speed_limit),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.torrent_option_share_limit),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.selectableGroup(),
                        ) {
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 0,
                                onClick = { selectedShareLimitOption = 0 },
                                label = stringResource(R.string.torrent_option_share_limit_global),
                            )
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 1,
                                onClick = { selectedShareLimitOption = 1 },
                                label = stringResource(R.string.torrent_option_share_limit_disable),
                            )
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 2,
                                onClick = { selectedShareLimitOption = 2 },
                                label = stringResource(R.string.torrent_option_share_limit_custom),
                            )
                        }

                        val isCustomEnabled = selectedShareLimitOption == 2

                        val ratioLimitRegex = remember {
                            val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
                            Regex("^\\d*\\$decimalSeparator?\\d*$|^$")
                        }
                        OutlinedTextField(
                            value = ratioLimit,
                            onValueChange = {
                                if (ratioLimitRegex.matches(it.text)) {
                                    ratioLimit = it
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_share_limit_ratio),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isCustomEnabled,
                        )

                        OutlinedTextField(
                            value = seedingTimeLimit,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    seedingTimeLimit = it
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_share_limit_total_minutes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isCustomEnabled,
                        )

                        OutlinedTextField(
                            value = inactiveSeedingTimeLimit,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    inactiveSeedingTimeLimit = it
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.torrent_option_share_limit_inactive_minutes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isCustomEnabled,
                        )
                    }
                }

                CheckboxWithLabel(
                    checked = isPrioritizeFirstLastPiecesEnabled,
                    onCheckedChange = { isPrioritizeFirstLastPiecesEnabled = it },
                    label = stringResource(R.string.torrent_option_prioritize_first_last_piece),
                )

                CheckboxWithLabel(
                    checked = isSequentialDownloadEnabled,
                    onCheckedChange = { isSequentialDownloadEnabled = it },
                    label = stringResource(R.string.torrent_option_sequential_download),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val autoTmm = if (torrent.isAutomaticTorrentManagementEnabled != isAutoTmmEnabled) {
                        isAutoTmmEnabled
                    } else {
                        null
                    }

                    val finalSavePath =
                        if (!isAutoTmmEnabled && savePath.text.isNotBlank() && torrent.savePath != savePath.text) {
                            savePath.text
                        } else {
                            null
                        }

                    val finalDownloadPath = if (!isAutoTmmEnabled) {
                        val oldPath = torrent.downloadPath ?: ""
                        val newPath = if (isDownloadPathEnabled) downloadPath.text else ""
                        if (oldPath != newPath) newPath else null
                    } else {
                        null
                    }

                    val toggleSequentialDownload =
                        torrent.isSequentialDownloadEnabled != isSequentialDownloadEnabled

                    val togglePrioritizeFirstLastPiece =
                        torrent.isFirstLastPiecesPrioritized != isPrioritizeFirstLastPiecesEnabled

                    val finalUploadSpeedLimit = uploadSpeedLimit.text.toIntOrNull()?.let { limit ->
                        val uploadLimit = limit * 1024
                        if (uploadLimit != torrent.uploadSpeedLimit) uploadLimit else null
                    }

                    val finalDownloadSpeedLimit = downloadSpeedLimit.text.toIntOrNull()?.let { limit ->
                        val downloadLimit = limit * 1024
                        if (downloadLimit != torrent.downloadSpeedLimit) downloadLimit else null
                    }

                    val (finalRatioLimit, finalSeedingTimeLimit, finalInactiveSeedingTimeLimit) =
                        when (selectedShareLimitOption) {
                            0 -> Triple(-2.0, -2, -2)
                            1 -> Triple(-1.0, -1, -1)
                            2 -> {
                                val ratio = ratioLimit.text.toDoubleOrNull() ?: -1.0
                                val seeding = seedingTimeLimit.text.toIntOrNull() ?: -1
                                val inactive = inactiveSeedingTimeLimit.text.toIntOrNull() ?: -1

                                if (ratio != -1.0 || seeding != -1 || inactive != -1) {
                                    Triple(ratio, seeding, inactive)
                                } else {
                                    Triple(null, null, null)
                                }
                            }
                            else -> Triple(null, null, null)
                        }.let { (ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit) ->
                            if (ratioLimit == null || seedingTimeLimit == null || inactiveSeedingTimeLimit == null) {
                                Triple(null, null, null)
                            } else if (ratioLimit != torrent.ratioLimit ||
                                seedingTimeLimit != torrent.seedingTimeLimit ||
                                inactiveSeedingTimeLimit != torrent.inactiveSeedingTimeLimit
                            ) {
                                Triple(ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit)
                            } else {
                                Triple(null, null, null)
                            }
                        }

                    onConfirm(
                        autoTmm,
                        finalSavePath,
                        finalDownloadPath,
                        toggleSequentialDownload,
                        togglePrioritizeFirstLastPiece,
                        finalUploadSpeedLimit,
                        finalDownloadSpeedLimit,
                        finalRatioLimit,
                        finalSeedingTimeLimit,
                        finalInactiveSeedingTimeLimit,
                    )
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}
