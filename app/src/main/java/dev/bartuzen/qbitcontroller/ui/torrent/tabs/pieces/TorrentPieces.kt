package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.theme.pieceDownloaded
import dev.bartuzen.qbitcontroller.ui.theme.pieceNotDownloaded
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabContent
import dev.bartuzen.qbitcontroller.utils.formatByte
import dev.bartuzen.qbitcontroller.utils.toDp

@Composable
fun TorrentPieces(
    serverConfig: ServerConfig,
    torrentHash: String,
    modifier: Modifier = Modifier,
    onError: (error: RequestResult) -> Unit,
    viewModel: TorrentPiecesViewModel = hiltViewModel()
) {
    TorrentTabContent(
        viewModel = viewModel,
        modifier = modifier,
        updateData = {
            viewModel.updatePieces(serverConfig, torrentHash)
        },
        onError = onError
    ) {
        TorrentPiecesGrid(viewModel = viewModel)
    }
}

@Composable
fun TorrentPiecesGrid(viewModel: TorrentPiecesViewModel) {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val width = displayMetrics.widthPixels.toDp(LocalContext.current) - 32
    val columns = width / 20
    val items = viewModel.pieces

    if (items.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                TorrentPiecesHeader(
                    viewModel = viewModel,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
                )
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.torrent_piece_map),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }
            items(items.size / columns + if (items.size % columns == 0) 0 else 1) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    for (i in rowIndex * columns until (rowIndex + 1) * columns) {
                        if (items.size <= i) {
                            break
                        }
                        val color = if (items[i] == PieceState.DOWNLOADED) {
                            MaterialTheme.colors.pieceDownloaded
                        } else {
                            MaterialTheme.colors.pieceNotDownloaded
                        }
                        Box(
                            modifier = Modifier
                                .background(color)
                                .width(16.dp)
                                .height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TorrentPiecesHeader(
    modifier: Modifier = Modifier,
    viewModel: TorrentPiecesViewModel
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        viewModel.properties?.let { properties ->
            Column {
                Text(
                    text = stringResource(R.string.torrent_pieces),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                )
                Text(
                    text = properties.piecesCount.toString()
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.torrent_piece_size),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                )
                Text(
                    text = formatByte(properties.pieceSize.toLong())
                )
            }
        }
    }
}