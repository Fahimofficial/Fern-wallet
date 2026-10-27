package com.ivy.base.links

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class SupportDestinationsTest {

    @Test
    fun `UPI link matches the one published in the QuantumMPV README`() {
        SupportDestinations.upiPaymentUri() shouldBe
            "upi://pay?pa=SimplyFahim%40Sbi&pn=SimplyFahim&cu=INR"
    }

    @Test
    fun `UPI link never pre-fills an amount`() {
        SupportDestinations.upiPaymentUri() shouldNotContain "am="
    }

    @Test
    fun `UPI link encodes payee values`() {
        SupportDestinations.upiPaymentUri(upiId = "a&b@bank", payeeName = "Jane Doe") shouldBe
            "upi://pay?pa=a%26b%40bank&pn=Jane%20Doe&cu=INR"
    }

    @Test
    fun `support destinations are the verified QuantumMPV links`() {
        SupportDestinations.UPI_ID shouldBe "SimplyFahim@Sbi"
        SupportDestinations.WEBSITE shouldBe "https://quantummpv.vercel.app/"
        SupportDestinations.REPOSITORY shouldBe "https://github.com/Fahimofficial/QuantumMPV"
        SupportDestinations.ISSUES shouldBe "https://github.com/Fahimofficial/QuantumMPV/issues"
    }

    @Test
    fun `Fern Wallet links point to its own repository, not QuantumMPV or upstream`() {
        FernLinks.REPOSITORY shouldBe "https://github.com/Fahimofficial/Fern-wallet"
        FernLinks.NEW_ISSUE shouldStartWith FernLinks.REPOSITORY
        FernLinks.API_RELEASES shouldBe
            "https://api.github.com/repos/Fahimofficial/Fern-wallet/releases"
        FernLinks.REPOSITORY shouldNotContain "QuantumMPV"
        FernLinks.REPOSITORY shouldNotContain "Ivy-Apps"
    }
}
