package com.parallelc.micts.trigger

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.parallelc.micts.domain.NativeTriggerResult
import org.lsposed.hiddenapibypass.HiddenApiBypass

private const val LOG_TAG = "NativeTrigger"

data class NativeTriggerPayload(
    val invocationTimeMs: Long,
    val omniEntryPoint: Int? = null,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putLong("invocation_time_ms", invocationTimeMs)
        if (omniEntryPoint != null && omniEntryPoint > 0) {
            putInt("omni.entry_point", omniEntryPoint)
        }
    }
}

internal fun buildSessionPayload(
    entryPoint: Int,
    invocationTimeMs: Long = SystemClock.elapsedRealtime(),
): NativeTriggerPayload = NativeTriggerPayload(
    invocationTimeMs = invocationTimeMs,
    omniEntryPoint = if (entryPoint > 0) entryPoint else null,
)

interface NativeTriggerGateway {
    fun invoke(entryPoint: Int, context: Context?, vibrate: Boolean): NativeTriggerResult
}

class AndroidNativeTriggerGateway : NativeTriggerGateway {
    @SuppressLint("PrivateApi")
    override fun invoke(
        entryPoint: Int,
        context: Context?,
        vibrate: Boolean,
    ): NativeTriggerResult = runCatching {
        val bundle = buildSessionPayload(entryPoint).toBundle()
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

        Log.i(LOG_TAG, "Native trigger result: entryPoint=$entryPoint, accepted=$accepted")

        if (accepted) {
            if (vibrate && context != null) vibrate(context)
            NativeTriggerResult.AcceptedUnverified
        } else if (entryPoint == 0 && context != null) {
            val fallbackTriggered = runCatching {
                val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(voiceIntent)
                true
            }.getOrDefault(false)

            if (fallbackTriggered) {
                if (vibrate) vibrate(context)
                NativeTriggerResult.AcceptedUnverified
            } else {
                NativeTriggerResult.Rejected
            }
        } else {
            NativeTriggerResult.Rejected
        }
    }.onFailure { error ->
        Log.e(LOG_TAG, "Native trigger failed", error)
    }.recoverCatching { error ->
        if (entryPoint == 0 && context != null) {
            val fallbackTriggered = runCatching {
                val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(voiceIntent)
                true
            }.getOrDefault(false)

            if (fallbackTriggered) {
                if (vibrate) vibrate(context)
                return@recoverCatching NativeTriggerResult.AcceptedUnverified
            }
        }
        throw error
    }.getOrElse { NativeTriggerResult.Error(it) }

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
        }.onFailure { error -> Log.e(LOG_TAG, "Trigger vibration failed", error) }
    }
}
