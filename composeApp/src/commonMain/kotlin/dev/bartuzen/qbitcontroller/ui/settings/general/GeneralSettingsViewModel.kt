package dev.bartuzen.qbitcontroller.ui.settings.general

import androidx.lifecycle.ViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager

class GeneralSettingsViewModel(
    settingsManager: SettingsManager,
) : ViewModel() {
    var notificationCheckInterval = settingsManager.notificationCheckInterval
    var areTorrentSwipeActionsEnabled = settingsManager.areTorrentSwipeActionsEnabled
    val checkUpdates = settingsManager.checkUpdates
}
