package com.ivy.settings.support

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ivy.base.links.SupportDestinations
import com.ivy.ui.R
import timber.log.Timber

/**
 * Opens an `https:` link (ACTION_VIEW) or a `mailto:` link (ACTION_SENDTO) from the Support screen.
 * Never throws: if no app can handle it, [copyValue] is copied and the user is told.
 */
internal fun openLink(context: Context, uri: String, copyValue: String) {
    val parsed = Uri.parse(uri)
    val intent = when (parsed.scheme) {
        "https" -> Intent(Intent.ACTION_VIEW, parsed).addCategory(Intent.CATEGORY_BROWSABLE)
        // Some mail apps ignore `?subject=` in the URI and only read the extra.
        "mailto" -> Intent(Intent.ACTION_SENDTO, parsed)
            .putExtra(Intent.EXTRA_SUBJECT, SupportDestinations.EMAIL_SUBJECT)
        else -> {
            Timber.w("Refusing to open unsupported link: %s", uri)
            return
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app can open link")
        copyText(context, copyValue)
        Toast.makeText(context, context.getString(R.string.no_app_to_open_link, copyValue), Toast.LENGTH_LONG)
            .show()
    }
}

private fun copyText(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(text, text))
}
