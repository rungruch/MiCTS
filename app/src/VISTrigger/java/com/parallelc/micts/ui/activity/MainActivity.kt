package com.parallelc.micts.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.MainApplication
import com.parallelc.micts.R
import com.parallelc.micts.capture.ScreenCaptureService
import com.parallelc.micts.config.AppConfig.CONFIG_NAME
import com.parallelc.micts.config.AppConfig.DEFAULT_CONFIG
import com.parallelc.micts.config.AppConfig.KEY_ASYNC_TRIGGER
import com.parallelc.micts.config.AppConfig.KEY_DEFAULT_DELAY
import com.parallelc.micts.config.AppConfig.KEY_TILE_DELAY
import com.parallelc.micts.config.AppConfig.KEY_VIBRATE
import com.parallelc.micts.data.CapturePreferenceStore
import com.parallelc.micts.data.ProjectionConsentStore
import com.parallelc.micts.data.TriggerPreferenceStore
import com.parallelc.micts.domain.CaptureMode
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.domain.CapturePermissionAction
import com.parallelc.micts.domain.CapturePermissionCoordinator
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
        private const val STATE_CAPTURE_REQUEST_IN_FLIGHT = "capture_request_in_flight"
        private const val STATE_CAPTURE_SERVICE_STARTED = "capture_service_started"

        fun createLensFallbackIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FORCE_LENS, true)
            }
    }

    private val coordinator = TriggerCoordinator()
    private val capturePermissionCoordinator = CapturePermissionCoordinator()
    private val nativeGateway = AndroidNativeTriggerGateway()
    private lateinit var triggerPreferences: TriggerPreferenceStore
    private lateinit var capturePreferences: CapturePreferenceStore
    private lateinit var projectionConsent: ProjectionConsentStore
    private var captureRequestInFlight = false
    private var captureServiceStarted = false

    private val projectionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        captureRequestInFlight = false
        val resultData = result.data
        if (result.resultCode != Activity.RESULT_OK || resultData == null) {
            showCapturePermissionDenied()
            return@registerForActivityResult
        }

        // Android 13 and below allow reusing this approval for later one-shot
        // captures. Android 14+ tokens are single-use; nothing is stored.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            capturePreferences.mode == CaptureMode.REMEMBER_CONSENT
        ) {
            projectionConsent.save(result.resultCode, resultData)
        }
        startCaptureService(
            ScreenCaptureService.createIntent(
                applicationContext,
                result.resultCode,
                resultData,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        triggerPreferences = TriggerPreferenceStore(this)
        capturePreferences = CapturePreferenceStore(this)
        projectionConsent = ProjectionConsentStore(this)
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
            TriggerAction.RequestLensCapture -> routeCapturePermission()
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
                    onRetake = ::requestMediaProjection,
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun routeCapturePermission() {
        when (
            capturePermissionCoordinator.nextAction(
                apiLevel = Build.VERSION.SDK_INT,
                mode = capturePreferences.mode,
                consentStored = projectionConsent.load() != null,
                explanationSeen = capturePreferences.consentExplanationSeen,
            )
        ) {
            CapturePermissionAction.ShowCaptureSetup -> showCaptureSetup()
            CapturePermissionAction.ShowConsentExplanation -> showConsentExplanation()
            CapturePermissionAction.CaptureWithStoredConsent -> captureWithStoredConsent()
            CapturePermissionAction.RequestMediaProjection -> requestMediaProjection()
        }
    }

    private fun requestMediaProjection() {
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

    private fun showCaptureSetup() {
        setContent {
            MiCTSTheme {
                CaptureSetupScreen(
                    onApproveOnce = {
                        capturePreferences.mode = CaptureMode.REMEMBER_CONSENT
                        requestMediaProjection()
                    },
                    onAskEveryTime = {
                        capturePreferences.mode = CaptureMode.ASK_EVERY_TIME
                        requestMediaProjection()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun showConsentExplanation() {
        setContent {
            MiCTSTheme {
                ConsentExplanationScreen(
                    onContinue = {
                        capturePreferences.consentExplanationSeen = true
                        capturePreferences.mode = CaptureMode.ASK_EVERY_TIME
                        requestMediaProjection()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun captureWithStoredConsent() {
        val consent = projectionConsent.load()
        if (consent == null) {
            requestMediaProjection()
            return
        }
        startCaptureService(
            ScreenCaptureService.createIntent(
                applicationContext,
                consent.first,
                consent.second,
                fromStoredConsent = true,
            ),
        )
    }

    private fun startCaptureService(serviceIntent: Intent) {
        (application as MainApplication).captureScope.launch {
            clearPermissionUiForCapture()
            runCatching {
                applicationContext.startForegroundService(serviceIntent)
            }.onSuccess {
                captureServiceStarted = true
            }.onFailure {
                launchCaptureFailure(CaptureFailureReason.SERVICE_START_FAILED)
            }
        }
    }

    private fun launchCaptureFailure(reason: CaptureFailureReason) {
        applicationContext.startActivity(
            CropActivity.createIntent(
                this,
                probablyProtected = false,
                failureReason = reason,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    private suspend fun clearPermissionUiForCapture() {
        setContent {}
        // Clear any visible Compose buffers from the transparent trampoline
        // before the capture service records a frame.
        window.attributes = window.attributes.apply { alpha = 0f }
        delay(150L)
    }
}

@Composable
internal fun CaptureSetupScreen(
    onApproveOnce: () -> Unit,
    onAskEveryTime: () -> Unit,
    onCancel: () -> Unit,
) {
    CapturePermissionScreen(
        title = stringResource(R.string.capture_setup_title),
        message = stringResource(R.string.capture_setup_message),
        disclosure = stringResource(R.string.capture_setup_privacy_disclosure),
        primaryLabel = stringResource(R.string.approve_once),
        onPrimary = onApproveOnce,
        secondaryLabel = stringResource(R.string.ask_every_time_instead),
        onSecondary = onAskEveryTime,
        onCancel = onCancel,
    )
}

@Composable
internal fun ConsentExplanationScreen(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    CapturePermissionScreen(
        title = stringResource(R.string.consent_explanation_title),
        message = stringResource(R.string.consent_explanation_message),
        disclosure = stringResource(R.string.ask_every_time_privacy_disclosure),
        primaryLabel = stringResource(R.string.continue_label),
        onPrimary = onContinue,
        onCancel = onCancel,
    )
}

@Composable
private fun CapturePermissionScreen(
    title: String,
    message: String,
    disclosure: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                    Text(text = message, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = disclosure,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = onPrimary,
                    ) {
                        Text(primaryLabel)
                    }
                    if (secondaryLabel != null && onSecondary != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = onSecondary,
                        ) {
                            Text(secondaryLabel)
                        }
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                        onClick = onCancel,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
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
