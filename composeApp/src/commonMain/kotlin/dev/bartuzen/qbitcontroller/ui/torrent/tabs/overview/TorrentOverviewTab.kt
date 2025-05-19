package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.RadioButtonWithLabel
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.toClipEntry
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.torrent_action_category
import qbitcontroller.composeapp.generated.resources.torrent_action_copy
import qbitcontroller.composeapp.generated.resources.torrent_action_copy_hash_v1
import qbitcontroller.composeapp.generated.resources.torrent_action_copy_hash_v2
import qbitcontroller.composeapp.generated.resources.torrent_action_copy_magnet_uri
import qbitcontroller.composeapp.generated.resources.torrent_action_copy_name
import qbitcontroller.composeapp.generated.resources.torrent_action_delete
import qbitcontroller.composeapp.generated.resources.torrent_action_export
import qbitcontroller.composeapp.generated.resources.torrent_action_force_reannounce
import qbitcontroller.composeapp.generated.resources.torrent_action_force_recheck
import qbitcontroller.composeapp.generated.resources.torrent_action_force_start
import qbitcontroller.composeapp.generated.resources.torrent_action_options
import qbitcontroller.composeapp.generated.resources.torrent_action_pause
import qbitcontroller.composeapp.generated.resources.torrent_action_rename_torrent
import qbitcontroller.composeapp.generated.resources.torrent_action_resume
import qbitcontroller.composeapp.generated.resources.torrent_action_super_seeding
import qbitcontroller.composeapp.generated.resources.torrent_action_tags
import qbitcontroller.composeapp.generated.resources.torrent_add_save_path
import qbitcontroller.composeapp.generated.resources.torrent_category_update_success
import qbitcontroller.composeapp.generated.resources.torrent_delete
import qbitcontroller.composeapp.generated.resources.torrent_delete_files
import qbitcontroller.composeapp.generated.resources.torrent_disable_force_start_success
import qbitcontroller.composeapp.generated.resources.torrent_disable_super_seeding_success
import qbitcontroller.composeapp.generated.resources.torrent_enable_force_start_success
import qbitcontroller.composeapp.generated.resources.torrent_enable_super_seeding_success
import qbitcontroller.composeapp.generated.resources.torrent_error_not_found
import qbitcontroller.composeapp.generated.resources.torrent_export_error
import qbitcontroller.composeapp.generated.resources.torrent_export_success
import qbitcontroller.composeapp.generated.resources.torrent_force_recheck_confirm
import qbitcontroller.composeapp.generated.resources.torrent_item_progress_format
import qbitcontroller.composeapp.generated.resources.torrent_no_categories
import qbitcontroller.composeapp.generated.resources.torrent_no_tags
import qbitcontroller.composeapp.generated.resources.torrent_option_automatic_torrent_management
import qbitcontroller.composeapp.generated.resources.torrent_option_download_path
import qbitcontroller.composeapp.generated.resources.torrent_option_download_path_enable
import qbitcontroller.composeapp.generated.resources.torrent_option_download_speed_limit
import qbitcontroller.composeapp.generated.resources.torrent_option_prioritize_first_last_piece
import qbitcontroller.composeapp.generated.resources.torrent_option_save_path
import qbitcontroller.composeapp.generated.resources.torrent_option_sequential_download
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_custom
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_disable
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_global
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_inactive_minutes
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_ratio
import qbitcontroller.composeapp.generated.resources.torrent_option_share_limit_total_minutes
import qbitcontroller.composeapp.generated.resources.torrent_option_speed_limit
import qbitcontroller.composeapp.generated.resources.torrent_option_update_success
import qbitcontroller.composeapp.generated.resources.torrent_option_upload_speed_limit
import qbitcontroller.composeapp.generated.resources.torrent_overview_added_on
import qbitcontroller.composeapp.generated.resources.torrent_overview_availability
import qbitcontroller.composeapp.generated.resources.torrent_overview_comment
import qbitcontroller.composeapp.generated.resources.torrent_overview_completed_on
import qbitcontroller.composeapp.generated.resources.torrent_overview_connections
import qbitcontroller.composeapp.generated.resources.torrent_overview_connections_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_created_by
import qbitcontroller.composeapp.generated.resources.torrent_overview_created_on
import qbitcontroller.composeapp.generated.resources.torrent_overview_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_overview_downloaded_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_hash_v1
import qbitcontroller.composeapp.generated.resources.torrent_overview_hash_v2
import qbitcontroller.composeapp.generated.resources.torrent_overview_information
import qbitcontroller.composeapp.generated.resources.torrent_overview_last_activity
import qbitcontroller.composeapp.generated.resources.torrent_overview_last_seen_complete
import qbitcontroller.composeapp.generated.resources.torrent_overview_peers
import qbitcontroller.composeapp.generated.resources.torrent_overview_peers_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_piece_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_overview_piece_downloading
import qbitcontroller.composeapp.generated.resources.torrent_overview_piece_map
import qbitcontroller.composeapp.generated.resources.torrent_overview_piece_not_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_overview_pieces
import qbitcontroller.composeapp.generated.resources.torrent_overview_pieces_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_popularity
import qbitcontroller.composeapp.generated.resources.torrent_overview_private
import qbitcontroller.composeapp.generated.resources.torrent_overview_private_no
import qbitcontroller.composeapp.generated.resources.torrent_overview_private_yes
import qbitcontroller.composeapp.generated.resources.torrent_overview_reannounce_in
import qbitcontroller.composeapp.generated.resources.torrent_overview_seeds
import qbitcontroller.composeapp.generated.resources.torrent_overview_seeds_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_time_active
import qbitcontroller.composeapp.generated.resources.torrent_overview_time_active_seeding_time_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_total_size
import qbitcontroller.composeapp.generated.resources.torrent_overview_transfer
import qbitcontroller.composeapp.generated.resources.torrent_overview_uploaded
import qbitcontroller.composeapp.generated.resources.torrent_overview_uploaded_format
import qbitcontroller.composeapp.generated.resources.torrent_overview_wasted
import qbitcontroller.composeapp.generated.resources.torrent_paused_success
import qbitcontroller.composeapp.generated.resources.torrent_reannounce_success
import qbitcontroller.composeapp.generated.resources.torrent_recheck_success
import qbitcontroller.composeapp.generated.resources.torrent_rename_name_cannot_be_blank
import qbitcontroller.composeapp.generated.resources.torrent_rename_success
import qbitcontroller.composeapp.generated.resources.torrent_rename_torrent_hint
import qbitcontroller.composeapp.generated.resources.torrent_resumed_success
import qbitcontroller.composeapp.generated.resources.torrent_tags_update_success
import java.text.DecimalFormatSymbols
import kotlin.math.ceil

@Composable
fun TorrentOverviewTab(
    serverId: Int,
    torrentHash: String,
    initialTorrentName: String?,
    isScreenActive: Boolean,
    snackbarEventFlow: MutableSharedFlow<String>,
    titleEventFlow: MutableSharedFlow<String>,
    actionsEventFlow: MutableSharedFlow<Pair<Int, List<ActionMenuItem>>>,
    onDeleteTorrent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentOverviewViewModel = koinViewModel(parameters = { parametersOf(serverId, torrentHash) }),
) {
    val scope = rememberCoroutineScope()

    val torrent by viewModel.torrent.collectAsStateWithLifecycle()
    val torrentProperties by viewModel.torrentProperties.collectAsStateWithLifecycle()
    val pieces by viewModel.torrentPieces.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    val torrentName by rememberSaveable(torrent?.name) { mutableStateOf(torrent?.name ?: initialTorrentName) }
    LaunchedEffect(torrentName) {
        launch {
            torrentName?.let { titleEventFlow.emit(it) }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    var showCopyMenu by rememberSaveable { mutableStateOf(false) }
    val actions = listOfNotNull(
        when (torrent?.state) {
            null -> null
            TorrentState.PAUSED_DL, TorrentState.PAUSED_UP,
            TorrentState.MISSING_FILES, TorrentState.ERROR,
            -> ActionMenuItem(
                title = stringResource(Res.string.torrent_action_resume),
                onClick = { viewModel.resumeTorrent() },
                showAsAction = true,
                icon = Icons.Filled.PlayArrow,
            )
            else -> ActionMenuItem(
                title = stringResource(Res.string.torrent_action_pause),
                onClick = { viewModel.pauseTorrent() },
                showAsAction = true,
                icon = Icons.Filled.Pause,
            )
        },
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_delete),
            onClick = { currentDialog = Dialog.Delete },
            showAsAction = true,
            icon = Icons.Filled.Delete,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_options),
            onClick = { currentDialog = Dialog.Options },
            showAsAction = false,
            icon = Icons.Filled.Settings,
            isEnabled = torrent != null,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_category),
            onClick = { currentDialog = Dialog.SetCategory },
            showAsAction = false,
            icon = Icons.Filled.Folder,
            isEnabled = torrent != null,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_tags),
            onClick = { currentDialog = Dialog.SetTags },
            showAsAction = false,
            icon = Icons.AutoMirrored.Filled.Label,
            isEnabled = torrent != null,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_rename_torrent),
            onClick = { currentDialog = Dialog.Rename },
            showAsAction = false,
            icon = Icons.Filled.DriveFileRenameOutline,
            isEnabled = torrent != null,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_force_recheck),
            onClick = { currentDialog = Dialog.Recheck },
            showAsAction = false,
            icon = Icons.Filled.Refresh,
            isEnabled = torrent != null,
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_force_reannounce),
            onClick = { viewModel.reannounceTorrent() },
            showAsAction = false,
            icon = Icons.AutoMirrored.Filled.Send,
            isEnabled = when (torrent?.state) {
                TorrentState.PAUSED_UP, TorrentState.PAUSED_DL, TorrentState.QUEUED_UP,
                TorrentState.QUEUED_DL, TorrentState.ERROR, TorrentState.CHECKING_UP,
                TorrentState.CHECKING_DL, null,
                -> false
                else -> true
            },
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_force_start),
            onClick = {
                val isEnabled = torrent?.isForceStartEnabled
                if (isEnabled != null) {
                    viewModel.setForceStart(!isEnabled)
                }
            },
            showAsAction = false,
            icon = Icons.Filled.Start,
            isEnabled = torrent != null,
            trailingIcon = {
                Checkbox(
                    checked = torrent?.isForceStartEnabled == true,
                    onCheckedChange = null,
                )
            },
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_super_seeding),
            onClick = {
                val isEnabled = torrent?.isSuperSeedingEnabled
                if (isEnabled != null) {
                    viewModel.setSuperSeeding(!isEnabled)
                }
            },
            showAsAction = false,
            icon = Icons.Filled.Star,
            isEnabled = torrent != null,
            trailingIcon = {
                Checkbox(
                    checked = torrent?.isSuperSeedingEnabled == true,
                    onCheckedChange = null,
                )
            },
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_copy),
            onClick = { showCopyMenu = true },
            showAsAction = false,
            icon = Icons.Filled.ContentCopy,
            isEnabled = torrent != null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null,
                )
            },
            dropdownMenu = {
                val scrollState = rememberScrollState()
                PersistentLaunchedEffect(showCopyMenu) {
                    if (showCopyMenu) {
                        scrollState.scrollTo(0)
                    }
                }

                val clipboard = LocalClipboard.current
                fun String.copyToClipboard() {
                    scope.launch {
                        clipboard.setClipEntry(toClipEntry())
                    }
                }

                DropdownMenu(
                    expanded = showCopyMenu,
                    onDismissRequest = { showCopyMenu = false },
                    scrollState = scrollState,
                ) {
                    Text(
                        text = stringResource(Res.string.torrent_action_copy),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(Res.string.torrent_action_copy_name))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Title, contentDescription = null)
                        },
                        onClick = {
                            torrent?.name?.copyToClipboard()
                            showCopyMenu = false
                        },
                        enabled = torrent?.name != null,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(Res.string.torrent_action_copy_hash_v1))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Key, contentDescription = null)
                        },
                        onClick = {
                            torrent?.hashV1?.copyToClipboard()
                            showCopyMenu = false
                        },
                        enabled = torrent?.hashV1 != null,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(Res.string.torrent_action_copy_hash_v2))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Key, contentDescription = null)
                        },
                        onClick = {
                            torrent?.hashV2?.copyToClipboard()
                            showCopyMenu = false
                        },
                        enabled = torrent?.hashV2 != null,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(Res.string.torrent_action_copy_magnet_uri))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Link, contentDescription = null)
                        },
                        onClick = {
                            torrent?.magnetUri?.copyToClipboard()
                            showCopyMenu = false
                        },
                        enabled = torrent?.magnetUri != null,
                    )
                }
            },
        ),
        ActionMenuItem(
            title = stringResource(Res.string.torrent_action_export),
            onClick = {
                torrent?.let { torrent ->
                    scope.launch {
                        val file = FileKit.openFileSaver(
                            suggestedName = torrent.name,
                            extension = "torrent",
                        )
                        if (file != null) {
                            viewModel.exportTorrent(file)
                        }
                    }
                }
            },
            showAsAction = false,
            icon = Icons.Filled.FileDownload,
            isEnabled = torrent != null,
        ),
    )

    LaunchedEffect(actions) {
        launch {
            actionsEventFlow.emit(0 to actions)
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentOverviewViewModel.Event.Error -> {
                snackbarEventFlow.emit(getErrorMessage(event.error))
            }
            TorrentOverviewViewModel.Event.TorrentNotFound -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_error_not_found))
            }
            TorrentOverviewViewModel.Event.TorrentDeleted -> {
                onDeleteTorrent()
            }
            TorrentOverviewViewModel.Event.TorrentPaused -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_paused_success))
            }
            TorrentOverviewViewModel.Event.TorrentResumed -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_resumed_success))
            }
            TorrentOverviewViewModel.Event.OptionsUpdated -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_option_update_success))
            }
            TorrentOverviewViewModel.Event.TorrentRechecked -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_recheck_success))
            }
            TorrentOverviewViewModel.Event.TorrentReannounced -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_reannounce_success))
            }
            TorrentOverviewViewModel.Event.TorrentRenamed -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_rename_success))
                viewModel.loadTorrent()
            }
            is TorrentOverviewViewModel.Event.ForceStartChanged -> {
                snackbarEventFlow.emit(
                    if (event.isEnabled) {
                        getString(Res.string.torrent_enable_force_start_success)
                    } else {
                        getString(Res.string.torrent_disable_force_start_success)
                    },
                )
            }
            is TorrentOverviewViewModel.Event.SuperSeedingChanged -> {
                snackbarEventFlow.emit(
                    if (event.isEnabled) {
                        getString(Res.string.torrent_enable_super_seeding_success)
                    } else {
                        getString(Res.string.torrent_disable_super_seeding_success)
                    },
                )
            }
            TorrentOverviewViewModel.Event.CategoryUpdated -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_category_update_success))
            }
            TorrentOverviewViewModel.Event.TagsUpdated -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_tags_update_success))
            }
            TorrentOverviewViewModel.Event.TorrentExported -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_export_success))
            }
            TorrentOverviewViewModel.Event.TorrentExportError -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_export_error))
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

    var showPiecesSheet by rememberSaveable { mutableStateOf(false) }
    if (showPiecesSheet) {
        PiecesBottomSheet(
            pieces = pieces ?: emptyList(),
            onDismiss = { showPiecesSheet = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTorrent() },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            if (torrent == null || torrentProperties == null || torrentName == null) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            }

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

                    ElevatedCard(
                        onClick = { showPiecesSheet = true },
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.torrent_overview_pieces),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )

                            PieceBar(
                                pieces = pieces ?: emptyList(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                        }
                    }

                    val labelWidth = listOfNotNull(
                        Res.string.torrent_overview_total_size,
                        Res.string.torrent_overview_added_on,
                        if (torrent.isPrivate != null) Res.string.torrent_overview_private else null,
                        Res.string.torrent_overview_hash_v1,
                        Res.string.torrent_overview_hash_v2,
                        Res.string.torrent_option_save_path,
                        Res.string.torrent_overview_comment,
                        Res.string.torrent_overview_pieces,
                        Res.string.torrent_overview_completed_on,
                        Res.string.torrent_overview_created_by,
                        Res.string.torrent_overview_created_on,

                        Res.string.torrent_overview_time_active,
                        Res.string.torrent_overview_downloaded,
                        Res.string.torrent_overview_uploaded,
                        Res.string.torrent_overview_reannounce_in,
                        Res.string.torrent_overview_last_activity,
                        Res.string.torrent_overview_last_seen_complete,
                        Res.string.torrent_overview_connections,
                        Res.string.torrent_overview_seeds,
                        Res.string.torrent_overview_peers,
                        Res.string.torrent_overview_wasted,
                        Res.string.torrent_overview_availability,
                        if (torrent.popularity != null) Res.string.torrent_overview_popularity else null,
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
                                    text = stringResource(Res.string.torrent_overview_information),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_total_size),
                                    value = properties.totalSize?.let { formatBytes(it) },
                                )
                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_added_on),
                                    value = formatDate(properties.additionDate),
                                )

                                if (torrent.isPrivate != null) {
                                    InfoRow(
                                        label = stringResource(Res.string.torrent_overview_private),
                                        value = if (torrent.isPrivate) {
                                            stringResource(Res.string.torrent_overview_private_yes)
                                        } else {
                                            stringResource(Res.string.torrent_overview_private_no)
                                        },
                                    )
                                }

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_hash_v1),
                                    value = torrent.hashV1,
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_hash_v2),
                                    value = torrent.hashV2,
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_option_save_path),
                                    value = properties.savePath,
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_comment),
                                    value = properties.comment,
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_pieces),
                                    value = if (properties.piecesCount != null && properties.pieceSize != null) {
                                        stringResource(
                                            Res.string.torrent_overview_pieces_format,
                                            properties.piecesCount,
                                            formatBytes(properties.pieceSize),
                                            properties.piecesHave,
                                        )
                                    } else {
                                        null
                                    },
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_completed_on),
                                    value = properties.completionDate?.let { formatDate(it) },
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_created_by),
                                    value = properties.createdBy,
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_created_on),
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
                                    text = stringResource(Res.string.torrent_overview_transfer),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_time_active),
                                    value = if (torrent.seedingTime > 0) {
                                        stringResource(
                                            Res.string.torrent_overview_time_active_seeding_time_format,
                                            formatSeconds(torrent.timeActive),
                                            formatSeconds(torrent.seedingTime),
                                        )
                                    } else {
                                        formatSeconds(torrent.timeActive)
                                    },
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_downloaded),
                                    value = stringResource(
                                        Res.string.torrent_overview_downloaded_format,
                                        formatBytes(torrent.downloaded),
                                        formatBytes(torrent.downloadedSession),
                                    ),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_uploaded),
                                    value = stringResource(
                                        Res.string.torrent_overview_uploaded_format,
                                        formatBytes(torrent.uploaded),
                                        formatBytes(torrent.uploadedSession),
                                    ),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_reannounce_in),
                                    value = formatSeconds(properties.nextReannounce),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_last_activity),
                                    value = formatDate(torrent.lastActivity),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_last_seen_complete),
                                    value = torrent.lastSeenComplete?.let { formatDate(it) },
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_connections),
                                    value = stringResource(
                                        Res.string.torrent_overview_connections_format,
                                        properties.connections,
                                        properties.connectionsLimit,
                                    ),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_seeds),
                                    value = stringResource(
                                        Res.string.torrent_overview_seeds_format,
                                        properties.seeds,
                                        properties.seedsTotal,
                                    ),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_peers),
                                    value = stringResource(
                                        Res.string.torrent_overview_peers_format,
                                        properties.peers,
                                        properties.peersTotal,
                                    ),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_wasted),
                                    value = formatBytes(properties.wasted),
                                )

                                InfoRow(
                                    label = stringResource(Res.string.torrent_overview_availability),
                                    value = torrent.availability?.floorToDecimal(3)?.toString(),
                                )
                                if (torrent.popularity != null) {
                                    InfoRow(
                                        label = stringResource(Res.string.torrent_overview_popularity),
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
                        Res.string.torrent_item_progress_format,
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
            val trackColor = progressColor.copy(alpha = 0.38f)

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

@Composable
private fun PieceBar(pieces: List<PieceState>, modifier: Modifier) {
    val pieceGroups = remember(pieces) {
        if (pieces.isEmpty()) {
            emptyList()
        } else {
            val groups = mutableListOf<Triple<PieceState, Int, Int>>()
            var startIndex = 0
            var currentState = pieces[0]

            for (i in 1 until pieces.size) {
                if (pieces[i] != currentState) {
                    groups.add(Triple(currentState, startIndex, i))
                    startIndex = i
                    currentState = pieces[i]
                }
            }

            groups.add(Triple(currentState, startIndex, pieces.size))
            groups
        }
    }

    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (pieces.isEmpty()) {
            drawRect(
                color = color,
                size = size,
                alpha = 0.25f,
            )
            return@Canvas
        }

        val pieceWidth = size.width / pieces.size

        pieceGroups.forEach { (state, startIndex, endIndex) ->
            val x = if (layoutDirection == LayoutDirection.Rtl) {
                size.width - (endIndex * pieceWidth)
            } else {
                startIndex * pieceWidth
            }

            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(width = (endIndex - startIndex) * pieceWidth, height = size.height),
                alpha = when (state) {
                    PieceState.NOT_DOWNLOADED -> 0.25f
                    PieceState.DOWNLOADING -> 0.5f
                    PieceState.DOWNLOADED -> 1f
                },
            )
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
        val density = LocalDensity.current
        val screenWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
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

@Composable
private fun PiecesBottomSheet(pieces: List<PieceState>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.torrent_overview_piece_map),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            val color = MaterialTheme.colorScheme.primary
            FlowRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LegendItem(
                    color = color.copy(alpha = 0.25f),
                    label = stringResource(Res.string.torrent_overview_piece_not_downloaded),
                )
                LegendItem(
                    color = color.copy(alpha = 0.5f),
                    label = stringResource(Res.string.torrent_overview_piece_downloading),
                )
                LegendItem(
                    color = color.copy(alpha = 1f),
                    label = stringResource(Res.string.torrent_overview_piece_downloaded),
                )
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val pieceSize = 16.dp
                val spacing = 4.dp
                val piecesPerRow = (maxWidth / (pieceSize + spacing)).toInt().coerceAtLeast(1)
                val rows = ceil(pieces.size.toFloat() / piecesPerRow).toInt()

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    items(rows) { rowIndex ->
                        val startIndex = rowIndex * piecesPerRow
                        val endIndex = minOf((rowIndex + 1) * piecesPerRow, pieces.size)
                        val rowPieces = pieces.slice(startIndex until endIndex)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pieceSize),
                        ) {
                            val pieceSizePx = pieceSize.toPx()
                            val spacingPx = spacing.toPx()

                            val rowWidth = (pieceSize + spacing) * piecesPerRow - spacing
                            val startPaddingPx = ((maxWidth - rowWidth) / 2).toPx()

                            rowPieces.forEachIndexed { colIndex, piece ->
                                val x = (colIndex * (pieceSizePx + spacingPx) + startPaddingPx).let {
                                    if (layoutDirection == LayoutDirection.Rtl) size.width - pieceSizePx - it else it
                                }

                                drawRect(
                                    color = color,
                                    topLeft = Offset(x, 0f),
                                    size = Size(pieceSizePx, pieceSizePx),
                                    alpha = when (piece) {
                                        PieceState.NOT_DOWNLOADED -> 0.25f
                                        PieceState.DOWNLOADING -> 0.5f
                                        PieceState.DOWNLOADED -> 1f
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawRect(
                color = color,
                size = size,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
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
        title = { Text(text = stringResource(Res.string.torrent_delete)) },
        text = {
            CheckboxWithLabel(
                checked = deleteFiles,
                onCheckedChange = { deleteFiles = it },
                label = stringResource(Res.string.torrent_delete_files),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onDelete(deleteFiles) },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun RecheckDialog(onDismiss: () -> Unit, onRecheck: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_action_force_recheck)) },
        text = { Text(text = stringResource(Res.string.torrent_force_recheck_confirm)) },
        confirmButton = {
            TextButton(onClick = onRecheck) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
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
    var error by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_action_rename_torrent)) },
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
                        text = stringResource(Res.string.torrent_rename_torrent_hint),
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
                            error = Res.string.torrent_rename_name_cannot_be_blank
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
                        error = Res.string.torrent_rename_name_cannot_be_blank
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
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
        title = { Text(text = stringResource(Res.string.torrent_action_category)) },
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
                    text = stringResource(Res.string.torrent_no_categories),
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
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
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
        title = { Text(text = stringResource(Res.string.torrent_action_tags)) },
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
                    text = stringResource(Res.string.torrent_no_tags),
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
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
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
        title = { Text(text = stringResource(Res.string.torrent_action_options)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CheckboxWithLabel(
                    checked = isAutoTmmEnabled,
                    onCheckedChange = {
                        isAutoTmmEnabled = it
                    },
                    label = stringResource(Res.string.torrent_option_automatic_torrent_management),
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
                                    text = stringResource(Res.string.torrent_add_save_path),
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
                            label = stringResource(Res.string.torrent_option_download_path_enable),
                            enabled = !isAutoTmmEnabled,
                        )

                        OutlinedTextField(
                            value = downloadPath,
                            onValueChange = { downloadPath = it },
                            label = {
                                Text(
                                    text = stringResource(Res.string.torrent_option_download_path),
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
                            text = stringResource(Res.string.torrent_option_speed_limit),
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
                                    text = stringResource(Res.string.torrent_option_upload_speed_limit),
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
                                    text = stringResource(Res.string.torrent_option_download_speed_limit),
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
                            text = stringResource(Res.string.torrent_option_share_limit),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.selectableGroup(),
                        ) {
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 0,
                                onClick = { selectedShareLimitOption = 0 },
                                label = stringResource(Res.string.torrent_option_share_limit_global),
                            )
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 1,
                                onClick = { selectedShareLimitOption = 1 },
                                label = stringResource(Res.string.torrent_option_share_limit_disable),
                            )
                            RadioButtonWithLabel(
                                selected = selectedShareLimitOption == 2,
                                onClick = { selectedShareLimitOption = 2 },
                                label = stringResource(Res.string.torrent_option_share_limit_custom),
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
                                    text = stringResource(Res.string.torrent_option_share_limit_ratio),
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
                                    text = stringResource(Res.string.torrent_option_share_limit_total_minutes),
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
                                    text = stringResource(Res.string.torrent_option_share_limit_inactive_minutes),
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
                    label = stringResource(Res.string.torrent_option_prioritize_first_last_piece),
                )

                CheckboxWithLabel(
                    checked = isSequentialDownloadEnabled,
                    onCheckedChange = { isSequentialDownloadEnabled = it },
                    label = stringResource(Res.string.torrent_option_sequential_download),
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
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
    )
}
