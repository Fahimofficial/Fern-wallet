package com.ivy.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.ui.testing.PaparazziRenderTest
import java.awt.image.BufferedImage
import java.util.UUID
import org.junit.Test

/**
 * Plays a real navigation through [NavigationRoot] and looks at the drawn frames: a matching card
 * must grow into the destination, a non-matching one must leave the normal screen transition.
 */
class SharedContainerTransitionTest : PaparazziRenderTest() {
    private val tapped = UUID(1, 1)
    private val other = UUID(2, 2)

    @Test
    fun `opened transaction grows out of its own card`() {
        val frames = transition(cardKey = SharedKeys.transaction(tapped), destination = editTransaction(tapped))
        val first = frames.firstDestinationFrame()

        // Starts where the card is (card: 120 x 80 dp at 40, 400 on a 412 dp wide screen).
        check(first.width < first.screenWidth * GrowingMaxWidth) { "first frame $first" }
        check(first.left > first.screenWidth * CardLeftMin) { "first frame $first" }
        frames.last().destinationBounds().assertFullScreen()
    }

    @Test
    fun `opened account grows out of its own card`() {
        val frames = transition(
            cardKey = SharedKeys.account(tapped),
            destination = TransactionsScreen(accountId = tapped),
        )
        val first = frames.firstDestinationFrame()

        check(first.width < first.screenWidth * GrowingMaxWidth) { "first frame $first" }
        frames.last().destinationBounds().assertFullScreen()
    }

    @Test
    fun `a different transaction's card doesn't connect, the screen transition plays`() {
        assertScreenTransition(cardKey = SharedKeys.transaction(other), destination = editTransaction(tapped))
    }

    @Test
    fun `an account card doesn't connect to a transaction with the same id`() {
        assertScreenTransition(cardKey = SharedKeys.account(tapped), destination = editTransaction(tapped))
    }

    @Test
    fun `a new transaction has no card to grow from`() {
        assertScreenTransition(cardKey = SharedKeys.transaction(tapped), destination = editTransaction(null))
    }

    @Test
    fun `a category list has no account card to grow from`() {
        assertScreenTransition(
            cardKey = SharedKeys.account(tapped),
            destination = TransactionsScreen(categoryId = tapped),
        )
    }

    @Test
    fun `outside NavigationRoot the modifier does nothing`() {
        var unchanged = false
        render {
            unchanged = Modifier.sharedContainer(SharedKeys.transaction(tapped), RectangleShape) === Modifier
        }
        check(unchanged)
    }

    private fun assertScreenTransition(cardKey: Any, destination: Screen) {
        val frames = transition(cardKey, destination)
        val first = frames.firstDestinationFrame()

        // The whole destination screen fades in at once, not from the card.
        check(first.width > first.screenWidth * FullWidthMin) { "first frame $first" }
        frames.last().destinationBounds().assertFullScreen()
    }

    private fun editTransaction(id: UUID?) =
        EditTransactionScreen(initialTransactionId = id, type = TransactionType.EXPENSE)

    /** A list with one card for [cardKey], opening [destination] as soon as it is shown. */
    private fun transition(cardKey: Any, destination: Screen): List<BufferedImage> {
        val navigation = Navigation().apply { navigateTo(ListScreen) }
        return renderFrames(durationMillis = DurationMillis, fps = Fps) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                NavigationRoot(navigation) { screen ->
                    if (screen == ListScreen) {
                        Box(
                            Modifier
                                .offset(x = 40.dp, y = 400.dp)
                                .size(width = 120.dp, height = 80.dp)
                                .sharedContainer(cardKey, RectangleShape)
                                .background(CardColor)
                        )
                        LaunchedEffect(Unit) { navigation.navigateTo(destination) }
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .sharedContainer(screen?.destinationKey(), RectangleShape)
                                .background(DestinationColor)
                        )
                    }
                }
            }
        }
    }

    // The production keys of the two destinations that have a shared container.
    private fun Screen.destinationKey(): Any? = when (this) {
        is EditTransactionScreen -> sharedKey
        is TransactionsScreen -> sharedKey
        else -> null
    }

    private fun List<BufferedImage>.firstDestinationFrame(): Bounds =
        asSequence().map { it.destinationBounds() }.firstOrNull { it.width > 0 }
            ?: error("the destination never appeared in ${this.size} frames")

    private fun Bounds.assertFullScreen() = check(width > screenWidth * FullWidthMin) { "last frame $this" }

    private data class Bounds(val left: Int, val width: Int, val screenWidth: Int)

    // Where the destination (red, possibly fading in over black) is drawn.
    private fun BufferedImage.destinationBounds(): Bounds {
        var left = width
        var right = -1
        for (y in 0 until height step ScanStep) {
            for (x in 0 until width) {
                val rgb = getRGB(x, y)
                val r = rgb shr 16 and 0xFF
                val g = rgb shr 8 and 0xFF
                val b = rgb and 0xFF
                if (r >= RedMin && g < OtherMax && b < OtherMax) {
                    left = minOf(left, x)
                    right = maxOf(right, x)
                }
            }
        }
        return Bounds(left = left, width = if (right < 0) 0 else right - left + 1, screenWidth = width)
    }

    private companion object {
        // Any screen without its own shared element works as the list.
        val ListScreen: Screen = SettingsScreen
        val CardColor = Color(0xFF0000FF)
        val DestinationColor = Color(0xFFFF0000)
        const val DurationMillis = 600L
        const val Fps = 60
        const val ScanStep = 8
        const val RedMin = 48
        const val OtherMax = 32
        const val GrowingMaxWidth = 0.7f
        const val CardLeftMin = 0.03f
        const val FullWidthMin = 0.9f
    }
}
