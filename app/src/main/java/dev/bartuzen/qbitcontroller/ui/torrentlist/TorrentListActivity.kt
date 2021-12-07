package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.ui.settings.SettingsActivity
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.theme.selectedServerBackground
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@ExperimentalFoundationApi
@AndroidEntryPoint
class TorrentListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: TorrentListViewModel = hiltViewModel()
            val theme by viewModel.themeFlow.collectAsState(Theme.SYSTEM_DEFAULT)
            val isDarkTheme = when (theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }

            AppTheme(darkTheme = isDarkTheme) {
                TorrentListContent(
                    modifier = Modifier.fillMaxSize(),
                    activity = this
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun TorrentListContent(
    activity: Activity,
    modifier: Modifier = Modifier,
    viewModel: TorrentListViewModel = hiltViewModel()
) {
    val servers by viewModel.serversFlow.collectAsState(emptyMap())
    val torrentSort by viewModel.sortFlow.collectAsState(null)
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        drawerContent = {
            TorrentListDrawer(
                servers = servers.values.toList(),
                scaffoldState = scaffoldState
            )
        }
    ) {
        CollapsingToolbarScaffold(
            modifier = modifier,
            state = rememberCollapsingToolbarScaffoldState(),
            scrollStrategy = ScrollStrategy.EnterAlways,
            toolbar = {
                TorrentListAppBar(
                    title = viewModel.currentServer?.name ?: stringResource(R.string.app_name),
                    scaffoldState = scaffoldState,
                    selectedTorrentSort = torrentSort,
                    activity = activity
                )
            }
        ) {
            LaunchedEffect(viewModel.currentServer) {
                viewModel.isLoading = true
                viewModel.updateTorrentList().invokeOnCompletion {
                    viewModel.isLoading = false
                }
            }

            LaunchedEffect(torrentSort) {
                if (torrentSort != null) {
                    viewModel.updateTorrentList()
                }
            }

            LaunchedEffect(true) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is TorrentListViewModel.TorrentListEvent.ShowError -> {
                            scaffoldState.snackbarHostState.showSnackbar(
                                context.getErrorMessage(event.message)
                            )
                        }
                    }
                }
            }

            LaunchedEffect(viewModel.isRefreshing) {
                if (viewModel.isRefreshing) {
                    viewModel.updateTorrentList().invokeOnCompletion {
                        viewModel.isRefreshing = false
                    }
                }
            }

            if (viewModel.currentServer != null) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(viewModel.isRefreshing),
                    onRefresh = {
                        viewModel.isRefreshing = true
                    }
                ) {
                    if (viewModel.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .height(4.dp)
                                .fillMaxWidth()
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .height(4.dp)
                                .fillMaxWidth()
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        viewModel.torrentList.forEach { torrent ->
                            item(
                                key = torrent.hash
                            ) {
                                TorrentCard(
                                    modifier = Modifier.animateItemPlacement(),
                                    torrent = torrent,
                                    onTorrentClick = { torrent ->
                                        val intent =
                                            Intent(activity, TorrentActivity::class.java).apply {
                                                putExtra(
                                                    TorrentActivity.Extras.TORRENT_HASH,
                                                    torrent.hash
                                                )
                                                putExtra(
                                                    TorrentActivity.Extras.SERVER_CONFIG,
                                                    viewModel.currentServer
                                                )
                                            }
                                        activity.startActivity(intent)
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentListAppBar(
    title: String,
    selectedTorrentSort: TorrentSort?,
    scaffoldState: ScaffoldState,
    activity: Activity,
    viewModel: TorrentListViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    scaffoldState.drawerState.open()
                }
            }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = null)
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            var sortExpanded by remember { mutableStateOf(false) }

            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    sortExpanded = true
                }) {
                    Text(stringResource(R.string.menu_sort))
                }
                DropdownMenuItem(onClick = {
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                    expanded = false
                }) {
                    Text(stringResource(R.string.menu_settings))
                }
            }

            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                listOf(
                    Pair(TorrentSort.NAME, stringResource(R.string.torrent_sort_name)),
                    Pair(TorrentSort.HASH, stringResource(R.string.torrent_sort_hash)),
                    Pair(TorrentSort.DOWNLOAD_SPEED, stringResource(R.string.torrent_sort_dlspeed)),
                    Pair(TorrentSort.UPLOAD_SPEED, stringResource(R.string.torrent_sort_upspeed)),
                    Pair(TorrentSort.PRIORITY, stringResource(R.string.torrent_sort_priority)),
                ).forEach { (torrentSort, text) ->
                    DropdownMenuItem(onClick = {
                        viewModel.setTorrentSort(torrentSort)
                        sortExpanded = false
                    }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTorrentSort == torrentSort,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TorrentListDrawer(
    servers: List<ServerConfig>,
    scaffoldState: ScaffoldState,
    modifier: Modifier = Modifier,
    viewModel: TorrentListViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
    ) {
        itemsIndexed(servers) { index, server ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.currentServer = server
                        viewModel.torrentList = emptyList()
                        scope.launch {
                            scaffoldState.drawerState.close()
                        }
                    }
                    .background(
                        if (viewModel.currentServer?.id == server.id) {
                            MaterialTheme.colors.selectedServerBackground
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(16.dp),
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = server.host,
                    style = MaterialTheme.typography.caption
                )
            }
            if (servers.size != index + 1) {
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TorrentCard(
    torrent: Torrent,
    onTorrentClick: (torrent: Torrent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp)
            .clickable {
                onTorrentClick(torrent)
            }
    ) {
        Column {
            Text(
                text = torrent.name,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 4.dp,
                        top = 8.dp
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val progress = (torrent.progress * 100).toInt()
                val progressText = stringResource(
                    R.string.torrent_item_progress,
                    formatByte(torrent.completed),
                    formatByte(torrent.size),
                    if (progress != 100) progress.toString() else "100"
                )

                Text(text = progressText)

                val time = formatTime(torrent.eta)
                if (time != "inf") {
                    Text(text = time)
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                progress = torrent.progress.toFloat(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatState(torrent.state))

                val speedList = mutableListOf<String>()

                if (torrent.uploadSpeed > 0) {
                    speedList.add("↑ ${formatBytePerSecond(torrent.uploadSpeed)}")
                }
                if (torrent.downloadSpeed > 0) {
                    speedList.add("↓ ${formatBytePerSecond(torrent.downloadSpeed)}")
                }
                Text(text = speedList.joinToString(" "))
            }
        }
    }

}