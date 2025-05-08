package dev.bartuzen.qbitcontroller.ui.settings.general

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    settingsManager: SettingsManager,
) : ViewModel() {
    var notificationCheckInterval = settingsManager.notificationCheckInterval
    var areTorrentSwipeActionsEnabled = settingsManager.areTorrentSwipeActionsEnabled
}
