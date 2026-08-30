package com.parallelc.micts.ui.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.R
import com.parallelc.micts.data.VisTriggerSettingsRepository
import com.parallelc.micts.domain.VisTriggerCoordinator
import com.parallelc.micts.trigger.AndroidNativeTriggerGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FROM_TILE = "from_tile"
    }

    private val nativeGateway = AndroidNativeTriggerGateway()
    private var triggerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val settings = VisTriggerSettingsRepository(this).load()
        val fromTile = intent.getBooleanExtra(EXTRA_FROM_TILE, false)

        triggerJob = lifecycleScope.launch {
            val delayMs = settings.delayFor(fromTile)
            if (delayMs > 0) delay(delayMs)

            val result = withContext(Dispatchers.Default) {
                nativeGateway.invoke(
                    entryPoint = 1,
                    context = this@MainActivity,
                    vibrate = settings.vibrate,
                )
            }
            if (VisTriggerCoordinator.shouldShowFailure(result)) {
                Toast.makeText(this@MainActivity, R.string.trigger_failed, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onDestroy() {
        triggerJob?.cancel()
        super.onDestroy()
    }
}
