package com.parallelc.micts.data

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

enum class ExternalActionKind {
    SEARCH,
    TRANSLATE,
}

sealed interface ExternalActionResult {
    data object Copied : ExternalActionResult
    data object Launched : ExternalActionResult
    data class BrowserFallback(
        val kind: ExternalActionKind,
        val uri: Uri,
    ) : ExternalActionResult
    data class Unavailable(val kind: ExternalActionKind) : ExternalActionResult
}

interface ExternalActionGateway {
    fun copy(text: String): ExternalActionResult
    fun search(text: String): ExternalActionResult
    fun translate(text: String): ExternalActionResult
    fun openBrowser(uri: Uri): Boolean
}

class AndroidExternalActionGateway(private val context: Context) : ExternalActionGateway {
    companion object {
        private const val ACTION_TRANSLATE = "android.intent.action.TRANSLATE"
    }

    override fun copy(text: String): ExternalActionResult {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("MiCTS selected text", text))
        return ExternalActionResult.Copied
    }

    override fun search(text: String): ExternalActionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text)
        return if (launch(intent)) {
            ExternalActionResult.Launched
        } else {
            ExternalActionResult.BrowserFallback(
                ExternalActionKind.SEARCH,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(text)}"),
            )
        }
    }

    override fun translate(text: String): ExternalActionResult {
        val intent = Intent(ACTION_TRANSLATE).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return if (launch(intent)) {
            ExternalActionResult.Launched
        } else {
            val targetLanguage = Locale.getDefault().language
                .takeIf { it.matches(Regex("[A-Za-z]{2,8}")) }
                ?: "en"
            ExternalActionResult.BrowserFallback(
                ExternalActionKind.TRANSLATE,
                Uri.parse(
                    "https://translate.google.com/?sl=auto&tl=$targetLanguage&text=${Uri.encode(text)}",
                ),
            )
        }
    }

    override fun openBrowser(uri: Uri): Boolean = launch(
        Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE),
    )

    private fun launch(intent: Intent): Boolean {
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
}
