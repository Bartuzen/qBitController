package dev.bartuzen.qbitcontroller.ui.torrent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.files.TorrentFiles
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.TorrentOverview
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces.TorrentPieces
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@AndroidEntryPoint
class TorrentActivity : ComponentActivity() {
    object Extras {
        const val TORRENT_HASH = "dev.bartuzen.qbitcontroller.TORRENT_HASH"
        const val SERVER_CONFIG = "dev.bartuzen.qbitcontroller.SERVER_CONFIG"
    }

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val torrentHash = intent.getStringExtra(Extras.TORRENT_HASH)
        val serverConfig = intent.getParcelableExtra<ServerConfig>(Extras.SERVER_CONFIG)

        if (serverConfig == null || torrentHash == null) {
            finish()
            showToast(R.string.an_error_occurred)
            return
        }

        setContent {
            val viewModel: TorrentViewModel = hiltViewModel()
            val theme by viewModel.themeFlow.collectAsState(Theme.SYSTEM_DEFAULT)
            val isDarkTheme = when (theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }

            LaunchedEffect(true) {
                viewModel.torrentHash = torrentHash
                viewModel.serverConfig = serverConfig
            }

            AppTheme(isDarkTheme) {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val collapsingToolbarState = rememberCollapsingToolbarScaffoldState()
                val scaffoldState = rememberScaffoldState()
                var snackbarMessage by remember { mutableStateOf("") }

                LaunchedEffect(true) {
                    viewModel.customEventFlow.collectLatest { event ->
                        when (event) {
                            is TorrentViewModel.TorrentActivityEvent.ShowError -> {
                                val message = when (event.message) {
                                    is Int -> context.getString(event.message)
                                    is RequestResult -> context.getErrorMessage(event.message)
                                    else -> event.message.toString()
                                }

                                scaffoldState.snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }

                LaunchedEffect(snackbarMessage) {
                    if (snackbarMessage.isNotBlank()) {
                        scaffoldState.snackbarHostState.showSnackbar(snackbarMessage)
                        snackbarMessage = ""
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    scaffoldState = scaffoldState
                ) {
                    CollapsingToolbarScaffold(
                        modifier = Modifier.fillMaxSize(),
                        state = collapsingToolbarState,
                        scrollStrategy = ScrollStrategy.EnterAlways,
                        toolbar = {
                            TopAppBar(
                                title = { Text(serverConfig.name) },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { finish() }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                                    }
                                },
                                actions = {
                                    viewModel.torrent?.let { torrent ->
                                        if (torrent.state == TorrentState.PAUSED_DL ||
                                            torrent.state == TorrentState.PAUSED_UP
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.resumeTorrent() }
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = stringResource(R.string.menu_resume)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { viewModel.pauseTorrent() }
                                            ) {
                                                Icon(
                                                    Icons.Default.Pause,
                                                    contentDescription = stringResource(R.string.menu_pause)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    ) {
                        val pagerState = rememberPagerState()

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ScrollableTabRow(
                                modifier = Modifier.fillMaxWidth(),
                                selectedTabIndex = pagerState.currentPage,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                        Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                                    )
                                }
                            ) {
                                listOf(
                                    stringResource(R.string.tab_torrent_overview),
                                    stringResource(R.string.tab_torrent_files),
                                    stringResource(R.string.tab_torrent_pieces),
                                ).forEachIndexed { index, title ->
                                    Tab(
                                        text = { Text(title.uppercase()) },
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                    )
                                }
                            }

                            HorizontalPager(
                                count = 3,
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> TorrentOverview(
                                        serverConfig = serverConfig,
                                        torrentHash = torrentHash,
                                        modifier = Modifier.fillMaxSize(),
                                        viewModel = viewModel,
                                        onError = { error ->
                                            snackbarMessage = context.getErrorMessage(error)
                                        }
                                    )
                                    1 -> TorrentFiles(
                                        serverConfig,
                                        torrentHash,
                                        modifier = Modifier.fillMaxSize(),
                                        onError = { error ->
                                            snackbarMessage = context.getErrorMessage(error)
                                        }
                                    )
                                    2 -> TorrentPieces(
                                        serverConfig,
                                        torrentHash,
                                        modifier = Modifier.fillMaxSize(),
                                        onError = { error ->
                                            snackbarMessage = context.getErrorMessage(error)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}