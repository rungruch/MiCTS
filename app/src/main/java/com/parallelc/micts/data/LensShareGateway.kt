package com.parallelc.micts.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.parallelc.micts.BuildConfig
import java.io.File

class LensShareGateway(private val context: Context) {
    companion object {
        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    }

    fun canShareToGoogle(): Boolean = createShareIntent(Uri.EMPTY).resolveActivity(
        context.packageManager,
    ) != null

    fun share(image: File): Boolean {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            image,
        )
        val intent = createShareIntent(uri)
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        return true
    }

    private fun createShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        setPackage(GOOGLE_APP_PACKAGE)
        putExtra(Intent.EXTRA_STREAM, uri)
        if (uri != Uri.EMPTY) {
            clipData = ClipData.newUri(context.contentResolver, "MiCTS capture", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
