package dev.bartuzen.qbitcontroller.utils

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

context(viewModel: ViewModel)
inline fun <reified T> SavedStateHandle.getSerializableStateFlow(key: String, initialValue: T): StateFlow<T> {
    val value = get<String>(key) ?: Json.encodeToString(initialValue)
    return getStateFlow(key, value)
        .map { Json.decodeFromString<T>(it) }
        .stateIn(viewModel.viewModelScope, SharingStarted.Eagerly, Json.decodeFromString(value))
}

inline fun <reified T> SavedStateHandle.getSerializable(key: String) = get<String>(key)?.let { Json.decodeFromString<T>(it) }

inline fun <reified T> SavedStateHandle.setSerializable(key: String, value: T?) = set(key, Json.encodeToString(value))
