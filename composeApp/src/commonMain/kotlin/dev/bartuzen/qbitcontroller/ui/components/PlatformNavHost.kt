package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun PlatformNavHost(
    navController: NavHostController,
    startDestination: Any,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)?,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)?,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)?,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)?,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    builder: NavGraphBuilder.() -> Unit,
) {
    if (enterTransition != null && exitTransition != null && popEnterTransition != null && popExitTransition != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            contentAlignment = contentAlignment,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
            builder = builder,
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            contentAlignment = contentAlignment,
            builder = builder,
        )
    }
}
