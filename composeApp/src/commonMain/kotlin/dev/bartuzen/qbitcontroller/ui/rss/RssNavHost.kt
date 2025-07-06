package dev.bartuzen.qbitcontroller.ui.rss

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentKeys
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentScreen
import dev.bartuzen.qbitcontroller.ui.components.PlatformNavHost
import dev.bartuzen.qbitcontroller.ui.main.Destination
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesKeys
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesScreen
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleScreen
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsScreen
import dev.bartuzen.qbitcontroller.ui.rss.rules.RssRulesScreen
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json

@Composable
fun RssNavHost(serverConfig: ServerConfig?, navigateToStartFlow: Flow<Unit>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    PersistentLaunchedEffect(Json.encodeToString(serverConfig)) {
        val serverId = serverConfig?.id
        if (serverId != null) {
            navController.navigate(Destination.Rss.Feeds(serverId)) {
                popUpTo(0) {
                    inclusive = true
                }
            }
        } else {
            navController.navigate(Destination.Empty) {
                popUpTo(0) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(navigateToStartFlow) {
        navigateToStartFlow.collectLatest {
            navController.popBackStack<Destination.Rss.Feeds>(inclusive = false)
        }
    }

    PlatformNavHost(
        navController = navController,
        startDestination = Destination.Empty,
        enterTransition = DefaultTransitions.Enter,
        exitTransition = DefaultTransitions.Exit,
        popEnterTransition = DefaultTransitions.PopEnter,
        popExitTransition = DefaultTransitions.PopExit,
        modifier = modifier,
    ) {
        composable<Destination.Empty> {}
        composable<Destination.Rss.Feeds>(
            enterTransition = { EnterTransition.None },
            exitTransition = DefaultTransitions.Exit,
            popEnterTransition = DefaultTransitions.PopEnter,
            popExitTransition = DefaultTransitions.PopExit,
        ) {
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
                onNavigateToArticles = { feedPath, uid ->
                    navController.navigate(
                        Destination.Rss.Articles(
                            args.serverId,
                            feedPath,
                            uid,
                        ),
                    )
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
    }
}
