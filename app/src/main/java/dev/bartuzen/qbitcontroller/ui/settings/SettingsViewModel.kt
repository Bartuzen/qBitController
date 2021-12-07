package dev.bartuzen.qbitcontroller.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    val state: SavedStateHandle
) : ViewModel() {
    val serversFlow = settingsManager.serversFlow
    val themeFlow = settingsManager.themeFlow

    fun setTheme(theme: Theme) = MainScope().launch {
        settingsManager.setTheme(theme)
    }
}