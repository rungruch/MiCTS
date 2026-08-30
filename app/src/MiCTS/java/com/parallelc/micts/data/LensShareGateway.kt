package com.parallelc.micts.data

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.core.content.FileProvider
import com.parallelc.micts.BuildConfig
import java.io.File

class LensShareGateway(private val context: Context) {
    companion object {
        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun canShareToGoogle(): Boolean = createShareIntent(Uri.EMPTY).resolveActivity(
        context.packageManager,
    ) != null

    @SuppressLint("QueryPermissionsNeeded")
    fun share(image: File): Boolean {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            image,
        )
        val intent = createShareIntent(uri)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    internal fun createShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = CaptureEncoding.mimeType
        setPackage(GOOGLE_APP_PACKAGE)
        putExtra(Intent.EXTRA_STREAM, uri)
        if (uri != Uri.EMPTY) {
            clipData = ClipData.newUri(context.contentResolver, "MiCTS capture", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
