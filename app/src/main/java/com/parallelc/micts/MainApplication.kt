package com.parallelc.micts

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.parallelc.micts.data.CapturePreferenceMigration
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        CapturePreferenceMigration(this).run()
    }

    val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(SettingsViewModel::class.java)
    }
}
