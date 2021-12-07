package dev.bartuzen.qbitcontroller.ui.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

abstract class BaseLoadingViewModel : ViewModel() {
    var isLoading by mutableStateOf(true)
    var isRefreshing by mutableStateOf(false)
}