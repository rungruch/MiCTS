package com.parallelc.micts

import android.app.Application
import com.parallelc.micts.data.CapturePreferenceMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        CapturePreferenceMigration(this).run()
    }
}
