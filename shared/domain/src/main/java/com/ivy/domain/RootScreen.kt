package com.ivy.domain

import android.net.Uri
import com.ivy.domain.appicon.AppIcon

interface RootScreen {
    /**
     * BuildConfig.DEBUG
     */
    val isDebug: Boolean

    /**
     * BuildConfig.VERSION_NAME
     */
    val buildVersionName: String

    /**
     * BuildConfig.VERSION_CODE
     */
    val buildVersionCode: Int

    fun reviewIvyWallet(dismissReviewCard: Boolean)

    fun shareIvyWallet()

    /**
     * Opens an http(s) URL in an external app. Never throws: if no app can handle it,
     * the user is told and the URL is copied to the clipboard instead.
     */
    fun openUrlInBrowser(url: String)

    /**
     * Opens a `upi://pay` link in the user's UPI app as an explicit user action.
     * If no UPI app is installed, the payee's UPI ID is copied and the user is informed.
     */
    fun openUpiPayment(upiUri: String)

    fun copyToClipboard(label: String, text: String)

    fun shareCSVFile(fileUri: Uri)

    fun shareZipFile(fileUri: Uri)

    fun openGooglePlayAppPage(appId: String)

    fun <T> pinWidget(widget: Class<T>)

    /** The launcher icon that is currently enabled. */
    val currentAppIcon: AppIcon

    /**
     * Switches the launcher icon. Some launchers take a few seconds to refresh,
     * and may remove the old icon from the home screen.
     */
    fun setAppIcon(icon: AppIcon)
}
