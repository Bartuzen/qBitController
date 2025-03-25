package dev.bartuzen.qbitcontroller.ui.rss.articles

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.rememberSearchStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class RssArticlesFragment() : Fragment() {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val feedPath get() = arguments?.getStringArrayList("feedPath")!!
    private val uid get() = arguments?.getString("uid")

    constructor(serverId: Int, feedPath: List<String>, feedUid: String?) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "feedPath" to ArrayList(feedPath),
            "uid" to feedUid,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    RssArticlesScreen(
                        serverId = serverId,
                        feedPath = feedPath,
                        uid = uid,
                        onFeedPathChanged = { newPath ->
                            setFragmentResult(
                                requestKey = "rssArticlesResult",
                                result = bundleOf("isUpdated" to true),
                            )
                        },
                        onNavigateBack = {
                            if (parentFragmentManager.backStackEntryCount > 0) {
                                parentFragmentManager.popBackStack()
                            } else {
                                requireActivity().finish()
                            }
                        },
                        onNavigateToAddTorrent = { torrentUrl ->
                            val intent = Intent(context, AddTorrentActivity::class.java).apply {
                                putExtra(AddTorrentActivity.Extras.SERVER_ID, serverId)
                                putExtra(AddTorrentActivity.Extras.TORRENT_URL, torrentUrl)
                            }
                            startActivity(intent)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RssArticlesScreen(
    serverId: Int,
    feedPath: List<String>,
    uid: String?,
    onFeedPathChanged: (newPath: List<String>) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAddTorrent: (torrentUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RssArticlesViewModel = hiltViewModel(
        creationCallback = { factory: RssArticlesViewModel.Factory ->
            factory.create(serverId, feedPath, uid)
        },
    ),
) {
    val articles by viewModel.filteredArticles.collectAsStateWithLifecycle()
    val feedPath by viewModel.feedPath.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    when (val dialog = currentDialog) {
        is Dialog.Details -> {
            val article = remember(articles, dialog.id) { articles?.find { it.id == dialog.id } }

            LaunchedEffect(article == null) {
                if (article == null) {
                    currentDialog = null
                }
            }

            if (article != null) {
                DetailsDialog(
                    article = article,
                    onDismiss = { currentDialog = null },
                    onDownloadClicked = {
                        onNavigateToAddTorrent(article.torrentUrl)
                        viewModel.markAsRead(article.path, article.id, false)
                        currentDialog = null
                    },
                    onMarkAsRead = {
                        viewModel.markAsRead(article.path, article.id)
                        currentDialog = null
                    },
                )
            }
        }
        else -> {}
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is RssArticlesViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(context, event.error))
                }
            }
            RssArticlesViewModel.Event.RssFeedNotFound -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_feed_not_found))
                }
            }
            is RssArticlesViewModel.Event.ArticleMarkedAsRead -> {
                if (event.showMessage) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.rss_mark_article_as_read_success))
                    }
                    viewModel.loadRssArticles()
                } else {
                    viewModel.updateRssArticles()
                }
            }
            RssArticlesViewModel.Event.AllArticlesMarkedAsRead -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_mark_all_articles_as_read_success))
                }
                viewModel.loadRssArticles()
            }
            RssArticlesViewModel.Event.FeedRefreshed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_refresh_feed_success))
                }
                scope.launch {
                    delay(1000)
                    viewModel.loadRssArticles()
                }
            }
            is RssArticlesViewModel.Event.FeedPathChanged -> {
                onFeedPathChanged(event.newPath)
            }
        }
    }

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        searchQuery = TextFieldValue()
        viewModel.setSearchQuery("")
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchMode) {
                        Text(
                            text = feedPath.lastOrNull() ?: stringResource(R.string.rss_all_articles),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        val focusRequester = remember { FocusRequester() }
                        PersistentLaunchedEffect {
                            focusRequester.requestFocus()
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.setSearchQuery(it.text)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                TextFieldDefaults.DecorationBox(
                                    value = searchQuery.text,
                                    enabled = true,
                                    innerTextField = innerTextField,
                                    interactionSource = remember { MutableInteractionSource() },
                                    singleLine = true,
                                    visualTransformation = VisualTransformation.None,
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.rss_filter),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.alpha(0.78f),
                                        )
                                    },
                                    trailingIcon = {
                                        AnimatedVisibility(
                                            visible = searchQuery.text.isNotEmpty(),
                                            enter = fadeIn(tween()),
                                            exit = fadeOut(tween()),
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    searchQuery = TextFieldValue()
                                                    viewModel.setSearchQuery("")
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    },
                                    container = {},
                                )
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    val actionMenuItems = remember {
                        listOf(
                            ActionMenuItem(
                                title = context.getString(R.string.action_search),
                                icon = Icons.Filled.Search,
                                onClick = { isSearchMode = true },
                                showAsAction = true,
                            ),
                            ActionMenuItem(
                                title = context.getString(R.string.rss_action_mark_all_as_read),
                                icon = Icons.Filled.MarkEmailRead,
                                onClick = { viewModel.markAllAsRead() },
                                showAsAction = true,
                            ),
                            ActionMenuItem(
                                title = context.getString(R.string.rss_action_refresh),
                                icon = Icons.Filled.Refresh,
                                onClick = { viewModel.refreshFeed() },
                                showAsAction = true,
                            ),
                        )
                    }

                    if (!isSearchMode) {
                        AppBarActions(items = actionMenuItems)
                    }
                },
                windowInsets = WindowInsets.safeDrawing
                    .exclude(WindowInsets.ime)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshRssArticles() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = articles ?: emptyList(),
                    key = { it.id },
                ) { article ->
                    ArticleItem(
                        article = article,
                        searchQuery = searchQuery.text.ifEmpty { null },
                        onClick = { currentDialog = Dialog.Details(article.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }

            SideEffect {
                if (!listState.isScrollInProgress) {
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                }
            }

            AnimatedVisibility(
                visible = isLoading,
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

@Composable
private fun ArticleItem(article: Article, searchQuery: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val title = if (searchQuery != null) {
                    rememberSearchStyle(
                        text = article.title,
                        searchQuery = searchQuery,
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onPrimary,
                            background = MaterialTheme.colorScheme.primary,
                        ),
                    )
                } else {
                    AnnotatedString(article.title)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatDate(article.date),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (!article.isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Serializable
private sealed class Dialog {
    @Serializable
    data class Details(val id: String) : Dialog()
}

@Composable
private fun DetailsDialog(
    article: Article,
    onDismiss: () -> Unit,
    onDownloadClicked: () -> Unit,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.rss_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!article.isRead) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.rss_new),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                HorizontalDivider()

                article.path.lastOrNull()?.let { feed ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = feed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )

                    Text(
                        text = formatDate(article.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (article.description != null) {
                    Text(
                        text = stringResource(R.string.rss_description),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    val linkColor = MaterialTheme.colorScheme.primary
                    val htmlDescription = remember(article.description, linkColor) {
                        htmlToAnnotatedString(
                            html = article.description,
                            style = HtmlStyle(
                                textLinkStyles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ),
                        )
                    }
                    Text(
                        text = htmlDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onDownloadClicked,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.rss_download))
                }

                FilledTonalButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.rss_mark_as_read))
                }
            }
        },
    )
}
