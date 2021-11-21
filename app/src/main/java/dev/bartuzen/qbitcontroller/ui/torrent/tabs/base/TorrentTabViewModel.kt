package dev.bartuzen.qbitcontroller.ui.torrent.tabs.base

import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.base.BaseLoadingViewModel
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class TorrentTabViewModel : BaseLoadingViewModel() {
    var eventFlow = MutableSharedFlow<TorrentEvent>()

    sealed class TorrentEvent {
        data class ShowError(val message: RequestResult) : TorrentEvent()
    }
}