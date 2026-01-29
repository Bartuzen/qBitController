package dev.bartuzen.qbitcontroller.ui.torrent

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.bartuzen.qbitcontroller.Telemetry
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFilesTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverviewTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers.TorrentPeersTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers.TorrentTrackersTab
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds.TorrentWebSeedsTab
import dev.bartuzen.qbitcontroller.utils.calculateWindowSizeClass
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.torrent_tab_files
import qbitcontroller.composeapp.generated.resources.torrent_tab_overview
import qbitcontroller.composeapp.generated.resources.torrent_tab_peers
import qbitcontroller.composeapp.generated.resources.torrent_tab_trackers
import qbitcontroller.composeapp.generated.resources.torrent_tab_web_seeds

object TorrentKeys {
    const val ServerId = "torrent.serverId"
    const val TorrentHash = "torrent.torrentHash"
    const val TorrentName = "torrent.torrentName"

    const val TorrentDeleted = "torrent.torrentDeleted"
}

@Composable
fun TorrentScreen(
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
    val bottomBarStateEventFlow = remember { MutableSharedFlow<Triple<Int, Dp, Boolean>>() }

    val tabTitles = listOf(
        stringResource(Res.string.torrent_tab_overview),
        stringResource(Res.string.torrent_tab_files),
        stringResource(Res.string.torrent_tab_trackers),
        stringResource(Res.string.torrent_tab_peers),
        stringResource(Res.string.torrent_tab_web_seeds),
    )

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val bottomBarHeights = remember { mutableStateMapOf<Int, Pair<Dp, Boolean>>() }

    LaunchedEffect(pagerState.currentPage) {
        val currentScreenTitle = when (pagerState.currentPage) {
            0 -> "Overview"
            1 -> "Files"
            2 -> "Trackers"
            3 -> "Peers"
            4 -> "WebSeeds"
            else -> null
        }

        if (currentScreenTitle != null) {
            Telemetry.setCurrentScreen("Torrent", currentScreenTitle)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        topBar = {
            var title by remember { mutableStateOf("") }
            val actionMenuItems = remember { mutableStateMapOf<Int, List<ActionMenuItem>>() }

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
                bottomBarStateEventFlow.collectLatest { (index, height, isAnimating) ->
                    bottomBarHeights[index] = height to isAnimating
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
        },
        snackbarHost = {
            val isAnimating = bottomBarHeights[pagerState.currentPage]?.second == true
            val widthSizeClass = calculateWindowSizeClass().widthSizeClass

            val bottomPadding = max(
                if (widthSizeClass != WindowWidthSizeClass.Compact) {
                    WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
                } else {
                    0.dp
                },
                (bottomBarHeights[pagerState.currentPage]?.first ?: 0.dp),
            )
            val bottomPaddingAnimated by animateDpAsState(
                targetValue = bottomPadding,
                animationSpec = if (isAnimating) snap() else tween(),
            )

            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomPaddingAnimated),
            )
        },
    ) { innerPadding ->
        val density = LocalDensity.current
        val containerWidth = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
        val minTabWidth = if (containerWidth < 600.dp) 72.dp else 160.dp
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(IntrinsicSize.Max),
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

            val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            var isScreenActive by remember(lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                mutableStateOf(lifecycleState.isAtLeast(Lifecycle.State.STARTED))
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
                        isScreenActive = isScreenActive && pagerState.currentPage == 0,
                        snackbarEventFlow = snackbarEventFlow,
                        titleEventFlow = titleEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        onDeleteTorrent = onDeleteTorrent,
                    )
                    1 -> TorrentFilesTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = isScreenActive && pagerState.currentPage == 1,
                        snackbarEventFlow = snackbarEventFlow,
                        bottomBarStateEventFlow = bottomBarStateEventFlow,
                    )
                    2 -> TorrentTrackersTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = isScreenActive && pagerState.currentPage == 2,
                        snackbarEventFlow = snackbarEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        bottomBarStateEventFlow = bottomBarStateEventFlow,
                    )
                    3 -> TorrentPeersTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = isScreenActive && pagerState.currentPage == 3,
                        snackbarEventFlow = snackbarEventFlow,
                        actionsEventFlow = actionsEventFlow,
                        bottomBarStateEventFlow = bottomBarStateEventFlow,
                    )
                    4 -> TorrentWebSeedsTab(
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = isScreenActive && pagerState.currentPage == 4,
                        snackbarEventFlow = snackbarEventFlow,
                    )
                }
            }
        }
    }
}
