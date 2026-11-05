package com.ivy.wallet.ui.theme.components

import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.MotionPreferences
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB2Row
import kotlin.math.abs

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BufferBattery(
    modifier: Modifier = Modifier,
    buffer: Double,
    balance: Double,
    currency: String,
    backgroundNotFilled: Color = UI.colors.pure,
    animateIn: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bufferExceeded = balance < buffer

    val leftToSpend = balance - buffer
    val bufferExceededPercent = if (balance != 0.0) {
        (balance - leftToSpend) / balance
    } else {
        1.0
    }

    val textColor = when {
        bufferExceededPercent <= 0.25 -> {
            UI.colors.pureInverse
        }
        bufferExceededPercent <= 0.50 -> {
            White
        }
        bufferExceededPercent <= 0.75 -> {
            White
        }
        else -> White
    }

    // The fill grows to its value when the card appears and glides when the value changes.
    val fill = remember {
        Animatable(if (animateIn && !MotionPreferences.reduceMotion) 0f else bufferExceededPercent.toFloat())
    }
    LaunchedEffect(bufferExceededPercent) {
        fill.animateTo(
            bufferExceededPercent.toFloat(),
            IvyMotion.standard(IvyMotion.Duration.Long),
        )
    }

    val palette = FernTheme.palette
    val progressColor = palette[ColorToken.SavingsProgress]
    val warningColor = palette[ColorToken.Upcoming]
    val overColor = palette[ColorToken.Overdue]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(backgroundNotFilled)
            .drawBehind {
                drawRect(
                    color = when {
                        bufferExceededPercent <= 0.25 -> progressColor

                        bufferExceededPercent <= 0.50 -> progressColor

                        bufferExceededPercent <= 0.75 -> warningColor

                        else -> overColor
                    },
                    size = size.copy(
                        width = size.width * fill.value.coerceIn(0f, 1f)
                    )
                )
            }
            .thenIf(onClick != null) {
                clickable {
                    onClick?.invoke()
                }
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        IvyIcon(
            icon = if (bufferExceeded) R.drawable.ic_buffer_exceeded else R.drawable.ic_buffer_ok,
            tint = textColor
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = if (bufferExceeded) {
                    stringResource(R.string.buffer_exceeded_by)
                } else {
                    stringResource(
                        R.string.left_to_spend
                    )
                },
                style = UI.typo.c.style(
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            AmountCurrencyB2Row(
                amount = abs(leftToSpend),
                currency = currency,
                textColor = textColor
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_0() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 0.0,
                balance = 100000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_balance_0() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 0.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_very_low() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 100000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_25() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 20000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_50() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 10000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_75() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 7500.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_90() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 5500.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_100() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 5000.0,
                currency = "BGN"
            )
        }
    }
}

@Preview
@Composable
private fun Preview_buffer_125() {
    IvyWalletComponentPreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UI.colors.medium),
            contentAlignment = Alignment.Center
        ) {
            BufferBattery(
                modifier = Modifier.padding(horizontal = 32.dp),
                buffer = 5000.0,
                balance = 2500.0,
                currency = "BGN"
            )
        }
    }
}
