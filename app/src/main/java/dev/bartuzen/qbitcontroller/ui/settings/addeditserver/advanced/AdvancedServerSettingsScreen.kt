package dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.DnsOverHttps
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference

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

    LaunchedEffect(
        trustSelfSignedCertificates,
        basicAuthEnabled,
        basicAuthUsername,
        basicAuthPassword,
        dnsOverHttps,
    ) {
        onUpdate(
            ServerConfig.AdvancedSettings(
                trustSelfSignedCertificates = trustSelfSignedCertificates,
                basicAuth = ServerConfig.AdvancedSettings.BasicAuth(
                    isEnabled = basicAuthEnabled,
                    username = if (basicAuthUsername.isNotEmpty()) basicAuthUsername else null,
                    password = if (basicAuthPassword.isNotEmpty()) basicAuthPassword else null,
                ),
                dnsOverHttps = dnsOverHttps,
            ),
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_server_advanced_title),
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
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                SwitchPreference(
                    value = trustSelfSignedCertificates,
                    onValueChange = { trustSelfSignedCertificates = it },
                    title = { Text(text = stringResource(R.string.settings_server_trust_self_signed_certificates)) },
                )
            }

            item {
                val items = mapOf(
                    null to R.string.settings_disabled,
                    DnsOverHttps.Cloudflare to R.string.settings_server_advanced_doh_cloudflare,
                    DnsOverHttps.Google to R.string.settings_server_advanced_doh_google,
                    DnsOverHttps.AdGuard to R.string.settings_server_advanced_doh_adguard,
                    DnsOverHttps.Quad9 to R.string.settings_server_advanced_doh_quad9,
                    DnsOverHttps.AliDNS to R.string.settings_server_advanced_doh_alidns,
                    DnsOverHttps.DNSPod to R.string.settings_server_advanced_doh_dnspod,
                    DnsOverHttps.DNS360 to R.string.settings_server_advanced_doh_360,
                    DnsOverHttps.Quad101 to R.string.settings_server_advanced_doh_quad101,
                    DnsOverHttps.Mullvad to R.string.settings_server_advanced_doh_mullvad,
                    DnsOverHttps.ControlD to R.string.settings_server_advanced_doh_controld,
                    DnsOverHttps.Njalla to R.string.settings_server_advanced_doh_njalla,
                    DnsOverHttps.Shecan to R.string.settings_server_advanced_doh_shecan,
                ).mapValues { stringResource(it.value) }

                ListPreference(
                    value = dnsOverHttps,
                    onValueChange = { dnsOverHttps = it },
                    values = items.keys.toList(),
                    title = { Text(text = stringResource(R.string.settings_server_advanced_doh)) },
                    valueToText = { AnnotatedString(items[it] ?: "") },
                    summary = items[dnsOverHttps]?.let { { Text(text = it) } },
                )
            }

            item {
                PreferenceCategory(
                    title = { Text(text = stringResource(R.string.settings_server_advanced_basic_auth)) },
                )
            }

            item {
                SwitchPreference(
                    value = basicAuthEnabled,
                    onValueChange = { basicAuthEnabled = it },
                    title = { Text(text = stringResource(R.string.settings_server_advanced_basic_auth_enabled)) },
                )
            }

            item {
                TextFieldPreference(
                    value = basicAuthUsername,
                    onValueChange = { basicAuthUsername = it },
                    title = { Text(text = stringResource(R.string.settings_server_advanced_basic_auth_username)) },
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
                    title = { Text(text = stringResource(R.string.settings_server_advanced_basic_auth_password)) },
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
