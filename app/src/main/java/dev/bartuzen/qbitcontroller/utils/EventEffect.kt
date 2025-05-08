package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
fun <T : Any> EventEffect(
    eventFlow: Flow<T>,
    lifeCycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend CoroutineScope.(T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(eventFlow) {
        withContext(Dispatchers.Main.immediate) {
            lifecycleOwner.repeatOnLifecycle(lifeCycleState) {
                eventFlow.collect {
                    this.collector(it)
                }
            }
        }
    }
}
