package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwipeableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {
    val dismissSnackbarState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        modifier = modifier.fillMaxWidth(),
        state = dismissSnackbarState,
        backgroundContent = {},
        onDismiss = {
            scope.launch {
                hostState.currentSnackbarData?.dismiss()
                delay(100)
                dismissSnackbarState.reset()
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                SnackbarHost(
                    hostState = hostState,
                    snackbar = snackbar,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        },
    )
}
