package com.ivy.base.links

/**
 * Single source of truth for Fern Wallet's own public project links.
 *
 * Derived from the canonical `origin` remote: https://github.com/SimplyFahimOfficial/Fern-wallet
 * Fern Wallet is a fork of Ivy Wallet (https://github.com/Ivy-Apps/ivy-wallet, GPL-3.0);
 * upstream links that are kept for attribution live in [UpstreamLinks].
 */
object FernLinks {
    const val GITHUB_OWNER = "SimplyFahimOfficial"
    const val GITHUB_REPO = "Fern-wallet"

    const val REPOSITORY = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val ISSUES = "$REPOSITORY/issues"
    const val NEW_ISSUE = "$REPOSITORY/issues/new/choose"
    const val RELEASES = "$REPOSITORY/releases"
    const val FORK = "$REPOSITORY/fork"
    const val ACTIONS = "$REPOSITORY/actions"
    const val LICENSE = "$REPOSITORY/blob/main/LICENSE"
    const val PRIVACY_POLICY = "$REPOSITORY/blob/main/PRIVACY.md"

    const val API_REPOSITORY = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO"
    const val API_CONTRIBUTORS = "$API_REPOSITORY/contributors"
    const val API_RELEASES = "$API_REPOSITORY/releases"
}

/** The upstream project Fern Wallet is derived from. Kept for attribution only. */
object UpstreamLinks {
    const val IVY_WALLET_REPOSITORY = "https://github.com/Ivy-Apps/ivy-wallet"
}

/**
 * Help & Support and "Support My Work" configuration: project help links, the developer's
 * contact page and payment details.
 *
 * This is the only place these values live. The Help & Support and Support My Work screens and
 * the UPI QR code all read from here, so updating a link is a one-line change.
 *
 * Nothing here is sent anywhere by the app: links open in apps the user picks, and the UPI QR
 * code is generated on the device.
 */
object SupportDestinations {
    const val DEVELOPER_NAME = "SimplyFahim"

    /** Fern Wallet's Telegram group: questions, feedback and help. */
    const val TELEGRAM_COMMUNITY = "https://t.me/Fernwallet"

    /** Public issue tracker: bug reports and feature requests. */
    const val REPORT_BUG = FernLinks.NEW_ISSUE

    /** Fern Wallet's page on the developer's website: project links, downloads and resources. */
    const val PROJECT_WEBSITE = "https://simplyfahim.pages.dev/fernwallet"

    /** The developer's contact page (website, email and social links). Kept off the app on purpose. */
    const val CONTACT_DEVELOPER = "https://simplyfahim.pages.dev/contactme"

    /** Every other way to pay. The website lists the providers, so the app needs no per-provider buttons. */
    const val MORE_WAYS_TO_PAY = "https://simplyfahim.pages.dev/pay-options"

    /** Public UPI ID (VPA) and the payee name UPI apps show before the user confirms. */
    const val UPI_ID = "SimplyFahim@sbi"
    const val UPI_PAYEE_NAME = "Md Fahim"
    const val UPI_CURRENCY = "INR"

    /**
     * Builds a standard UPI payment link that only identifies the payee:
     * `upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR`.
     *
     * No amount (`am`) is ever set: the user enters it and confirms in their own UPI app.
     * The same string is used for the "Pay with UPI" intent and as the QR code payload.
     * `@` is kept literal (it is valid in a URI query and is what bank-issued UPI QR codes use);
     * any other character outside the RFC 3986 unreserved set is percent-encoded.
     */
    fun upiPaymentUri(
        upiId: String = UPI_ID,
        payeeName: String = UPI_PAYEE_NAME,
        currency: String = UPI_CURRENCY,
    ): String = buildString {
        append("upi://pay?pa=")
        append(encodeQueryValue(upiId))
        append("&pn=")
        append(encodeQueryValue(payeeName))
        append("&cu=")
        append(encodeQueryValue(currency))
    }

    private val unescapedUriQueryValueCharacters: Set<Char> =
        (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~', '@')).toSet()
    private const val BYTE_MASK = 0xFF

    private fun encodeQueryValue(value: String): String = buildString {
        // RFC 3986 unreserved characters plus `@` stay literal; other bytes are percent-encoded.
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val unsigned = byte.toInt() and BYTE_MASK
            val c = unsigned.toChar()
            if (c in unescapedUriQueryValueCharacters) append(c) else append("%%%02X".format(unsigned))
        }
    }
}
