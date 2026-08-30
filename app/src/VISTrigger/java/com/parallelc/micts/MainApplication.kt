package com.parallelc.micts

import android.app.Application
import com.parallelc.micts.data.VisTriggerPreferenceMigration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VisTriggerPreferenceMigration(this).run()
    }
}
