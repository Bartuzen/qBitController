package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun PersistentLaunchedEffect(vararg keys: Any?, block: suspend () -> Unit) {
    var savedState by rememberSaveable { mutableStateOf<Array<out Any?>?>(null) }

    LaunchedEffect(*keys) {
        if (!savedState.contentEquals(keys)) {
            savedState = keys
            block()
        }
    }
}
