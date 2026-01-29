package dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import dev.bartuzen.qbitcontroller.model.DnsOverHttps
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.supportsDnsOverHttps
import dev.bartuzen.qbitcontroller.network.supportsSelfSignedCertificates
import dev.bartuzen.qbitcontroller.preferences.ListPreference
import dev.bartuzen.qbitcontroller.preferences.PreferenceCategory
import dev.bartuzen.qbitcontroller.preferences.SwitchPreference
import dev.bartuzen.qbitcontroller.preferences.TextFieldPreference
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import org.jetbrains.compose.resources.pluralStringResource
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_disabled
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_basic_auth
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_basic_auth_enabled
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_basic_auth_password
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_basic_auth_username
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_custom_headers
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_custom_headers_description
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_custom_headers_summary
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_360
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_adguard
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_alidns
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_cloudflare
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_controld
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_dnspod
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_google
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_mullvad
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_njalla
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_quad101
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_quad9
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_doh_shecan
import qbitcontroller.composeapp.generated.resources.settings_server_advanced_title
import qbitcontroller.composeapp.generated.resources.settings_server_trust_self_signed_certificates

object AdvancedServerSettingsKeys {
    const val AdvancedSettings = "advancedServerSettings.advancedSettings"
}

@Composable
fun AdvancedServerSettingsScreen(
    advancedSettings: ServerConfig.AdvancedSettings,
    onNavigateBack: () -> Unit,
    onUpdate: (advancedSettings: ServerConfig.AdvancedSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var trustSelfSignedCertificates by rememberSaveable { mutableStateOf(advancedSettings.trustSelfSignedCertificates) }
    var basicAuthEnabled by rememberSaveable { mutableStateOf(advancedSettings.basicAuth.isEnabled) }
    var basicAuthUsername by rememberSaveable {
        mutableStateOf(
            advancedSettings.basicAuth.username ?: "",
        )
    }
    var basicAuthPassword by rememberSaveable {
        mutableStateOf(
            advancedSettings.basicAuth.password ?: "",
        )
    }
    var dnsOverHttps by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(advancedSettings.dnsOverHttps) }
    var customHeaders by rememberSaveable {
        mutableStateOf(advancedSettings.customHeaders.joinToString("\n") { "${it.key}=${it.value}" })
    }

    LaunchedEffect(
        trustSelfSignedCertificates,
        basicAuthEnabled,
        basicAuthUsername,
        basicAuthPassword,
        dnsOverHttps,
        customHeaders,
    ) {
        onUpdate(
            ServerConfig.AdvancedSettings(
                trustSelfSignedCertificates = trustSelfSignedCertificates,
                basicAuth = ServerConfig.AdvancedSettings.BasicAuth(
                    isEnabled = basicAuthEnabled,
                    username = basicAuthUsername.ifEmpty { null },
                    password = basicAuthPassword.ifEmpty { null },
                ),
                dnsOverHttps = dnsOverHttps,
                customHeaders = customHeaders
                    .split("\n")
                    .filter { it.isNotBlank() && it.contains("=") }
                    .map {
                        val (key, value) = it.split("=", limit = 2)
                        ServerConfig.AdvancedSettings.CustomHeader(key, value)
                    },
            ),
        )
    }

    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_server_advanced_title),
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
            if (supportsSelfSignedCertificates()) {
                item {
                    SwitchPreference(
                        value = trustSelfSignedCertificates,
                        onValueChange = { trustSelfSignedCertificates = it },
                        title = { Text(text = stringResource(Res.string.settings_server_trust_self_signed_certificates)) },
                    )
                }
            }

            if (supportsDnsOverHttps()) {
                item {
                    val items = mapOf(
                        null to Res.string.settings_disabled,
                        DnsOverHttps.Cloudflare to Res.string.settings_server_advanced_doh_cloudflare,
                        DnsOverHttps.Google to Res.string.settings_server_advanced_doh_google,
                        DnsOverHttps.AdGuard to Res.string.settings_server_advanced_doh_adguard,
                        DnsOverHttps.Quad9 to Res.string.settings_server_advanced_doh_quad9,
                        DnsOverHttps.AliDNS to Res.string.settings_server_advanced_doh_alidns,
                        DnsOverHttps.DNSPod to Res.string.settings_server_advanced_doh_dnspod,
                        DnsOverHttps.DNS360 to Res.string.settings_server_advanced_doh_360,
                        DnsOverHttps.Quad101 to Res.string.settings_server_advanced_doh_quad101,
                        DnsOverHttps.Mullvad to Res.string.settings_server_advanced_doh_mullvad,
                        DnsOverHttps.ControlD to Res.string.settings_server_advanced_doh_controld,
                        DnsOverHttps.Njalla to Res.string.settings_server_advanced_doh_njalla,
                        DnsOverHttps.Shecan to Res.string.settings_server_advanced_doh_shecan,
                    ).mapValues { stringResource(it.value) }

                    ListPreference(
                        value = dnsOverHttps,
                        onValueChange = { dnsOverHttps = it },
                        values = items.keys.toList(),
                        title = { Text(text = stringResource(Res.string.settings_server_advanced_doh)) },
                        valueToText = { AnnotatedString(items[it] ?: "") },
                        summary = items[dnsOverHttps]?.let { { Text(text = it) } },
                    )
                }
            }

            item {
                TextFieldPreference(
                    value = customHeaders,
                    onValueChange = {
                        customHeaders = it
                    },
                    title = { Text(text = stringResource(Res.string.settings_server_advanced_custom_headers)) },
                    textToValue = { it },
                    textField = { value, onValueChange, onOk ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardActions = KeyboardActions { onOk() },
                            supportingText = {
                                Text(text = stringResource(Res.string.settings_server_advanced_custom_headers_description))
                            },
                        )
                    },
                    summary = {
                        val count = customHeaders
                            .split("\n")
                            .filter { it.isNotBlank() && it.contains("=") }
                            .size
                        Text(
                            text = pluralStringResource(
                                Res.plurals.settings_server_advanced_custom_headers_summary,
                                count,
                                count,
                            ),
                        )
                    },
                    modifier = Modifier.animateContentSize(),
                )
            }

            item {
                PreferenceCategory(
                    title = { Text(text = stringResource(Res.string.settings_server_advanced_basic_auth)) },
                )
            }

            item {
                SwitchPreference(
                    value = basicAuthEnabled,
                    onValueChange = { basicAuthEnabled = it },
                    title = { Text(text = stringResource(Res.string.settings_server_advanced_basic_auth_enabled)) },
                )
            }

            item {
                TextFieldPreference(
                    value = basicAuthUsername,
                    onValueChange = { basicAuthUsername = it },
                    title = { Text(text = stringResource(Res.string.settings_server_advanced_basic_auth_username)) },
                    textToValue = { it },
                    enabled = basicAuthEnabled,
                    summary = basicAuthUsername.takeIf { it.isNotEmpty() }?.let { { Text(text = it) } },
                    modifier = Modifier.animateContentSize(),
                )
            }

            item {
                var showPassword by rememberSaveable { mutableStateOf(false) }
                TextFieldPreference(
                    value = basicAuthPassword,
                    onValueChange = { basicAuthPassword = it },
                    title = { Text(text = stringResource(Res.string.settings_server_advanced_basic_auth_password)) },
                    textToValue = { it },
                    enabled = basicAuthEnabled,
                    summary = basicAuthPassword.takeIf { it.isNotEmpty() }?.let {
                        { Text(text = '\u2022'.toString().repeat(it.length)) }
                    },
                    textField = { value, onValueChange, onOk ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValueChange,
                            keyboardActions = KeyboardActions { onOk() },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Filled.Visibility
                                        } else {
                                            Icons.Filled.VisibilityOff
                                        },
                                        contentDescription = null,
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    onDialogStateChange = { showPassword = false },
                    modifier = Modifier.animateContentSize(),
                )
            }
        }
    }
}
