package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SyncAlt
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.TrafficStats
import dev.bartuzen.qbitcontroller.generated.BuildConfig
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.ServerState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.EmptyListMessage
import dev.bartuzen.qbitcontroller.ui.components.LazyColumnItemMinHeight
import dev.bartuzen.qbitcontroller.ui.components.PlatformBackHandler
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.components.SearchBar
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.ui.icons.Priority
import dev.bartuzen.qbitcontroller.ui.theme.isDarkTheme
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.excludeBottom
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.getTorrentStateColor
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.notificationPermissionLauncher
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.rememberSearchStyle
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.stringResourceSaver
import dev.bartuzen.qbitcontroller.utils.topAppBarColor
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.about_description
import qbitcontroller.composeapp.generated.resources.action_search
import qbitcontroller.composeapp.generated.resources.action_select_all
import qbitcontroller.composeapp.generated.resources.action_select_inverse
import qbitcontroller.composeapp.generated.resources.app_name
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.main_action_about
import qbitcontroller.composeapp.generated.resources.percentage_format
import qbitcontroller.composeapp.generated.resources.size_kibibytes
import qbitcontroller.composeapp.generated.resources.size_mebibytes
import qbitcontroller.composeapp.generated.resources.stats_all_time_download
import qbitcontroller.composeapp.generated.resources.stats_all_time_share_ratio
import qbitcontroller.composeapp.generated.resources.stats_all_time_upload
import qbitcontroller.composeapp.generated.resources.stats_average_time_in_queue
import qbitcontroller.composeapp.generated.resources.stats_category_cache_statistics
import qbitcontroller.composeapp.generated.resources.stats_category_performance_statistics
import qbitcontroller.composeapp.generated.resources.stats_category_user_statistics
import qbitcontroller.composeapp.generated.resources.stats_connected_peers
import qbitcontroller.composeapp.generated.resources.stats_ms_format
import qbitcontroller.composeapp.generated.resources.stats_queued_io_jobs
import qbitcontroller.composeapp.generated.resources.stats_read_cache_hits
import qbitcontroller.composeapp.generated.resources.stats_read_cache_overload
import qbitcontroller.composeapp.generated.resources.stats_session_waste
import qbitcontroller.composeapp.generated.resources.stats_total_buffer_size
import qbitcontroller.composeapp.generated.resources.stats_total_queued_size
import qbitcontroller.composeapp.generated.resources.stats_write_cache_overload
import qbitcontroller.composeapp.generated.resources.torrent_add_speed_limit_too_big
import qbitcontroller.composeapp.generated.resources.torrent_add_success
import qbitcontroller.composeapp.generated.resources.torrent_category_update_success
import qbitcontroller.composeapp.generated.resources.torrent_delete_files
import qbitcontroller.composeapp.generated.resources.torrent_deleted_success
import qbitcontroller.composeapp.generated.resources.torrent_item_progress_format
import qbitcontroller.composeapp.generated.resources.torrent_list_action_add_torrent
import qbitcontroller.composeapp.generated.resources.torrent_list_action_delete
import qbitcontroller.composeapp.generated.resources.torrent_list_action_pause
import qbitcontroller.composeapp.generated.resources.torrent_list_action_priority
import qbitcontroller.composeapp.generated.resources.torrent_list_action_priority_decrease
import qbitcontroller.composeapp.generated.resources.torrent_list_action_priority_increase
import qbitcontroller.composeapp.generated.resources.torrent_list_action_priority_maximize
import qbitcontroller.composeapp.generated.resources.torrent_list_action_priority_minimize
import qbitcontroller.composeapp.generated.resources.torrent_list_action_resume
import qbitcontroller.composeapp.generated.resources.torrent_list_action_set_category
import qbitcontroller.composeapp.generated.resources.torrent_list_action_set_location
import qbitcontroller.composeapp.generated.resources.torrent_list_action_shutdown
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_addition_date
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_completion_date
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_connected_leeches
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_connected_seeds
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_download_speed
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_eta
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_hash
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_last_activity
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_name
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_priority
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_progress
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_ratio
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_reverse
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_size
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_status
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_total_leeches
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_total_seeds
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_upload_speed
import qbitcontroller.composeapp.generated.resources.torrent_list_action_sort_uploaded
import qbitcontroller.composeapp.generated.resources.torrent_list_action_statistics
import qbitcontroller.composeapp.generated.resources.torrent_list_categories
import qbitcontroller.composeapp.generated.resources.torrent_list_category_tag_all
import qbitcontroller.composeapp.generated.resources.torrent_list_category_tag_format
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_download_path_default
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_download_path_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_download_path_no
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_download_path_yes
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_enable_download_path_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_name_cannot_be_empty
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_save_path_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_create_category_success
import qbitcontroller.composeapp.generated.resources.torrent_list_create_subcategory
import qbitcontroller.composeapp.generated.resources.torrent_list_create_tag
import qbitcontroller.composeapp.generated.resources.torrent_list_create_tag_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_create_tag_name_cannot_be_empty
import qbitcontroller.composeapp.generated.resources.torrent_list_create_tag_success
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_category
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_category_confirm
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_category_success
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_tag
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_tag_desc
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_tag_success
import qbitcontroller.composeapp.generated.resources.torrent_list_delete_torrents
import qbitcontroller.composeapp.generated.resources.torrent_list_edit_category
import qbitcontroller.composeapp.generated.resources.torrent_list_edit_category_error
import qbitcontroller.composeapp.generated.resources.torrent_list_edit_category_success
import qbitcontroller.composeapp.generated.resources.torrent_list_empty_add
import qbitcontroller.composeapp.generated.resources.torrent_list_empty_description
import qbitcontroller.composeapp.generated.resources.torrent_list_empty_rss
import qbitcontroller.composeapp.generated.resources.torrent_list_empty_search
import qbitcontroller.composeapp.generated.resources.torrent_list_empty_title
import qbitcontroller.composeapp.generated.resources.torrent_list_free_space
import qbitcontroller.composeapp.generated.resources.torrent_list_no_result_description
import qbitcontroller.composeapp.generated.resources.torrent_list_no_result_reset
import qbitcontroller.composeapp.generated.resources.torrent_list_no_result_title
import qbitcontroller.composeapp.generated.resources.torrent_list_no_server_add
import qbitcontroller.composeapp.generated.resources.torrent_list_no_server_description
import qbitcontroller.composeapp.generated.resources.torrent_list_no_server_title
import qbitcontroller.composeapp.generated.resources.torrent_list_priority_decrease_success
import qbitcontroller.composeapp.generated.resources.torrent_list_priority_increase_success
import qbitcontroller.composeapp.generated.resources.torrent_list_priority_maximize_success
import qbitcontroller.composeapp.generated.resources.torrent_list_priority_minimize_success
import qbitcontroller.composeapp.generated.resources.torrent_list_search_torrents
import qbitcontroller.composeapp.generated.resources.torrent_list_set_location_hint
import qbitcontroller.composeapp.generated.resources.torrent_list_shutdown_confirm
import qbitcontroller.composeapp.generated.resources.torrent_list_shutdown_success
import qbitcontroller.composeapp.generated.resources.torrent_list_speed_format
import qbitcontroller.composeapp.generated.resources.torrent_list_speed_format_limit
import qbitcontroller.composeapp.generated.resources.torrent_list_status
import qbitcontroller.composeapp.generated.resources.torrent_list_status_active
import qbitcontroller.composeapp.generated.resources.torrent_list_status_all
import qbitcontroller.composeapp.generated.resources.torrent_list_status_checking
import qbitcontroller.composeapp.generated.resources.torrent_list_status_completed
import qbitcontroller.composeapp.generated.resources.torrent_list_status_downloading
import qbitcontroller.composeapp.generated.resources.torrent_list_status_error
import qbitcontroller.composeapp.generated.resources.torrent_list_status_format
import qbitcontroller.composeapp.generated.resources.torrent_list_status_inactive
import qbitcontroller.composeapp.generated.resources.torrent_list_status_moving
import qbitcontroller.composeapp.generated.resources.torrent_list_status_paused
import qbitcontroller.composeapp.generated.resources.torrent_list_status_resumed
import qbitcontroller.composeapp.generated.resources.torrent_list_status_seeding
import qbitcontroller.composeapp.generated.resources.torrent_list_status_set_as_default
import qbitcontroller.composeapp.generated.resources.torrent_list_status_stalled
import qbitcontroller.composeapp.generated.resources.torrent_list_switch_speed_limit_alternative_success
import qbitcontroller.composeapp.generated.resources.torrent_list_switch_speed_limit_regular_success
import qbitcontroller.composeapp.generated.resources.torrent_list_tags
import qbitcontroller.composeapp.generated.resources.torrent_list_torrents_delete_success
import qbitcontroller.composeapp.generated.resources.torrent_list_torrents_pause_success
import qbitcontroller.composeapp.generated.resources.torrent_list_torrents_resume_success
import qbitcontroller.composeapp.generated.resources.torrent_list_torrents_selected
import qbitcontroller.composeapp.generated.resources.torrent_list_trackers_all
import qbitcontroller.composeapp.generated.resources.torrent_list_trackers_format
import qbitcontroller.composeapp.generated.resources.torrent_list_trackers_title
import qbitcontroller.composeapp.generated.resources.torrent_list_trackers_trackerless
import qbitcontroller.composeapp.generated.resources.torrent_list_uncategorized
import qbitcontroller.composeapp.generated.resources.torrent_list_untagged
import qbitcontroller.composeapp.generated.resources.torrent_location_cannot_be_blank
import qbitcontroller.composeapp.generated.resources.torrent_location_update_success
import qbitcontroller.composeapp.generated.resources.torrent_no_categories
import qbitcontroller.composeapp.generated.resources.torrent_queueing_is_not_enabled
import qbitcontroller.composeapp.generated.resources.torrent_speed_alternative_speed_limits
import qbitcontroller.composeapp.generated.resources.torrent_speed_download_limit
import qbitcontroller.composeapp.generated.resources.torrent_speed_limits_title
import qbitcontroller.composeapp.generated.resources.torrent_speed_update_success
import qbitcontroller.composeapp.generated.resources.torrent_speed_upload_limit
import kotlin.math.abs
import kotlin.math.roundToInt

object TorrentListKeys {
    const val ServerId = "torrentList.serverId"
}

@Composable
fun TorrentListScreen(
    currentServer: ServerConfig?,
    addTorrentFlow: Flow<Int>,
    deleteTorrentFlow: Flow<Unit>,
    onSelectServer: (serverId: Int) -> Unit,
    onNavigateToTorrent: (serverId: Int, torrentHash: String, torrentName: String) -> Unit,
    onNavigateToAddTorrent: (serverId: Int) -> Unit,
    onNavigateToRss: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddEditServer: (serverId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentListViewModel = koinViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val torrents by viewModel.filteredTorrentList.collectAsStateWithLifecycle()
    val mainData = viewModel.mainData.collectAsStateWithLifecycle().value
    val servers by viewModel.serversFlow.collectAsStateWithLifecycle()
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

    PersistentLaunchedEffect(Json.encodeToString(currentServer)) {
        viewModel.setCurrentServer(currentServer)
    }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
        viewModel.setScreenActive(lifecycleState.isAtLeast(Lifecycle.State.STARTED))
    }

    LaunchedEffect(Unit) {
        addTorrentFlow.collect { serverId ->
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.torrent_add_success))
            }
            viewModel.loadMainData()
        }
    }

    LaunchedEffect(Unit) {
        deleteTorrentFlow.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.torrent_deleted_success))
            }
            viewModel.loadMainData()
        }
    }

    LaunchedEffect(mainData?.torrents) {
        selectedTorrents.removeAll { hash -> mainData?.torrents?.none { it.hash == hash } != false }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentListViewModel.Event.Error -> {
                val errorMessage = getErrorMessage(event.error)
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
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_queueing_is_not_enabled))
                }
            }
            TorrentListViewModel.Event.CategoryEditingFailed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_list_edit_category_error))
                }
            }
            is TorrentListViewModel.Event.TorrentsDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getPluralString(
                            Res.plurals.torrent_list_torrents_delete_success,
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
                        getPluralString(
                            Res.plurals.torrent_list_torrents_pause_success,
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
                        getPluralString(
                            Res.plurals.torrent_list_torrents_resume_success,
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
                        getString(Res.string.torrent_list_delete_category_success, event.name),
                    )
                }
            }
            is TorrentListViewModel.Event.TagDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.torrent_list_delete_tag_success, event.name),
                    )
                }
            }
            TorrentListViewModel.Event.TorrentsPriorityDecreased -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.torrent_list_priority_decrease_success),
                    )
                }
            }
            TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.torrent_list_priority_increase_success),
                    )
                }
            }
            TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.torrent_list_priority_maximize_success),
                    )
                }
            }
            TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.torrent_list_priority_minimize_success),
                    )
                }
            }
            TorrentListViewModel.Event.LocationUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_location_update_success))
                }
            }
            TorrentListViewModel.Event.CategoryCreated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_list_create_category_success))
                }
            }
            TorrentListViewModel.Event.CategoryEdited -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_list_edit_category_success))
                }
            }
            TorrentListViewModel.Event.TagCreated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_list_create_tag_success))
                }
            }
            is TorrentListViewModel.Event.SpeedLimitsToggled -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    if (event.switchedToAlternativeLimit) {
                        snackbarHostState.showSnackbar(
                            getString(Res.string.torrent_list_switch_speed_limit_alternative_success),
                        )
                    } else {
                        snackbarHostState.showSnackbar(
                            getString(Res.string.torrent_list_switch_speed_limit_regular_success),
                        )
                    }
                }
            }
            TorrentListViewModel.Event.SpeedLimitsUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_speed_update_success))
                }
            }
            TorrentListViewModel.Event.Shutdown -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_list_shutdown_success))
                }
            }
            TorrentListViewModel.Event.TorrentCategoryUpdated -> {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_category_update_success))
                }
            }
        }
    }

    PlatformBackHandler(enabled = isSearchMode) {
        isSearchMode = false
        filterQuery = TextFieldValue()
        viewModel.setSearchQuery("")
    }

    PlatformBackHandler(enabled = selectedTorrents.isNotEmpty()) {
        selectedTorrents.clear()
    }

    val notificationPermissionLauncher = notificationPermissionLauncher()
    PersistentLaunchedEffect {
        if (servers.isNotEmpty()) {
            notificationPermissionLauncher?.invoke()
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    PersistentLaunchedEffect(Json.encodeToString(currentServer)) {
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
                    val hideServerUrls by viewModel.hideServerUrls.collectAsStateWithLifecycle()

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
                        hideServerUrls = hideServerUrls,
                        onServerSelected = onSelectServer,
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
                        onNavigateToEditServer = onNavigateToAddEditServer,
                    )
                }
            },
        ) {
            val listState = rememberLazyListState()
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SwipeableSnackbarHost(hostState = snackbarHostState)
                },
                topBar = {
                    val currentSorting by viewModel.torrentSort.collectAsStateWithLifecycle()
                    val isReverseSorting by viewModel.isReverseSorting.collectAsStateWithLifecycle()

                    TopBar(
                        serverId = serverId,
                        currentServer = currentServer,
                        mainData = mainData,
                        listState = listState,
                        isSearchMode = isSearchMode,
                        searchQuery = searchQuery,
                        filterQuery = filterQuery,
                        currentSorting = currentSorting,
                        isReverseSorting = isReverseSorting,
                        canFocusNow = drawerState.isClosed,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSearchQueryChange = {
                            filterQuery = it
                            viewModel.setSearchQuery(it.text)
                        },
                        onSearchModeChange = { isSearchMode = it },
                        onTorrentSortChange = { viewModel.setTorrentSort(it) },
                        onReverseSortingChange = { viewModel.changeReverseSorting() },
                        onDialogOpen = { currentDialog = it },
                        onNavigateToAddTorrent = onNavigateToAddTorrent,
                    )
                },
                bottomBar = {
                    Box {
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))

                        AnimatedVisibility(
                            visible = selectedTorrents.isNotEmpty(),
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            BottomBarSelection(
                                torrents = torrents,
                                selectedTorrents = selectedTorrents,
                                canFocusNow = drawerState.isClosed,
                                isQueueingEnabled = mainData?.serverState?.isQueueingEnabled == true,
                                onPauseTorrents = { viewModel.pauseTorrents(selectedTorrents.toList()) },
                                onResumeTorrents = { viewModel.resumeTorrents(selectedTorrents.toList()) },
                                onDeleteTorrents = { currentDialog = Dialog.DeleteSelectedTorrents },
                                onMaximizeTorrentsPriority = {
                                    viewModel.maximizeTorrentPriority(selectedTorrents.toList())
                                },
                                onIncreaseTorrentsPriority = {
                                    viewModel.increaseTorrentPriority(selectedTorrents.toList())
                                },
                                onDecreaseTorrentsPriority = {
                                    viewModel.decreaseTorrentPriority(selectedTorrents.toList())
                                },
                                onMinimizeTorrentsPriority = {
                                    viewModel.minimizeTorrentPriority(selectedTorrents.toList())
                                },
                                onSetTorrentsLocation = { currentDialog = Dialog.SetSelectedTorrentsLocation },
                                onSetTorrentsCategory = { currentDialog = Dialog.SetSelectedTorrentsCategory },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshMainData() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding.excludeBottom())
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                ) {
                    Column {
                        /*AnimatedNullableVisibility(
                            value = mainData,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) { _, mainData ->
                            val text = buildAnnotatedString {
                                fun appendStat(color: Color, speed: String, total: String) {
                                    withStyle(SpanStyle(color = color)) {
                                        append(speed)
                                    }
                                    append(" ")
                                    withStyle(SpanStyle(color = color.copy(alpha = 0.5f))) {
                                        append("($total)")
                                    }
                                }

                                appendStat(
                                    color = MaterialTheme.colorScheme.primary,
                                    speed = "↓ ${formatBytesPerSecond(mainData.serverState.downloadSpeed)}",
                                    total = formatBytes(mainData.serverState.downloadSession),
                                )

                                append(" ")

                                appendStat(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    speed = "↑ ${formatBytesPerSecond(mainData.serverState.uploadSpeed)}",
                                    total = formatBytes(mainData.serverState.uploadSession),
                                )
                            }

                            Text(
                                text = text,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(topAppBarColor(listState))
                                    .focusProperties {
                                        canFocus = drawerState.isClosed
                                    }
                                    .clickable {
                                        currentDialog = Dialog.SpeedLimits
                                    }
                                    .padding(4.dp),
                            )
                        }*/

                        val swipeEnabled by viewModel.areTorrentSwipeActionsEnabled.collectAsStateWithLifecycle()
                        val trafficStats by viewModel.trafficStatsInList.collectAsStateWithLifecycle()

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
                                                onNavigateToTorrent(serverId, torrent.hash, torrent.name)
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
                                        trafficStats = trafficStats,
                                        onPauseTorrent = { viewModel.pauseTorrents(listOf(torrent.hash)) },
                                        onResumeTorrent = { viewModel.resumeTorrents(listOf(torrent.hash)) },
                                        onDeleteTorrent = { currentDialog = Dialog.DeleteTorrent(torrent.hash) },
                                    )
                                }

                                item {
                                    LazyColumnItemMinHeight()
                                    Spacer(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
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
                                                filterQuery = TextFieldValue()
                                                viewModel.resetFilters()
                                                isSearchMode = false
                                            },
                                            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                                            modifier = Modifier.focusProperties {
                                                canFocus = drawerState.isClosed
                                            },
                                        )
                                    }
                                    2 -> {
                                        NoTorrentsMessage(
                                            serverId = serverId,
                                            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                                            onNavigateToAddTorrent = onNavigateToAddTorrent,
                                            onNavigateToRss = onNavigateToRss,
                                            onNavigateToSearch = onNavigateToSearch,
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
                            text = stringResource(Res.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        val actionMenuItems = listOf(
                            ActionMenuItem(
                                title = stringResource(Res.string.main_action_about),
                                icon = Icons.Filled.Info,
                                onClick = {
                                    currentDialog = Dialog.About
                                },
                                showAsAction = false,
                            ),
                        )

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
                    title = stringResource(Res.string.torrent_list_no_server_title),
                    description = stringResource(Res.string.torrent_list_no_server_description),
                    actionButton = {
                        Button(
                            onClick = { onNavigateToAddEditServer(null) },
                        ) {
                            Text(text = stringResource(Res.string.torrent_list_no_server_add))
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
    trafficStats: TrafficStats,
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
                containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Unspecified,
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
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                val trafficStatsString = when (trafficStats) {
                    TrafficStats.NONE -> null
                    TrafficStats.TOTAL -> buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("↓ ${formatBytes(torrent.downloaded)}")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                            append("↑ ${formatBytes(torrent.uploaded)}")
                        }
                    }
                    TrafficStats.SESSION -> buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("↓ ${formatBytes(torrent.downloadedSession)}")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                            append("↑ ${formatBytes(torrent.uploadedSession)}")
                        }
                    }
                    TrafficStats.COMPLETE -> buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("↓ ${formatBytes(torrent.downloaded)}")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
                            append("(${formatBytes(torrent.downloadedSession)})")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                            append("↑ ${formatBytes(torrent.uploaded)}")
                        }
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))) {
                            append("(${formatBytes(torrent.uploadedSession)})")
                        }
                    }
                }

                if (trafficStatsString != null) {
                    Text(text = trafficStatsString)
                }

                Spacer(modifier = Modifier.height(4.dp))

                val progressColor by animateColorAsState(
                    targetValue = getTorrentStateColor(torrent.state),
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
                    Text(text = formatTorrentState(torrent.state),
                        style = MaterialTheme.typography.labelSmall)
                    if (torrent.eta != null) {
                        Text(text = formatSeconds(torrent.eta),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    val speedText = buildList {
                        if (torrent.downloadSpeed > 0) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("↓ ${formatBytesPerSecond(torrent.downloadSpeed)}")
                            }
                        }

                        if (torrent.downloadSpeed > 0 && torrent.uploadSpeed > 0) {
                            append(" ")
                        }

                        if (torrent.uploadSpeed > 0) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                                append("↑ ${formatBytesPerSecond(torrent.uploadSpeed)}")
                            }
                        }
                    }.joinToString(" ")
                    Text(text = speedText,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    serverId: Int?,
    servers: List<ServerConfig>,
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
    hideServerUrls: Boolean,
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
    onNavigateToEditServer: (serverId: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = remember {
        listOf(
            Triple(TorrentFilter.ALL, Res.string.torrent_list_status_all, Icons.Outlined.FilterAlt),
            Triple(
                TorrentFilter.DOWNLOADING,
                Res.string.torrent_list_status_downloading,
                Icons.Outlined.KeyboardDoubleArrowDown,
            ),
            Triple(TorrentFilter.SEEDING, Res.string.torrent_list_status_seeding, Icons.Outlined.KeyboardDoubleArrowUp),
            Triple(TorrentFilter.COMPLETED, Res.string.torrent_list_status_completed, Icons.Outlined.Done),
            Triple(TorrentFilter.RESUMED, Res.string.torrent_list_status_resumed, Icons.Filled.PlayArrow),
            Triple(TorrentFilter.PAUSED, Res.string.torrent_list_status_paused, Icons.Filled.Pause),
            Triple(TorrentFilter.ACTIVE, Res.string.torrent_list_status_active, Icons.Outlined.ToggleOn),
            Triple(TorrentFilter.INACTIVE, Res.string.torrent_list_status_inactive, Icons.Outlined.ToggleOff),
            Triple(TorrentFilter.STALLED, Res.string.torrent_list_status_stalled, Icons.Filled.SyncAlt),
            Triple(TorrentFilter.CHECKING, Res.string.torrent_list_status_checking, Icons.Outlined.Cached),
            Triple(TorrentFilter.MOVING, Res.string.torrent_list_status_moving, Icons.AutoMirrored.Outlined.DriveFileMove),
            Triple(TorrentFilter.ERROR, Res.string.torrent_list_status_error, Icons.Outlined.ErrorOutline),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
            }

            items(servers) { serverConfig ->
                Column {
                    DrawerServerItem(
                        name = serverConfig.name,
                        url = serverConfig.visibleUrl,
                        isSelected = serverId == serverConfig.id,
                        hideServerUrls = hideServerUrls,
                        onClick = {
                            onServerSelected(serverConfig.id)
                            onDrawerClose()
                        },
                        onLongClick = {
                            onNavigateToEditServer(serverConfig.id)
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
                    text = stringResource(Res.string.torrent_list_status),
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
                            Res.string.torrent_list_status_format,
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
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(Res.string.torrent_list_status_set_as_default))
                            },
                            leadingIcon = { Icon(imageVector = Icons.Filled.PushPin, contentDescription = null) },
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
                    text = stringResource(Res.string.torrent_list_categories),
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
                AnimatedVisibility(
                    visible = !areCategoriesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Folder,
                        text = stringResource(
                            Res.string.torrent_list_category_tag_format,
                            stringResource(Res.string.torrent_list_category_tag_all),
                            counts?.allCount ?: 0,
                        ),
                        isSelected = selectedCategory is CategoryTag.All,
                        onClick = {
                            onSelectCategory(CategoryTag.All)
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
                    visible = !areCategoriesCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    DrawerItem(
                        icon = Icons.Filled.Folder,
                        text = stringResource(
                            Res.string.torrent_list_category_tag_format,
                            stringResource(Res.string.torrent_list_uncategorized),
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
                            Res.string.torrent_list_category_tag_format,
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
                    ) {
                        if (areSubcategoriesEnabled) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(Res.string.torrent_list_create_subcategory))
                                },
                                leadingIcon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDrawerClose()
                                    onDialogOpen(Dialog.CreateSubcategory(parent = category))
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(Res.string.torrent_list_edit_category))
                            },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
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
                                Text(text = stringResource(Res.string.torrent_list_delete_category))
                            },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
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
                    text = stringResource(Res.string.torrent_list_tags),
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
                            Res.string.torrent_list_category_tag_format,
                            stringResource(Res.string.torrent_list_category_tag_all),
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
                            Res.string.torrent_list_category_tag_format,
                            stringResource(Res.string.torrent_list_untagged),
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
                            Res.string.torrent_list_category_tag_format,
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
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(Res.string.torrent_list_delete_tag))
                            },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
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
                    text = stringResource(Res.string.torrent_list_trackers_title),
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
                            Res.string.torrent_list_trackers_format,
                            stringResource(Res.string.torrent_list_trackers_all),
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
                            Res.string.torrent_list_trackers_format,
                            stringResource(Res.string.torrent_list_trackers_trackerless),
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
                            Res.string.torrent_list_trackers_format,
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
                .background(DrawerDefaults.modalContainerColor.copy(alpha = if (isDarkTheme()) 0.5f else 0.9f)),
        )
    }
}

@Composable
private fun DrawerServerItem(
    name: String?,
    url: String,
    isSelected: Boolean,
    hideServerUrls: Boolean,
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
        val showName = name != null
        val showUrl = !hideServerUrls || name == null

        if (showName) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Unspecified,
            )
        }

        if (showName && showUrl) {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (showUrl) {
            Text(
                text = url,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Unspecified,
            )
        }
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
    listState: LazyListState,
    isSearchMode: Boolean,
    searchQuery: String,
    filterQuery: TextFieldValue,
    currentSorting: TorrentSort,
    isReverseSorting: Boolean,
    canFocusNow: Boolean,
    onOpenDrawer: () -> Unit,
    onSearchQueryChange: (newQuery: TextFieldValue) -> Unit,
    onSearchModeChange: (newValue: Boolean) -> Unit,
    onTorrentSortChange: (newSort: TorrentSort) -> Unit,
    onReverseSortingChange: () -> Unit,
    onDialogOpen: (dialog: Dialog) -> Unit,
    onNavigateToAddTorrent: (serverId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    TopAppBar(
        modifier = modifier,
        title = {
            if (!isSearchMode) {
                Column {
                    /*Text(
                        text = currentServer.name ?: stringResource(Res.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )*/
                    Row {
                        AnimatedNullableVisibility(
                            value = mainData?.serverState?.freeSpace?.takeIf { it >= 0 },
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) { _, freeSpace ->
                            Text(
                                text = stringResource(Res.string.torrent_list_free_space, formatBytes(freeSpace)),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.alpha(0.78f),
                            )
                        }
                    }
                    AnimatedNullableVisibility(
                        value = mainData,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) { _, mainData ->
                        Text(
                            text = stringResource(
                                Res.string.torrent_list_speed_format_limit,
                                formatBytesPerSecond(mainData.serverState.downloadSpeed),
                                formatBytesPerSecond(mainData.serverState.uploadSpeed),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(topAppBarColor(listState))
                                .focusProperties {
                                    canFocus = canFocusNow
                                }
                                .clickable {
                                    onDialogOpen(Dialog.SpeedLimits)
                                },
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
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
                    placeholder = stringResource(Res.string.torrent_list_search_torrents),
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

            val actionMenuItems = listOf(
                if (!isSearchMode) {
                    ActionMenuItem(
                        title = stringResource(Res.string.action_search),
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
                    title = stringResource(Res.string.torrent_list_action_add_torrent),
                    icon = Icons.Filled.Add,
                    onClick = { onNavigateToAddTorrent(serverId) },
                    showAsAction = true,
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_statistics),
                    icon = Icons.Outlined.Analytics,
                    onClick = {
                        onDialogOpen(Dialog.Statistics)
                    },
                    showAsAction = false,
                    isEnabled = mainData != null,
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_shutdown),
                    icon = Icons.Filled.PowerSettingsNew,
                    onClick = {
                        onDialogOpen(Dialog.Shutdown)
                    },
                    showAsAction = false,
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_sort),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = {
                        showSortMenu = true
                    },
                    showAsAction = false,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = null,
                        )
                    },
                    dropdownMenu = {
                        val scrollState = rememberScrollState()
                        LaunchedEffect(showSortMenu) {
                            if (showSortMenu) {
                                scrollState.scrollTo(0)
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            scrollState = scrollState,
                        ) {
                            Text(
                                text = stringResource(Res.string.torrent_list_action_sort),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )

                            val sortOptions = remember {
                                listOf(
                                    Res.string.torrent_list_action_sort_name to TorrentSort.NAME,
                                    Res.string.torrent_list_action_sort_status to TorrentSort.STATUS,
                                    Res.string.torrent_list_action_sort_hash to TorrentSort.HASH,
                                    Res.string.torrent_list_action_sort_download_speed to TorrentSort.DOWNLOAD_SPEED,
                                    Res.string.torrent_list_action_sort_upload_speed to TorrentSort.UPLOAD_SPEED,
                                    Res.string.torrent_list_action_sort_priority to TorrentSort.PRIORITY,
                                    Res.string.torrent_list_action_sort_eta to TorrentSort.ETA,
                                    Res.string.torrent_list_action_sort_size to TorrentSort.SIZE,
                                    Res.string.torrent_list_action_sort_progress to TorrentSort.PROGRESS,
                                    Res.string.torrent_list_action_sort_ratio to TorrentSort.RATIO,
                                    Res.string.torrent_list_action_sort_connected_seeds to TorrentSort.CONNECTED_SEEDS,
                                    Res.string.torrent_list_action_sort_total_seeds to TorrentSort.TOTAL_SEEDS,
                                    Res.string.torrent_list_action_sort_connected_leeches to TorrentSort.CONNECTED_LEECHES,
                                    Res.string.torrent_list_action_sort_total_leeches to TorrentSort.TOTAL_LEECHES,
                                    Res.string.torrent_list_action_sort_addition_date to TorrentSort.ADDITION_DATE,
                                    Res.string.torrent_list_action_sort_completion_date to TorrentSort.COMPLETION_DATE,
                                    Res.string.torrent_list_action_sort_last_activity to TorrentSort.LAST_ACTIVITY,
                                    Res.string.torrent_list_action_sort_downloaded to TorrentSort.DOWNLOADED,
                                    Res.string.torrent_list_action_sort_uploaded to TorrentSort.UPLOADED,
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
                                        Text(text = stringResource(Res.string.torrent_list_action_sort_reverse))
                                    }
                                },
                                onClick = {
                                    onReverseSortingChange()
                                    showSortMenu = false
                                },
                            )
                        }
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.main_action_about),
                    icon = Icons.Filled.Info,
                    onClick = {
                        onDialogOpen(Dialog.About)
                    },
                    showAsAction = false,
                ),
            )

            AppBarActions(
                items = actionMenuItems,
                canFocus = canFocusNow,
            )
        },
        colors = listState.topAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )
}

@Composable
private fun BottomBarSelection(
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
                    Res.plurals.torrent_list_torrents_selected,
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
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                )
            }
        },
        actions = {
            var showPriorityMenu by rememberSaveable { mutableStateOf(false) }

            val actionMenuItems = listOf(
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_pause),
                    icon = Icons.Filled.Pause,
                    showAsAction = true,
                    onClick = {
                        onPauseTorrents()
                        selectedTorrents.clear()
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_resume),
                    icon = Icons.Filled.PlayArrow,
                    showAsAction = true,
                    onClick = {
                        onResumeTorrents()
                        selectedTorrents.clear()
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_delete),
                    icon = Icons.Filled.Delete,
                    showAsAction = true,
                    onClick = {
                        onDeleteTorrents()
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_priority),
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
                        ) {
                            Text(
                                text = stringResource(Res.string.torrent_list_action_priority),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(Res.string.torrent_list_action_priority_maximize))
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
                                    Text(text = stringResource(Res.string.torrent_list_action_priority_increase))
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
                                    Text(text = stringResource(Res.string.torrent_list_action_priority_decrease))
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
                                    Text(text = stringResource(Res.string.torrent_list_action_priority_minimize))
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
                    title = stringResource(Res.string.torrent_list_action_set_location),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    showAsAction = false,
                    onClick = {
                        onSetTorrentsLocation()
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.torrent_list_action_set_category),
                    icon = Icons.AutoMirrored.Filled.Label,
                    showAsAction = false,
                    onClick = {
                        onSetTorrentsCategory()
                    },
                ),
                ActionMenuItem(
                    title = stringResource(Res.string.action_select_all),
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
                    title = stringResource(Res.string.action_select_inverse),
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

            AppBarActions(
                items = actionMenuItems,
                canFocus = canFocusNow,
                bottom = true,
            )
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
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

    var nameError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_list_create_category_name_cannot_be_empty),
    ) { mutableStateOf(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onConfirm(name.text, savePath.text, downloadPathEnabled, downloadPath.text)
                    } else {
                        nameError = Res.string.torrent_list_create_category_name_cannot_be_empty
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = {
            Text(
                text = stringResource(
                    when {
                        isSubcategory -> Res.string.torrent_list_create_subcategory
                        category != null -> Res.string.torrent_list_edit_category
                        else -> Res.string.torrent_list_create_category
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
                        if (it.text != name.text) {
                            nameError = null
                        }
                        name = it
                    },
                    readOnly = category != null,
                    label = {
                        Text(
                            text = stringResource(Res.string.torrent_list_create_category_hint),
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
                        .animateContentSize()
                        .focusRequester(focusRequester),
                )

                OutlinedTextField(
                    value = savePath,
                    onValueChange = {
                        savePath = it
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.torrent_list_create_category_save_path_hint),
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        value = when (downloadPathEnabled) {
                            true -> stringResource(Res.string.torrent_list_create_category_download_path_yes)
                            false -> stringResource(Res.string.torrent_list_create_category_download_path_no)
                            null -> stringResource(Res.string.torrent_list_create_category_download_path_default)
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_list_create_category_enable_download_path_hint),
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
                                    text = stringResource(Res.string.torrent_list_create_category_download_path_default),
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
                                    text = stringResource(Res.string.torrent_list_create_category_download_path_yes),
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
                                    text = stringResource(Res.string.torrent_list_create_category_download_path_no),
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
                            text = stringResource(Res.string.torrent_list_create_category_download_path_hint),
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
                                nameError = Res.string.torrent_list_create_category_name_cannot_be_empty
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
            Text(text = stringResource(Res.string.torrent_list_delete_category))
        },
        text = {
            val description = rememberReplaceAndApplyStyle(
                text = stringResource(Res.string.torrent_list_delete_category_confirm),
                oldValue = "%1\$s",
                newValue = category,
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            )
            Text(text = description)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun CreateTagDialog(onDismiss: () -> Unit, onConfirm: (tags: List<String>) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var nameError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_list_create_tag_name_cannot_be_empty),
    ) { mutableStateOf(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onConfirm(name.text.split("\n"))
                    } else {
                        nameError = Res.string.torrent_list_create_tag_name_cannot_be_empty
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = {
            Text(text = stringResource(Res.string.torrent_list_create_tag))
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.text != name.text) {
                        nameError = null
                    }
                    name = it
                },
                label = {
                    Text(
                        text = stringResource(Res.string.torrent_list_create_tag_hint),
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
                            nameError = Res.string.torrent_list_create_tag_name_cannot_be_empty
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
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
            Text(text = stringResource(Res.string.torrent_list_delete_tag))
        },
        text = {
            val description = rememberReplaceAndApplyStyle(
                text = stringResource(Res.string.torrent_list_delete_tag_desc),
                oldValue = "%1\$s",
                newValue = tag,
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            )
            Text(text = description)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(Res.string.dialog_ok))
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
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteFiles) },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = {
            Text(text = pluralStringResource(Res.plurals.torrent_list_delete_torrents, count, count))
        },
        text = {
            CheckboxWithLabel(
                checked = deleteFiles,
                onCheckedChange = { deleteFiles = it },
                label = stringResource(Res.string.torrent_delete_files),
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
    var locationError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_location_cannot_be_blank),
    ) { mutableStateOf(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (location.text.isNotBlank()) {
                        onConfirm(location.text)
                    } else {
                        locationError = Res.string.torrent_location_cannot_be_blank
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(Res.string.torrent_list_action_set_location)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = location,
                onValueChange = {
                    if (it.text != location.text) {
                        locationError = null
                    }
                    location = it
                },
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(Res.string.torrent_list_set_location_hint),
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
                            locationError = Res.string.torrent_location_cannot_be_blank
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
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
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory) },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(Res.string.torrent_list_action_set_category)) },
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
                    text = stringResource(Res.string.torrent_no_categories),
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
    var uploadSpeedError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_add_speed_limit_too_big),
    ) { mutableStateOf(null) }

    var downloadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(currentDownloadSpeedLimit.takeIf { it != 0 }?.toString() ?: ""))
    }
    var downloadSpeedUnit by rememberSaveable { mutableIntStateOf(0) }
    var downloadSpeedError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_add_speed_limit_too_big),
    ) { mutableStateOf(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        textHorizontalPadding = 16.dp,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(
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
                            uploadSpeedError = Res.string.torrent_add_speed_limit_too_big
                        }
                        if (download == null) {
                            downloadSpeedError = Res.string.torrent_add_speed_limit_too_big
                        }

                        if (upload != null && download != null) {
                            onSetSpeedLimits(upload, download)
                        }
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = {
            Text(text = stringResource(Res.string.torrent_speed_limits_title))
        },
        text = {
            Column {
                CheckboxWithLabel(
                    checked = alternativeLimits,
                    onCheckedChange = { alternativeLimits = it },
                    label = stringResource(Res.string.torrent_speed_alternative_speed_limits),
                )

                Spacer(modifier = Modifier.height(4.dp))

                val unitTextWidth = max(
                    measureTextWidth(stringResource(Res.string.size_kibibytes)),
                    measureTextWidth(stringResource(Res.string.size_mebibytes)),
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
                                if (it.text != uploadSpeedLimit.text) {
                                    uploadSpeedError = null
                                }
                                uploadSpeedLimit = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_speed_upload_limit),
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
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
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
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            value = when (uploadSpeedUnit) {
                                1 -> stringResource(Res.string.size_mebibytes)
                                else -> stringResource(Res.string.size_kibibytes)
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
                                        text = stringResource(Res.string.size_kibibytes),
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
                                        text = stringResource(Res.string.size_mebibytes),
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
                                if (it.text != downloadSpeedLimit.text) {
                                    downloadSpeedError = null
                                }
                                downloadSpeedLimit = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_speed_download_limit),
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
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
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
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            value = when (downloadSpeedUnit) {
                                1 -> stringResource(Res.string.size_mebibytes)
                                else -> stringResource(Res.string.size_kibibytes)
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
                                        text = stringResource(Res.string.size_kibibytes),
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
                                        text = stringResource(Res.string.size_mebibytes),
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
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        title = { Text(text = stringResource(Res.string.torrent_list_action_statistics)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatisticsHeader(
                    title = stringResource(Res.string.stats_category_user_statistics),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_all_time_upload),
                    value = formatBytes(state.allTimeUpload),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_all_time_download),
                    value = formatBytes(state.allTimeDownload),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_all_time_share_ratio),
                    value = state.globalRatio,
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_session_waste),
                    value = formatBytes(state.sessionWaste),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_connected_peers),
                    value = state.connectedPeers.toString(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatisticsHeader(
                    title = stringResource(Res.string.stats_category_cache_statistics),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_read_cache_hits),
                    value = stringResource(Res.string.percentage_format, state.readCacheHits),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_total_buffer_size),
                    value = formatBytes(state.bufferSize),
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatisticsHeader(
                    title = stringResource(Res.string.stats_category_performance_statistics),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_write_cache_overload),
                    value = stringResource(Res.string.percentage_format, state.writeCacheOverload),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_read_cache_overload),
                    value = stringResource(Res.string.percentage_format, state.readCacheOverload),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_queued_io_jobs),
                    value = state.queuedIOJobs.toString(),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_average_time_in_queue),
                    value = stringResource(Res.string.stats_ms_format, state.averageTimeInQueue),
                )

                StatisticRow(
                    label = stringResource(Res.string.stats_total_queued_size),
                    value = formatBytes(state.queuedSize),
                )
            }
        },
    )
}

@Composable
private fun StatisticsHeader(title: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        HorizontalDivider()
    }
}

@Composable
private fun StatisticRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ShutdownDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.torrent_list_action_shutdown))
        },
        text = {
            Text(text = stringResource(Res.string.torrent_list_shutdown_confirm))
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(Res.string.dialog_ok))
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
                    AppIcon()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(text = BuildConfig.Version)
                }

                val description = buildAnnotatedString {
                    val parts = stringResource(Res.string.about_description).split("%1\$s")
                    parts.forEachIndexed { index, part ->
                        append(part)
                        if (index != parts.lastIndex) {
                            withLink(
                                link = LinkAnnotation.Url(
                                    url = BuildConfig.SourceCodeUrl,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                    ),
                                ),
                            ) {
                                append(BuildConfig.SourceCodeUrl)
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
            Button(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
    )
}

@Composable
private fun NoTorrentsMessage(
    serverId: Int,
    contentPadding: PaddingValues,
    onNavigateToAddTorrent: (serverId: Int) -> Unit,
    onNavigateToRss: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyListMessage(
        icon = Icons.Default.Download,
        title = stringResource(Res.string.torrent_list_empty_title),
        description = stringResource(Res.string.torrent_list_empty_description),
        actionButton = {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(
                    onClick = onNavigateToRss,
                ) {
                    Text(text = stringResource(Res.string.torrent_list_empty_rss))
                }
                OutlinedButton(
                    onClick = onNavigateToSearch,
                ) {
                    Text(text = stringResource(Res.string.torrent_list_empty_search))
                }
                Button(
                    onClick = { onNavigateToAddTorrent(serverId) },
                ) {
                    Text(text = stringResource(Res.string.torrent_list_empty_add))
                }
            }
        },
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun NoResultsMessage(onResetFilters: () -> Unit, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    EmptyListMessage(
        icon = Icons.Default.FilterList,
        title = stringResource(Res.string.torrent_list_no_result_title),
        description = stringResource(Res.string.torrent_list_no_result_description),
        actionButton = {
            Button(onClick = onResetFilters) {
                Text(text = stringResource(Res.string.torrent_list_no_result_reset))
            }
        },
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
expect fun AppIcon(modifier: Modifier = Modifier)
