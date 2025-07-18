package dev.bartuzen.qbitcontroller.ui.settings.appearance

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.preferences.ListPreference
import dev.bartuzen.qbitcontroller.preferences.SwitchPreference
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import org.koin.compose.viewmodel.koinViewModel
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_category_appearance
import qbitcontroller.composeapp.generated.resources.settings_pure_black_dark_mode
import qbitcontroller.composeapp.generated.resources.settings_theme
import qbitcontroller.composeapp.generated.resources.settings_theme_dark
import qbitcontroller.composeapp.generated.resources.settings_theme_light
import qbitcontroller.composeapp.generated.resources.settings_theme_system_default

@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceSettingsViewModel = koinViewModel(),
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_category_appearance),
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
                colors = listState.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                LanguagePreference()
            }
            item {
                val theme by viewModel.theme.flow.collectAsStateWithLifecycle()

                val lightString = stringResource(Res.string.settings_theme_light)
                val darkString = stringResource(Res.string.settings_theme_dark)
                val systemString = stringResource(Res.string.settings_theme_system_default)

                ListPreference(
                    value = theme,
                    onValueChange = { viewModel.theme.value = it },
                    values = listOf(Theme.LIGHT, Theme.DARK, Theme.SYSTEM_DEFAULT),
                    title = { Text(text = stringResource(Res.string.settings_theme)) },
                    valueToText = { themeValue ->
                        AnnotatedString(
                            when (themeValue) {
                                Theme.LIGHT -> lightString
                                Theme.DARK -> darkString
                                Theme.SYSTEM_DEFAULT -> systemString
                            },
                        )
                    },
                    summary = {
                        Text(
                            text = when (theme) {
                                Theme.LIGHT -> lightString
                                Theme.DARK -> darkString
                                Theme.SYSTEM_DEFAULT -> systemString
                            },
                        )
                    },
                )
            }

            item {
                DynamicColorsPreference()
            }

            item {
                val pureBlackDarkMode by viewModel.pureBlackDarkMode.flow.collectAsStateWithLifecycle()
                SwitchPreference(
                    value = pureBlackDarkMode,
                    onValueChange = { viewModel.pureBlackDarkMode.value = it },
                    title = { Text(text = stringResource(Res.string.settings_pure_black_dark_mode)) },
                )
            }
        }
    }
}

@Composable
expect fun LanguagePreference()

@Composable
expect fun DynamicColorsPreference()
