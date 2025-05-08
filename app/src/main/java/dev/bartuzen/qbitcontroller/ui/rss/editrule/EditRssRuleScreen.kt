package dev.bartuzen.qbitcontroller.ui.rss.editrule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditRssRuleScreen(
    serverId: Int,
    ruleName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRssRuleViewModel = koinViewModel(parameters = { parametersOf(serverId, ruleName) }),
) {
    val rssRule by viewModel.rssRule.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val isFetched by viewModel.isFetched.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is EditRssRuleViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                launch {
                    snackbarHostState.showSnackbar(getErrorMessage(context, event.error))
                }
            }
            EditRssRuleViewModel.Event.RuleUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_rule_saved_successfully))
                }
            }
            EditRssRuleViewModel.Event.RuleNotFound -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.rss_rule_not_found))
                }
            }
        }
    }

    var isEnabled by rememberSaveable { mutableStateOf(false) }
    var useRegex by rememberSaveable { mutableStateOf(false) }
    var mustContain by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var mustNotContain by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var episodeFilter by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var smartFilter by rememberSaveable { mutableStateOf(false) }
    var savePathEnabled by rememberSaveable { mutableStateOf(false) }
    var savePath by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var ignoreDays by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var addPausedIndex by rememberSaveable { mutableIntStateOf(0) }
    var contentLayoutIndex by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf("") }
    val selectedFeedUrls = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }

    PersistentLaunchedEffect(rssRule != null) {
        rssRule?.let { rule ->
            isEnabled = rule.isEnabled
            useRegex = rule.useRegex
            mustContain = TextFieldValue(rule.mustContain)
            mustNotContain = TextFieldValue(rule.mustNotContain)
            episodeFilter = TextFieldValue(rule.episodeFilter)
            smartFilter = rule.smartFilter
            savePathEnabled = rule.savePath.isNotEmpty()
            savePath = TextFieldValue(rule.savePath)
            ignoreDays = TextFieldValue(rule.ignoreDays.toString())

            addPausedIndex = when (rule.addPaused) {
                null -> 0
                true -> 1
                false -> 2
            }

            contentLayoutIndex = when (rule.torrentContentLayout) {
                "Original" -> 1
                "Subfolder" -> 2
                "NoSubfolder" -> 3
                else -> 0
            }

            category = rule.assignedCategory
            selectedFeedUrls.addAll(rule.affectedFeeds)
        }
    }

    fun constructRuleDefinition(): RssRule? {
        if (categories == null || feeds == null) return null

        val addPaused = when (addPausedIndex) {
            1 -> true
            2 -> false
            else -> null
        }

        val contentLayout = when (contentLayoutIndex) {
            1 -> "Original"
            2 -> "Subfolder"
            3 -> "NoSubfolder"
            else -> null
        }

        return RssRule(
            isEnabled = isEnabled,
            mustContain = mustContain.text,
            mustNotContain = mustNotContain.text,
            useRegex = useRegex,
            episodeFilter = episodeFilter.text,
            ignoreDays = ignoreDays.text.toIntOrNull() ?: 0,
            addPaused = addPaused,
            assignedCategory = category,
            savePath = if (savePathEnabled) savePath.text else "",
            torrentContentLayout = contentLayout,
            smartFilter = smartFilter,
            affectedFeeds = selectedFeedUrls,
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ruleName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                    val actionMenuItems = remember(isFetched) {
                        listOf(
                            ActionMenuItem(
                                title = context.getString(R.string.rss_rule_action_save),
                                icon = Icons.Filled.Save,
                                onClick = {
                                    constructRuleDefinition()?.let { rule ->
                                        viewModel.setRule(rule)
                                    }
                                },
                                showAsAction = true,
                                isEnabled = isFetched,
                            ),
                        )
                    }

                    AppBarActions(items = actionMenuItems)
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                ) {
                    CheckboxWithLabel(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        label = stringResource(R.string.rss_rule_enabled),
                        enabled = isFetched,
                    )

                    CheckboxWithLabel(
                        checked = useRegex,
                        onCheckedChange = { useRegex = it },
                        label = stringResource(R.string.rss_rule_use_regular_expressions),
                        enabled = isFetched,
                    )

                    OutlinedTextField(
                        value = mustContain,
                        onValueChange = { mustContain = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rss_rule_must_contain),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isFetched,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = mustNotContain,
                        onValueChange = { mustNotContain = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rss_rule_must_not_contain),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isFetched,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = episodeFilter,
                        onValueChange = { episodeFilter = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rss_rule_episode_filter),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isFetched,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CheckboxWithLabel(
                        checked = smartFilter,
                        onCheckedChange = { smartFilter = it },
                        label = stringResource(R.string.rss_rule_use_smart_episode_filter),
                        enabled = isFetched,
                    )
                }

                HorizontalDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoriesExpanded,
                        onExpandedChange = {
                            if (isFetched) {
                                categoriesExpanded = it
                            }
                        },
                    ) {
                        OutlinedTextField(
                            label = {
                                Text(
                                    text = stringResource(R.string.rss_rule_category),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            enabled = isFetched,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = categoriesExpanded,
                            onDismissRequest = { categoriesExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "") },
                                onClick = {
                                    category = ""
                                    categoriesExpanded = false
                                },
                            )
                            categories?.forEach { currentCategory ->
                                DropdownMenuItem(
                                    text = { Text(text = currentCategory) },
                                    onClick = {
                                        category = currentCategory
                                        categoriesExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    CheckboxWithLabel(
                        checked = savePathEnabled,
                        onCheckedChange = { savePathEnabled = it },
                        label = stringResource(R.string.rss_rule_save_to_a_different_directory),
                        enabled = isFetched,
                    )

                    OutlinedTextField(
                        value = savePath,
                        onValueChange = { savePath = it },
                        label = {
                            Text(
                                text = stringResource(R.string.rss_rule_save_to),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isFetched && savePathEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = ignoreDays,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                ignoreDays = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.rss_rule_ignore_subsequent_matches_for),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isFetched,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var addPausedExpanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = addPausedExpanded,
                        onExpandedChange = {
                            if (isFetched) {
                                addPausedExpanded = it
                            }
                        },
                    ) {
                        OutlinedTextField(
                            label = {
                                Text(
                                    text = stringResource(R.string.rss_rule_add_paused),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            value = when (addPausedIndex) {
                                0 -> stringResource(R.string.rss_rule_use_global_settings)
                                1 -> stringResource(R.string.rss_rule_add_paused_always)
                                2 -> stringResource(R.string.rss_rule_add_paused_never)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            enabled = isFetched,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addPausedExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = addPausedExpanded,
                            onDismissRequest = { addPausedExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.rss_rule_use_global_settings)) },
                                onClick = {
                                    addPausedIndex = 0
                                    addPausedExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.rss_rule_add_paused_always)) },
                                onClick = {
                                    addPausedIndex = 1
                                    addPausedExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.rss_rule_add_paused_never)) },
                                onClick = {
                                    addPausedIndex = 2
                                    addPausedExpanded = false
                                },
                            )
                        }
                    }

                    var contentLayoutExpanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = contentLayoutExpanded,
                        onExpandedChange = {
                            if (isFetched) {
                                contentLayoutExpanded = it
                            }
                        },
                    ) {
                        OutlinedTextField(
                            label = {
                                Text(
                                    text = stringResource(R.string.rss_rule_torrent_content_layout),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            value = when (contentLayoutIndex) {
                                0 -> stringResource(R.string.rss_rule_use_global_settings)
                                1 -> stringResource(R.string.torrent_add_content_layout_original)
                                2 -> stringResource(R.string.torrent_add_content_layout_subfolder)
                                3 -> stringResource(R.string.torrent_add_content_layout_no_subfolder)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            enabled = isFetched,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contentLayoutExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = contentLayoutExpanded,
                            onDismissRequest = { contentLayoutExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.rss_rule_use_global_settings)) },
                                onClick = {
                                    contentLayoutIndex = 0
                                    contentLayoutExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.torrent_add_content_layout_original)) },
                                onClick = {
                                    contentLayoutIndex = 1
                                    contentLayoutExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.torrent_add_content_layout_subfolder)) },
                                onClick = {
                                    contentLayoutIndex = 2
                                    contentLayoutExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.torrent_add_content_layout_no_subfolder)) },
                                onClick = {
                                    contentLayoutIndex = 4
                                    contentLayoutExpanded = false
                                },
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.rss_rule_apply_rule_to_feeds),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = OutlinedTextFieldDefaults.UnfocusedBorderThickness,
                                color = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
                                shape = OutlinedTextFieldDefaults.shape,
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (feeds?.isNotEmpty() == true) {
                            feeds?.forEach { (name, url) ->
                                CheckboxWithLabel(
                                    checked = url in selectedFeedUrls,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedFeedUrls.add(url)
                                        } else {
                                            selectedFeedUrls.remove(url)
                                        }
                                    },
                                    label = name,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.rss_rule_no_feed_found),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
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
