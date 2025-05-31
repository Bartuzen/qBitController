package dev.bartuzen.qbitcontroller.ui.log

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.main.Destination
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json

@Composable
fun LogsNavHost(serverConfig: ServerConfig?, navigateToStartFlow: Flow<Unit>, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    PersistentLaunchedEffect(Json.encodeToString(serverConfig)) {
        val serverId = serverConfig?.id
        if (serverId != null) {
            navController.navigate(Destination.Log(serverId)) {
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
            navController.popBackStack<Destination.Log>(inclusive = false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Empty,
        enterTransition = DefaultTransitions.Enter,
        exitTransition = DefaultTransitions.Exit,
        popEnterTransition = DefaultTransitions.PopEnter,
        popExitTransition = DefaultTransitions.PopExit,
        modifier = modifier,
    ) {
        composable<Destination.Empty> {}
        composable<Destination.Log>(
            enterTransition = { EnterTransition.None },
            exitTransition = DefaultTransitions.Exit,
            popEnterTransition = DefaultTransitions.PopEnter,
            popExitTransition = DefaultTransitions.PopExit,
        ) {
            val args = it.toRoute<Destination.Log>()
            LogScreen(
                serverId = args.serverId,
            )
        }
    }
}
