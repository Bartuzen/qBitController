package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend CoroutineScope.(T) -> Unit,
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(state) {
        collect {
            action(it)
        }
    }
}

fun <T> Flow<T>.launchAndCollectLatestIn(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend CoroutineScope.(T) -> Unit,
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(state) {
        collectLatest {
            action(it)
        }
    }
}

@Composable
fun <T : Any> EventEffect(
    eventFlow: Flow<T>,
    lifeCycleState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: CoroutineScope.(T) -> Unit,
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
