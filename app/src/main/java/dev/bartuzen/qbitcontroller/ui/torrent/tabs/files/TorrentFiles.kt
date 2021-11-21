package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabContent

@Composable
fun TorrentFiles(
    serverConfig: ServerConfig,
    torrentHash: String,
    modifier: Modifier = Modifier,
    onError: (error: RequestResult) -> Unit,
    viewModel: TorrentFilesViewModel = hiltViewModel()
) {
    TorrentTabContent(
        viewModel = viewModel,
        modifier = modifier,
        updateData = {
            viewModel.updateFiles(serverConfig, torrentHash)
        },
        onError = onError
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.files.size) { index ->
                TorrentFileItem(
                    file = viewModel.files[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 16.dp)
                )
            }
            item {
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TorrentFileItem(
    file: TorrentFile,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.FileCopy,
            contentDescription = null,
            modifier = Modifier
                .width(48.dp)
                .height(48.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = file.name,
            style = TextStyle(
                color = MaterialTheme.colors.onBackground
            )
        )
    }
}