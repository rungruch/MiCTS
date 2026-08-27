package com.parallelc.micts.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.parallelc.micts.domain.CompatibilityReport
import com.parallelc.micts.domain.FastCaptureAvailability

class CompatibilityReportProvider(private val context: Context) {
    companion object {
        private const val CONTEXTUAL_SEARCH_ACTION =
            "android.app.contextualsearch.action.LAUNCH_CONTEXTUAL_SEARCH"
        private const val CONTEXTUAL_SEARCH_FEATURE =
            "com.google.android.feature.CONTEXTUAL_SEARCH"
        private const val CONTEXTUAL_SEARCH_SERVICE = "contextual_search"
    }

    fun create(selectedTriggerService: String): CompatibilityReport {
        val packageManager = context.packageManager
        val googlePackage = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    LensShareGateway.GOOGLE_APP_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(LensShareGateway.GOOGLE_APP_PACKAGE, 0)
            }
        }.getOrNull()
        val activeAssistant = Settings.Secure.getString(
            context.contentResolver,
            "voice_interaction_service",
        )
        val contextualIntent = Intent(CONTEXTUAL_SEARCH_ACTION).setPackage(
            LensShareGateway.GOOGLE_APP_PACKAGE,
        )
        val captureMode = CapturePreferenceStore(context).mode
        val fastCaptureAvailability = FastCaptureGatewayFactory.create(context).availability()

        return CompatibilityReport(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            googleAppInstalled = googlePackage != null,
            googleAppVersion = googlePackage?.versionName,
            activeAssistant = activeAssistant,
            googleIsDefaultAssistant = activeAssistant?.startsWith(
                LensShareGateway.GOOGLE_APP_PACKAGE,
            ) == true,
            contextualSearchActivityResolvable = contextualIntent.resolveActivity(
                packageManager,
            ) != null,
            contextualSearchFeatureDeclared = packageManager.hasSystemFeature(
                CONTEXTUAL_SEARCH_FEATURE,
            ),
            contextualSearchServiceAvailable = runCatching {
                context.getSystemService(CONTEXTUAL_SEARCH_SERVICE) != null
            }.getOrDefault(false),
            lensShareAvailable = LensShareGateway(context).canShareToGoogle(),
            selectedTriggerService = selectedTriggerService,
            captureMode = captureMode,
            fastCaptureApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            fastCaptureEnabled = fastCaptureAvailability ==
                FastCaptureAvailability.CONNECTING || fastCaptureAvailability ==
                FastCaptureAvailability.READY,
            fastCaptureConnected = fastCaptureAvailability == FastCaptureAvailability.READY,
        )
    }
}
