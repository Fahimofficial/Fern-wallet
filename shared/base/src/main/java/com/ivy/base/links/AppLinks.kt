package com.ivy.base.links

/**
 * Single source of truth for Fern Wallet's own public project links.
 *
 * Derived from the canonical `origin` remote: https://github.com/Fahimofficial/Fern-wallet
 * Fern Wallet is a fork of Ivy Wallet (https://github.com/Ivy-Apps/ivy-wallet, GPL-3.0);
 * upstream links that are kept for attribution live in [UpstreamLinks].
 */
object FernLinks {
    const val GITHUB_OWNER = "Fahimofficial"
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
 * Support destinations for the developer (Fahim), as published in the QuantumMPV README:
 * https://github.com/Fahimofficial/QuantumMPV
 *
 * These belong to the QuantumMPV project, not to Fern Wallet. They are presented in the
 * Support section of Settings with that ownership stated explicitly.
 */
object SupportDestinations {
    const val PROJECT_NAME = "QuantumMPV"
    const val DEVELOPER_NAME = "SimplyFahim"

    const val WEBSITE = "https://quantummpv.vercel.app/"
    const val REPOSITORY = "https://github.com/Fahimofficial/QuantumMPV"
    const val ISSUES = "https://github.com/Fahimofficial/QuantumMPV/issues"

    /** Public UPI ID (VPA) documented in the QuantumMPV README. */
    const val UPI_ID = "SimplyFahim@Sbi"
    const val UPI_CURRENCY = "INR"

    /**
     * Builds a standard UPI deep link (`upi://pay`) that only pre-fills the payee.
     * No amount is set: the user chooses it and confirms in their own UPI app.
     * Matches the link published in the QuantumMPV README:
     * `upi://pay?pa=SimplyFahim%40Sbi&pn=SimplyFahim&cu=INR`
     */
    fun upiPaymentUri(
        upiId: String = UPI_ID,
        payeeName: String = DEVELOPER_NAME,
        currency: String = UPI_CURRENCY,
    ): String = buildString {
        append("upi://pay?pa=")
        append(encodeQueryValue(upiId))
        append("&pn=")
        append(encodeQueryValue(payeeName))
        append("&cu=")
        append(encodeQueryValue(currency))
    }

    private fun encodeQueryValue(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
