package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Speed
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
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.bartuzen.qbitcontroller.model.PeerFlag
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.network.ImageLoaderProvider
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.utils.AnimatedListVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.getCountryName
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.action_select_all
import qbitcontroller.composeapp.generated.resources.action_select_inverse
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.percentage_format
import qbitcontroller.composeapp.generated.resources.torrent_error_not_found
import qbitcontroller.composeapp.generated.resources.torrent_peers_action_add
import qbitcontroller.composeapp.generated.resources.torrent_peers_action_ban
import qbitcontroller.composeapp.generated.resources.torrent_peers_add_hint
import qbitcontroller.composeapp.generated.resources.torrent_peers_added
import qbitcontroller.composeapp.generated.resources.torrent_peers_ban_desc
import qbitcontroller.composeapp.generated.resources.torrent_peers_ban_title
import qbitcontroller.composeapp.generated.resources.torrent_peers_banned
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_client
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_connection
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_country
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_download_speed
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_downloaded
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_peer_id_client
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_progress
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_relevance
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_section_files
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_section_flags
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_section_overview
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_section_transfer
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_upload_speed
import qbitcontroller.composeapp.generated.resources.torrent_peers_details_uploaded
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_encrypted_handshake
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_encrypted_traffic
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_incoming_connection
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_interested_local_choked_peer
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_interested_local_unchoked_peer
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_interested_peer_choked_local
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_interested_peer_unchoked_local
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_not_interested_local_unchoked_peer
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_not_interested_peer_unchoked_local
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_optimistic_unchoke
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_peer_from_dht
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_peer_from_lsd
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_peer_from_pex
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_peer_snubbed
import qbitcontroller.composeapp.generated.resources.torrent_peers_flag_utp
import qbitcontroller.composeapp.generated.resources.torrent_peers_invalid
import qbitcontroller.composeapp.generated.resources.torrent_peers_ip_format
import qbitcontroller.composeapp.generated.resources.torrent_peers_selected

@Composable
fun TorrentPeersTab(
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    snackbarEventFlow: MutableSharedFlow<String>,
    actionsEventFlow: MutableSharedFlow<Pair<Int, List<ActionMenuItem>>>,
    bottomBarStateEventFlow: MutableSharedFlow<Triple<Int, Dp, Boolean>>,
    modifier: Modifier = Modifier,
    viewModel: TorrentPeersViewModel = koinViewModel(parameters = { parametersOf(serverId, torrentHash) }),
) {
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val selectedPeers = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    val actions = listOf(
        ActionMenuItem(
            title = stringResource(Res.string.torrent_peers_action_add),
            icon = Icons.Filled.Add,
            onClick = { currentDialog = Dialog.Add },
            showAsAction = true,
        ),
    )

    LaunchedEffect(actions) {
        launch {
            actionsEventFlow.emit(3 to actions)
        }
    }

    LaunchedEffect(peers) {
        selectedPeers.removeAll { peerKey -> peers?.none { it.address == peerKey } != false }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    BackHandler(enabled = isScreenActive && selectedPeers.isNotEmpty()) {
        selectedPeers.clear()
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentPeersViewModel.Event.Error -> {
                snackbarEventFlow.emit(getErrorMessage(event.error))
            }
            TorrentPeersViewModel.Event.TorrentNotFound -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_error_not_found))
            }
            TorrentPeersViewModel.Event.PeersInvalid -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_peers_invalid))
            }
            TorrentPeersViewModel.Event.PeersBanned -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_peers_banned))
            }
            TorrentPeersViewModel.Event.PeersAdded -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_peers_added))
            }
        }
    }

    when (val dialog = currentDialog) {
        Dialog.Add -> {
            AddPeersDialog(
                onDismiss = { currentDialog = null },
                onAdd = { peersList ->
                    viewModel.addPeers(peersList)
                    currentDialog = null
                },
            )
        }
        is Dialog.Ban -> {
            LaunchedEffect(selectedPeers.isEmpty()) {
                if (selectedPeers.isEmpty()) {
                    currentDialog = null
                }
            }

            BanPeersDialog(
                count = selectedPeers.size,
                onDismiss = { currentDialog = null },
                onBan = {
                    viewModel.banPeers(selectedPeers.toList())
                    currentDialog = null
                    selectedPeers.clear()
                },
            )
        }
        is Dialog.Details -> {
            val peer = remember(peers, dialog.address) { peers?.find { it.address == dialog.address } }

            LaunchedEffect(peer == null) {
                if (peer == null) {
                    currentDialog = null
                }
            }

            if (peer != null) {
                PeerDetailsDialog(
                    peer = peer,
                    onDismiss = { currentDialog = null },
                )
            }
        }
        null -> {}
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            var bottomBarHeight by remember { mutableStateOf(0.dp) }
            val visibleState = remember { MutableTransitionState(selectedPeers.isNotEmpty()) }

            LaunchedEffect(selectedPeers.isNotEmpty()) {
                visibleState.targetState = selectedPeers.isNotEmpty()
            }

            LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
                if (visibleState.isIdle && !visibleState.currentState) {
                    bottomBarHeight = 0.dp
                }
            }

            LaunchedEffect(bottomBarHeight, visibleState.isIdle) {
                bottomBarStateEventFlow.emit(
                    Triple(3, bottomBarHeight, !visibleState.isIdle),
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

                        LaunchedEffect(selectedPeers.size) {
                            if (selectedPeers.isNotEmpty()) {
                                selectedSize = selectedPeers.size
                            }
                        }

                        Text(
                            text = pluralStringResource(
                                Res.plurals.torrent_peers_selected,
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
                        IconButton(onClick = { selectedPeers.clear() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        val actionMenuItems = listOf(
                            ActionMenuItem(
                                title = stringResource(Res.string.torrent_peers_action_ban),
                                icon = Icons.Filled.DoNotDisturbAlt,
                                onClick = { currentDialog = Dialog.Ban },
                                showAsAction = true,
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_all),
                                icon = Icons.Filled.SelectAll,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newPeers = peers
                                        ?.filter { it.address !in selectedPeers }
                                        ?.map { it.address }
                                        ?: return@onClick
                                    selectedPeers.addAll(newPeers)
                                },
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_inverse),
                                icon = Icons.Filled.FlipToBack,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newPeers = peers
                                        ?.filter { it.address !in selectedPeers }
                                        ?.map { it.address }
                                        ?: return@onClick
                                    selectedPeers.clear()
                                    selectedPeers.addAll(newPeers)
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
            onRefresh = { viewModel.refreshPeers() },
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
                    items = peers ?: emptyList(),
                    key = { it.address },
                ) { peer ->
                    PeerItem(
                        peer = peer,
                        serverId = serverId,
                        selected = peer.address in selectedPeers,
                        flagUrl = peer.countryCode?.let { viewModel.getFlagUrl(it) },
                        onClick = {
                            if (selectedPeers.isNotEmpty()) {
                                if (peer.address !in selectedPeers) {
                                    selectedPeers += peer.address
                                } else {
                                    selectedPeers -= peer.address
                                }
                            } else {
                                currentDialog = Dialog.Details(peer.address)
                            }
                        },
                        onLongClick = {
                            if (peer.address !in selectedPeers) {
                                selectedPeers += peer.address
                            } else {
                                selectedPeers -= peer.address
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
private fun PeerItem(
    peer: TorrentPeer,
    serverId: Int,
    selected: Boolean,
    flagUrl: String?,
    onClick: (() -> Unit),
    onLongClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) {
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
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (flagUrl != null) {
                    val imageLoaderProvider = koinInject<ImageLoaderProvider>()
                    AsyncImage(
                        model = flagUrl,
                        contentDescription = null,
                        imageLoader = imageLoaderProvider.getImageLoader(serverId),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                    )
                }

                Text(
                    text = stringResource(Res.string.torrent_peers_ip_format, peer.ip, peer.port),
                    style = MaterialTheme.typography.titleMedium,
                )

                if (peer.client != null) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = peer.client,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val progressText = if (peer.progress < 1) {
                    (peer.progress * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }

                StatItem(
                    label = stringResource(Res.string.torrent_peers_details_progress),
                    value = stringResource(Res.string.percentage_format, progressText),
                    icon = Icons.Outlined.Downloading,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(Res.string.torrent_peers_details_download_speed),
                    value = formatBytesPerSecond(peer.downloadSpeed.toLong()),
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(Res.string.torrent_peers_details_upload_speed),
                    value = formatBytesPerSecond(peer.uploadSpeed.toLong()),
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(Res.string.torrent_peers_details_connection),
                    value = peer.connection,
                    icon = Icons.Outlined.Speed,
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedListVisibility(
                items = peer.flags,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) { flags ->
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
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = flags.joinToString(" ") { it.flag },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
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
            text = value,
            style = MaterialTheme.typography.titleSmall,
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
    data class Details(val address: String) : Dialog()

    @Serializable
    data object Ban : Dialog()
}

@Composable
private fun AddPeersDialog(onDismiss: () -> Unit, onAdd: (peers: List<String>) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_peers_action_add)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(
                        text = stringResource(Res.string.torrent_peers_add_hint),
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
                    onAdd(text.text.split("\n"))
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
private fun BanPeersDialog(count: Int, onDismiss: () -> Unit, onBan: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = pluralStringResource(Res.plurals.torrent_peers_ban_title, count, count)) },
        text = { Text(text = pluralStringResource(Res.plurals.torrent_peers_ban_desc, count, count)) },
        confirmButton = {
            Button(onClick = onBan) {
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
private fun PeerDetailsDialog(peer: TorrentPeer, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.torrent_peers_ip_format, peer.ip, peer.port)) },
        text = {
            Column {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val progress = if (peer.progress < 1) {
                        (peer.progress * 100).floorToDecimal(1).toString()
                    } else {
                        "100"
                    }

                    val relevance = if (peer.relevance < 1) {
                        (peer.relevance * 100).floorToDecimal(1).toString()
                    } else {
                        "100"
                    }

                    DetailsSectionHeader(title = stringResource(Res.string.torrent_peers_details_section_overview))

                    if (peer.countryCode != null) {
                        DetailRow(
                            label = stringResource(Res.string.torrent_peers_details_country),
                            value = getCountryName(peer.countryCode),
                        )
                    }

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_connection),
                        value = peer.connection,
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_client),
                        value = peer.client,
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_peer_id_client),
                        value = peer.peerIdClient,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DetailsSectionHeader(
                        title = stringResource(Res.string.torrent_peers_details_section_transfer),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_progress),
                        value = stringResource(Res.string.percentage_format, progress),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_download_speed),
                        value = formatBytesPerSecond(peer.downloadSpeed.toLong()),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_upload_speed),
                        value = formatBytesPerSecond(peer.uploadSpeed.toLong()),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_downloaded),
                        value = formatBytes(peer.downloaded),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_uploaded),
                        value = formatBytes(peer.uploaded),
                    )

                    DetailRow(
                        label = stringResource(Res.string.torrent_peers_details_relevance),
                        value = stringResource(Res.string.percentage_format, relevance),
                    )
                }

                AnimatedListVisibility(
                    items = peer.flags,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) { flags ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        DetailsSectionHeader(
                            title = stringResource(Res.string.torrent_peers_details_section_flags),
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                flags.forEach { flag ->
                                    val resId = when (flag) {
                                        PeerFlag.INTERESTED_LOCAL_CHOKED_PEER ->
                                            Res.string.torrent_peers_flag_interested_local_choked_peer
                                        PeerFlag.INTERESTED_LOCAL_UNCHOKED_PEER ->
                                            Res.string.torrent_peers_flag_interested_local_unchoked_peer
                                        PeerFlag.INTERESTED_PEER_CHOKED_LOCAL ->
                                            Res.string.torrent_peers_flag_interested_peer_choked_local
                                        PeerFlag.INTERESTED_PEER_UNCHOKED_LOCAL ->
                                            Res.string.torrent_peers_flag_interested_peer_unchoked_local
                                        PeerFlag.NOT_INTERESTED_LOCAL_UNCHOKED_PEER ->
                                            Res.string.torrent_peers_flag_not_interested_local_unchoked_peer
                                        PeerFlag.NOT_INTERESTED_PEER_UNCHOKED_LOCAL ->
                                            Res.string.torrent_peers_flag_not_interested_peer_unchoked_local
                                        PeerFlag.OPTIMISTIC_UNCHOKE -> Res.string.torrent_peers_flag_optimistic_unchoke
                                        PeerFlag.PEER_SNUBBED -> Res.string.torrent_peers_flag_peer_snubbed
                                        PeerFlag.INCOMING_CONNECTION -> Res.string.torrent_peers_flag_incoming_connection
                                        PeerFlag.PEER_FROM_DHT -> Res.string.torrent_peers_flag_peer_from_dht
                                        PeerFlag.PEER_FROM_PEX -> Res.string.torrent_peers_flag_peer_from_pex
                                        PeerFlag.PEER_FROM_LSD -> Res.string.torrent_peers_flag_peer_from_lsd
                                        PeerFlag.ENCRYPTED_TRAFFIC -> Res.string.torrent_peers_flag_encrypted_traffic
                                        PeerFlag.ENCRYPTED_HANDSHAKE -> Res.string.torrent_peers_flag_encrypted_handshake
                                        PeerFlag.UTP -> Res.string.torrent_peers_flag_utp
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = flag.flag,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = stringResource(resId),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedListVisibility(
                    items = peer.files,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) { files ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        DetailsSectionHeader(
                            title = stringResource(Res.string.torrent_peers_details_section_files),
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                files.forEach { file ->
                                    Text(
                                        text = file,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
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
private fun DetailsSectionHeader(title: String, modifier: Modifier = Modifier) {
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
private fun DetailRow(label: String, value: String?, modifier: Modifier = Modifier) {
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

        if (value != null) {
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Text(
                text = "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
