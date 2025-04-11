package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.material.color.MaterialColors
import dev.bartuzen.qbitcontroller.BuildConfig
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.ServerState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.EmptyListMessage
import dev.bartuzen.qbitcontroller.ui.components.SearchBar
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.ui.icons.Priority
import dev.bartuzen.qbitcontroller.ui.log.LogActivity
import dev.bartuzen.qbitcontroller.ui.rss.RssActivity
import dev.bartuzen.qbitcontroller.ui.search.SearchActivity
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.adaptiveIconPainterResource
import dev.bartuzen.qbitcontroller.utils.dropdownMenuHeight
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.rememberSearchStyle
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.SortedMap
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TorrentListScreen(
    isScreenActive: Boolean,
    serverIdFlow: Flow<Int>,
    modifier: Modifier = Modifier,
    viewModel: TorrentListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val torrents by viewModel.filteredTorrentList.collectAsStateWithLifecycle()
    val mainData = viewModel.mainData.collectAsStateWithLifecycle().value
    val servers by viewModel.serversFlow.collectAsStateWithLifecycle()
    val currentServer = viewModel.currentServer.collectAsStateWithLifecycle().value
    val serverId = currentServer?.id

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val selectedTracker by viewModel.selectedTracker.collectAsStateWithLifecycle()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val selectedTorrents = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var filterQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        serverIdFlow.collect { serverId ->
            viewModel.setCurrentServer(serverId)
        }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    LaunchedEffect(torrents) {
        selectedTorrents.removeAll { hash -> torrents?.none { it.hash == hash } != false }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentListViewModel.Event.Error -> {
                val errorMessage = getErrorMessage(context, event.error)
                if (snackbarHostState.currentSnackbarData?.visuals?.message != errorMessage) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Indefinite)
                    }
                }
            }

            TorrentListViewModel.Event.UpdateMainDataSuccess -> {
                if (snackbarHostState.currentSnackbarData?.visuals?.duration == SnackbarDuration.Indefinite) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }

            TorrentListViewModel.Event.ServerChanged -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                isSearchMode = false
                selectedTorrents.clear()
            }

            TorrentListViewModel.Event.QueueingNotEnabled -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_queueing_is_not_enabled))
                }
            }

            TorrentListViewModel.Event.CategoryEditingFailed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_list_edit_category_error))
                }
            }

            is TorrentListViewModel.Event.TorrentsDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.resources.getQuantityString(
                            R.plurals.torrent_list_torrents_delete_success,
                            event.count,
                            event.count,
                        ),
                    )
                }
            }

            is TorrentListViewModel.Event.TorrentsPaused -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.resources.getQuantityString(
                            R.plurals.torrent_list_torrents_pause_success,
                            event.count,
                            event.count,
                        ),
                    )
                }
            }

            is TorrentListViewModel.Event.TorrentsResumed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.resources.getQuantityString(
                            R.plurals.torrent_list_torrents_resume_success,
                            event.count,
                            event.count,
                        ),
                    )
                }
            }

            is TorrentListViewModel.Event.CategoryDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_delete_category_success, event.name),
                    )
                }
            }

            is TorrentListViewModel.Event.TagDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_delete_tag_success, event.name),
                    )
                }
            }

            TorrentListViewModel.Event.TorrentsPriorityDecreased -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_priority_decrease_success),
                    )
                }
            }

            TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_priority_increase_success),
                    )
                }
            }

            TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_priority_maximize_success),
                    )
                }
            }

            TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.torrent_list_priority_minimize_success),
                    )
                }
            }

            TorrentListViewModel.Event.LocationUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_location_update_success))
                }
            }

            TorrentListViewModel.Event.CategoryCreated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_list_create_category_success))
                }
            }

            TorrentListViewModel.Event.CategoryEdited -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_list_edit_category_success))
                }
            }

            TorrentListViewModel.Event.TagCreated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_list_create_tag_success))
                }
            }

            is TorrentListViewModel.Event.SpeedLimitsToggled -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    if (event.switchedToAlternativeLimit) {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.torrent_list_switch_speed_limit_alternative_success),
                        )
                    } else {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.torrent_list_switch_speed_limit_regular_success),
                        )
                    }
                }
            }

            TorrentListViewModel.Event.SpeedLimitsUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_speed_update_success))
                }
            }

            TorrentListViewModel.Event.Shutdown -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_list_shutdown_success))
                }
            }

            TorrentListViewModel.Event.TorrentCategoryUpdated -> {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_category_update_success))
                }
            }
        }
    }

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        filterQuery = TextFieldValue()
        viewModel.setSearchQuery("")
    }

    BackHandler(enabled = selectedTorrents.isNotEmpty()) {
        selectedTorrents.clear()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

        PersistentLaunchedEffect {
            if (servers.isNotEmpty() && permissionState.status.shouldShowRationale) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    PersistentLaunchedEffect(currentServer) {
        currentDialog = null
    }

    when (val dialog = currentDialog) {
        Dialog.CreateCategory -> {
            CreateEditCategoryDialog(
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { name, savePath, downloadPathEnabled, downloadPath ->
                    currentDialog = null
                    viewModel.createCategory(
                        name = name,
                        savePath = savePath,
                        downloadPathEnabled = downloadPathEnabled,
                        downloadPath = downloadPath,
                    )
                },
            )
        }

        is Dialog.CreateSubcategory -> {
            LaunchedEffect(mainData?.categories) {
                if (mainData?.categories?.find { dialog.parent == it.name } == null) {
                    currentDialog = null
                }
            }

            CreateEditCategoryDialog(
                isSubcategory = true,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { name, savePath, downloadPathEnabled, downloadPath ->
                    currentDialog = null
                    viewModel.createCategory(
                        name = "${dialog.parent}/$name",
                        savePath = savePath,
                        downloadPathEnabled = downloadPathEnabled,
                        downloadPath = downloadPath,
                    )
                },
            )
        }

        is Dialog.EditCategory -> {
            LaunchedEffect(mainData?.categories) {
                if (mainData?.categories?.find { dialog.category.name == it.name } == null) {
                    currentDialog = null
                }
            }

            CreateEditCategoryDialog(
                category = dialog.category,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { name, savePath, downloadPathEnabled, downloadPath ->
                    currentDialog = null
                    viewModel.editCategory(
                        name = name,
                        savePath = savePath,
                        downloadPathEnabled = downloadPathEnabled,
                        downloadPath = downloadPath,
                    )
                },
            )
        }

        Dialog.Statistics -> {
            LaunchedEffect(mainData == null) {
                if (mainData == null) {
                    currentDialog = null
                }
            }

            if (mainData != null) {
                StatisticsDialog(
                    state = mainData.serverState,
                    onDismiss = {
                        currentDialog = null
                    },
                )
            }
        }

        is Dialog.DeleteTag -> {
            LaunchedEffect(mainData?.tags) {
                if (mainData?.tags?.find { dialog.tag == it } == null) {
                    currentDialog = null
                }
            }

            DeleteTagDialog(
                tag = dialog.tag,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = {
                    currentDialog = null
                    viewModel.deleteTag(dialog.tag)
                },
            )
        }

        is Dialog.DeleteCategory -> {
            LaunchedEffect(mainData?.categories) {
                if (mainData?.categories?.find { dialog.category == it.name } == null) {
                    currentDialog = null
                }
            }

            DeleteCategoryDialog(
                category = dialog.category,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = {
                    currentDialog = null
                    viewModel.deleteCategory(dialog.category)
                },
            )
        }

        Dialog.CreateTag -> {
            CreateTagDialog(
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { tags ->
                    currentDialog = null
                    viewModel.createTags(tags)
                },
            )
        }

        Dialog.Shutdown -> {
            ShutdownDialog(
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = {
                    currentDialog = null
                    viewModel.shutdown()
                },
            )
        }

        Dialog.About -> {
            AboutDialog(
                onDismiss = {
                    currentDialog = null
                },
            )
        }

        is Dialog.DeleteTorrent -> {
            LaunchedEffect(mainData?.torrents) {
                if (mainData?.torrents?.find { dialog.hash == it.hash } == null) {
                    currentDialog = null
                }
            }

            DeleteTorrentsDialog(
                count = 1,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { deleteFiles ->
                    currentDialog = null
                    viewModel.deleteTorrents(listOf(dialog.hash), deleteFiles)
                },
            )
        }

        is Dialog.DeleteSelectedTorrents -> {
            LaunchedEffect(selectedTorrents.isEmpty()) {
                if (selectedTorrents.isEmpty()) {
                    currentDialog = null
                }
            }

            DeleteTorrentsDialog(
                count = selectedTorrents.size,
                onDismiss = {
                    currentDialog = null
                },
                onConfirm = { deleteFiles ->
                    currentDialog = null
                    viewModel.deleteTorrents(selectedTorrents.toList(), deleteFiles)
                    selectedTorrents.clear()
                },
            )
        }

        Dialog.SetSelectedTorrentsLocation -> {
            LaunchedEffect(selectedTorrents.isEmpty()) {
                if (selectedTorrents.isEmpty()) {
                    currentDialog = null
                }
            }

            if (mainData != null) {
                val commonLocation = remember {
                    mainData.torrents
                        .filter { it.hash in selectedTorrents }
                        .distinctBy { it.savePath }
                        .takeIf { it.size == 1 }
                        ?.first()
                        ?.savePath
                }
                SetTorrentsLocationDialog(
                    initialLocation = commonLocation,
                    onDismiss = {
                        currentDialog = null
                    },
                    onConfirm = { location ->
                        currentDialog = null
                        viewModel.setLocation(selectedTorrents.toList(), location)
                        selectedTorrents.clear()
                    },
                )
            }
        }

        Dialog.SetSelectedTorrentsCategory -> {
            LaunchedEffect(selectedTorrents.isEmpty()) {
                if (selectedTorrents.isEmpty()) {
                    currentDialog = null
                }
            }

            if (mainData != null) {
                val commonCategory = remember {
                    mainData.torrents
                        .filter { it.hash in selectedTorrents }
                        .distinctBy { it.category }
                        .takeIf { it.size == 1 }
                        ?.first()
                        ?.category
                }
                SetTorrentsCategoryDialog(
                    initialSelectedCategory = commonCategory,
                    categories = mainData.categories.map { it.name },
                    onDismiss = {
                        currentDialog = null
                    },
                    onConfirm = { category ->
                        currentDialog = null
                        viewModel.setCategory(selectedTorrents.toList(), category)
                        selectedTorrents.clear()
                    },
                )
            }
        }

        Dialog.SpeedLimits -> {
            LaunchedEffect(mainData == null) {
                if (mainData == null) {
                    currentDialog = null
                }
            }

            if (mainData != null) {
                SpeedLimitsDialog(
                    currentAlternativeLimits = mainData.serverState.useAlternativeSpeedLimits,
                    currentUploadSpeedLimit = mainData.serverState.uploadSpeedLimit / 1024,
                    currentDownloadSpeedLimit = mainData.serverState.downloadSpeedLimit / 1024,
                    onDismiss = {
                        currentDialog = null
                    },
                    onToggleAlternativeLimits = {
                        currentDialog = null
                        viewModel.toggleSpeedLimitsMode(mainData.serverState.useAlternativeSpeedLimits)
                    },
                    onSetSpeedLimits = { uploadLimit, downloadLimit ->
                        currentDialog = null
                        viewModel.setSpeedLimits(downloadLimit, uploadLimit)
                    },
                )
            }
        }

        null -> {}
    }

    if (serverId != null) {
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                    drawerState = drawerState,
                ) {
                    val counts by viewModel.counts.collectAsStateWithLifecycle()

                    val areStatesCollapsed by viewModel.areStatesCollapsed.collectAsStateWithLifecycle()
                    val areCategoriesCollapsed by viewModel.areCategoriesCollapsed.collectAsStateWithLifecycle()
                    val areTagsCollapsed by viewModel.areTagsCollapsed.collectAsStateWithLifecycle()
                    val areTrackersCollapsed by viewModel.areTrackersCollapsed.collectAsStateWithLifecycle()

                    DrawerContent(
                        serverId = serverId,
                        servers = servers,
                        selectedFilter = selectedFilter,
                        selectedCategory = selectedCategory,
                        selectedTag = selectedTag,
                        selectedTracker = selectedTracker,
                        areStatesCollapsed = areStatesCollapsed,
                        areCategoriesCollapsed = areCategoriesCollapsed,
                        areTagsCollapsed = areTagsCollapsed,
                        areTrackersCollapsed = areTrackersCollapsed,
                        mainData = mainData,
                        counts = counts,
                        isDrawerOpen = drawerState.isOpen,
                        onServerSelected = { viewModel.setCurrentServer(it) },
                        onDialogOpen = { currentDialog = it },
                        onSetDefaultTorrentStatus = { viewModel.setDefaultTorrentStatus(it) },
                        onDrawerClose = { scope.launch { drawerState.close() } },
                        onCollapseStates = { viewModel.setFiltersCollapseState(!areStatesCollapsed) },
                        onCollapseCategories = { viewModel.setCategoriesCollapseState(!areCategoriesCollapsed) },
                        onCollapseTags = { viewModel.setTagsCollapseState(!areTagsCollapsed) },
                        onCollapseTrackers = { viewModel.setTrackersCollapseState(!areTrackersCollapsed) },
                        onSelectFilter = { viewModel.setSelectedFilter(it) },
                        onSelectCategory = { viewModel.setSelectedCategory(it) },
                        onSelectTag = { viewModel.setSelectedTag(it) },
                        onSelectTracker = { viewModel.setSelectedTracker(it) },
                    )
                }
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
                snackbarHost = {
                    SwipeableSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                    )
                },
                topBar = {
                    val currentSorting by viewModel.torrentSort.collectAsStateWithLifecycle()
                    val isReverseSorting by viewModel.isReverseSorting.collectAsStateWithLifecycle()

                    TopBar(
                        serverId = serverId,
                        currentServer = currentServer,
                        mainData = mainData,
                        isSearchMode = isSearchMode,
                        searchQuery = searchQuery,
                        filterQuery = filterQuery,
                        currentSorting = currentSorting,
                        isReverseSorting = isReverseSorting,
                        snackbarHostState = snackbarHostState,
                        canFocusNow = drawerState.isClosed && selectedTorrents.isEmpty(),
                        onLoadMainData = { viewModel.loadMainData() },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSearchQueryChange = {
                            filterQuery = it
                            viewModel.setSearchQuery(it.text)
                        },
                        onSearchModeChange = { isSearchMode = it },
                        onTorrentSortChange = { viewModel.setTorrentSort(it) },
                        onReverseSortingChange = { viewModel.changeReverseSorting() },
                        onDialogOpen = { currentDialog = it },
                    )

                    AnimatedVisibility(
                        visible = selectedTorrents.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        TopBarSelection(
                            torrents = torrents,
                            selectedTorrents = selectedTorrents,
                            canFocusNow = drawerState.isClosed,
                            isQueueingEnabled = mainData?.serverState?.isQueueingEnabled == true,
                            onPauseTorrents = { viewModel.pauseTorrents(selectedTorrents.toList()) },
                            onResumeTorrents = { viewModel.resumeTorrents(selectedTorrents.toList()) },
                            onDeleteTorrents = { currentDialog = Dialog.DeleteSelectedTorrents },
                            onMaximizeTorrentsPriority = { viewModel.maximizeTorrentPriority(selectedTorrents.toList()) },
                            onIncreaseTorrentsPriority = { viewModel.increaseTorrentPriority(selectedTorrents.toList()) },
                            onDecreaseTorrentsPriority = { viewModel.decreaseTorrentPriority(selectedTorrents.toList()) },
                            onMinimizeTorrentsPriority = { viewModel.minimizeTorrentPriority(selectedTorrents.toList()) },
                            onSetTorrentsLocation = { currentDialog = Dialog.SetSelectedTorrentsLocation },
                            onSetTorrentsCategory = { currentDialog = Dialog.SetSelectedTorrentsCategory },
                        )
                    }
                },
            ) { innerPadding ->
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshMainData() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                ) {
                    Column {
                        AnimatedNullableVisibility(
                            value = mainData,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) { _, mainData ->
                            Text(
                                text = stringResource(
                                    R.string.torrent_list_speed_format,
                                    formatBytesPerSecond(mainData.serverState.downloadSpeed),
                                    formatBytes(mainData.serverState.downloadSession),
                                    formatBytesPerSecond(mainData.serverState.uploadSpeed),
                                    formatBytes(mainData.serverState.uploadSession),
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusProperties {
                                        canFocus = drawerState.isClosed
                                    }
                                    .clickable {
                                        currentDialog = Dialog.SpeedLimits
                                    }
                                    .padding(4.dp),
                            )
                        }

                        val listState = rememberLazyListState()
                        val swipeEnabled by viewModel.areTorrentSwipeActionsEnabled.collectAsStateWithLifecycle()
                        val torrentLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                val isTorrentDeleted = result.data?.getBooleanExtra(
                                    TorrentActivity.Extras.TORRENT_DELETED,
                                    false,
                                ) == true
                                if (isTorrentDeleted) {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.torrent_deleted_success),
                                        )
                                    }
                                    viewModel.loadMainData()
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(
                                    items = torrents.orEmpty(),
                                    key = { torrent -> "$serverId-${torrent.hash}" },
                                ) { torrent ->
                                    TorrentItem(
                                        torrent = torrent,
                                        selected = torrent.hash in selectedTorrents,
                                        searchQuery = searchQuery.ifEmpty { null },
                                        onClick = {
                                            if (selectedTorrents.isNotEmpty()) {
                                                if (torrent.hash !in selectedTorrents) {
                                                    selectedTorrents += torrent.hash
                                                } else {
                                                    selectedTorrents -= torrent.hash
                                                }
                                            } else {
                                                val intent = Intent(context, TorrentActivity::class.java).apply {
                                                    putExtra(TorrentActivity.Extras.SERVER_ID, serverId)
                                                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                                                    putExtra(TorrentActivity.Extras.TORRENT_NAME, torrent.name)
                                                }
                                                torrentLauncher.launch(intent)
                                            }
                                        },
                                        onLongClick = {
                                            if (torrent.hash !in selectedTorrents) {
                                                selectedTorrents += torrent.hash
                                            } else {
                                                selectedTorrents -= torrent.hash
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .focusProperties {
                                                canFocus = drawerState.isClosed
                                            },
                                        swipeEnabled = swipeEnabled,
                                        onPauseTorrent = { viewModel.pauseTorrents(listOf(torrent.hash)) },
                                        onResumeTorrent = { viewModel.resumeTorrents(listOf(torrent.hash)) },
                                        onDeleteTorrent = { currentDialog = Dialog.DeleteTorrent(torrent.hash) },
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                                }
                            }

                            SideEffect {
                                if (!listState.isScrollInProgress) {
                                    listState.requestScrollToItem(
                                        index = listState.firstVisibleItemIndex,
                                        scrollOffset = listState.firstVisibleItemScrollOffset,
                                    )
                                }
                            }

                            val emptyListState = if (torrents?.isEmpty() == true) {
                                val hasFilters = searchQuery != "" ||
                                    selectedCategory != CategoryTag.All ||
                                    selectedTag != CategoryTag.All ||
                                    selectedFilter != TorrentFilter.ALL ||
                                    selectedTracker != Tracker.All
                                if (hasFilters) 1 else 2
                            } else {
                                0
                            }

                            AnimatedContent(
                                emptyListState,
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) { emptyListState ->
                                when (emptyListState) {
                                    1 -> {
                                        NoResultsMessage(
                                            onResetFilters = {
                                                viewModel.resetFilters()
                                                isSearchMode = false
                                            },
                                            modifier = Modifier.focusProperties {
                                                canFocus = drawerState.isClosed
                                            },
                                        )
                                    }
                                    2 -> {
                                        NoTorrentsMessage(
                                            serverId = serverId,
                                            onTorrentAdded = {
                                                viewModel.loadMainData()
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar(
                                                        context.getString(R.string.torrent_add_success),
                                                    )
                                                }
                                            },
                                            modifier = Modifier.focusProperties {
                                                canFocus = drawerState.isClosed
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val isLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()
                    AnimatedVisibility(
                        visible = isLoading == true,
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
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        val actionMenuItems = remember {
                            listOf(
                                ActionMenuItem(
                                    title = context.getString(R.string.main_action_settings),
                                    icon = Icons.Filled.Settings,
                                    onClick = {
                                        val intent = Intent(context, SettingsActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                    showAsAction = false,
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.main_action_about),
                                    icon = Icons.Filled.Info,
                                    onClick = {
                                        currentDialog = Dialog.About
                                    },
                                    showAsAction = false,
                                ),
                            )
                        }

                        AppBarActions(
                            items = actionMenuItems,
                            canFocus = drawerState.isClosed,
                        )
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
            ) {
                EmptyListMessage(
                    icon = Icons.Filled.Dns,
                    title = stringResource(R.string.torrent_list_no_server_title),
                    description = stringResource(R.string.torrent_list_no_server_description),
                    actionButton = {
                        Button(
                            onClick = {
                                val intent = Intent(context, SettingsActivity::class.java).apply {
                                    putExtra(SettingsActivity.Extras.MOVE_TO_ADD_SERVER, true)
                                }
                                context.startActivity(intent)
                            },
                        ) {
                            Text(text = stringResource(R.string.torrent_list_no_server_add))
                        }
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun TorrentItem(
    torrent: Torrent,
    selected: Boolean,
    swipeEnabled: Boolean,
    searchQuery: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPauseTorrent: () -> Unit,
    onResumeTorrent: () -> Unit,
    onDeleteTorrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var swipeBackgroundAlpha by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = swipeBackgroundAlpha),
                shape = CardDefaults.elevatedShape,
            ),
    ) {
        val cardModifier = if (swipeEnabled) {
            var isDragging by remember { mutableStateOf(false) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            val offsetXAnimated by animateFloatAsState(
                targetValue = offsetX,
                animationSpec = if (!isDragging) spring() else tween(durationMillis = 0),
            )
            val maxSwipeDistance = with(LocalDensity.current) { 56.dp.toPx() }

            val isTorrentPaused = torrent.state in listOf(
                TorrentState.PAUSED_DL,
                TorrentState.PAUSED_UP,
                TorrentState.MISSING_FILES,
                TorrentState.ERROR,
            )

            val imageVector = if (isTorrentPaused) {
                Icons.Filled.PlayArrow
            } else {
                Icons.Filled.Pause
            }
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            )

            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            )

            LaunchedEffect(offsetXAnimated) {
                swipeBackgroundAlpha = abs(offsetXAnimated) / maxSwipeDistance
            }

            Modifier
                .offset { IntOffset(offsetXAnimated.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-maxSwipeDistance, maxSwipeDistance)
                    },
                    onDragStarted = {
                        isDragging = true
                    },
                    onDragStopped = {
                        if (offsetX == maxSwipeDistance) {
                            if (isTorrentPaused) {
                                onResumeTorrent()
                            } else {
                                onPauseTorrent()
                            }
                        } else if (offsetX == -maxSwipeDistance) {
                            onDeleteTorrent()
                        }
                        offsetX = 0f
                        isDragging = false
                    },
                )
        } else {
            Modifier
        }

        ElevatedCard(
            modifier = cardModifier,
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Unspecified,
            ),
        ) {
            Column(
                modifier = Modifier
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val name = if (searchQuery != null) {
                    rememberSearchStyle(
                        text = torrent.name,
                        searchQuery = searchQuery,
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onPrimary,
                            background = MaterialTheme.colorScheme.primary,
                        ),
                    )
                } else {
                    AnnotatedString(torrent.name)
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        lineBreak = LineBreak.Paragraph,
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

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
}

@Composable
private fun DrawerContent(
    serverId: Int?,
    servers: SortedMap<Int, ServerConfig>,
    selectedFilter: TorrentFilter,
    selectedCategory: CategoryTag,
    selectedTag: CategoryTag,
    selectedTracker: Tracker,
    areStatesCollapsed: Boolean,
    areCategoriesCollapsed: Boolean,
    areTagsCollapsed: Boolean,
    areTrackersCollapsed: Boolean,
    mainData: MainData?,
    counts: TorrentListViewModel.Counts?,
    isDrawerOpen: Boolean,
    onServerSelected: (serverId: Int) -> Unit,
    onDialogOpen: (dialog: Dialog) -> Unit,
    onSetDefaultTorrentStatus: (status: TorrentFilter) -> Unit,
    onDrawerClose: () -> Unit,
    onCollapseStates: () -> Unit,
    onCollapseCategories: () -> Unit,
    onCollapseTags: () -> Unit,
    onCollapseTrackers: () -> Unit,
    onSelectFilter: (torrentFilter: TorrentFilter) -> Unit,
    onSelectCategory: (category: CategoryTag) -> Unit,
    onSelectTag: (tag: CategoryTag) -> Unit,
    onSelectTracker: (tracker: Tracker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val statuses = remember {
        listOf(
            Triple(TorrentFilter.ALL, R.string.torrent_list_status_all, Icons.Outlined.FilterAlt),
            Triple(
                TorrentFilter.DOWNLOADING,
                R.string.torrent_list_status_downloading,
                Icons.Outlined.KeyboardDoubleArrowDown,
            ),
            Triple(TorrentFilter.SEEDING, R.string.torrent_list_status_seeding, Icons.Outlined.KeyboardDoubleArrowUp),
            Triple(TorrentFilter.COMPLETED, R.string.torrent_list_status_completed, Icons.Outlined.Done),
            Triple(TorrentFilter.RESUMED, R.string.torrent_list_status_resumed, Icons.Filled.PlayArrow),
            Triple(TorrentFilter.PAUSED, R.string.torrent_list_status_paused, Icons.Filled.Pause),
            Triple(TorrentFilter.ACTIVE, R.string.torrent_list_status_active, Icons.Outlined.ToggleOn),
            Triple(TorrentFilter.INACTIVE, R.string.torrent_list_status_inactive, Icons.Outlined.ToggleOff),
            Triple(TorrentFilter.STALLED, R.string.torrent_list_status_stalled, Icons.Filled.SyncAlt),
            Triple(TorrentFilter.CHECKING, R.string.torrent_list_status_checking, Icons.Outlined.Cached),
            Triple(TorrentFilter.MOVING, R.string.torrent_list_status_moving, Icons.AutoMirrored.Outlined.DriveFileMove),
            Triple(TorrentFilter.ERROR, R.string.torrent_list_status_error, Icons.Outlined.ErrorOutline),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
            }

            items(servers.toList()) { (id, serverConfig) ->
                Column {
                    DrawerServerItem(
                        name = serverConfig.name,
                        url = serverConfig.visibleUrl,
                        isSelected = serverId == id,
                        onClick = {
                            onServerSelected(id)
                            onDrawerClose()
                        },
                        onLongClick = {
                            val intent = Intent(context, SettingsActivity::class.java).apply {
                                putExtra(SettingsActivity.Extras.EDIT_SERVER_ID, serverConfig.id)
                            }
                            context.startActivity(intent)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )

                    HorizontalDivider()
                }
            }

            item {
                DrawerTitleItem(
                    text = stringResource(R.string.torrent_list_status),
                    isCollapsed = areStatesCollapsed,
                    onClick = {
                        onCollapseStates()
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = isDrawerOpen
                    },
                )
            }

            items(statuses) { (status, stringResource, icon) ->
                AnimatedVisibility(
                    visible = !areStatesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    var showMenu by rememberSaveable { mutableStateOf(false) }

                    DrawerItem(
                        icon = icon,
                        iconModifier = if (status == TorrentFilter.STALLED) {
                            Modifier.graphicsLayer(scaleX = -1f, rotationZ = 90f)
                        } else {
                            Modifier
                        },
                        text = stringResource(
                            R.string.torrent_list_status_format,
                            stringResource(stringResource),
                            counts?.stateCountMap?.get(status) ?: 0,
                        ),
                        isSelected = selectedFilter == status,
                        onClick = {
                            onSelectFilter(status)
                            onDrawerClose()
                        },
                        onLongClick = {
                            showMenu = true
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                        modifier = Modifier.dropdownMenuHeight(),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.torrent_list_status_set_as_default))
                            },
                            onClick = {
                                showMenu = false
                                onDrawerClose()
                                onSetDefaultTorrentStatus(status)
                            },
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                DrawerTitleItem(
                    text = stringResource(R.string.torrent_list_categories),
                    isCollapsed = areCategoriesCollapsed,
                    onClick = {
                        onCollapseCategories()
                    },
                    action = {
                        AnimatedVisibility(
                            visible = !areCategoriesCollapsed,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier
                                    .focusProperties {
                                        canFocus = isDrawerOpen
                                    }
                                    .clickable(
                                        indication = ripple(
                                            bounded = false,
                                            radius = 16.dp,
                                        ),
                                        interactionSource = null,
                                    ) {
                                        onDialogOpen(Dialog.CreateCategory)
                                        onDrawerClose()
                                    },
                            )
                        }
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = isDrawerOpen
                    },
                )
            }

            item {
                var showMenu by rememberSaveable { mutableStateOf(false) }
                AnimatedVisibility(
                    visible = !areCategoriesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Folder,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            stringResource(R.string.torrent_list_category_tag_all),
                            counts?.allCount ?: 0,
                        ),
                        isSelected = selectedCategory is CategoryTag.All,
                        onClick = {
                            onSelectCategory(CategoryTag.All)
                            onDrawerClose()
                        },
                        onLongClick = {
                            showMenu = true
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !areCategoriesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Folder,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            stringResource(R.string.torrent_list_uncategorized),
                            counts?.uncategorizedCount ?: 0,
                        ),
                        isSelected = selectedCategory is CategoryTag.Uncategorized,
                        onClick = {
                            onSelectCategory(CategoryTag.Uncategorized)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            val areSubcategoriesEnabled = mainData?.serverState?.areSubcategoriesEnabled == true
            items(counts?.categoryMap ?: emptyList()) { (category, count) ->
                AnimatedVisibility(
                    visible = !areCategoriesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    var showMenu by rememberSaveable { mutableStateOf(false) }

                    DrawerItem(
                        icon = Icons.Filled.Folder,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            if (!areSubcategoriesEnabled) category else category.split("/").last(),
                            count,
                        ),
                        isSelected = (selectedCategory as? CategoryTag.Item)?.name == category,
                        onClick = {
                            onSelectCategory(CategoryTag.Item(category))
                            onDrawerClose()
                        },
                        onLongClick = {
                            showMenu = true
                        },
                        startPadding = if (areSubcategoriesEnabled) (category.count { it == '/' } * 16).dp else 0.dp,
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                        modifier = Modifier.dropdownMenuHeight(),
                    ) {
                        if (areSubcategoriesEnabled) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.torrent_list_create_subcategory))
                                },
                                onClick = {
                                    showMenu = false
                                    onDrawerClose()
                                    onDialogOpen(Dialog.CreateSubcategory(parent = category))
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.torrent_list_edit_category))
                            },
                            onClick = {
                                showMenu = false
                                onDrawerClose()
                                mainData?.categories?.find { it.name == category }?.let {
                                    onDialogOpen(Dialog.EditCategory(category = it))
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.torrent_list_delete_category))
                            },
                            onClick = {
                                showMenu = false
                                onDrawerClose()
                                onDialogOpen(Dialog.DeleteCategory(category = category))
                            },
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                DrawerTitleItem(
                    text = stringResource(R.string.torrent_list_tags),
                    isCollapsed = areTagsCollapsed,
                    onClick = {
                        onCollapseTags()
                    },
                    action = {
                        AnimatedVisibility(
                            visible = !areTagsCollapsed,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier
                                    .focusProperties {
                                        canFocus = isDrawerOpen
                                    }
                                    .clickable(
                                        indication = ripple(
                                            bounded = false,
                                            radius = 16.dp,
                                        ),
                                        interactionSource = null,
                                    ) {
                                        onDialogOpen(Dialog.CreateTag)
                                        onDrawerClose()
                                    },
                            )
                        }
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = isDrawerOpen
                    },
                )
            }

            item {
                AnimatedVisibility(
                    visible = !areTagsCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Sell,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            stringResource(R.string.torrent_list_category_tag_all),
                            counts?.allCount ?: 0,
                        ),
                        isSelected = selectedTag is CategoryTag.All,
                        onClick = {
                            onSelectTag(CategoryTag.All)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !areTagsCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Sell,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            stringResource(R.string.torrent_list_untagged),
                            counts?.untaggedCount ?: 0,
                        ),
                        isSelected = selectedTag is CategoryTag.Uncategorized,
                        onClick = {
                            onSelectTag(CategoryTag.Uncategorized)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            items(counts?.tagMap ?: emptyList()) { (tag, count) ->
                var showMenu by rememberSaveable { mutableStateOf(false) }

                AnimatedVisibility(
                    visible = !areTagsCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Sell,
                        text = stringResource(
                            R.string.torrent_list_category_tag_format,
                            tag,
                            count,
                        ),
                        isSelected = (selectedTag as? CategoryTag.Item)?.name == tag,
                        onClick = {
                            onSelectTag(CategoryTag.Item(tag))
                            onDrawerClose()
                        },
                        onLongClick = {
                            showMenu = true
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                        modifier = Modifier.dropdownMenuHeight(),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.torrent_list_delete_tag))
                            },
                            onClick = {
                                showMenu = false
                                onDrawerClose()
                                onDialogOpen(Dialog.DeleteTag(tag = tag))
                            },
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                DrawerTitleItem(
                    text = stringResource(R.string.torrent_list_trackers_title),
                    isCollapsed = areTrackersCollapsed,
                    onClick = {
                        onCollapseTrackers()
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = isDrawerOpen
                    },
                )
            }

            item {
                AnimatedVisibility(
                    visible = !areTrackersCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.LocationOn,
                        text = stringResource(
                            R.string.torrent_list_trackers_format,
                            stringResource(R.string.torrent_list_trackers_all),
                            counts?.allCount ?: 0,
                        ),
                        isSelected = selectedTracker is Tracker.All,
                        onClick = {
                            onSelectTracker(Tracker.All)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !areTrackersCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.LocationOn,
                        text = stringResource(
                            R.string.torrent_list_trackers_format,
                            stringResource(R.string.torrent_list_trackers_trackerless),
                            counts?.trackerlessCount ?: 0,
                        ),
                        isSelected = selectedTracker is Tracker.Trackerless,
                        onClick = {
                            onSelectTracker(Tracker.Trackerless)
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            items(mainData?.trackers?.toList() ?: emptyList()) { (tracker, torrentHashes) ->
                AnimatedVisibility(
                    visible = !areTrackersCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.LocationOn,
                        text = stringResource(
                            R.string.torrent_list_trackers_format,
                            tracker,
                            torrentHashes.size,
                        ),
                        isSelected = (selectedTracker as? Tracker.Named)?.name == tracker,
                        onClick = {
                            onSelectTracker(Tracker.Named(tracker))
                            onDrawerClose()
                        },
                        modifier = Modifier.focusProperties {
                            canFocus = isDrawerOpen
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
            }
        }

        Spacer(
            modifier = Modifier
                .windowInsetsTopHeight(WindowInsets.safeDrawing)
                .fillMaxWidth()
                .background(DrawerDefaults.modalContainerColor.copy(alpha = if (isSystemInDarkTheme()) 0.5f else 0.9f)),
        )
    }
}

@Composable
private fun DrawerServerItem(
    name: String?,
    url: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(16.dp),
    ) {
        if (name != null) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = url,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Unspecified,
        )
    }
}

@Composable
private fun DrawerTitleItem(
    text: String,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(8.dp),
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (!isCollapsed) 0f else -90f,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
        if (action != null) {
            Spacer(modifier = Modifier.weight(1f))
            action()
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    startPadding: Dp = 0.dp,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(start = startPadding),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current,
            modifier = iconModifier,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Unspecified,
        )
    }
}

@Composable
private fun TopBar(
    serverId: Int,
    currentServer: ServerConfig,
    mainData: MainData?,
    isSearchMode: Boolean,
    searchQuery: String,
    filterQuery: TextFieldValue,
    currentSorting: TorrentSort,
    isReverseSorting: Boolean,
    snackbarHostState: SnackbarHostState,
    canFocusNow: Boolean,
    onLoadMainData: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSearchQueryChange: (newQuery: TextFieldValue) -> Unit,
    onSearchModeChange: (newValue: Boolean) -> Unit,
    onTorrentSortChange: (newSort: TorrentSort) -> Unit,
    onReverseSortingChange: () -> Unit,
    onDialogOpen: (dialog: Dialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    TopAppBar(
        modifier = modifier,
        title = {
            if (!isSearchMode) {
                Column {
                    Text(
                        text = currentServer.name ?: stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedNullableVisibility(
                        value = mainData?.serverState?.freeSpace?.takeIf { it >= 0 },
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) { _, freeSpace ->
                        Text(
                            text = stringResource(R.string.torrent_list_free_space, formatBytes(freeSpace)),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.alpha(0.78f),
                        )
                    }
                }
            } else {
                val focusRequester = remember { FocusRequester() }
                PersistentLaunchedEffect {
                    focusRequester.requestFocus()
                }

                SearchBar(
                    value = filterQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.torrent_list_search_torrents),
                    modifier = Modifier
                        .focusProperties {
                            canFocus = canFocusNow
                        }
                        .focusRequester(focusRequester),
                )
            }
        },
        navigationIcon = {
            if (!isSearchMode) {
                IconButton(
                    onClick = {
                        scope.launch {
                            onOpenDrawer()
                        }
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = canFocusNow
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        onSearchModeChange(false)
                        onSearchQueryChange(TextFieldValue())
                    },
                    modifier = Modifier.focusProperties {
                        canFocus = canFocusNow
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            var showSortMenu by rememberSaveable { mutableStateOf(false) }
            val updatedServerId by rememberUpdatedState(serverId)
            val addTorrentLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val isAdded = result.data?.getBooleanExtra(
                            AddTorrentActivity.Extras.IS_ADDED,
                            false,
                        ) == true
                        if (isAdded) {
                            onLoadMainData()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(context.getString(R.string.torrent_add_success))
                            }
                        }
                    }
                }

            val actionMenuItems = remember(mainData != null, isSearchMode, searchQuery.isNotEmpty()) {
                listOf(
                    if (!isSearchMode) {
                        ActionMenuItem(
                            title = context.getString(R.string.action_search),
                            icon = Icons.Filled.Search,
                            onClick = {
                                onSearchModeChange(true)
                            },
                            showAsAction = true,
                        )
                    } else {
                        ActionMenuItem(
                            title = null,
                            icon = Icons.Filled.Close,
                            onClick = { onSearchQueryChange(TextFieldValue()) },
                            isHidden = searchQuery.isEmpty(),
                            showAsAction = true,
                        )
                    },
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_add_torrent),
                        icon = Icons.Filled.Add,
                        onClick = {
                            val intent = Intent(context, AddTorrentActivity::class.java).apply {
                                putExtra(AddTorrentActivity.Extras.SERVER_ID, updatedServerId)
                            }
                            addTorrentLauncher.launch(intent)
                        },
                        showAsAction = true,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_rss),
                        icon = Icons.Filled.RssFeed,
                        onClick = {
                            val intent = Intent(context, RssActivity::class.java).apply {
                                putExtra(RssActivity.Extras.SERVER_ID, updatedServerId)
                            }
                            context.startActivity(intent)
                        },
                        showAsAction = true,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_search_online),
                        icon = Icons.Filled.TravelExplore,
                        onClick = {
                            val intent = Intent(context, SearchActivity::class.java).apply {
                                putExtra(SearchActivity.Extras.SERVER_ID, updatedServerId)
                            }
                            context.startActivity(intent)
                        },
                        showAsAction = true,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_statistics),
                        icon = Icons.Outlined.Analytics,
                        onClick = {
                            onDialogOpen(Dialog.Statistics)
                        },
                        showAsAction = false,
                        isEnabled = mainData != null,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_execution_log),
                        icon = Icons.Filled.Description,
                        onClick = {
                            val intent = Intent(context, LogActivity::class.java).apply {
                                putExtra(LogActivity.Extras.SERVER_ID, updatedServerId)
                            }
                            context.startActivity(intent)
                        },
                        showAsAction = false,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_shutdown),
                        icon = Icons.Filled.PowerSettingsNew,
                        onClick = {
                            onDialogOpen(Dialog.Shutdown)
                        },
                        showAsAction = false,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_sort),
                        icon = Icons.AutoMirrored.Filled.Sort,
                        onClick = {
                            showSortMenu = true
                        },
                        showAsAction = false,
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowRight,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.main_action_settings),
                        icon = Icons.Filled.Settings,
                        onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                        showAsAction = false,
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.main_action_about),
                        icon = Icons.Filled.Info,
                        onClick = {
                            onDialogOpen(Dialog.About)
                        },
                        showAsAction = false,
                    ),
                )
            }

            AppBarActions(
                items = actionMenuItems,
                canFocus = canFocusNow,
            )

            val sortMenuScrollState = rememberScrollState()
            LaunchedEffect(showSortMenu) {
                if (showSortMenu) {
                    sortMenuScrollState.scrollTo(0)
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                scrollState = sortMenuScrollState,
                modifier = Modifier.dropdownMenuHeight(),
            ) {
                Text(
                    text = stringResource(R.string.torrent_list_action_sort),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                val sortOptions = remember {
                    listOf(
                        R.string.torrent_list_action_sort_name to TorrentSort.NAME,
                        R.string.torrent_list_action_sort_status to TorrentSort.STATUS,
                        R.string.torrent_list_action_sort_hash to TorrentSort.HASH,
                        R.string.torrent_list_action_sort_download_speed to TorrentSort.DOWNLOAD_SPEED,
                        R.string.torrent_list_action_sort_upload_speed to TorrentSort.UPLOAD_SPEED,
                        R.string.torrent_list_action_sort_priority to TorrentSort.PRIORITY,
                        R.string.torrent_list_action_sort_eta to TorrentSort.ETA,
                        R.string.torrent_list_action_sort_size to TorrentSort.SIZE,
                        R.string.torrent_list_action_sort_progress to TorrentSort.PROGRESS,
                        R.string.torrent_list_action_sort_ratio to TorrentSort.RATIO,
                        R.string.torrent_list_action_sort_connected_seeds to TorrentSort.CONNECTED_SEEDS,
                        R.string.torrent_list_action_sort_total_seeds to TorrentSort.TOTAL_SEEDS,
                        R.string.torrent_list_action_sort_connected_leeches to TorrentSort.CONNECTED_LEECHES,
                        R.string.torrent_list_action_sort_total_leeches to TorrentSort.TOTAL_LEECHES,
                        R.string.torrent_list_action_sort_addition_date to TorrentSort.ADDITION_DATE,
                        R.string.torrent_list_action_sort_completion_date to TorrentSort.COMPLETION_DATE,
                        R.string.torrent_list_action_sort_last_activity to TorrentSort.LAST_ACTIVITY,
                    )
                }
                sortOptions.forEach { (stringId, torrentSort) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = currentSorting == torrentSort,
                                    onClick = null,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(stringId))
                            }
                        },
                        onClick = {
                            onTorrentSortChange(torrentSort)
                            showSortMenu = false
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isReverseSorting,
                                onCheckedChange = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.torrent_list_action_sort_reverse))
                        }
                    },
                    onClick = {
                        onReverseSortingChange()
                        showSortMenu = false
                    },
                )
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )
}

@Composable
private fun TopBarSelection(
    torrents: List<Torrent>?,
    selectedTorrents: SnapshotStateList<String>,
    canFocusNow: Boolean,
    isQueueingEnabled: Boolean,
    onPauseTorrents: () -> Unit,
    onResumeTorrents: () -> Unit,
    onDeleteTorrents: () -> Unit,
    onMaximizeTorrentsPriority: () -> Unit,
    onIncreaseTorrentsPriority: () -> Unit,
    onDecreaseTorrentsPriority: () -> Unit,
    onMinimizeTorrentsPriority: () -> Unit,
    onSetTorrentsLocation: () -> Unit,
    onSetTorrentsCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TopAppBar(
        modifier = modifier,
        title = {
            var selectedSize by rememberSaveable { mutableIntStateOf(0) }

            LaunchedEffect(selectedTorrents.size) {
                if (selectedTorrents.isNotEmpty()) {
                    selectedSize = selectedTorrents.size
                }
            }

            Text(
                text = pluralStringResource(
                    R.plurals.torrent_list_torrents_selected,
                    selectedSize,
                    selectedSize,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    selectedTorrents.clear()
                },
                modifier = Modifier.focusProperties {
                    canFocus = canFocusNow
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = {
            var showPriorityMenu by rememberSaveable { mutableStateOf(false) }

            val actionMenuItems = remember(isQueueingEnabled) {
                listOf(
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_pause),
                        icon = Icons.Filled.Pause,
                        showAsAction = true,
                        onClick = {
                            onPauseTorrents()
                            selectedTorrents.clear()
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_resume),
                        icon = Icons.Filled.PlayArrow,
                        showAsAction = true,
                        onClick = {
                            onResumeTorrents()
                            selectedTorrents.clear()
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_delete),
                        icon = Icons.Filled.Delete,
                        showAsAction = true,
                        onClick = {
                            onDeleteTorrents()
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_priority),
                        icon = Icons.Outlined.Priority,
                        showAsAction = true,
                        isVisible = isQueueingEnabled,
                        onClick = {
                            showPriorityMenu = true
                        },
                        dropdownMenu = {
                            DropdownMenu(
                                expanded = showPriorityMenu,
                                onDismissRequest = { showPriorityMenu = false },
                                modifier = Modifier.dropdownMenuHeight(),
                            ) {
                                Text(
                                    text = stringResource(R.string.torrent_list_action_priority),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(R.string.torrent_list_action_priority_maximize))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardDoubleArrowUp,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showPriorityMenu = false
                                        onMaximizeTorrentsPriority()
                                        selectedTorrents.clear()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(R.string.torrent_list_action_priority_increase))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowUp,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showPriorityMenu = false
                                        onIncreaseTorrentsPriority()
                                        selectedTorrents.clear()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(R.string.torrent_list_action_priority_decrease))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showPriorityMenu = false
                                        onDecreaseTorrentsPriority()
                                        selectedTorrents.clear()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(R.string.torrent_list_action_priority_minimize))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showPriorityMenu = false
                                        onMinimizeTorrentsPriority()
                                        selectedTorrents.clear()
                                    },
                                )
                            }
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_set_location),
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        showAsAction = false,
                        onClick = {
                            onSetTorrentsLocation()
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.torrent_list_action_set_category),
                        icon = Icons.AutoMirrored.Filled.Label,
                        showAsAction = false,
                        onClick = {
                            onSetTorrentsCategory()
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.action_select_all),
                        icon = Icons.Filled.SelectAll,
                        showAsAction = false,
                        onClick = onClick@{
                            val newTorrents = torrents
                                ?.filter { it.hash !in selectedTorrents }
                                ?.map { it.hash }
                                ?: return@onClick
                            selectedTorrents.addAll(newTorrents)
                        },
                    ),
                    ActionMenuItem(
                        title = context.getString(R.string.action_select_inverse),
                        icon = Icons.Filled.FlipToBack,
                        showAsAction = false,
                        onClick = onClick@{
                            val newTorrents = torrents
                                ?.filter { it.hash !in selectedTorrents }
                                ?.map { it.hash }
                                ?: return@onClick
                            selectedTorrents.clear()
                            selectedTorrents.addAll(newTorrents)
                        },
                    ),
                )
            }

            AppBarActions(
                items = actionMenuItems,
                canFocus = canFocusNow,
            )
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )
}

@Serializable
private sealed class Dialog {
    @Serializable
    data object CreateCategory : Dialog()

    @Serializable
    data class CreateSubcategory(val parent: String) : Dialog()

    @Serializable
    data class EditCategory(val category: Category) : Dialog()

    @Serializable
    data class DeleteCategory(val category: String) : Dialog()

    @Serializable
    data object CreateTag : Dialog()

    @Serializable
    data class DeleteTag(val tag: String) : Dialog()

    @Serializable
    data class DeleteTorrent(val hash: String) : Dialog()

    @Serializable
    data object DeleteSelectedTorrents : Dialog()

    @Serializable
    data object SetSelectedTorrentsLocation : Dialog()

    @Serializable
    data object SetSelectedTorrentsCategory : Dialog()

    @Serializable
    data object SpeedLimits : Dialog()

    @Serializable
    data object Statistics : Dialog()

    @Serializable
    data object Shutdown : Dialog()

    @Serializable
    data object About : Dialog()
}

@Composable
private fun CreateEditCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, savePath: String, downloadPathEnabled: Boolean?, downloadPath: String) -> Unit,
    modifier: Modifier = Modifier,
    category: Category? = null,
    isSubcategory: Boolean = false,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(category?.name ?: "")) }
    var savePath by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(category?.savePath ?: ""))
    }
    var downloadPathEnabled by rememberSaveable {
        mutableStateOf(
            when (category?.downloadPath) {
                Category.DownloadPath.Default -> null
                Category.DownloadPath.No -> false
                is Category.DownloadPath.Yes -> true
                null -> null
            },
        )
    }
    var downloadPath by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue((category?.downloadPath as? Category.DownloadPath.Yes)?.path ?: ""))
    }

    var nameError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onConfirm(name.text, savePath.text, downloadPathEnabled, downloadPath.text)
                    } else {
                        nameError = R.string.torrent_list_create_category_name_cannot_be_empty
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = {
            Text(
                text = stringResource(
                    when {
                        isSubcategory -> R.string.torrent_list_create_subcategory
                        category != null -> R.string.torrent_list_edit_category
                        else -> R.string.torrent_list_create_category
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val focusRequester = remember { FocusRequester() }
                PersistentLaunchedEffect {
                    if (category == null) {
                        focusRequester.requestFocus()
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    readOnly = category != null,
                    label = {
                        Text(
                            text = stringResource(R.string.torrent_list_create_category_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(text = stringResource(it)) } },
                    trailingIcon = nameError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                OutlinedTextField(
                    value = savePath,
                    onValueChange = {
                        savePath = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.torrent_list_create_category_save_path_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                var expanded by rememberSaveable { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                    },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        value = when (downloadPathEnabled) {
                            true -> stringResource(R.string.torrent_list_create_category_download_path_yes)
                            false -> stringResource(R.string.torrent_list_create_category_download_path_no)
                            null -> stringResource(R.string.torrent_list_create_category_download_path_default)
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_list_create_category_enable_download_path_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.torrent_list_create_category_download_path_default),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                expanded = false
                                downloadPathEnabled = null
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.torrent_list_create_category_download_path_yes),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                expanded = false
                                downloadPathEnabled = true
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.torrent_list_create_category_download_path_no),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                expanded = false
                                downloadPathEnabled = false
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = downloadPath,
                    onValueChange = {
                        downloadPath = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.torrent_list_create_category_download_path_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    enabled = downloadPathEnabled == true,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.text.isNotBlank()) {
                                onConfirm(name.text, savePath.text, downloadPathEnabled, downloadPath.text)
                            } else {
                                nameError = R.string.torrent_list_create_category_name_cannot_be_empty
                            }
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    category: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.torrent_list_delete_category))
        },
        text = {
            val description = rememberReplaceAndApplyStyle(
                text = stringResource(R.string.torrent_list_delete_category_confirm),
                oldValue = "%1\$s",
                newValue = category,
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            )
            Text(text = description)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun CreateTagDialog(onDismiss: () -> Unit, onConfirm: (tags: List<String>) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var nameError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onConfirm(name.text.split("\n"))
                    } else {
                        nameError = R.string.torrent_list_create_tag_name_cannot_be_empty
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = {
            Text(text = stringResource(R.string.torrent_list_create_tag))
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = {
                    Text(
                        text = stringResource(R.string.torrent_list_create_tag_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(text = stringResource(it)) } },
                trailingIcon = nameError?.let {
                    {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                        )
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.text.isNotBlank()) {
                            onConfirm(name.text.split("\n"))
                        } else {
                            nameError = R.string.torrent_list_create_tag_name_cannot_be_empty
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
    )
}

@Composable
private fun DeleteTagDialog(tag: String, onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.torrent_list_delete_tag))
        },
        text = {
            val description = rememberReplaceAndApplyStyle(
                text = stringResource(R.string.torrent_list_delete_tag_desc),
                oldValue = "%1\$s",
                newValue = tag,
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            )
            Text(text = description)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun DeleteTorrentsDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: (deleteFiles: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteFiles by rememberSaveable { mutableStateOf(false) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 16.dp,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteFiles) },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = {
            Text(text = pluralStringResource(R.plurals.torrent_list_delete_torrents, count, count))
        },
        text = {
            CheckboxWithLabel(
                checked = deleteFiles,
                onCheckedChange = { deleteFiles = it },
                label = stringResource(R.string.torrent_delete_files),
            )
        },
    )
}

@Composable
private fun SetTorrentsLocationDialog(
    initialLocation: String?,
    onDismiss: () -> Unit,
    onConfirm: (location: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var location by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialLocation ?: "", TextRange(Int.MAX_VALUE)))
    }
    var locationError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (location.text.isNotBlank()) {
                        onConfirm(location.text)
                    } else {
                        locationError = R.string.torrent_location_cannot_be_blank
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(R.string.torrent_list_action_set_location)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = location,
                onValueChange = {
                    location = it
                    locationError = null
                },
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(R.string.torrent_list_set_location_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = locationError != null,
                supportingText = locationError?.let { { Text(text = stringResource(it)) } },
                trailingIcon = locationError?.let {
                    {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                        )
                    }
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (location.text.isNotBlank()) {
                            onConfirm(location.text)
                        } else {
                            locationError = R.string.torrent_location_cannot_be_blank
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
    )
}

@Composable
private fun SetTorrentsCategoryDialog(
    initialSelectedCategory: String?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (category: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(initialSelectedCategory) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCategory) },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(R.string.torrent_list_action_set_category)) },
        text = {
            if (categories.isNotEmpty()) {
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
    )
}

@Composable
private fun SpeedLimitsDialog(
    currentAlternativeLimits: Boolean,
    currentUploadSpeedLimit: Int,
    currentDownloadSpeedLimit: Int,
    onDismiss: () -> Unit,
    onToggleAlternativeLimits: () -> Unit,
    onSetSpeedLimits: (uploadSpeed: Int, downloadSpeed: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var alternativeLimits by rememberSaveable { mutableStateOf(currentAlternativeLimits) }

    var uploadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentUploadSpeedLimit.takeIf { it != 0 }?.toString() ?: ""))
    }
    var uploadSpeedUnit by rememberSaveable { mutableIntStateOf(0) }
    var uploadSpeedError by rememberSaveable { mutableStateOf<Int?>(null) }

    var downloadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentDownloadSpeedLimit.takeIf { it != 0 }?.toString() ?: ""))
    }
    var downloadSpeedUnit by rememberSaveable { mutableIntStateOf(0) }
    var downloadSpeedError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 16.dp,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentAlternativeLimits != alternativeLimits) {
                        onToggleAlternativeLimits()
                    } else {
                        fun convertSpeedToBytes(speed: String, unit: Int): Int? {
                            if (speed.isBlank()) {
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

                        val upload = convertSpeedToBytes(uploadSpeedLimit.text, uploadSpeedUnit)
                        val download = convertSpeedToBytes(downloadSpeedLimit.text, downloadSpeedUnit)

                        if (upload == null) {
                            uploadSpeedError = R.string.torrent_add_speed_limit_too_big
                        }
                        if (download == null) {
                            downloadSpeedError = R.string.torrent_add_speed_limit_too_big
                        }

                        if (upload != null && download != null) {
                            onSetSpeedLimits(upload, download)
                        }
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = {
            Text(text = stringResource(R.string.torrent_speed_limits_title))
        },
        text = {
            Column {
                CheckboxWithLabel(
                    checked = alternativeLimits,
                    onCheckedChange = { alternativeLimits = it },
                    label = stringResource(R.string.torrent_speed_alternative_speed_limits),
                )

                Spacer(modifier = Modifier.height(4.dp))

                val unitTextWidth = max(
                    measureTextWidth(stringResource(R.string.size_kibibytes)),
                    measureTextWidth(stringResource(R.string.size_mebibytes)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    OutlinedTextField(
                        value = uploadSpeedLimit,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                uploadSpeedLimit = it
                                uploadSpeedError = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_speed_upload_limit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingText = uploadSpeedError?.let { { Text(text = stringResource(it)) } },
                        trailingIcon = uploadSpeedError?.let {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                )
                            }
                        },
                        isError = uploadSpeedError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = alternativeLimits == currentAlternativeLimits,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    var expanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = it
                        },
                        modifier = Modifier
                            .width(unitTextWidth + 72.dp)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            value = when (uploadSpeedUnit) {
                                1 -> stringResource(R.string.size_mebibytes)
                                else -> stringResource(R.string.size_kibibytes)
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            enabled = alternativeLimits == currentAlternativeLimits,
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.size_kibibytes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    uploadSpeedUnit = 0
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.size_mebibytes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    uploadSpeedUnit = 1
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    OutlinedTextField(
                        value = downloadSpeedLimit,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                downloadSpeedLimit = it
                                downloadSpeedError = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_speed_download_limit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingText = downloadSpeedError?.let { { Text(text = stringResource(it)) } },
                        trailingIcon = downloadSpeedError?.let {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                )
                            }
                        },
                        isError = downloadSpeedError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = alternativeLimits == currentAlternativeLimits,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    var expanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = it
                        },
                        modifier = Modifier
                            .width(unitTextWidth + 72.dp)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            value = when (downloadSpeedUnit) {
                                1 -> stringResource(R.string.size_mebibytes)
                                else -> stringResource(R.string.size_kibibytes)
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            enabled = alternativeLimits == currentAlternativeLimits,
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.size_kibibytes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    downloadSpeedUnit = 0
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.size_mebibytes),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    downloadSpeedUnit = 1
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun StatisticsDialog(state: ServerState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 8.dp,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(R.string.torrent_list_action_statistics)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stats_category_user_statistics),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_all_time_upload),
                            value = formatBytes(state.allTimeUpload),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_all_time_download),
                            value = formatBytes(state.allTimeDownload),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_all_time_share_ratio),
                            value = state.globalRatio,
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_session_waste),
                            value = formatBytes(state.sessionWaste),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_connected_peers),
                            value = state.connectedPeers.toString(),
                        )
                    }
                }

                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stats_category_cache_statistics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_total_buffer_size),
                            value = formatBytes(state.bufferSize),
                        )
                    }
                }

                OutlinedCard(
                    elevation = CardDefaults.outlinedCardElevation(0.dp),
                    colors = CardDefaults.outlinedCardColors(Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stats_category_performance_statistics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_write_cache_overload),
                            value = stringResource(R.string.percentage_format, state.writeCacheOverload),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_read_cache_overload),
                            value = stringResource(R.string.percentage_format, state.readCacheOverload),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_queued_io_jobs),
                            value = state.queuedIOJobs.toString(),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_average_time_in_queue),
                            value = stringResource(R.string.stats_ms_format, state.averageTimeInQueue),
                        )

                        StatisticRow(
                            label = stringResource(R.string.stats_total_queued_size),
                            value = formatBytes(state.queuedSize),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun StatisticRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ShutdownDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.torrent_list_action_shutdown))
        },
        text = {
            Text(text = stringResource(R.string.torrent_list_shutdown_confirm))
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        text = {
            Column {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = adaptiveIconPainterResource(R.mipmap.ic_launcher_round),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(text = BuildConfig.VERSION_NAME)
                }

                val description = buildAnnotatedString {
                    val parts = stringResource(R.string.about_description).split("%1\$s")
                    parts.forEachIndexed { index, part ->
                        append(part)
                        if (index != parts.lastIndex) {
                            withLink(
                                link = LinkAnnotation.Url(
                                    url = BuildConfig.SOURCE_CODE_URL,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                    ),
                                ),
                            ) {
                                append(BuildConfig.SOURCE_CODE_URL)
                            }
                        }
                    }
                }
                Text(
                    text = description,
                    style = TextStyle(lineBreak = LineBreak.Paragraph),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun NoTorrentsMessage(serverId: Int, onTorrentAdded: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val addTorrentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isAdded = result.data?.getBooleanExtra(AddTorrentActivity.Extras.IS_ADDED, false) == true
            if (isAdded) {
                onTorrentAdded()
            }
        }
    }

    EmptyListMessage(
        icon = Icons.Default.Download,
        title = stringResource(R.string.torrent_list_empty_title),
        description = stringResource(R.string.torrent_list_empty_description),
        actionButton = {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, RssActivity::class.java).apply {
                            putExtra(RssActivity.Extras.SERVER_ID, serverId)
                        }
                        context.startActivity(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.torrent_list_empty_rss))
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, SearchActivity::class.java).apply {
                            putExtra(SearchActivity.Extras.SERVER_ID, serverId)
                        }
                        context.startActivity(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.torrent_list_empty_search))
                }
                Button(
                    onClick = {
                        val intent = Intent(context, AddTorrentActivity::class.java).apply {
                            putExtra(AddTorrentActivity.Extras.SERVER_ID, serverId)
                        }
                        addTorrentLauncher.launch(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.torrent_list_empty_add))
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun NoResultsMessage(onResetFilters: () -> Unit, modifier: Modifier = Modifier) {
    EmptyListMessage(
        icon = Icons.Default.FilterList,
        title = stringResource(R.string.torrent_list_no_result_title),
        description = stringResource(R.string.torrent_list_no_result_description),
        actionButton = {
            Button(onClick = onResetFilters) {
                Text(text = stringResource(R.string.torrent_list_no_result_reset))
            }
        },
        modifier = modifier,
    )
}
