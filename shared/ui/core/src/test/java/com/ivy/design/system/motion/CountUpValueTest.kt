package com.ivy.design.system.motion

import io.kotest.matchers.shouldBe
import org.junit.Test

class CountUpValueTest {
    @Test
    fun `starts at from, ends exactly at to, interpolates between`() {
        countUpValue(from = 0.0, to = 200.0, progress = 0f) shouldBe 0.0
        countUpValue(from = 0.0, to = 200.0, progress = 0.5f) shouldBe 100.0
        // Exact at the end, without floating-point drift (0.1 + (0.3 - 0.1) != 0.3).
        countUpValue(from = 0.1, to = 0.3, progress = 1f) shouldBe 0.3
    }

    @Test
    fun `counts down too`() {
        countUpValue(from = 500.0, to = -500.0, progress = 0.5f) shouldBe 0.0
    }
}
