package dev.bartuzen.qbitcontroller.ui.torrent.tabs.base

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.flow.collect

@Composable
fun TorrentTabContent(
    viewModel: TorrentTabViewModel,
    updateData: suspend () -> Unit,
    modifier: Modifier = Modifier,
    onError: (error: RequestResult) -> Unit,
    addEmptySpaceIfNotLoading: Boolean = true,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    content: @Composable () -> Unit
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TorrentTabViewModel.TorrentEvent.ShowError -> {
                    onError(event.message)
                }
            }
        }
    }

    LaunchedEffect(true) {
        updateData()
        viewModel.isLoading = false
    }

    LaunchedEffect(viewModel.isRefreshing) {
        if (viewModel.isRefreshing) {
            updateData()
            viewModel.isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(viewModel.isRefreshing),
            onRefresh = {
                viewModel.isRefreshing = true
            },
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth()
                )
            } else if (addEmptySpaceIfNotLoading) {
                Spacer(
                    modifier = Modifier
                        .height(4.dp)
                        .fillMaxWidth()
                )
            }
            content()
        }
    }
}