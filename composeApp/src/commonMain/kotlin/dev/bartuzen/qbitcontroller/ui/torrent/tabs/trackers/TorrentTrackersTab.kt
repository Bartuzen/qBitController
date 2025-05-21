package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.action_select_all
import qbitcontroller.composeapp.generated.resources.action_select_inverse
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.torrent_error_not_found
import qbitcontroller.composeapp.generated.resources.torrent_trackers_action_add
import qbitcontroller.composeapp.generated.resources.torrent_trackers_action_delete
import qbitcontroller.composeapp.generated.resources.torrent_trackers_action_edit
import qbitcontroller.composeapp.generated.resources.torrent_trackers_add_hint
import qbitcontroller.composeapp.generated.resources.torrent_trackers_added
import qbitcontroller.composeapp.generated.resources.torrent_trackers_delete_desc
import qbitcontroller.composeapp.generated.resources.torrent_trackers_delete_title
import qbitcontroller.composeapp.generated.resources.torrent_trackers_deleted
import qbitcontroller.composeapp.generated.resources.torrent_trackers_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_trackers_edit_hint
import qbitcontroller.composeapp.generated.resources.torrent_trackers_edited
import qbitcontroller.composeapp.generated.resources.torrent_trackers_leeches
import qbitcontroller.composeapp.generated.resources.torrent_trackers_peers
import qbitcontroller.composeapp.generated.resources.torrent_trackers_seeds
import qbitcontroller.composeapp.generated.resources.torrent_trackers_selected
import qbitcontroller.composeapp.generated.resources.torrent_trackers_url_cannot_be_empty

@Composable
fun TorrentTrackersTab(
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    snackbarEventFlow: MutableSharedFlow<String>,
    actionsEventFlow: MutableSharedFlow<Pair<Int, List<ActionMenuItem>>>,
    bottomBarStateEventFlow: MutableSharedFlow<Triple<Int, Dp, Boolean>>,
    modifier: Modifier = Modifier,
    viewModel: TorrentTrackersViewModel = koinViewModel(parameters = { parametersOf(serverId, torrentHash) }),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val selectedTrackers = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    val actions = listOf(
        ActionMenuItem(
            title = stringResource(Res.string.torrent_trackers_action_add),
            icon = Icons.Filled.Add,
            onClick = { currentDialog = Dialog.Add },
            showAsAction = true,
        ),
    )

    LaunchedEffect(actions) {
        launch {
            actionsEventFlow.emit(2 to actions)
        }
    }

    LaunchedEffect(trackers) {
        selectedTrackers.removeAll { url -> trackers?.none { it.url == url } != false }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    BackHandler(enabled = isScreenActive && selectedTrackers.isNotEmpty()) {
        selectedTrackers.clear()
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentTrackersViewModel.Event.Error -> {
                snackbarEventFlow.emit(getErrorMessage(event.error))
            }
            TorrentTrackersViewModel.Event.TorrentNotFound -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_error_not_found))
            }
            TorrentTrackersViewModel.Event.TrackersAdded -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_trackers_added))
            }
            TorrentTrackersViewModel.Event.TrackersDeleted -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_trackers_deleted))
            }
            TorrentTrackersViewModel.Event.TrackerEdited -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_trackers_edited))
            }
        }
    }

    when (currentDialog) {
        Dialog.Add -> {
            AddTrackersDialog(
                onDismiss = { currentDialog = null },
                onAdd = { urls ->
                    viewModel.addTrackers(urls)
                    currentDialog = null
                },
            )
        }
        is Dialog.Delete -> {
            LaunchedEffect(selectedTrackers.isEmpty()) {
                if (selectedTrackers.isEmpty()) {
                    currentDialog = null
                }
            }

            DeleteSelectedTrackersDialog(
                count = selectedTrackers.size,
                onDismiss = { currentDialog = null },
                onDelete = {
                    viewModel.deleteTrackers(selectedTrackers.toList())
                    currentDialog = null
                    selectedTrackers.clear()
                },
            )
        }
        is Dialog.Edit -> {
            LaunchedEffect(selectedTrackers.size != 1) {
                if (selectedTrackers.size != 1) {
                    currentDialog = null
                }
            }

            selectedTrackers.firstOrNull()?.let { tracker ->
                EditSelectedTrackerDialog(
                    initialUrl = tracker,
                    onDismiss = { currentDialog = null },
                    onEdit = { url ->
                        viewModel.editTracker(tracker, url)
                        currentDialog = null
                    },
                )
            }
        }
        null -> {}
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            var bottomBarHeight by remember { mutableStateOf(0.dp) }
            val visibleState = remember { MutableTransitionState(selectedTrackers.isNotEmpty()) }

            LaunchedEffect(selectedTrackers.isNotEmpty()) {
                visibleState.targetState = selectedTrackers.isNotEmpty()
            }

            LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
                if (visibleState.isIdle && !visibleState.currentState) {
                    bottomBarHeight = 0.dp
                }
            }

            LaunchedEffect(bottomBarHeight, visibleState.isIdle) {
                bottomBarStateEventFlow.emit(
                    Triple(2, bottomBarHeight, !visibleState.isIdle),
                )
            }

            val density = LocalDensity.current
            AnimatedVisibility(
                visibleState = visibleState,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.onGloballyPositioned {
                    bottomBarHeight = with(density) { it.size.height.toDp() }
                },
            ) {
                TopAppBar(
                    title = {
                        var selectedSize by rememberSaveable { mutableIntStateOf(0) }

                        LaunchedEffect(selectedTrackers.size) {
                            if (selectedTrackers.isNotEmpty()) {
                                selectedSize = selectedTrackers.size
                            }
                        }

                        Text(
                            text = pluralStringResource(
                                Res.plurals.torrent_trackers_selected,
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
                        IconButton(onClick = { selectedTrackers.clear() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        val actionMenuItems = listOf(
                            ActionMenuItem(
                                title = stringResource(Res.string.torrent_trackers_action_delete),
                                icon = Icons.Filled.Delete,
                                onClick = { currentDialog = Dialog.Delete },
                                showAsAction = true,
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.torrent_trackers_action_edit),
                                icon = Icons.Filled.DriveFileRenameOutline,
                                onClick = { currentDialog = Dialog.Edit },
                                showAsAction = true,
                                isEnabled = selectedTrackers.size == 1,
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_all),
                                icon = Icons.Filled.SelectAll,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newTrackers = trackers
                                        ?.filter { it.url !in selectedTrackers && it.tier != null }
                                        ?.map { it.url }
                                        ?: return@onClick
                                    selectedTrackers.addAll(newTrackers)
                                },
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_inverse),
                                icon = Icons.Filled.FlipToBack,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newTrackers = trackers
                                        ?.filter { it.url !in selectedTrackers && it.tier != null }
                                        ?.map { it.url }
                                        ?: return@onClick
                                    selectedTrackers.clear()
                                    selectedTrackers.addAll(newTrackers)
                                },
                            ),
                        )

                        AppBarActions(items = actionMenuItems, bottom = true)
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTrackers() },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = trackers ?: emptyList(),
                    key = { if (it.tier == null) "0${it.url}" else "1${it.url}" },
                ) { tracker ->
                    TrackerItem(
                        tracker = tracker,
                        selected = tracker.url in selectedTrackers,
                        selectable = tracker.tier != null,
                        onClick = {
                            if (selectedTrackers.isNotEmpty()) {
                                if (tracker.url !in selectedTrackers) {
                                    selectedTrackers += tracker.url
                                } else {
                                    selectedTrackers -= tracker.url
                                }
                            }
                        },
                        onLongClick = {
                            if (tracker.url !in selectedTrackers) {
                                selectedTrackers += tracker.url
                            } else {
                                selectedTrackers -= tracker.url
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
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
fun TrackerItem(
    tracker: TorrentTracker,
    selected: Boolean,
    selectable: Boolean,
    onClick: (() -> Unit),
    onLongClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected && selectable) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                Color.Unspecified
            },
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .run {
                    if (selectable) {
                        combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = tracker.url,
                style = MaterialTheme.typography.titleMedium,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatItem(
                    label = stringResource(Res.string.torrent_trackers_peers),
                    value = tracker.peers,
                    icon = Icons.Outlined.People,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(Res.string.torrent_trackers_seeds),
                    value = tracker.seeds,
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(Res.string.torrent_trackers_leeches),
                    value = tracker.leeches,
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(Res.string.torrent_trackers_downloaded),
                    value = tracker.downloaded,
                    icon = Icons.Outlined.FileDownload,
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedNullableVisibility(
                value = tracker.message,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) { _, message ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int?, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = value?.toString() ?: "-",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Serializable
sealed class Dialog {
    @Serializable
    data object Add : Dialog()

    @Serializable
    data object Edit : Dialog()

    @Serializable
    data object Delete : Dialog()
}

@Composable
fun AddTrackersDialog(onDismiss: () -> Unit, onAdd: (urls: List<String>) -> Unit, modifier: Modifier = Modifier) {
    var urls by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_trackers_action_add)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = urls,
                onValueChange = { urls = it },
                label = {
                    Text(
                        text = stringResource(Res.string.torrent_trackers_add_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                maxLines = 10,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(urls.text.split("\n"))
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
fun DeleteSelectedTrackersDialog(count: Int, onDismiss: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = pluralStringResource(Res.plurals.torrent_trackers_delete_title, count, count)) },
        text = { Text(text = pluralStringResource(Res.plurals.torrent_trackers_delete_desc, count, count)) },
        confirmButton = {
            Button(onClick = onDelete) {
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
fun EditSelectedTrackerDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onEdit: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialUrl, TextRange(Int.MAX_VALUE)))
    }
    var error by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_trackers_action_edit)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = {
                    Text(
                        text = stringResource(Res.string.torrent_trackers_edit_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                isError = error != null,
                supportingText = error?.let { { Text(text = stringResource(it)) } },
                trailingIcon = error?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (url.text.isNotBlank()) {
                            onEdit(url.text)
                        } else {
                            error = Res.string.torrent_trackers_url_cannot_be_empty
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.text.isNotBlank()) {
                        onEdit(url.text)
                    } else {
                        error = Res.string.torrent_trackers_url_cannot_be_empty
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
