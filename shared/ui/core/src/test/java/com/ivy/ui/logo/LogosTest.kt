package com.ivy.ui.logo

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.Test

class LogosTest {
    @Test
    fun `ids are unique and the initials key isn't a logo`() {
        Logos.all.map { it.id }.toSet().size shouldBe Logos.all.size
        Logos.find(Logos.InitialsKey) shouldBe null
    }

    @Test
    fun `search matches names and aliases, ignoring case and spaces`() {
        Logos.search("kotak 811").map { it.id } shouldContain "kotak"
        Logos.search("INDIE").map { it.id } shouldContain "indusind"
        Logos.search("amex").map { it.id } shouldContain "americanexpress"
        Logos.search("gpay").map { it.id } shouldContain "googlepay"
    }

    @Test
    fun `category narrows the results`() {
        val cards = Logos.search("", LogoCategory.Cards).map { it.id }
        cards shouldContain "visa"
        cards shouldNotContain "hdfcbank"
    }

    @Test
    fun `initials take the significant words`() {
        initialsOf("Bank of Baroda") shouldBe "BB"
        initialsOf("IDFC FIRST Bank") shouldBe "IF"
        initialsOf("slice") shouldBe "S"
        initialsOf("811") shouldBe "8"
        initialsOf("  ") shouldBe "?"
    }
}
