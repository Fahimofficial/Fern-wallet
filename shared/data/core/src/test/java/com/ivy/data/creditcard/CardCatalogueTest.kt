package com.ivy.data.creditcard

import com.ivy.data.model.creditcard.CardNetwork
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

/** Integrity checks for the bundled `fern/card_catalogue.json`. */
class CardCatalogueTest {

    private val catalogue = CardCatalogue()

    @Test
    fun `catalogue loads with banks, products and a verification date`() {
        catalogue.banks.shouldNotBeEmpty()
        catalogue.products.shouldNotBeEmpty()
        catalogue.verifiedOn.isNotBlank() shouldBe true
    }

    @Test
    fun `bank and product ids are unique`() {
        catalogue.banks.groupBy { it.id }.filterValues { it.size > 1 }.keys.shouldBeEmpty()
        catalogue.products.groupBy { it.id }.filterValues { it.size > 1 }.keys.shouldBeEmpty()
    }

    @Test
    fun `every product belongs to a bank that issues credit cards`() {
        val issuers = catalogue.banks.filter { it.creditCardIssuer }.map { it.id }.toSet()
        catalogue.products.filter { it.bankId !in issuers }.map { it.id }.shouldBeEmpty()
    }

    @Test
    fun `every product network maps to a known card network`() {
        val known = CardNetwork.entries.map { it.name }.toSet()
        catalogue.products.flatMap { it.networks }.filter { it !in known }.shouldBeEmpty()
    }

    @Test
    fun `no artwork or logo is bundled without an authorised asset`() {
        // Bank and network logos are trademarks; none are shipped until reuse is authorised.
        catalogue.banks.mapNotNull { it.logoAsset }.shouldBeEmpty()
        catalogue.products.mapNotNull { it.artworkAsset }.shouldBeEmpty()
    }

    @Test
    fun `search finds a bank by alias, ignoring case and punctuation`() {
        catalogue.searchBanks("hdfc").first().id shouldBe "hdfc-bank"
        catalogue.searchBanks("  HDFC-Bank ").firstOrNull()?.id shouldBe "hdfc-bank"
        catalogue.searchBanks("zzzz-no-such-bank").shouldBeEmpty()
    }

    @Test
    fun `discontinued products are hidden unless requested`() {
        val bankId = catalogue.products.first().bankId
        val all = catalogue.productsOf(bankId, includeUnavailable = true)
        catalogue.productsOf(bankId).all { it.available } shouldBe true
        (all.size >= catalogue.productsOf(bankId).size) shouldBe true
    }

    @Test
    fun `unknown ids resolve to null instead of failing`() {
        catalogue.bank("not-a-bank") shouldBe null
        catalogue.product(null) shouldBe null
    }
}
