package com.ivy.legacy.ui.component.transaction

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import io.kotest.matchers.shouldBe
import org.junit.Test

class HighlightMatchesTest {
    private val mark = SpanStyle(fontWeight = FontWeight.Black)

    @Test
    fun `marks every case-insensitive match`() {
        val result = highlightMatches("Coffee and coffee beans", "COFFEE", mark)
        result.text shouldBe "Coffee and coffee beans"
        result.spanStyles.map { it.start to it.end } shouldBe listOf(0 to 6, 11 to 17)
    }

    @Test
    fun `blank or missing query marks nothing`() {
        highlightMatches("Rent", "  ", mark).spanStyles shouldBe emptyList()
        highlightMatches("Rent", null, mark).spanStyles shouldBe emptyList()
    }
}
