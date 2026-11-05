package com.ivy.settings.appearance

import io.kotest.matchers.shouldBe
import org.junit.Test

class HexInputTest {
    @Test
    fun `keeps up to six hex digits in upper case and drops everything else`() {
        sanitizeHexInput("#13876a") shouldBe "13876A"
        sanitizeHexInput(" 13 87 6A ") shouldBe "13876A"
        sanitizeHexInput("ff00ff00ff") shouldBe "FF00FF"
        sanitizeHexInput("zz-12_g") shouldBe "12"
        sanitizeHexInput("") shouldBe ""
    }
}
