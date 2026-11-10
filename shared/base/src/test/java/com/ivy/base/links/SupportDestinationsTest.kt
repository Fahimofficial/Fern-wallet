package com.ivy.base.links

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class SupportDestinationsTest {

    @Test
    fun `UPI link identifies the payee only`() {
        SupportDestinations.upiPaymentUri() shouldBe
            "upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR"
    }

    @Test
    fun `UPI link never pre-fills an amount`() {
        SupportDestinations.upiPaymentUri() shouldNotContain "am="
    }

    @Test
    fun `UPI link encodes payee values`() {
        SupportDestinations.upiPaymentUri(upiId = "a&b@bank", payeeName = "Jane Doe") shouldBe
            "upi://pay?pa=a%26b@bank&pn=Jane%20Doe&cu=INR"
    }

    @Test
    fun `support details are the developer's own`() {
        SupportDestinations.UPI_ID shouldBe "SimplyFahim@sbi"
        SupportDestinations.UPI_PAYEE_NAME shouldBe "Md Fahim"
        SupportDestinations.UPI_CURRENCY shouldBe "INR"
    }

    @Test
    fun `help and support links are Fern Wallet's own`() {
        SupportDestinations.TELEGRAM_COMMUNITY shouldBe "https://t.me/Fernwallet"
        SupportDestinations.REPORT_BUG shouldBe "https://github.com/SimplyFahimOfficial/Fern-wallet/issues/new/choose"
        SupportDestinations.PROJECT_WEBSITE shouldBe "https://simplyfahim.pages.dev/fernwallet"
        SupportDestinations.CONTACT_DEVELOPER shouldBe "https://simplyfahim.pages.dev/contactme"
        SupportDestinations.MORE_WAYS_TO_PAY shouldBe "https://simplyfahim.pages.dev/pay-options"
    }

    @Test
    fun `Fern Wallet links point to its own repository, not QuantumMPV or upstream`() {
        FernLinks.REPOSITORY shouldBe "https://github.com/SimplyFahimOfficial/Fern-wallet"
        FernLinks.NEW_ISSUE shouldStartWith FernLinks.REPOSITORY
        FernLinks.API_RELEASES shouldBe
            "https://api.github.com/repos/SimplyFahimOfficial/Fern-wallet/releases"
        FernLinks.REPOSITORY shouldNotContain "QuantumMPV"
        FernLinks.REPOSITORY shouldNotContain "Ivy-Apps"
    }
}
