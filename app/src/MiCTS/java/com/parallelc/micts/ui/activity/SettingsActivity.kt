package com.parallelc.micts.ui.activity

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.config.AppConfig
import com.parallelc.micts.config.Language
import com.parallelc.micts.domain.AutoResolution
import com.parallelc.micts.domain.TriggerStrategy
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        val viewModel = (application as MainApplication).settingsViewModel
        setContent {
            MiCTSTheme {
                val locale by viewModel.locale.collectAsState()
                val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
                CompositionLocalProvider(
                    LocalContext provides createConfigurationContext(configuration),
                ) {
                    LeanSettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeanSettingsScreen(viewModel: SettingsViewModel) {
    val appConfig by viewModel.appConfig.collectAsState()
    var menuExpanded by remember { viewModel.menuExpanded }
    var languageExpanded by remember { viewModel.languageExpanded }
    var strategyMenuExpanded by remember { mutableStateOf(false) }
    val strategy = TriggerStrategy.fromStoredName(
        appConfig[AppConfig.KEY_TRIGGER_STRATEGY] as? String,
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(BuildConfig.APP_NAME)
                        Text(
                            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.language)) },
                            onClick = {
                                menuExpanded = false
                                languageExpanded = true
                            },
                        )
                    }
                    DropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                    ) {
                        Language.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(stringResource(language.id)) },
                                onClick = {
                                    viewModel.updateAppConfig(AppConfig.KEY_LANGUAGE, language.ordinal)
                                    languageExpanded = false
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionLabel(stringResource(R.string.app_settings))
            SliderSetting(
                title = stringResource(R.string.default_trigger_delay),
                value = (appConfig[AppConfig.KEY_DEFAULT_DELAY] as Long).toFloat(),
                onValueChange = {
                    viewModel.updateAppConfig(AppConfig.KEY_DEFAULT_DELAY, it.toLong())
                },
            )
            SliderSetting(
                title = stringResource(R.string.tile_trigger_delay),
                value = (appConfig[AppConfig.KEY_TILE_DELAY] as Long).toFloat(),
                onValueChange = {
                    viewModel.updateAppConfig(AppConfig.KEY_TILE_DELAY, it.toLong())
                },
            )
            SwitchSetting(
                title = stringResource(R.string.vibrate),
                checked = appConfig[AppConfig.KEY_VIBRATE] as Boolean,
                onCheckedChange = {
                    viewModel.updateAppConfig(AppConfig.KEY_VIBRATE, it)
                },
            )
            SwitchSetting(
                title = stringResource(R.string.async_trigger),
                checked = appConfig[AppConfig.KEY_ASYNC_TRIGGER] as Boolean,
                onCheckedChange = {
                    viewModel.updateAppConfig(AppConfig.KEY_ASYNC_TRIGGER, it)
                },
            )

            SectionLabel(stringResource(R.string.trigger_strategy))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(triggerStrategyLabel(strategy), modifier = Modifier.weight(1f))
                Button(onClick = { strategyMenuExpanded = true }) {
                    Text(stringResource(R.string.change))
                }
                DropdownMenu(
                    expanded = strategyMenuExpanded,
                    onDismissRequest = { strategyMenuExpanded = false },
                ) {
                    listOf(
                        TriggerStrategy.AUTO,
                        TriggerStrategy.NATIVE_ONLY,
                        TriggerStrategy.LENS_FALLBACK,
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(triggerStrategyLabel(option)) },
                            onClick = {
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
                                strategyMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (strategy == TriggerStrategy.AUTO) {
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

        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun SliderSetting(title: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, modifier = Modifier.weight(1f))
            Text("${value.toInt()} ms", textAlign = TextAlign.End)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..2000f)
    }
}

@Composable
private fun triggerStrategyLabel(strategy: TriggerStrategy): String = when (strategy) {
    TriggerStrategy.AUTO -> stringResource(R.string.trigger_strategy_auto)
    TriggerStrategy.NATIVE_ONLY -> stringResource(R.string.trigger_strategy_native)
    else -> stringResource(R.string.trigger_strategy_lens)
}
