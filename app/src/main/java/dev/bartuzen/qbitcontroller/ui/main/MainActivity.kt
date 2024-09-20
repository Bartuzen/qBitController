package dev.bartuzen.qbitcontroller.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    @Inject
    lateinit var notificationManager: AppNotificationManager

    private val serverIdChannel = Channel<Int>()
    private val serverIdFlow = serverIdChannel.receiveAsFlow()

    private var navController: NavHostController? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
            if (serverId != -1) {
                lifecycleScope.launch {
                    serverIdChannel.send(serverId)
                }
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    this@MainActivity.navController = navController
                }

                NavHost(
                    navController = navController,
                    startDestination = Destination.TorrentList,
                    modifier = Modifier.semantics {
                        testTagsAsResourceId = true
                    },
                ) {
                    composable<Destination.TorrentList> {
                        var currentLifecycle by remember { mutableStateOf(lifecycle.currentState) }
                        DisposableEffect(Unit) {
                            val observer = LifecycleEventObserver { _, event ->
                                currentLifecycle = event.targetState
                            }
                            lifecycle.addObserver(observer)

                            onDispose {
                                lifecycle.removeObserver(observer)
                            }
                        }

                        val isScreenActive = currentLifecycle.isAtLeast(Lifecycle.State.RESUMED) &&
                            navController.currentDestination == it.destination
                        TorrentListScreen(
                            isScreenActive = isScreenActive,
                            serverIdFlow = serverIdFlow,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationManager.startWorker()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        if (serverId != -1) {
            lifecycleScope.launch {
                serverIdChannel.send(serverId)
            }
            navController?.popBackStack(route = Destination.TorrentList, inclusive = false)
        }
    }
}
