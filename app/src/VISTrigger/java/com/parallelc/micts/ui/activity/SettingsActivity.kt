package com.parallelc.micts.ui.activity

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.R
import com.parallelc.micts.config.Language
import com.parallelc.micts.ui.theme.MiCTSTheme
import com.parallelc.micts.ui.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModels<SettingsViewModel> { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        setContent {
            MiCTSTheme {
                val locale by viewModel.locale.collectAsState()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    val configuration = Configuration(resources.configuration).apply {
                        setLocale(locale)
                    }
                    CompositionLocalProvider(
                        LocalContext provides createConfigurationContext(configuration),
                    ) {
                        VisTriggerSettingsScreen(viewModel)
                    }
                } else {
                    VisTriggerSettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisTriggerSettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { menuExpanded = true }) {
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
                                    viewModel.setLanguage(language)
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
                value = settings.defaultDelay.toFloat(),
                onValueChange = { viewModel.setDefaultDelay(it.toLong()) },
            )
            SliderSetting(
                title = stringResource(R.string.tile_trigger_delay),
                value = settings.tileDelay.toFloat(),
                onValueChange = { viewModel.setTileDelay(it.toLong()) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.vibrate)) },
                trailingContent = {
                    Switch(
                        checked = settings.vibrate,
                        onCheckedChange = viewModel::setVibrate,
                    )
                },
            )
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
private fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, modifier = Modifier.weight(1f))
            Text("${value.toInt()} ms", textAlign = TextAlign.End)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..2000f)
    }
}
