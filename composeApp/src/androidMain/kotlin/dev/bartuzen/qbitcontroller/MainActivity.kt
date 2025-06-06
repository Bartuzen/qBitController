package dev.bartuzen.qbitcontroller

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.ui.main.DeepLinkDestination
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentKeys
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListKeys
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val notificationManager by inject<AppNotificationManager>()

    private val serverManager by inject<ServerManager>()

    private val navigationChannel = Channel<DeepLinkDestination>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FileKit.init(this)

        setContent {
            PersistentLaunchedEffect {
                onNewIntent(intent)
            }

            MainScreen(navigationFlow = navigationChannel.receiveAsFlow())
        }
    }

    override fun onResume() {
        super.onResume()
        notificationManager.startWorker()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            lifecycleScope.launch {
                navigationChannel.send(DeepLinkDestination.Settings)
            }
        }

        var torrentUrl: String? = null
        var torrentFileUris: List<Uri>? = null

        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { uri ->
                when (uri.scheme) {
                    "magnet" -> torrentUrl = uri.toString()
                    "content" -> torrentFileUris = listOf(uri)
                }
            }
            Intent.ACTION_SEND -> {
                when (intent.type) {
                    "text/plain" -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        torrentUrl = text
                    }
                    "application/x-bittorrent" -> intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                        ?.let { uri ->
                            torrentFileUris = listOf(uri)
                        }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                when (intent.type) {
                    "application/x-bittorrent" -> intent.getParcelableArrayListExtraCompat<Uri>(
                        Intent.EXTRA_STREAM,
                    )?.let { uris ->
                        torrentFileUris = uris
                    }
                }
            }
        }

        if (torrentUrl != null || torrentFileUris != null) {
            if (serverManager.serversFlow.value.isNotEmpty()) {
                lifecycleScope.launch {
                    navigationChannel.send(
                        DeepLinkDestination.AddTorrent(
                            torrentUrl = torrentUrl,
                            torrentFileUris = torrentFileUris?.map { it.toString() },
                        ),
                    )
                }
            } else {
                lifecycleScope.launch {
                    navigationChannel.send(DeepLinkDestination.TorrentList(null))
                }
            }
        }

        val torrentServerId = intent.getIntExtra(TorrentKeys.ServerId, -1)
        val torrentHash = intent.getStringExtra(TorrentKeys.TorrentHash)
        val torrentName = intent.getStringExtra(TorrentKeys.TorrentName)

        if (torrentServerId != -1 && torrentHash != null && torrentName != null) {
            lifecycleScope.launch {
                navigationChannel.send(DeepLinkDestination.Torrent(torrentServerId, torrentHash, torrentName))
            }
            intent.removeExtra(TorrentKeys.ServerId)
            intent.removeExtra(TorrentKeys.TorrentHash)
            intent.removeExtra(TorrentKeys.TorrentName)
        }

        val torrentListServerId = intent.getIntExtra(TorrentListKeys.ServerId, -1)
        if (torrentListServerId != -1) {
            lifecycleScope.launch {
                navigationChannel.send(DeepLinkDestination.TorrentList(torrentListServerId))
            }
            intent.removeExtra(TorrentListKeys.ServerId)
        }
    }

    private inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String) =
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(name)
        }

    private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String) =
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(name) as? T
        }
}
