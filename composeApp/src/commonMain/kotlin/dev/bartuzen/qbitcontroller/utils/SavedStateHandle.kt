@file:Suppress("ktlint")

package dev.bartuzen.qbitcontroller.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

context(viewModel: ViewModel)
inline fun <reified T> SavedStateHandle.getSerializableStateFlow(key: String, initialValue: T): StateFlow<T> {
    val value = get<String>(key) ?: Json.encodeToString(initialValue)
    return getStateFlow<String>(key, value)
        .map { Json.decodeFromString<T>(it) }
        .stateIn(viewModel.viewModelScope, SharingStarted.Eagerly, Json.decodeFromString(value))
}
