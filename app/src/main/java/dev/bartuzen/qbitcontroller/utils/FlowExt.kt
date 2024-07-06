package dev.bartuzen.qbitcontroller.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
