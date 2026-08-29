package com.parallelc.micts.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.config.TriggerService
import com.parallelc.micts.config.XposedConfig
import com.parallelc.micts.data.CompatibilityReportProvider
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.CompatibilityReport
import com.parallelc.micts.domain.TriggerStrategy
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        viewModel = (application as MainApplication).settingsViewModel
        setContent {
            MiCTSTheme {
                val locale by viewModel.locale.collectAsState()
                val config = Configuration(resources.configuration).apply { setLocale(locale) }
                CompositionLocalProvider(LocalContext provides createConfigurationContext(config)) {
                    SettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var showAboutDialog by remember { viewModel.showAboutDialog }

    var topAppBarState = remember { viewModel.topAppBarState }
    val appIcon = LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.applicationInfo.packageName)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { viewModel.menuExpanded }
                    var languageExpanded by remember { viewModel.languageExpanded }
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        val context = LocalContext.current
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language)) },
                                onClick = {
                                    menuExpanded = false
                                    languageExpanded = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                onClick = {
                                    menuExpanded = false
                                    showAboutDialog = true
                                }
                            )
                                }
                        }
                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false },
                        ) {
                            CompositionLocalProvider(LocalContext provides context) {
                                Language.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.id)) },
                                        onClick = {
                                            viewModel.updateAppConfig(AppConfig.KEY_LANGUAGE, option.ordinal)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }

                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        SettingsPage(modifier = Modifier.fillMaxSize().padding(paddingValues), viewModel)
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row {
                    Image(
                        painter = rememberDrawablePainter(drawable = appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Text(buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url = "https://github.com/rungruch/MiCTS")) {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("https://github.com/rungruch/MiCTS")
                        }
                    }
                })
            },
            confirmButton = {},
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = modifier.verticalScroll(remember { viewModel.scrollState })
    ) {
        val appConfig by viewModel.appConfig.collectAsState()
        val xposedService by viewModel.xposedService.collectAsState()
        val xposedConfig by viewModel.xposedConfig.collectAsState()

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.app_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        SliderSettingItem(
            title = stringResource(R.string.default_trigger_delay),
            value = (appConfig[AppConfig.KEY_DEFAULT_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_DEFAULT_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        SliderSettingItem(
            title = stringResource(R.string.tile_trigger_delay),
            value = (appConfig[AppConfig.KEY_TILE_DELAY] as Long).toFloat(),
            onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_TILE_DELAY, it.toLong())},
            valueRange = 0f..2000f
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.vibrate)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_VIBRATE] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_VIBRATE, it)
                        xposedService?.run { viewModel.updateXposedConfig(XposedConfig.KEY_VIBRATE, it) }
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.async_trigger)) },
            trailingContent = {
                Switch(
                    checked = appConfig[AppConfig.KEY_ASYNC_TRIGGER] as Boolean,
                    onCheckedChange = {
                        viewModel.updateAppConfig(AppConfig.KEY_ASYNC_TRIGGER, it)
                    }
                )
            }
        )

        if (BuildConfig.APP_NAME == "MiCTS") {
            TriggerStrategySettings(appConfig, viewModel)
            ListItem(
                headlineContent = { Text(stringResource(R.string.local_text_recognition)) },
                supportingContent = {
                    Text(stringResource(R.string.local_text_recognition_summary))
                },
                trailingContent = {
                    Switch(
                        checked = appConfig[AppConfig.KEY_LOCAL_TEXT_RECOGNITION] as Boolean,
                        onCheckedChange = { enabled ->
                            viewModel.updateAppConfig(
                                AppConfig.KEY_LOCAL_TEXT_RECOGNITION,
                                enabled,
                            )
                        },
                    )
                },
            )
            AiSettingsSection(appConfig, viewModel)
            val selectedTriggerService = (xposedConfig[XposedConfig.KEY_TRIGGER_SERVICE] as? Int)
                ?.let { ordinal -> TriggerService.entries.getOrNull(ordinal)?.name }
                ?: TriggerService.VIS.name
            CompatibilityReportSection(selectedTriggerService)
        }

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.module_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        if (xposedService == null) {
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                headlineContent = { Text(stringResource(R.string.access_xposed_service_failed)) },
                trailingContent = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        exitProcess(1)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                        )
                    }
                }
            )
            return@Column
        }

        if (BuildConfig.APP_NAME == "MiCTS") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.system_trigger_service)) },
                trailingContent = {
                    Box {
                        var triggerServiceExpanded by remember { viewModel.triggerServiceExpanded }
                        val selectedOption = TriggerService.entries[xposedConfig[XposedConfig.KEY_TRIGGER_SERVICE] as Int].name
                        val options = TriggerService.getSupportedServices()

                        TextButton(onClick = { triggerServiceExpanded = true }) {
                            Text(text = selectedOption)
                        }

                        if (options.size <= 1) return@Box

                        DropdownMenu(
                            expanded = triggerServiceExpanded,
                            onDismissRequest = { triggerServiceExpanded = false }
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        triggerServiceExpanded = false
                                        viewModel.updateXposedConfig(XposedConfig.KEY_TRIGGER_SERVICE, option.ordinal)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        val isXiaomi = Build.MANUFACTURER == "Xiaomi"
        val isMeizu = Build.MANUFACTURER == "meizu"
        if (isXiaomi) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.trigger_by_long_press_gesture_handle)) },
                trailingContent = {
                    Switch(
                        checked = xposedConfig[XposedConfig.KEY_GESTURE_TRIGGER] as Boolean,
                        onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_GESTURE_TRIGGER, it) }
                    )
                }
            )
        }

        if (isXiaomi || isMeizu) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.trigger_by_long_press_home_button)) },
                trailingContent = {
                    Switch(
                        checked = xposedConfig[XposedConfig.KEY_HOME_TRIGGER] as Boolean,
                        onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_HOME_TRIGGER, it) }
                    )
                }
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.device_spoof_for_google)) },
            trailingContent = {
                Switch(
                    checked = xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean,
                    onCheckedChange = { viewModel.updateXposedConfig(XposedConfig.KEY_DEVICE_SPOOF, it) }
                )
            }
        )

        if (xposedConfig[XposedConfig.KEY_DEVICE_SPOOF] as Boolean) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ModelSpoofFields(xposedConfig, viewModel)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TriggerStrategySettings(
    appConfig: Map<String, Any>,
    viewModel: SettingsViewModel,
) {
    val strategy = TriggerStrategy.fromStoredName(
        appConfig[AppConfig.KEY_TRIGGER_STRATEGY] as? String,
    )
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.trigger_strategy)) },
        supportingContent = {
            Text(stringResource(R.string.lens_fallback_notice))
        },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(triggerStrategyLabel(strategy))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    listOf(
                        TriggerStrategy.AUTO,
                        TriggerStrategy.NATIVE_ONLY,
                        TriggerStrategy.LENS_FALLBACK,
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(triggerStrategyLabel(option)) },
                            onClick = {
                                expanded = false
                                viewModel.updateAppConfig(
                                    AppConfig.KEY_TRIGGER_STRATEGY,
                                    option.name,
                                )
                                if (option == TriggerStrategy.AUTO) {
                                    viewModel.updateAppConfig(
                                        AppConfig.KEY_AUTO_RESOLUTION,
                                        AutoResolution.UNKNOWN.name,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
    )
    TextButton(
        onClick = {
            viewModel.updateAppConfig(
                AppConfig.KEY_AUTO_RESOLUTION,
                AutoResolution.UNKNOWN.name,
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(stringResource(R.string.reset_auto_detection))
    }
}

@Composable
private fun triggerStrategyLabel(strategy: TriggerStrategy): String = when (strategy) {
    TriggerStrategy.AUTO -> stringResource(R.string.trigger_strategy_auto)
    TriggerStrategy.NATIVE_ONLY -> stringResource(R.string.trigger_strategy_native)
    else -> stringResource(R.string.trigger_strategy_lens)
}

@Composable
private fun CompatibilityReportSection(selectedTriggerService: String) {
    val context = LocalContext.current
    val report = remember(selectedTriggerService) {
        CompatibilityReportProvider(context).create(selectedTriggerService)
    }
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.compatibility_report),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
    SelectionContainer {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            compatibilityLines(report).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.compatibility_entitlement_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun compatibilityLines(report: CompatibilityReport): List<String> {
    val yes = stringResource(R.string.status_yes)
    val no = stringResource(R.string.status_no)
    val notInstalled = stringResource(R.string.status_not_installed)
    val none = stringResource(R.string.status_none)
    fun status(value: Boolean) = if (value) yes else no

    return listOf(
        "${stringResource(R.string.compatibility_device)}: ${report.manufacturer} ${report.model}",
        "${stringResource(R.string.compatibility_android)}: ${report.androidVersion} (API ${report.apiLevel})",
        "${stringResource(R.string.compatibility_google_app)}: ${report.googleAppVersion ?: notInstalled}",
        "${stringResource(R.string.compatibility_active_assistant)}: ${report.activeAssistant ?: none}",
        "${stringResource(R.string.compatibility_google_assistant)}: ${status(report.googleIsDefaultAssistant)}",
        "${stringResource(R.string.compatibility_cts_activity)}: ${status(report.contextualSearchActivityResolvable)}",
        "${stringResource(R.string.compatibility_cts_feature)}: ${status(report.contextualSearchFeatureDeclared)}",
        "${stringResource(R.string.compatibility_cts_service)}: ${status(report.contextualSearchServiceAvailable)}",
        "${stringResource(R.string.compatibility_lens_share)}: ${status(report.lensShareAvailable)}",
        "${stringResource(R.string.compatibility_trigger_service)}: ${report.selectedTriggerService}",
        "${stringResource(R.string.compatibility_consent_reuse)}: ${status(report.consentReuseSupported)}",
        "${stringResource(R.string.compatibility_consent_stored)}: ${status(report.consentStored)}",
    )
}

@Composable
fun SliderSettingItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${value.toInt()} ms",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                textAlign = TextAlign.End
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 39
        )
    }
}

@Composable
fun ModelSpoofFields(
    xposedConfig: Map<String, Any?>,
    viewModel: SettingsViewModel,
) {
     Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MANUFACTURER] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MANUFACTURER, it) },
            label = { Text(stringResource(R.string.manufacturer)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_BRAND] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_BRAND, it) },
            label = { Text(stringResource(R.string.brand)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_MODEL] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_MODEL, it) },
            label = { Text(stringResource(R.string.model)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = xposedConfig[XposedConfig.KEY_SPOOF_DEVICE] as String,
            onValueChange = { viewModel.updateXposedConfig(XposedConfig.KEY_SPOOF_DEVICE, it) },
            label = { Text(stringResource(R.string.device)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AiSettingsSection(
    appConfig: Map<String, Any>,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val aiEnabled = appConfig[AppConfig.KEY_AI_ENABLED] as? Boolean ?: false
    val aiBaseUrl = appConfig[AppConfig.KEY_AI_BASE_URL] as? String ?: AppConfig.DEFAULT_AI_BASE_URL
    val aiApiKey = appConfig[AppConfig.KEY_AI_API_KEY] as? String ?: ""
    val aiModel = appConfig[AppConfig.KEY_AI_MODEL] as? String ?: AppConfig.DEFAULT_AI_MODEL
    val aiSendImage = appConfig[AppConfig.KEY_AI_SEND_IMAGE] as? Boolean ?: true
    val aiPrivacyAccepted = appConfig[AppConfig.KEY_AI_PRIVACY_ACCEPTED] as? Boolean ?: false

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.ai_assistant),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    )

    ListItem(
        headlineContent = { Text(stringResource(R.string.ai_enabled)) },
        supportingContent = { Text(stringResource(R.string.ai_assistant_summary)) },
        trailingContent = {
            Switch(
                checked = aiEnabled,
                onCheckedChange = { checked ->
                    if (checked && !aiPrivacyAccepted) {
                        showPrivacyDialog = true
                    } else {
                        viewModel.updateAppConfig(AppConfig.KEY_AI_ENABLED, checked)
                    }
                }
            )
        }
    )

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.ai_privacy_title)) },
            text = { Text(stringResource(R.string.ai_privacy_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    viewModel.updateAppConfig(AppConfig.KEY_AI_PRIVACY_ACCEPTED, true)
                    viewModel.updateAppConfig(AppConfig.KEY_AI_ENABLED, true)
                }) {
                    Text(stringResource(R.string.ai_privacy_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    AnimatedVisibility(visible = aiEnabled) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.shapes.medium,
                )
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = aiBaseUrl,
                onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_AI_BASE_URL, it) },
                label = { Text(stringResource(R.string.ai_base_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = aiApiKey,
                onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_AI_API_KEY, it) },
                label = { Text(stringResource(R.string.ai_api_key)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = aiModel,
                onValueChange = { viewModel.updateAppConfig(AppConfig.KEY_AI_MODEL, it) },
                label = { Text(stringResource(R.string.ai_model)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.ai_send_image)) },
                supportingContent = { Text(stringResource(R.string.ai_send_image_summary)) },
                trailingContent = {
                    Switch(
                        checked = aiSendImage,
                        onCheckedChange = { viewModel.updateAppConfig(AppConfig.KEY_AI_SEND_IMAGE, it) }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = {
                        isTesting = true
                        viewModel.testAiConnection(aiBaseUrl, aiApiKey) { result ->
                            isTesting = false
                            result.fold(
                                onSuccess = { count ->
                                    Toast.makeText(
                                        context,
                                        resources.getQuantityString(
                                            R.plurals.ai_test_success,
                                            count,
                                            count,
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        resources.getString(
                                            R.string.ai_test_failed,
                                            error.localizedMessage ?: error.message,
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    enabled = !isTesting && aiBaseUrl.isNotBlank()
                ) {
                    Text(stringResource(R.string.ai_test_connection))
                }
            }
        }
    }
}
