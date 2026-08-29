package com.parallelc.micts.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parallelc.micts.R
import com.parallelc.micts.data.CaptureFiles
import com.parallelc.micts.data.LensShareGateway
import com.parallelc.micts.domain.CaptureFailureReason
import com.parallelc.micts.ui.theme.MiCTSTheme

/**
 * Minimal Lens fallback for the standalone build.
 *
 * The activity exists as a foreground trampoline because Android does not
 * allow the capture service to launch another app directly. It never decodes
 * or edits the captured image: the complete temporary JPEG is handed to Lens.
 */
class LensFallbackActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_PROBABLY_PROTECTED = "probably_protected"
        private const val EXTRA_FAILURE_REASON = "failure_reason"

        fun createIntent(
            context: android.content.Context,
            probablyProtected: Boolean,
            failureReason: CaptureFailureReason?,
            autoLens: Boolean = false,
        ): Intent = Intent(context, LensFallbackActivity::class.java).apply {
            putExtra(EXTRA_PROBABLY_PROTECTED, probablyProtected)
            failureReason?.let { putExtra(EXTRA_FAILURE_REASON, it.name) }
        }
    }

    private var lensUnavailable by mutableStateOf(false)
    private var lensAttempted = false

    private val failureReason: CaptureFailureReason?
        get() = intent.getStringExtra(EXTRA_FAILURE_REASON)
            ?.let { value -> CaptureFailureReason.entries.firstOrNull { it.name == value } }

    private val probablyProtected: Boolean
        get() = intent.getBooleanExtra(EXTRA_PROBABLY_PROTECTED, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()
        setContent {
            MiCTSTheme {
                val failure = failureReason
                when {
                    failure != null -> LensCaptureProblem(
                        title = stringResource(R.string.capture_failed_title),
                        message = stringResource(captureFailureMessage(failure)),
                        onRetake = ::retake,
                        onCancel = ::cancel,
                    )
                    probablyProtected -> LensCaptureProblem(
                        title = stringResource(R.string.protected_capture_title),
                        message = stringResource(R.string.protected_capture_message),
                        onRetake = ::retake,
                        onCancel = ::cancel,
                    )
                    else -> LensLoadingScreen(onCancel = ::cancel)
                }

                if (lensUnavailable) {
                    LensUnavailableDialog(
                        onDismiss = ::cancel,
                        onOpenGoogleStore = {
                            lensUnavailable = false
                            lensAttempted = false
                            openGoogleStore()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (failureReason == null && !probablyProtected && !lensAttempted) {
            lensAttempted = true
            requestLens()
        }
    }

    private fun requestLens() {
        val capture = CaptureFiles.capture(this)
        val lens = LensShareGateway(this)
        if (capture.isFile && lens.canShareToGoogle() && lens.share(capture)) {
            finish()
        } else {
            lensUnavailable = true
        }
    }

    private fun retake() {
        CaptureFiles.deleteCapture(this)
        startActivity(MainActivity.createLensFallbackIntent(this))
        finish()
    }

    private fun cancel() {
        CaptureFiles.deleteCapture(this)
        finish()
    }

    private fun openGoogleStore() {
        val packageName = LensShareGateway.GOOGLE_APP_PACKAGE
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun LensLoadingScreen(onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.opening_lens))
                TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun LensCaptureProblem(
    title: String,
    message: String,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                message,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetake, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Text(stringResource(R.string.retake))
            }
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        }
    }
}

@androidx.compose.runtime.Composable
internal fun LensUnavailableDialog(
    onDismiss: () -> Unit,
    onOpenGoogleStore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lens_unavailable_title)) },
        text = { Text(stringResource(R.string.lens_unavailable_message)) },
        confirmButton = {
            TextButton(onClick = onOpenGoogleStore) {
                Text(stringResource(R.string.open_google_app_store))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private fun captureFailureMessage(reason: CaptureFailureReason): Int = when (reason) {
    CaptureFailureReason.INVALID_PERMISSION_RESULT -> R.string.capture_permission_invalid
    CaptureFailureReason.SERVICE_START_FAILED -> R.string.capture_service_failed
    CaptureFailureReason.PROJECTION_STOPPED -> R.string.capture_projection_stopped
    CaptureFailureReason.CONSENT_EXPIRED -> R.string.capture_consent_expired
    CaptureFailureReason.TIMED_OUT -> R.string.capture_timed_out
    CaptureFailureReason.EMPTY_IMAGE -> R.string.capture_empty_image
    CaptureFailureReason.WRITE_FAILED -> R.string.capture_write_failed
    CaptureFailureReason.UNKNOWN -> R.string.capture_unknown_error
}
