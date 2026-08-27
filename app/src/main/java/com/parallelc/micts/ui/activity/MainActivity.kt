package com.parallelc.micts.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.R
import com.parallelc.micts.capture.ScreenCaptureService
import com.parallelc.micts.config.AppConfig.CONFIG_NAME
import com.parallelc.micts.config.AppConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.AppConfig.KEY_ASYNC_TRIGGER
import com.parallelc.micts.config.AppConfig.KEY_DEFAULT_DELAY
import com.parallelc.micts.config.AppConfig.KEY_TILE_DELAY
import com.parallelc.micts.config.AppConfig.KEY_VIBRATE
import com.parallelc.micts.data.TriggerPreferenceStore
import com.parallelc.micts.data.ProjectionConsentStore
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.NativeTriggerResult
import com.parallelc.micts.domain.TriggerAction
import com.parallelc.micts.domain.TriggerCoordinator
import com.parallelc.micts.domain.TriggerStrategy
import com.parallelc.micts.trigger.AndroidNativeTriggerGateway
import com.parallelc.micts.ui.theme.MiCTSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_FROM_TILE = "from_tile"
        private const val EXTRA_FORCE_LENS = "force_lens_fallback"
        private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
        private const val STATE_CAPTURE_REQUEST_IN_FLIGHT = "capture_request_in_flight"
        private const val STATE_CAPTURE_SERVICE_STARTED = "capture_service_started"

        fun createLensFallbackIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FORCE_LENS, true)
            }
    }

    private val coordinator = TriggerCoordinator()
    private val nativeGateway = AndroidNativeTriggerGateway()
    private val projectionConsent by lazy { ProjectionConsentStore(this) }
    private lateinit var triggerPreferences: TriggerPreferenceStore
    private var captureRequestInFlight = false
    private var captureServiceStarted = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // Best-effort: a denied notification permission must not block the
        // capture flow on Android 13+.
    }

    private val projectionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        captureRequestInFlight = false
        val resultData = result.data
        if (result.resultCode != Activity.RESULT_OK || resultData == null) {
            showCapturePermissionDenied()
            return@registerForActivityResult
        }

        projectionConsent.save(result.resultCode, resultData)
        armCapture(result.resultCode, resultData)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        triggerPreferences = TriggerPreferenceStore(this)
        captureRequestInFlight = savedInstanceState?.getBoolean(
            STATE_CAPTURE_REQUEST_IN_FLIGHT,
            false,
        ) ?: false
        captureServiceStarted = savedInstanceState?.getBoolean(
            STATE_CAPTURE_SERVICE_STARTED,
            false,
        ) ?: false
        if (captureRequestInFlight || captureServiceStarted) return

        val preferences = getSharedPreferences(CONFIG_NAME, MODE_PRIVATE)
        val delayKey = if (intent.getBooleanExtra(EXTRA_FROM_TILE, false)) {
            KEY_TILE_DELAY
        } else {
            KEY_DEFAULT_DELAY
        }
        val delayMs = preferences.getLong(delayKey, DEFAULT_CONFIG[delayKey] as Long)
        val vibrate = preferences.getBoolean(KEY_VIBRATE, DEFAULT_CONFIG[KEY_VIBRATE] as Boolean)
        val asyncTrigger = preferences.getBoolean(
            KEY_ASYNC_TRIGGER,
            DEFAULT_CONFIG[KEY_ASYNC_TRIGGER] as Boolean,
        )

        val triggerFlow: suspend () -> Unit = {
            if (delayMs > 0 && !intent.getBooleanExtra(EXTRA_FORCE_LENS, false)) {
                delay(delayMs)
            }
            if (BuildConfig.APP_NAME == "VISTrigger") {
                invokeVisTrigger(vibrate)
            } else if (intent.getBooleanExtra(EXTRA_FORCE_LENS, false)) {
                performAction(TriggerAction.RequestLensCapture, vibrate)
            } else {
                performAction(
                    coordinator.nextAction(
                        triggerPreferences.strategy,
                        triggerPreferences.autoResolution,
                    ),
                    vibrate,
                )
            }
        }
        if (asyncTrigger) {
            lifecycleScope.launch { triggerFlow() }
        } else {
            runBlocking { triggerFlow() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_CAPTURE_REQUEST_IN_FLIGHT, captureRequestInFlight)
        outState.putBoolean(STATE_CAPTURE_SERVICE_STARTED, captureServiceStarted)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        // The projection service launches CropActivity after it has saved one frame. Keeping this
        // transparent trampoline underneath would leave a blank task when the crop screen exits.
        if (captureServiceStarted && !isChangingConfigurations) finish()
    }

    private fun invokeVisTrigger(vibrate: Boolean) {
        val result = nativeGateway.invoke(1, this, vibrate)
        if (result != NativeTriggerResult.AcceptedUnverified) {
            Toast.makeText(this, R.string.trigger_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun performAction(action: TriggerAction, vibrate: Boolean) {
        when (action) {
            TriggerAction.InvokeNative -> {
                val strategy = triggerPreferences.strategy
                val currentResolution = triggerPreferences.autoResolution
                val result = nativeGateway.invoke(1, this, vibrate)
                if (strategy == TriggerStrategy.NATIVE_ONLY &&
                    result != NativeTriggerResult.AcceptedUnverified
                ) {
                    Toast.makeText(this, R.string.trigger_failed, Toast.LENGTH_SHORT).show()
                }
                val transition = coordinator.afterNative(strategy, currentResolution, result)
                if (transition.autoResolution != currentResolution) {
                    triggerPreferences.autoResolution = transition.autoResolution
                }
                performAction(transition.action, vibrate)
            }
            TriggerAction.RequestNativeConfirmation -> showNativeConfirmation(vibrate)
            TriggerAction.RequestLensCapture -> requestScreenCapture()
            TriggerAction.Finish -> finish()
        }
    }

    private fun showNativeConfirmation(vibrate: Boolean) {
        setContent {
            MiCTSTheme {
                NativeConfirmationDialog(
                    onDismiss = ::finish,
                    onNativeWorked = {
                        val transition = coordinator.afterConfirmation(nativeWorked = true)
                        triggerPreferences.autoResolution = transition.autoResolution
                        performAction(transition.action, vibrate)
                    },
                    onUseLensFallback = {
                        val transition = coordinator.afterConfirmation(nativeWorked = false)
                        triggerPreferences.autoResolution = transition.autoResolution
                        performAction(transition.action, vibrate)
                    },
                )
            }
        }
    }

    private fun showCapturePermissionDenied() {
        setContent {
            MiCTSTheme {
                CaptureProblem(
                    title = stringResource(R.string.capture_permission_denied_title),
                    message = stringResource(R.string.capture_permission_denied),
                    onRetake = ::requestScreenCapture,
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun requestScreenCapture() {
        maybeRequestNotificationsOnce()
        if (ScreenCaptureService.isArmed(this)) {
            // The projection is already live from an earlier approval: just
            // ask it to record one frame. No dialog, no re-prompt.
            triggerCapture()
            return
        }
        val storedConsent = projectionConsent.load()
        if (storedConsent != null) {
            // Consent approved earlier but the armed service died (process
            // killed / beyond boot). Re-arm silently from the stored token —
            // no new dialog — and capture immediately.
            startCaptureService(ScreenCaptureService.armIntent(this, storedConsent.first, storedConsent.second), captureAfterArm = true)
            return
        }
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val permissionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay(),
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        captureRequestInFlight = true
        projectionPermissionLauncher.launch(permissionIntent)
    }

    private fun armCapture(resultCode: Int, resultData: Intent) {
        startCaptureService(
            ScreenCaptureService.armIntent(this, resultCode, resultData),
            captureAfterArm = true,
        )
    }

    private fun triggerCapture() {
        captureRequestInFlight = false
        runCatching {
            startForegroundService(ScreenCaptureService.captureIntent(this))
        }.onSuccess {
            captureServiceStarted = true
        }.onFailure {
            showCaptureServiceStartFailed()
        }
    }

    private fun startCaptureService(intent: Intent, captureAfterArm: Boolean) {
        captureRequestInFlight = false
        runCatching {
            startForegroundService(intent)
        }.onSuccess {
            captureServiceStarted = true
            if (captureAfterArm) {
                // Arm and capture run back-to-back; the service captures after
                // its projection is ready so only one dialog (the initial
                // approval) is ever required.
                startForegroundService(ScreenCaptureService.captureIntent(this))
            }
        }.onFailure {
            showCaptureServiceStartFailed()
        }
    }

    private fun showCaptureServiceStartFailed() {
        startActivity(
            CropActivity.createIntent(
                this,
                probablyProtected = false,
                failureReason = CaptureFailureReason.SERVICE_START_FAILED,
            ),
        )
        finish()
    }

    private fun maybeRequestNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val preferences = getSharedPreferences(CONFIG_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_NOTIFICATIONS_ASKED, false)) return
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
internal fun NativeConfirmationDialog(
    onDismiss: () -> Unit,
    onNativeWorked: () -> Unit,
    onUseLensFallback: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.native_confirmation_title)) },
        text = { Text(stringResource(R.string.native_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onNativeWorked) {
                Text(stringResource(R.string.native_confirmation_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onUseLensFallback) {
                Text(stringResource(R.string.native_confirmation_no))
            }
        },
    )
}
