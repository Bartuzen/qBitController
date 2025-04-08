package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class TorrentTrackersFragment() : Fragment() {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash,
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

                    TorrentTrackersTab(
                        fragment = this@TorrentTrackersFragment,
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = currentLifecycle == Lifecycle.State.RESUMED,
                    )
                }
            }
        }
}

@Composable
private fun TorrentTrackersTab(
    fragment: TorrentTrackersFragment,
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    modifier: Modifier = Modifier,
    viewModel: TorrentTrackersViewModel = hiltViewModel(
        creationCallback = { factory: TorrentTrackersViewModel.Factory ->
            factory.create(serverId, torrentHash)
        },
    ),
) {
    val activity = fragment.requireActivity()
    val context = LocalContext.current

    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val selectedTrackers = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var actionMode by remember { mutableStateOf<ActionMode?>(null) }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    LaunchedEffect(Unit) {
        activity.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_trackers, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            currentDialog = Dialog.Add
                        }
                        else -> return false
                    }

                    return true
                }
            },
            fragment.viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    LaunchedEffect(selectedTrackers.isNotEmpty()) {
        if (selectedTrackers.isNotEmpty()) {
            actionMode = activity.startActionMode(
                object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_trackers_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_delete -> {
                                currentDialog = Dialog.Delete
                            }
                            R.id.menu_edit -> {
                                currentDialog = Dialog.Edit
                            }
                            R.id.menu_select_all -> {
                                val newTrackers = trackers
                                    ?.filter { it.url !in selectedTrackers && it.tier != null }
                                    ?.map { it.url }
                                    ?: return false
                                selectedTrackers.addAll(newTrackers)
                            }
                            R.id.menu_select_inverse -> {
                                val newTrackers = trackers
                                    ?.filter { it.url !in selectedTrackers && it.tier != null }
                                    ?.map { it.url }
                                    ?: return false
                                selectedTrackers.clear()
                                selectedTrackers.addAll(newTrackers)
                            }
                            else -> return false
                        }

                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        actionMode = null
                        selectedTrackers.clear()
                    }
                },
            )
        } else {
            actionMode?.finish()
        }
    }

    LaunchedEffect(selectedTrackers.size == 1) {
        actionMode?.menu?.findItem(R.id.menu_edit)?.isEnabled = selectedTrackers.size == 1
    }

    LaunchedEffect(selectedTrackers.size) {
        if (selectedTrackers.isNotEmpty()) {
            actionMode?.title = context.resources.getQuantityString(
                R.plurals.torrent_trackers_selected,
                selectedTrackers.size,
                selectedTrackers.size,
            )
        }
    }

    LaunchedEffect(trackers) {
        selectedTrackers.removeAll { url -> trackers?.none { it.url == url } != false }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
        if (!isScreenActive) {
            actionMode?.finish()
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentTrackersViewModel.Event.Error -> {
                fragment.showSnackbar(getErrorMessage(context, event.error), view = activity.view)
            }
            TorrentTrackersViewModel.Event.TorrentNotFound -> {
                fragment.showSnackbar(R.string.torrent_error_not_found, view = activity.view)
            }
            TorrentTrackersViewModel.Event.TrackersAdded -> {
                fragment.showSnackbar(R.string.torrent_trackers_added, view = activity.view)
            }
            TorrentTrackersViewModel.Event.TrackersDeleted -> {
                fragment.showSnackbar(R.string.torrent_trackers_deleted, view = activity.view)
            }
            TorrentTrackersViewModel.Event.TrackerEdited -> {
                fragment.showSnackbar(R.string.torrent_trackers_edited, view = activity.view)
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
                    actionMode?.finish()
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshTrackers() },
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
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
                    label = stringResource(R.string.torrent_trackers_peers),
                    value = tracker.peers,
                    icon = Icons.Outlined.People,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.torrent_trackers_seeds),
                    value = tracker.seeds,
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.torrent_trackers_leeches),
                    value = tracker.leeches,
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = stringResource(R.string.torrent_trackers_downloaded),
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
        title = { Text(text = stringResource(R.string.torrent_trackers_action_add)) },
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
                        text = stringResource(R.string.torrent_trackers_add_hint),
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
            TextButton(
                onClick = {
                    onAdd(urls.text.split("\n"))
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
fun DeleteSelectedTrackersDialog(count: Int, onDismiss: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = pluralStringResource(R.plurals.torrent_trackers_delete_title, count, count)) },
        text = { Text(text = pluralStringResource(R.plurals.torrent_trackers_delete_desc, count, count)) },
        confirmButton = {
            TextButton(onClick = onDelete) {
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
fun EditSelectedTrackerDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onEdit: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialUrl)) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_trackers_action_add)) },
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
                        text = stringResource(R.string.torrent_trackers_edit_hint),
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
                            error = R.string.torrent_trackers_url_cannot_be_empty
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
                    if (url.text.isNotBlank()) {
                        onEdit(url.text)
                    } else {
                        error = R.string.torrent_trackers_url_cannot_be_empty
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
