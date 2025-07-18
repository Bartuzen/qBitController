package dev.bartuzen.qbitcontroller.ui.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.preferences.ListPreference
import dev.bartuzen.qbitcontroller.preferences.ListPreferenceType
import dev.bartuzen.qbitcontroller.preferences.Preference
import dev.bartuzen.qbitcontroller.preferences.SwitchPreference
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.theme.defaultPrimaryColor
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import dev.zt64.compose.pipette.HsvColor
import dev.zt64.compose.pipette.RingColorPicker
import dev.zt64.compose.pipette.SquareColorPicker
import org.koin.compose.viewmodel.koinViewModel
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.settings_app_color
import qbitcontroller.composeapp.generated.resources.settings_app_color_reset
import qbitcontroller.composeapp.generated.resources.settings_category_appearance
import qbitcontroller.composeapp.generated.resources.settings_enable_dynamic_colors
import qbitcontroller.composeapp.generated.resources.settings_palette_style
import qbitcontroller.composeapp.generated.resources.settings_palette_style_content
import qbitcontroller.composeapp.generated.resources.settings_palette_style_expressive
import qbitcontroller.composeapp.generated.resources.settings_palette_style_fidelity
import qbitcontroller.composeapp.generated.resources.settings_palette_style_fruit_salad
import qbitcontroller.composeapp.generated.resources.settings_palette_style_monochrome
import qbitcontroller.composeapp.generated.resources.settings_palette_style_neutral
import qbitcontroller.composeapp.generated.resources.settings_palette_style_rainbow
import qbitcontroller.composeapp.generated.resources.settings_palette_style_tonal_spot
import qbitcontroller.composeapp.generated.resources.settings_palette_style_vibrant
import qbitcontroller.composeapp.generated.resources.settings_pure_black_dark_mode
import qbitcontroller.composeapp.generated.resources.settings_theme
import qbitcontroller.composeapp.generated.resources.settings_theme_dark
import qbitcontroller.composeapp.generated.resources.settings_theme_light
import qbitcontroller.composeapp.generated.resources.settings_theme_system_default
import kotlin.to

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
        val enableDynamicColors by viewModel.enableDynamicColors.flow.collectAsStateWithLifecycle()
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

            if (areDynamicColorsSupported()) {
                item {
                    SwitchPreference(
                        value = enableDynamicColors,
                        onValueChange = { viewModel.enableDynamicColors.value = it },
                        title = { Text(text = stringResource(Res.string.settings_enable_dynamic_colors)) },
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = !areDynamicColorsSupported() || !enableDynamicColors,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        val appColor by viewModel.appColor.flow.collectAsStateWithLifecycle()
                        var showDialog by rememberSaveable { mutableStateOf(false) }

                        if (showDialog) {
                            var currentColor by rememberSaveable(stateSaver = HsvColor.Saver) {
                                mutableStateOf(HsvColor(appColor))
                            }
                            Dialog(
                                title = { Text(text = stringResource(Res.string.settings_app_color)) },
                                onDismissRequest = { showDialog = false },
                                text = {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        RingColorPicker(
                                            color = currentColor,
                                            onColorChange = { currentColor = it },
                                            modifier = Modifier.size(196.dp),
                                        )

                                        SquareColorPicker(
                                            color = currentColor,
                                            onColorChange = { currentColor = it },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.size(108.dp),
                                        )
                                    }
                                },
                                confirmButton = {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        TextButton(
                                            onClick = { currentColor = HsvColor(defaultPrimaryColor) },
                                        ) {
                                            Text(text = stringResource(Res.string.settings_app_color_reset))
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        TextButton(onClick = { showDialog = false }) {
                                            Text(text = stringResource(Res.string.dialog_cancel))
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.appColor.value = currentColor.toColor()
                                                showDialog = false
                                            },
                                        ) {
                                            Text(text = stringResource(Res.string.dialog_ok))
                                        }
                                    }
                                },
                            )
                        }

                        Preference(
                            title = { Text(text = stringResource(Res.string.settings_app_color)) },
                            onClick = { showDialog = true },
                            widgetContainer = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(appColor),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )

                        val paletteStyle by viewModel.paletteStyle.flow.collectAsStateWithLifecycle()
                        val items = mapOf(
                            PaletteStyle.TonalSpot to Res.string.settings_palette_style_tonal_spot,
                            PaletteStyle.Neutral to Res.string.settings_palette_style_neutral,
                            PaletteStyle.Vibrant to Res.string.settings_palette_style_vibrant,
                            PaletteStyle.Expressive to Res.string.settings_palette_style_expressive,
                            PaletteStyle.Rainbow to Res.string.settings_palette_style_rainbow,
                            PaletteStyle.FruitSalad to Res.string.settings_palette_style_fruit_salad,
                            PaletteStyle.Monochrome to Res.string.settings_palette_style_monochrome,
                            PaletteStyle.Fidelity to Res.string.settings_palette_style_fidelity,
                            PaletteStyle.Content to Res.string.settings_palette_style_content,
                        ).mapValues { stringResource(it.value) }

                        ListPreference(
                            value = paletteStyle,
                            onValueChange = { viewModel.paletteStyle.value = it },
                            title = { Text(text = stringResource(Res.string.settings_palette_style)) },
                            values = items.keys.toList(),
                            valueToText = { AnnotatedString(items[it] ?: "") },
                            type = ListPreferenceType.DropdownMenu,
                        )
                    }
                }
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

expect fun areDynamicColorsSupported(): Boolean
