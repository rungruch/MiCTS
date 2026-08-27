package com.parallelc.micts

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.parallelc.micts.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicBoolean

class MainApplication : Application() {
    val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fastCaptureInFlight = AtomicBoolean(false)

    val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this)
            .create(SettingsViewModel::class.java)
    }

    fun tryStartFastCapture(): Boolean = fastCaptureInFlight.compareAndSet(false, true)

    fun finishFastCapture() {
        fastCaptureInFlight.set(false)
    }
}
