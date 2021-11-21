package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabContent
import dev.bartuzen.qbitcontroller.utils.formatByte
import dev.bartuzen.qbitcontroller.utils.formatBytePerSecond
import dev.bartuzen.qbitcontroller.utils.formatState
import dev.bartuzen.qbitcontroller.utils.formatTime

@Composable
fun TorrentOverview(
    serverConfig: ServerConfig,
    torrentHash: String,
    onError: (error: RequestResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentViewModel = hiltViewModel()
) {
    TorrentTabContent(
        viewModel = viewModel,
        modifier = modifier,
        updateData = {
            viewModel.updateOverview(serverConfig, torrentHash)
        },
        onError = onError
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            viewModel.torrent?.let { torrent ->

                Text(
                    text = torrent.name,
                    style = TextStyle(fontSize = 18.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val progress = (torrent.progress * 100).toInt()
                            val progressText = stringResource(
                                R.string.torrent_item_progress,
                                formatByte(torrent.completed),
                                formatByte(torrent.size),
                                if (progress != 100) progress.toString() else "100"
                            )

                            Text(text = progressText)

                            val time = formatTime(torrent.eta)
                            if (time != "inf") {
                                Text(text = time)
                            }
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            progress = torrent.progress.toFloat(),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatState(torrent.state))

                            val speedList = mutableListOf<String>()
                            if (torrent.uploadSpeed > 0) {
                                speedList.add("↑ ${formatBytePerSecond(torrent.uploadSpeed)}")
                            }
                            if (torrent.downloadSpeed > 0) {
                                speedList.add("↓ ${formatBytePerSecond(torrent.downloadSpeed)}")
                            }
                            Text(text = speedList.joinToString(" "))
                        }
                    }
                }
            }
        }
    }
}