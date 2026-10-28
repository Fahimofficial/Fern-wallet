package com.ivy.data.creditcard

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only catalogue of Indian banks and credit-card products, bundled with the app
 * (`resources/fern/card_catalogue.json`, see docs/Card-Catalogue.md).
 *
 * User cards only store catalogue *ids*; the catalogue never overwrites a user's card settings,
 * and a card whose product was removed from the catalogue keeps working (its saved
 * product name is shown instead).
 */
@Singleton
class CardCatalogue @Inject constructor() {
    private val data: CatalogueData by lazy { load() }

    val verifiedOn: String get() = data.verifiedOn
    val banks: List<CatalogueBank> get() = data.banks
    val products: List<CatalogueProduct> get() = data.products

    private val banksById by lazy { data.banks.associateBy { it.id } }
    private val productsById by lazy { data.products.associateBy { it.id } }

    fun bank(id: String?): CatalogueBank? = id?.let(banksById::get)

    fun product(id: String?): CatalogueProduct? = id?.let(productsById::get)

    fun productsOf(bankId: String, includeUnavailable: Boolean = false): List<CatalogueProduct> =
        data.products.filter { it.bankId == bankId && (includeUnavailable || it.available) }

    /** Banks that issue credit cards first, then the rest; filtered by name, alias or product. */
    fun searchBanks(query: String): List<CatalogueBank> {
        val q = normalize(query)
        val matches = if (q.isEmpty()) {
            data.banks
        } else {
            data.banks.filter { bank ->
                bank.searchTerms().any { normalize(it).contains(q) } ||
                    productsOf(bank.id).any { normalize(it.name).contains(q) }
            }
        }
        return matches.sortedWith(
            compareByDescending<CatalogueBank> { it.creditCardIssuer }
                .thenByDescending { q.isNotEmpty() && normalize(it.displayName).startsWith(q) }
                .thenBy { it.displayName }
        )
    }

    fun searchProducts(bankId: String, query: String): List<CatalogueProduct> {
        val q = normalize(query)
        return productsOf(bankId)
            .filter { product ->
                q.isEmpty() || normalize(product.name).contains(q) ||
                    product.coBrandPartner?.let { normalize(it).contains(q) } == true
            }
            .sortedBy { it.name }
    }

    private fun load(): CatalogueData {
        val stream = CardCatalogue::class.java.classLoader?.getResourceAsStream(RESOURCE)
            ?: return CatalogueData()
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(CatalogueData.serializer(), text)
    }

    companion object {
        const val RESOURCE = "fern/card_catalogue.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
    }
}

@Keep
@Serializable
@Suppress("DataClassDefaultValues") // optional JSON fields
data class CatalogueData(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    @SerialName("verifiedOn") val verifiedOn: String = "",
    @SerialName("sources") val sources: List<CatalogueSource> = emptyList(),
    @SerialName("banks") val banks: List<CatalogueBank> = emptyList(),
    @SerialName("products") val products: List<CatalogueProduct> = emptyList(),
)

@Keep
@Serializable
data class CatalogueSource(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("url") val url: String,
    @SerialName("accessed") val accessed: String,
)

@Keep
@Serializable
@Suppress("DataClassDefaultValues") // optional JSON fields
data class CatalogueBank(
    @SerialName("id") val id: String,
    /** Canonical (legal) name as listed by the RBI. */
    @SerialName("name") val name: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("aliases") val aliases: List<String> = emptyList(),
    /** PUBLIC_SECTOR, PRIVATE_SECTOR, SMALL_FINANCE, PAYMENTS, FOREIGN, ..., NBFC */
    @SerialName("category") val category: String,
    @SerialName("website") val website: String? = null,
    /** Listed in the RBI's directory of banks (status 1). */
    @SerialName("rbiListed") val rbiListed: Boolean = false,
    /** Reports credit cards outstanding in the RBI card statistics (status 2). */
    @SerialName("creditCardIssuer") val creditCardIssuer: Boolean = false,
    @SerialName("sourceIds") val sourceIds: List<String> = emptyList(),
    /** Optional bundled logo drawable name; only set for assets whose reuse is authorised. */
    @SerialName("logoAsset") val logoAsset: String? = null,
)

/** Every name a user might type to find this bank. */
fun CatalogueBank.searchTerms(): List<String> = listOf(displayName, name) + aliases

@Keep
@Serializable
@Suppress("DataClassDefaultValues") // optional JSON fields
data class CatalogueProduct(
    @SerialName("id") val id: String,
    @SerialName("bankId") val bankId: String,
    /** Official product name as published by the issuer. */
    @SerialName("name") val name: String,
    /** RUPAY, VISA, MASTERCARD, AMEX, DINERS, JCB – only when the issuer states it. */
    @SerialName("networks") val networks: List<String> = emptyList(),
    @SerialName("variant") val variant: String? = null,
    @SerialName("coBrandPartner") val coBrandPartner: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("verifiedOn") val verifiedOn: String? = null,
    /** Currently offered according to the issuer's site on [verifiedOn] (status 3). */
    @SerialName("available") val available: Boolean = true,
    /** Optional bundled artwork drawable; only for assets whose reuse is authorised. */
    @SerialName("artworkAsset") val artworkAsset: String? = null,
)
