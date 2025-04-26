package dev.bartuzen.qbitcontroller.ui.torrent

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers.TorrentPeersTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds.TorrentWebSeedsTab
import dev.bartuzen.qbitcontroller.utils.AnimatedNullableVisibility
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val TORRENT_NAME = "dev.bartuzen.qbitcontroller.TORRENT_NAME"

        const val TORRENT_DELETED = "dev.bartuzen.qbitcontroller.TORRENT_DELETED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val torrentName = intent.getStringExtra(Extras.TORRENT_NAME)

        if (serverId == -1 || torrentHash == null) {
            finish()
            return
        }

        setContent {
            AppTheme {
                TorrentScreen(
                    serverId = serverId,
                    torrentHash = torrentHash,
                    torrentName = torrentName,
                    onNavigateBack = { finish() },
                    onDeleteTorrent = {
                        val intent = Intent().apply {
                            putExtra(Extras.TORRENT_DELETED, true)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TorrentScreen(
    serverId: Int,
    torrentHash: String,
    torrentName: String?,
    onNavigateBack: () -> Unit,
    onDeleteTorrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val snackbarEventFlow = remember { MutableSharedFlow<String>() }
    val titleEventFlow = remember { MutableSharedFlow<String>() }
    val actionsEventFlow = remember { MutableSharedFlow<Pair<Int, List<ActionMenuItem>>>() }
    val selectionActionsEventFlow = remember { MutableSharedFlow<Pair<Int, SelectionData?>>() }

    val tabTitles = listOf(
        stringResource(R.string.torrent_tab_overview),
        stringResource(R.string.torrent_tab_files),
        stringResource(R.string.torrent_tab_trackers),
        stringResource(R.string.torrent_tab_peers),
        stringResource(R.string.torrent_tab_web_seeds),
    )

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })

    var selectionActionMenuItems = remember { mutableStateMapOf<Int, SelectionData>() }
    val currentSelectionActionMenuItems = selectionActionMenuItems[pagerState.currentPage]

    BackHandler(enabled = currentSelectionActionMenuItems != null) {
        currentSelectionActionMenuItems?.onCancel()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        topBar = {
            var title by remember { mutableStateOf("") }
            var actionMenuItems = remember { mutableStateMapOf<Int, List<ActionMenuItem>>() }

            LaunchedEffect(Unit) {
                titleEventFlow.collectLatest {
                    title = it
                }
            }

            LaunchedEffect(Unit) {
                actionsEventFlow.collectLatest { (index, items) ->
                    actionMenuItems[index] = items
                }
            }

            LaunchedEffect(Unit) {
                selectionActionsEventFlow.collect { (index, selectionData) ->
                    if (selectionData != null) {
                        selectionActionMenuItems[index] = selectionData
                    } else {
                        selectionActionMenuItems -= index
                    }
                }
            }

            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    val items = if (pagerState.currentPage != 0) {
                        actionMenuItems[pagerState.currentPage].orEmpty()
                    } else {
                        emptyList()
                    } + actionMenuItems[0].orEmpty()
                    AppBarActions(items = items)
                },
            )

            AnimatedNullableVisibility(
                value = currentSelectionActionMenuItems,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) { _, (title, actionMenuItems, onCancel) ->
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        AppBarActions(items = actionMenuItems)
                    },
                )
            }
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        },
    ) { innerPadding ->
        val minTabWidth = if (LocalConfiguration.current.screenWidthDp < 600) 72.dp else 160.dp
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(IntrinsicSize.Min),
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.sizeIn(minWidth = minTabWidth),
                    )
                }
            }

            HorizontalDivider()

            LaunchedEffect(Unit) {
                snackbarEventFlow.collectLatest {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(it)
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = tabTitles.size - 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> TorrentOverviewTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        initialTorrentName = torrentName,
                        isScreenActive = pagerState.currentPage == 0,
                        snackbarEventFlow = snackbarEventFlow,
                        titleEventFlow = titleEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        onDeleteTorrent = onDeleteTorrent,
                    )
                    1 -> TorrentFilesTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = pagerState.currentPage == 1,
                        snackbarEventFlow = snackbarEventFlow,
                        selectionActionsEventFlow = selectionActionsEventFlow,
                    )
                    2 -> TorrentTrackersTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = pagerState.currentPage == 2,
                        snackbarEventFlow = snackbarEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        selectionActionsEventFlow = selectionActionsEventFlow,
                    )
                    3 -> TorrentPeersTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = pagerState.currentPage == 3,
                        snackbarEventFlow = snackbarEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        selectionActionsEventFlow = selectionActionsEventFlow,
                    )
                    4 -> TorrentWebSeedsTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = pagerState.currentPage == 4,
                        snackbarEventFlow = snackbarEventFlow,
                    )
                }
            }
        }
    }
}

data class SelectionData(
    val title: String,
    val actionMenuItems: List<ActionMenuItem>,
    val onCancel: () -> Unit,
)
