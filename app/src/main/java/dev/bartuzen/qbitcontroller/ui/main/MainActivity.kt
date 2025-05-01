package dev.bartuzen.qbitcontroller.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentKeys
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentScreen
import dev.bartuzen.qbitcontroller.ui.log.LogScreen
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesKeys
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesScreen
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleScreen
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsScreen
import dev.bartuzen.qbitcontroller.ui.rss.rules.RssRulesScreen
import dev.bartuzen.qbitcontroller.ui.search.plugins.SearchPluginsScreen
import dev.bartuzen.qbitcontroller.ui.search.result.SearchResultScreen
import dev.bartuzen.qbitcontroller.ui.search.start.SearchStartScreen
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentKeys
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentScreen
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListKeys
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListScreen
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getParcelableArrayListExtraCompat
import dev.bartuzen.qbitcontroller.utils.getParcelableExtraCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var notificationManager: AppNotificationManager

    private val serverIdChannel = Channel<Int>()

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    this@MainActivity.navController = navController
                }

                PersistentLaunchedEffect {
                    onNewIntent(intent)
                }

                val transitionDuration = 400
                NavHost(
                    navController = navController,
                    startDestination = Destination.TorrentList,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it / 10 },
                            animationSpec = tween(transitionDuration),
                        ) + fadeIn(animationSpec = tween(transitionDuration))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 10 },
                            animationSpec = tween(transitionDuration),
                        ) + fadeOut(animationSpec = tween(transitionDuration))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 10 },
                            animationSpec = tween(transitionDuration),
                        ) + fadeIn(animationSpec = tween(transitionDuration))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it / 10 },
                            animationSpec = tween(transitionDuration),
                        ) + fadeOut(animationSpec = tween(transitionDuration))
                    },
                ) {
                    composable<Destination.TorrentList> {
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

                        val addTorrentChannel = remember { Channel<Int>() }
                        val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                        LaunchedEffect(torrentAdded) {
                            if (torrentAdded != null) {
                                addTorrentChannel.send(torrentAdded)
                                it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                            }
                        }

                        val deleteTorrentChannel = remember { Channel<Unit>() }
                        val torrentDeleted = it.savedStateHandle.get<Boolean?>(TorrentKeys.TorrentDeleted)
                        LaunchedEffect(torrentDeleted) {
                            if (torrentDeleted != null) {
                                deleteTorrentChannel.send(Unit)
                                it.savedStateHandle.remove<Boolean?>(TorrentKeys.TorrentDeleted)
                            }
                        }

                        val isScreenActive = currentLifecycle.isAtLeast(Lifecycle.State.RESUMED) &&
                            navController.currentDestination == it.destination
                        TorrentListScreen(
                            isScreenActive = isScreenActive,
                            serverIdFlow = serverIdChannel.receiveAsFlow(),
                            addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                            deleteTorrentFlow = deleteTorrentChannel.receiveAsFlow(),
                            onNavigateToTorrent = { serverId, torrentHash, torrentName ->
                                navController.navigate(Destination.Torrent(serverId, torrentHash, torrentName))
                            },
                            onNavigateToAddTorrent = { initialServerId ->
                                navController.navigate(Destination.AddTorrent(initialServerId))
                            },
                            onNavigateToRss = { serverId ->
                                navController.navigate(Destination.Rss.Feeds(serverId))
                            },
                            onNavigateToSearch = { serverId ->
                                navController.navigate(Destination.Search.Start(serverId))
                            },
                            onNavigateToLog = { serverId ->
                                navController.navigate(Destination.Log(serverId))
                            },
                        )
                    }

                    composable<Destination.Torrent> {
                        val args = it.toRoute<Destination.Torrent>()
                        TorrentScreen(
                            serverId = args.serverId,
                            torrentHash = args.torrentHash,
                            torrentName = args.torrentName,
                            onNavigateBack = { navController.navigateUp() },
                            onDeleteTorrent = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(TorrentKeys.TorrentDeleted, true)

                                navController.navigateUp()
                            },
                        )
                    }

                    composable<Destination.AddTorrent> {
                        val args = it.toRoute<Destination.AddTorrent>()
                        AddTorrentScreen(
                            initialServerId = args.initialServerId,
                            torrentUrl = args.torrentUrl,
                            torrentFileUris = args.torrentFileUris,
                            onNavigateBack = { navController.navigateUp() },
                            onAddTorrent = { serverId ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(AddTorrentKeys.TorrentAdded, serverId)

                                navController.navigateUp()
                            },
                        )
                    }

                    composable<Destination.Rss.Feeds> {
                        val args = it.toRoute<Destination.Rss.Feeds>()

                        val articleUpdate = remember { Channel<Unit>() }
                        val isUpdated = it.savedStateHandle.get<Boolean?>(RssArticlesKeys.IsUpdated)
                        LaunchedEffect(isUpdated) {
                            if (isUpdated != null) {
                                articleUpdate.send(Unit)
                                it.savedStateHandle.remove<Boolean?>(RssArticlesKeys.IsUpdated)
                            }
                        }

                        RssFeedsScreen(
                            serverId = args.serverId,
                            articleUpdateFlow = articleUpdate.receiveAsFlow(),
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToArticles = { feedPath, uid ->
                                navController.navigate(Destination.Rss.Articles(args.serverId, feedPath, uid))
                            },
                            onNavigateToRules = {
                                navController.navigate(Destination.Rss.Rules(args.serverId))
                            },
                        )
                    }

                    composable<Destination.Rss.Articles> {
                        val args = it.toRoute<Destination.Rss.Articles>()

                        val addTorrentChannel = remember { Channel<Unit>() }
                        val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                        LaunchedEffect(torrentAdded) {
                            if (torrentAdded != null) {
                                addTorrentChannel.send(Unit)
                                it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                            }
                        }

                        RssArticlesScreen(
                            serverId = args.serverId,
                            feedPath = args.feedPath,
                            uid = args.uid,
                            addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                            onFeedPathChange = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(RssArticlesKeys.IsUpdated, true)
                            },
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToAddTorrent = { torrentUrl ->
                                navController.navigate(Destination.AddTorrent(args.serverId, torrentUrl))
                            },
                        )
                    }

                    composable<Destination.Rss.Rules> {
                        val args = it.toRoute<Destination.Rss.Rules>()
                        RssRulesScreen(
                            serverId = args.serverId,
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToEditRule = { ruleName ->
                                navController.navigate(Destination.Rss.EditRule(args.serverId, ruleName))
                            },
                        )
                    }

                    composable<Destination.Rss.EditRule> {
                        val args = it.toRoute<Destination.Rss.EditRule>()
                        EditRssRuleScreen(
                            serverId = args.serverId,
                            ruleName = args.ruleName,
                            onNavigateBack = { navController.navigateUp() },
                        )
                    }

                    composable<Destination.Search.Start> {
                        val args = it.toRoute<Destination.Search.Start>()
                        SearchStartScreen(
                            serverId = args.serverId,
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToSearchResult = { searchQuery, category, plugins ->
                                navController.navigate(
                                    Destination.Search.Result(args.serverId, searchQuery, category, plugins),
                                )
                            },
                            onNavigateToPlugins = { navController.navigate(Destination.Search.Plugins(args.serverId)) },
                        )
                    }

                    composable<Destination.Search.Result> {
                        val args = it.toRoute<Destination.Search.Result>()

                        val addTorrentChannel = remember { Channel<Unit>() }
                        val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                        LaunchedEffect(torrentAdded) {
                            if (torrentAdded != null) {
                                addTorrentChannel.send(Unit)
                                it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                            }
                        }

                        SearchResultScreen(
                            serverId = args.serverId,
                            searchQuery = args.searchQuery,
                            category = args.category,
                            plugins = args.plugins,
                            addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToAddTorrent = { torrentUrl ->
                                navController.navigate(Destination.AddTorrent(args.serverId, torrentUrl))
                            },
                        )
                    }

                    composable<Destination.Search.Plugins> {
                        val args = it.toRoute<Destination.Search.Plugins>()
                        SearchPluginsScreen(
                            serverId = args.serverId,
                            onNavigateBack = { navController.navigateUp() },
                        )
                    }

                    composable<Destination.Log> {
                        val args = it.toRoute<Destination.Log>()
                        LogScreen(
                            serverId = args.serverId,
                            onNavigateBack = { navController.navigateUp() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationManager.startWorker()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        var torrentUrl: String? = null
        var torrentFileUris: List<Uri>? = null

        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { uri ->
                when (uri.scheme) {
                    "magnet" -> torrentUrl = uri.toString()
                    "content" -> torrentFileUris = listOf(uri)
                }
            }
            Intent.ACTION_SEND -> {
                when (intent.type) {
                    "text/plain" -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        torrentUrl = text
                    }
                    "application/x-bittorrent" -> intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                        ?.let { uri ->
                            torrentFileUris = listOf(uri)
                        }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                when (intent.type) {
                    "application/x-bittorrent" -> intent.getParcelableArrayListExtraCompat<Uri>(
                        Intent.EXTRA_STREAM,
                    )?.let { uris ->
                        torrentFileUris = uris
                    }
                }
            }
        }

        if (torrentUrl != null || torrentFileUris != null) {
            navController?.navigate(
                Destination.AddTorrent(
                    torrentUrl = torrentUrl,
                    torrentFileUris = torrentFileUris?.map { it.toString() },
                ),
                builder = {
                    launchSingleTop = true
                },
            )
        }

        val torrentServerId = intent.getIntExtra(TorrentKeys.ServerId, -1)
        val torrentHash = intent.getStringExtra(TorrentKeys.TorrentHash)
        val torrentName = intent.getStringExtra(TorrentKeys.TorrentName)

        if (torrentServerId != -1 && torrentHash != null && torrentName != null) {
            lifecycleScope.launch {
                serverIdChannel.send(torrentServerId)
            }
            navController?.navigate(Destination.Torrent(torrentServerId, torrentHash, torrentName))
            intent.removeExtra(TorrentKeys.ServerId)
            intent.removeExtra(TorrentKeys.TorrentHash)
            intent.removeExtra(TorrentKeys.TorrentName)
        }

        val torrentListServerId = intent.getIntExtra(TorrentListKeys.ServerId, -1)
        if (torrentListServerId != -1) {
            lifecycleScope.launch {
                serverIdChannel.send(torrentListServerId)
            }
            navController?.popBackStack(route = Destination.TorrentList, inclusive = false)
            intent.removeExtra(TorrentListKeys.ServerId)
        }
    }
}
