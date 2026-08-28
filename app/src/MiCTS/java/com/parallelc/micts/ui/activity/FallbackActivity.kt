package com.parallelc.micts.ui.activity

import android.content.Context
import android.content.Intent
import com.parallelc.micts.domain.CaptureFailureReason

object FallbackActivity {
    fun createIntent(
        context: Context,
        probablyProtected: Boolean,
        failureReason: CaptureFailureReason?,
        autoLens: Boolean = false,
    ): Intent = LensFallbackActivity.createIntent(
        context,
        probablyProtected,
        failureReason,
        autoLens,
    )
}
