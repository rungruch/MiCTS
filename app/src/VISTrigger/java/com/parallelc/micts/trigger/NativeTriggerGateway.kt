package com.parallelc.micts.trigger

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.domain.NativeTriggerResult
import com.parallelc.micts.module
import org.lsposed.hiddenapibypass.HiddenApiBypass

private const val LOG_TAG = "NativeTrigger"

interface NativeTriggerGateway {
    fun invoke(entryPoint: Int, context: Context?, vibrate: Boolean): NativeTriggerResult
}

class AndroidNativeTriggerGateway : NativeTriggerGateway {
    @SuppressLint("PrivateApi")
    override fun invoke(
        entryPoint: Int,
        context: Context?,
        vibrate: Boolean,
    ): NativeTriggerResult {
        return runCatching {
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", entryPoint)
                putBoolean("micts_trigger", true)
            }
            val serviceClass = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService",
            )
            val serviceBinder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "voiceinteraction")
            val voiceService = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService\$Stub",
            ).getMethod("asInterface", IBinder::class.java).invoke(null, serviceBinder)

            val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HiddenApiBypass.invoke(
                    serviceClass,
                    voiceService,
                    "showSessionFromSession",
                    null,
                    bundle,
                    7,
                    "hyperOS_home",
                ) as Boolean
            } else {
                HiddenApiBypass.invoke(
                    serviceClass,
                    voiceService,
                    "showSessionFromSession",
                    null,
                    bundle,
                    7,
                ) as Boolean
            }

            if (accepted) {
                if (vibrate && context != null) vibrate(context)
                NativeTriggerResult.AcceptedUnverified
            } else {
                NativeTriggerResult.Rejected
            }
        }.onFailure { error ->
            val message = "Native trigger failed: ${error.stackTraceToString()}"
            module?.log(Log.ERROR, BuildConfig.APP_NAME, message)
                ?: Log.e(LOG_TAG, message)
        }.getOrElse { NativeTriggerResult.Error(it) }
    }

    private fun vibrate(context: Context) {
        runCatching {
            context.getSystemService(Vibrator::class.java).run {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setFlags(128)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        vibrate(
                            effect,
                            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrate(effect, attributes)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(longArrayOf(0, 1, 75, 76), -1, attributes)
                }
            }
        }.onFailure { error ->
            val message = "Trigger vibration failed: ${error.stackTraceToString()}"
            module?.log(Log.ERROR, BuildConfig.APP_NAME, message)
                ?: Log.e(LOG_TAG, message)
        }
    }
}

fun triggerCircleToSearch(entryPoint: Int, context: Context?, vibrate: Boolean): Boolean {
    return AndroidNativeTriggerGateway().invoke(entryPoint, context, vibrate) ==
        NativeTriggerResult.AcceptedUnverified
}
