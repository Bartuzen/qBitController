package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SwipeableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {
    val dismissSnackbarState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissSnackbarState.currentValue != SwipeToDismissBoxValue.Settled) {
        withContext(NonCancellable) {
            if (dismissSnackbarState.currentValue != SwipeToDismissBoxValue.Settled) {
                hostState.currentSnackbarData?.dismiss()
                delay(100)
                dismissSnackbarState.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissSnackbarState,
        backgroundContent = {},
        content = {
            val alpha = if (dismissSnackbarState.dismissDirection == SwipeToDismissBoxValue.Settled) {
                1f
            } else {
                1 - dismissSnackbarState.progress
            }

            SnackbarHost(
                hostState = hostState,
                snackbar = snackbar,
                modifier = Modifier.alpha(alpha),
            )
        },
    )
}
