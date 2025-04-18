package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.PeerFlag
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.AnimatedListVisibility
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.getCountryName
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class TorrentPeersFragment() : Fragment() {
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

                    TorrentPeersTab(
                        fragment = this@TorrentPeersFragment,
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = currentLifecycle == Lifecycle.State.RESUMED,
                    )
                }
            }
        }
}

@Composable
private fun TorrentPeersTab(
    fragment: TorrentPeersFragment,
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    modifier: Modifier = Modifier,
    viewModel: TorrentPeersViewModel = hiltViewModel(
        creationCallback = { factory: TorrentPeersViewModel.Factory ->
            factory.create(serverId, torrentHash)
        },
    ),
) {
    val activity = fragment.requireActivity()
    val context = LocalContext.current

    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val selectedPeers = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var actionMode by remember { mutableStateOf<ActionMode?>(null) }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    LaunchedEffect(Unit) {
        activity.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_peers, menu)
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

    LaunchedEffect(selectedPeers.isNotEmpty()) {
        if (selectedPeers.isNotEmpty()) {
            actionMode = activity.startActionMode(
                object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_peers_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_ban_peers -> {
                                currentDialog = Dialog.Ban
                            }
                            R.id.menu_select_all -> {
                                val newPeers = peers
                                    ?.filter { it.address !in selectedPeers }
                                    ?.map { it.address }
                                    ?: return false
                                selectedPeers.addAll(newPeers)
                            }
                            R.id.menu_select_inverse -> {
                                val newPeers = peers
                                    ?.filter { it.address !in selectedPeers }
                                    ?.map { it.address }
                                    ?: return false
                                selectedPeers.clear()
                                selectedPeers.addAll(newPeers)
                            }
                            else -> return false
                        }

                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        actionMode = null
                        selectedPeers.clear()
                    }
                },
            )
        } else {
            actionMode?.finish()
        }
    }

    LaunchedEffect(selectedPeers.size) {
        if (selectedPeers.isNotEmpty()) {
            actionMode?.title = context.resources.getQuantityString(
                R.plurals.torrent_peers_selected,
                selectedPeers.size,
                selectedPeers.size,
            )
        }
    }

    LaunchedEffect(peers) {
        selectedPeers.removeAll { peerKey -> peers?.none { it.address == peerKey } != false }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
        if (!isScreenActive) {
            actionMode?.finish()
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentPeersViewModel.Event.Error -> {
                fragment.showSnackbar(getErrorMessage(context, event.error), view = activity.view)
            }
            TorrentPeersViewModel.Event.TorrentNotFound -> {
                fragment.showSnackbar(R.string.torrent_error_not_found, view = activity.view)
            }
            TorrentPeersViewModel.Event.PeersInvalid -> {
                fragment.showSnackbar(R.string.torrent_peers_invalid, view = activity.view)
            }
            TorrentPeersViewModel.Event.PeersBanned -> {
                fragment.showSnackbar(R.string.torrent_peers_banned, view = activity.view)
            }
            TorrentPeersViewModel.Event.PeersAdded -> {
                fragment.showSnackbar(R.string.torrent_peers_added, view = activity.view)
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
                    actionMode?.finish()
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshPeers() },
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
                items = peers ?: emptyList(),
                key = { it.address },
            ) { peer ->
                PeerItem(
                    peer = peer,
                    selected = peer.address in selectedPeers,
                    flagUrl = peer.countryCode?.let { viewModel.getFlagUrl(it) },
                    imageLoader = viewModel.imageLoader,
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

@Composable
private fun PeerItem(
    peer: TorrentPeer,
    selected: Boolean,
    flagUrl: String?,
    imageLoader: ImageLoader,
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
                    AsyncImage(
                        model = flagUrl,
                        contentDescription = null,
                        imageLoader = imageLoader,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.torrent_peers_ip_format, peer.ip, peer.port),
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
                    label = stringResource(R.string.torrent_peers_details_progress),
                    value = stringResource(R.string.percentage_format, progressText),
                    icon = Icons.Outlined.Downloading,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(R.string.torrent_peers_details_download_speed),
                    value = formatBytesPerSecond(peer.downloadSpeed.toLong()),
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(R.string.torrent_peers_details_upload_speed),
                    value = formatBytesPerSecond(peer.uploadSpeed.toLong()),
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )

                StatItem(
                    label = stringResource(R.string.torrent_peers_details_connection),
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
        title = { Text(text = stringResource(R.string.torrent_peers_action_add)) },
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
                        text = stringResource(R.string.torrent_peers_add_hint),
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
                    onAdd(text.text.split("\n"))
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
private fun BanPeersDialog(count: Int, onDismiss: () -> Unit, onBan: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = pluralStringResource(R.plurals.torrent_peers_ban_title, count, count)) },
        text = { Text(text = pluralStringResource(R.plurals.torrent_peers_ban_desc, count, count)) },
        confirmButton = {
            TextButton(onClick = onBan) {
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
private fun PeerDetailsDialog(peer: TorrentPeer, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.torrent_peers_ip_format, peer.ip, peer.port)) },
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

                    DetailsSectionHeader(title = stringResource(R.string.torrent_peers_details_section_overview))

                    if (peer.countryCode != null) {
                        DetailRow(
                            label = stringResource(R.string.torrent_peers_details_country),
                            value = getCountryName(peer.countryCode),
                        )
                    }

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_connection),
                        value = peer.connection,
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_client),
                        value = peer.client,
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_peer_id_client),
                        value = peer.peerIdClient,
                    )

                    DetailsSectionHeader(
                        title = stringResource(R.string.torrent_peers_details_section_transfer),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_progress),
                        value = stringResource(R.string.percentage_format, progress),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_download_speed),
                        value = formatBytesPerSecond(peer.downloadSpeed.toLong()),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_upload_speed),
                        value = formatBytesPerSecond(peer.uploadSpeed.toLong()),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_downloaded),
                        value = formatBytes(peer.downloaded),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_uploaded),
                        value = formatBytes(peer.uploaded),
                    )

                    DetailRow(
                        label = stringResource(R.string.torrent_peers_details_relevance),
                        value = stringResource(R.string.percentage_format, relevance),
                    )
                }

                AnimatedListVisibility(
                    items = peer.flags,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) { flags ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        DetailsSectionHeader(
                            title = stringResource(R.string.torrent_peers_details_section_flags),
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
                                            R.string.torrent_peers_flag_interested_local_choked_peer
                                        PeerFlag.INTERESTED_LOCAL_UNCHOKED_PEER ->
                                            R.string.torrent_peers_flag_interested_local_unchoked_peer
                                        PeerFlag.INTERESTED_PEER_CHOKED_LOCAL ->
                                            R.string.torrent_peers_flag_interested_peer_choked_local
                                        PeerFlag.INTERESTED_PEER_UNCHOKED_LOCAL ->
                                            R.string.torrent_peers_flag_interested_peer_unchoked_local
                                        PeerFlag.NOT_INTERESTED_LOCAL_UNCHOKED_PEER ->
                                            R.string.torrent_peers_flag_not_interested_local_unchoked_peer
                                        PeerFlag.NOT_INTERESTED_PEER_UNCHOKED_LOCAL ->
                                            R.string.torrent_peers_flag_not_interested_peer_unchoked_local
                                        PeerFlag.OPTIMISTIC_UNCHOKE -> R.string.torrent_peers_flag_optimistic_unchoke
                                        PeerFlag.PEER_SNUBBED -> R.string.torrent_peers_flag_peer_snubbed
                                        PeerFlag.INCOMING_CONNECTION -> R.string.torrent_peers_flag_incoming_connection
                                        PeerFlag.PEER_FROM_DHT -> R.string.torrent_peers_flag_peer_from_dht
                                        PeerFlag.PEER_FROM_PEX -> R.string.torrent_peers_flag_peer_from_pex
                                        PeerFlag.PEER_FROM_LSD -> R.string.torrent_peers_flag_peer_from_lsd
                                        PeerFlag.ENCRYPTED_TRAFFIC -> R.string.torrent_peers_flag_encrypted_traffic
                                        PeerFlag.ENCRYPTED_HANDSHAKE -> R.string.torrent_peers_flag_encrypted_handshake
                                        PeerFlag.UTP -> R.string.torrent_peers_flag_utp
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
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        DetailsSectionHeader(
                            title = stringResource(R.string.torrent_peers_details_section_files),
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
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_ok))
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
