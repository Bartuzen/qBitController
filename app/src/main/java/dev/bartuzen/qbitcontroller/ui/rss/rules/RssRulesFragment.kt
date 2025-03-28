package dev.bartuzen.qbitcontroller.ui.rss.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleFragment
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class RssRulesFragment() : Fragment() {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    RssRulesScreen(
                        serverId = serverId,
                        onNavigateBack = {
                            if (parentFragmentManager.backStackEntryCount > 0) {
                                parentFragmentManager.popBackStack()
                            } else {
                                requireActivity().finish()
                            }
                        },
                        onNavigateToEditRule = { ruleName ->
                            val fragment = EditRssRuleFragment(serverId, ruleName)
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                replace(R.id.container, fragment)
                                addToBackStack(null)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
}

@Composable
private fun RssRulesScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditRule: (ruleName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RssRulesViewModel = hiltViewModel(
        creationCallback = { factory: RssRulesViewModel.Factory ->
            factory.create(serverId)
        },
    ),
) {
    val rssRules by viewModel.rssRules.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is RssRulesViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(context, event.error))
                }
            }
            RssRulesViewModel.Event.RuleCreated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_rule_create_success))
                }
            }
            RssRulesViewModel.Event.RuleRenamed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_rule_rename_success))
                }
            }
            RssRulesViewModel.Event.RuleDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_rule_delete_success))
                }
            }
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<RssRuleDialog?>(null) }
    when (val dialog = currentDialog) {
        is RssRuleDialog.CreateRule -> {
            CreateRuleDialog(
                onDismiss = { currentDialog = null },
                onConfirm = { name ->
                    viewModel.createRule(name)
                    currentDialog = null
                },
            )
        }
        is RssRuleDialog.RenameRule -> {
            LaunchedEffect(rssRules) {
                if (rssRules?.containsKey(dialog.ruleName) != true) {
                    currentDialog = null
                }
            }

            RenameRuleDialog(
                ruleName = dialog.ruleName,
                onDismiss = { currentDialog = null },
                onConfirm = { newName ->
                    viewModel.renameRule(dialog.ruleName, newName)
                    currentDialog = null
                },
            )
        }
        is RssRuleDialog.DeleteRule -> {
            LaunchedEffect(rssRules) {
                if (rssRules?.containsKey(dialog.ruleName) != true) {
                    currentDialog = null
                }
            }

            DeleteRuleDialog(
                ruleName = dialog.ruleName,
                onDismiss = { currentDialog = null },
                onConfirm = {
                    viewModel.deleteRule(dialog.ruleName)
                    currentDialog = null
                },
            )
        }
        null -> {}
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.rss_rules)) },
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
                                title = context.getString(R.string.rss_rule_action_create),
                                icon = Icons.Filled.Add,
                                onClick = { currentDialog = RssRuleDialog.CreateRule },
                                showAsAction = true,
                            ),
                        )
                    }

                    AppBarActions(items = actionMenuItems)
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
            onRefresh = { viewModel.refreshRssRules() },
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
                    items = rssRules?.toList() ?: emptyList(),
                    key = { it.first },
                ) { (ruleName, rule) ->
                    RuleItem(
                        ruleName = ruleName,
                        rule = rule,
                        onClick = { onNavigateToEditRule(ruleName) },
                        onRename = { currentDialog = RssRuleDialog.RenameRule(ruleName) },
                        onDelete = { currentDialog = RssRuleDialog.DeleteRule(ruleName) },
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
private fun RuleItem(
    ruleName: String,
    rule: RssRule,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = ruleName,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (rule.isEnabled) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = stringResource(R.string.rss_rule_item_enabled),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = stringResource(R.string.rss_rule_item_disabled),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (rule.affectedFeeds.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RssFeed,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.rss_rule_feeds,
                                        rule.affectedFeeds.size,
                                        rule.affectedFeeds.size,
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.padding(start = 8.dp)) {
                var showMenu by rememberSaveable { mutableStateOf(false) }
                IconButton(
                    onClick = { showMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.rss_rule_rename)) },
                        onClick = {
                            onRename()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.rss_rule_delete)) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Serializable
private sealed class RssRuleDialog {
    @Serializable
    data object CreateRule : RssRuleDialog()

    @Serializable
    data class RenameRule(val ruleName: String) : RssRuleDialog()

    @Serializable
    data class DeleteRule(val ruleName: String) : RssRuleDialog()
}

@Composable
private fun CreateRuleDialog(onDismiss: () -> Unit, onConfirm: (name: String) -> Unit) {
    var nameValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var nameError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.rss_rule_action_create))
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = nameValue,
                onValueChange = {
                    nameValue = it
                    nameError = null
                },
                label = {
                    Text(
                        text = stringResource(R.string.rss_rule_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { error -> { Text(text = stringResource(error)) } },
                trailingIcon = nameError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (nameValue.text.isBlank()) {
                            nameError = R.string.rss_rule_name_cannot_be_empty
                        } else {
                            onConfirm(nameValue.text)
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameValue.text.isBlank()) {
                        nameError = R.string.rss_rule_name_cannot_be_empty
                    } else {
                        onConfirm(nameValue.text)
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun RenameRuleDialog(ruleName: String, onDismiss: () -> Unit, onConfirm: (newName: String) -> Unit) {
    var nameValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(ruleName, TextRange(Int.MAX_VALUE)))
    }
    var nameError by rememberSaveable { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.rss_rule_rename)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = nameValue,
                onValueChange = {
                    nameValue = it
                    nameError = null
                },
                label = {
                    Text(
                        text = stringResource(R.string.rss_rule_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { error -> { Text(text = stringResource(error)) } },
                trailingIcon = nameError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (nameValue.text.isBlank()) {
                            nameError = R.string.rss_rule_name_cannot_be_empty
                        } else {
                            onConfirm(nameValue.text)
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameValue.text.isBlank()) {
                        nameError = R.string.rss_rule_name_cannot_be_empty
                    } else {
                        onConfirm(nameValue.text)
                    }
                },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun DeleteRuleDialog(ruleName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.rss_rule_delete)) },
        text = {
            val description = rememberReplaceAndApplyStyle(
                text = stringResource(R.string.rss_rule_delete_confirm),
                oldValue = "%1\$s",
                newValue = ruleName,
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            )
            Text(text = description)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}
