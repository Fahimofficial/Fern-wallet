package com.ivy.design.system

import androidx.compose.animation.core.Spring
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

class FernMotionTest {
    @After
    fun reset() {
        FernMotion.reduceMotion = false
    }

    @Test
    fun `tween uses the given duration`() {
        FernMotion.tween<Float>(FernMotion.Duration.Emphasized).durationMillis shouldBe 350
    }

    @Test
    fun `reduced motion makes tweens instant and springs non-bouncy`() {
        FernMotion.reduceMotion = true

        FernMotion.tween<Float>(FernMotion.Duration.Emphasized).durationMillis shouldBe 0
        FernMotion.spring<Float>().dampingRatio shouldBe Spring.DampingRatioNoBouncy
    }

    @Test
    fun `springs have a small bounce unless asked not to`() {
        FernMotion.spring<Float>().dampingRatio shouldBe 0.85f
        FernMotion.spring<Float>(bouncy = false).dampingRatio shouldBe Spring.DampingRatioNoBouncy
    }
}
