package dev.bartuzen.qbitcontroller.ui.search

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
import dev.bartuzen.qbitcontroller.ui.search.plugins.SearchPluginsScreen
import dev.bartuzen.qbitcontroller.ui.search.result.SearchResultScreen
import dev.bartuzen.qbitcontroller.ui.search.start.SearchStartScreen
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json

@Composable
fun SearchNavHost(serverConfig: ServerConfig?, navigateToStartFlow: Flow<Unit>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    PersistentLaunchedEffect(Json.encodeToString(serverConfig)) {
        val serverId = serverConfig?.id
        if (serverId != null) {
            navController.navigate(Destination.Search.Start(serverId)) {
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
            navController.popBackStack<Destination.Search.Start>(inclusive = false)
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
        composable<Destination.Search.Start>(
            enterTransition = { EnterTransition.None },
            exitTransition = DefaultTransitions.Exit,
            popEnterTransition = DefaultTransitions.PopEnter,
            popExitTransition = DefaultTransitions.PopExit,
        ) {
            val args = it.toRoute<Destination.Search.Start>()
            SearchStartScreen(
                serverId = args.serverId,
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
