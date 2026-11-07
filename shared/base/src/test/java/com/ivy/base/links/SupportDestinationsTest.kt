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
        SupportDestinations.KOFI_URL shouldBe "https://ko-fi.com/simplyfahim"
        SupportDestinations.WEBSITE shouldBe "https://SimplyFahim.pages.dev"
        SupportDestinations.GITHUB_PROFILE shouldBe "https://github.com/SimplyFahimOfficial/"
    }

    @Test
    fun `support screen links to Fern Wallet's own repository`() {
        SupportDestinations.APP_REPOSITORY shouldBe "https://github.com/SimplyFahimOfficial/Fern-wallet"
        SupportDestinations.APP_REPOSITORY shouldNotContain "QuantumMPV"
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
