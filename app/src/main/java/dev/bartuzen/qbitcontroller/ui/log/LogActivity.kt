package dev.bartuzen.qbitcontroller.ui.log

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.LogType
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1)
        if (serverId == -1) {
            finish()
            return
        }

        setContent {
            AppTheme {
                LogScreen(
                    serverId = serverId,
                    onNavigateBack = { finish() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is LogViewModel.Event.Error -> {
                val errorMessage = getErrorMessage(context, event.error)
                if (snackbarHostState.currentSnackbarData?.visuals?.message != errorMessage) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch {
                        snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Indefinite)
                    }
                }
            }

            LogViewModel.Event.UpdateSuccess -> {
                if (snackbarHostState.currentSnackbarData?.visuals?.duration == SnackbarDuration.Indefinite) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeed(serverId)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(
                    ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
                ),
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.execution_log_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshRssFeed(serverId) },
            modifier = Modifier.padding(innerPadding),
        ) {
            val logs by viewModel.logs.collectAsStateWithLifecycle()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = logs ?: emptyList(),
                    key = { it.id },
                ) { log ->
                    val logColor = when (log.type) {
                        LogType.NORMAL -> null
                        LogType.INFO -> R.color.log_info
                        LogType.WARNING -> R.color.log_warning
                        LogType.CRITICAL -> R.color.log_critical
                    }?.let { colorId ->
                        harmonizeWithPrimary(colorResource(colorId))
                    }

                    val logText = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = colorResource(R.color.log_timestamp))) {
                            append(formatDate(log.timestamp))
                        }

                        append(" - ")

                        withStyle(style = SpanStyle(color = logColor ?: LocalTextStyle.current.color)) {
                            append(log.message)
                        }
                    }

                    Text(
                        text = logText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }

                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                }
            }

            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            AnimatedVisibility(
                visible = isLoading == true,
                enter = expandVertically(tween(durationMillis = 500)),
                exit = shrinkVertically(tween(durationMillis = 500)),
            ) {
                LinearProgressIndicator(
                    strokeCap = StrokeCap.Butt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }
}
