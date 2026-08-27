package com.parallelc.micts

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(SettingsViewModel::class.java)
    }
}
