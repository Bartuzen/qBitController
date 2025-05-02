package dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun TorrentWebSeedsTab(
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    snackbarEventFlow: MutableSharedFlow<String>,
    modifier: Modifier = Modifier,
    viewModel: TorrentWebSeedsViewModel = hiltViewModel(
        creationCallback = { factory: TorrentWebSeedsViewModel.Factory ->
            factory.create(serverId, torrentHash)
        },
    ),
) {
    val context = LocalContext.current

    val webSeeds by viewModel.webSeeds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentWebSeedsViewModel.Event.Error -> {
                snackbarEventFlow.emit(getErrorMessage(context, event.error))
            }
            TorrentWebSeedsViewModel.Event.TorrentNotFound -> {
                snackbarEventFlow.emit(context.getString(R.string.torrent_error_not_found))
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshWebSeeds() },
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
                items = webSeeds ?: emptyList(),
                key = { it },
            ) { webSeed ->
                WebSeedItem(
                    webSeed = webSeed,
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
private fun WebSeedItem(webSeed: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Text(
            text = webSeed,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
