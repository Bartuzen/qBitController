package dev.bartuzen.qbitcontroller.ui.settings.appearance

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    settingsManager: SettingsManager,
) : ViewModel() {
    val theme = settingsManager.theme
}
