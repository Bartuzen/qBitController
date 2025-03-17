package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class StateMapViewModel : ViewModel() {
    val states: MutableMap<Int, ArrayDeque<Any>> = mutableMapOf()
}

@Composable
inline fun <reified T : Any> rememberInMemory(vararg inputs: Any?, crossinline init: @DisallowComposableCalls () -> T): T {
    val viewModel: StateMapViewModel = viewModel()
    val key = currentCompositeKeyHash

    val value = remember(*inputs) {
        val states = viewModel.states[key] ?: ArrayDeque<Any>().also { viewModel.states[key] = it }
        states.removeFirstOrNull() as T? ?: init()
    }

    val valueState = rememberUpdatedState(value)
    DisposableEffect(key) {
        onDispose {
            viewModel.states[key]?.addFirst(valueState.value)
        }
    }

    return value
}

@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
fun LaunchedEffectInMemory(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    rememberInMemory(*keys) { LaunchedEffectInMemoryImpl(applyContext, block) }
}

private class LaunchedEffectInMemoryImpl(
    parentCoroutineContext: CoroutineContext,
    private val task: suspend CoroutineScope.() -> Unit,
) : RememberObserver {
    private val scope = CoroutineScope(parentCoroutineContext)
    private var job: Job? = null

    override fun onRemembered() {
        job?.cancel("Old job was still running!")
        job = scope.launch(block = task)
    }

    override fun onForgotten() {
        job?.cancel()
        job = null
    }

    override fun onAbandoned() {
        job?.cancel()
        job = null
    }
}
