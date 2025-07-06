package dev.bartuzen.qbitcontroller.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

object DefaultTransitions {
    private const val TransitionDuration = 400

    val Enter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? =
        if (currentPlatform != Platform.Mobile.IOS) {
            {
                slideInHorizontally(
                    initialOffsetX = { it / 10 },
                    animationSpec = tween(TransitionDuration),
                ) + fadeIn(animationSpec = tween(TransitionDuration))
            }
        } else {
            null
        }

    val Exit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? =
        if (currentPlatform != Platform.Mobile.IOS) {
            {
                slideOutHorizontally(
                    targetOffsetX = { -it / 10 },
                    animationSpec = tween(TransitionDuration),
                ) + fadeOut(animationSpec = tween(TransitionDuration))
            }
        } else {
            null
        }

    val PopEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? =
        if (currentPlatform != Platform.Mobile.IOS) {
            {
                slideInHorizontally(
                    initialOffsetX = { -it / 10 },
                    animationSpec = tween(TransitionDuration),
                ) + fadeIn(animationSpec = tween(TransitionDuration))
            }
        } else {
            null
        }

    val PopExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? =
        if (currentPlatform != Platform.Mobile.IOS) {
            {
                slideOutHorizontally(
                    targetOffsetX = { it / 10 },
                    animationSpec = tween(TransitionDuration),
                ) + fadeOut(animationSpec = tween(TransitionDuration))
            }
        } else {
            null
        }

    object NavBar {
        val Enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 200,
                    easing = LinearOutSlowInEasing,
                ),
            ) + scaleIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 200,
                    easing = LinearOutSlowInEasing,
                ),
                initialScale = 1f,
            )
        }

        val Exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing,
                ),
            )
        }

        val PopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 200,
                    easing = LinearOutSlowInEasing,
                ),
            ) + scaleIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 200,
                    easing = LinearOutSlowInEasing,
                ),
                initialScale = 1f,
            )
        }

        val PopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing,
                ),
            )
        }
    }
}
