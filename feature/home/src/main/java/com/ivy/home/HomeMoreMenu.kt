package com.ivy.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.MotionPreferences
import kotlinx.coroutines.delay
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import com.ivy.design.system.motion.pressScale
import com.ivy.domain.RootScreen
import com.ivy.legacy.Constants
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.colorLerp
import com.ivy.legacy.utils.lerp
import com.ivy.legacy.utils.navigationBarInset
import com.ivy.legacy.utils.openUrl
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.statusBarInset
import com.ivy.legacy.utils.toDensityPx
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.SupportScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BufferBattery
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role

private const val SWIPE_UP_THRESHOLD_CLOSE_MORE_MENU = 300

@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean,

    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,

    setExpanded: (Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    plannedDue: Int = 0,
    topCategory: TopCategory? = null,
    celebrateGoal: Boolean = false,
    onGoalCelebrated: () -> Unit = {},
) {
    val ivyContext = ivyWalletCtx()

    // Kept as State and read in layout/draw lambdas: the reveal animates without recomposing
    // the menu on every frame.
    val percentExpanded = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounce(),
        label = "more_menu"
    )
    val iconRotation = animateFloatAsState(
        targetValue = if (expanded) -180f else 0f,
        animationSpec = springBounce(),
        label = "more_menu_arrow"
    )
    val onScreen by remember { derivedStateOf { percentExpanded.value > 0.01f } }
    // The tile wave greets an opening; a menu that is already open (restored, screenshots) just shows.
    val startedOpen = remember { expanded }

    val buttonSizePx = 40.dp.toDensityPx()

    val xBase = ivyContext.screenWidth - 24.dp.toDensityPx()
    val yBaseCollapsed = 20.dp.toDensityPx() + statusBarInset()
    val yBaseExpanded = ivyContext.screenHeight - 48.dp.toDensityPx() - navigationBarInset()

    // Background
    val colorMedium = UI.colors.medium
    if (onScreen) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .clickableNoIndication(rememberInteractionSource()) {
                    // do nothing
                }
                .zIndex(500f)
        ) {
            val radiusCollapsed = buttonSizePx / 2f
            val radiusExpanded = ivyContext.screenHeight * 1.5f
            val progress = percentExpanded.value
            val radius = lerp(radiusCollapsed, radiusExpanded, progress)

            val yBackground = lerp(
                start = yBaseCollapsed + radius,
                end = yBaseExpanded,
                fraction = progress
            )

            drawCircle(
                color = colorMedium,
                center = Offset(
                    x = xBase - buttonSizePx / 2f,
                    y = yBackground
                ),
                radius = radius
            )
        }
    }

    if (onScreen) {
        Column(
            modifier = modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize()
                .graphicsLayer { alpha = percentExpanded.value.coerceIn(0f, 1f) }
                .verticalScroll(rememberScrollState())
                .zIndex(510f)
                .verticalSwipeListener(
                    sensitivity = SWIPE_UP_THRESHOLD_CLOSE_MORE_MENU,
                    state = rememberSwipeListenerState(),
                    onSwipeUp = {
                        setExpanded(false)
                    }
                )
        ) {
            val modalId = remember {
                UUID.randomUUID()
            }

            AddModalBackHandling(
                modalId = modalId,
                visible = expanded
            ) {
                setExpanded(false)
            }

            Content(
                theme = theme,
                onSwitchTheme = onSwitchTheme,
                balance = balance,
                buffer = buffer,
                currency = currency,
                onBufferClick = onBufferClick,
                onCurrencyClick = onCurrencyClick,
                plannedDue = plannedDue,
                topCategory = topCategory,
                celebrateGoal = celebrateGoal,
                onGoalCelebrated = onGoalCelebrated,
                tileWave = !startedOpen,
            )
        }
    }

    MoreMenuButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val yButton = lerp(
                    start = yBaseCollapsed,
                    end = yBaseExpanded - buttonSizePx,
                    fraction = percentExpanded.value
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = xBase.roundToInt() - buttonSizePx.roundToInt(),
                        y = yButton.roundToInt()
                    )
                }
            }
            .graphicsLayer { rotationZ = iconRotation.value }
            .thenIf(expanded) {
                zIndex(520f)
            }
            .testTag("home_more_menu_arrow"),
        progress = { percentExpanded.value },
    ) {
        setExpanded(!expanded)
    }
}

// Own restart scope: only this button recomposes as its background color animates.
@Composable
private fun MoreMenuButton(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CircleButtonFilled(
        modifier = modifier,
        backgroundColor = colorLerp(UI.colors.medium, UI.colors.pure, progress()),
        icon = R.drawable.ic_expandarrow,
        onClick = onClick,
    )
}

@Suppress("LongParameterList")
@Composable
private fun ColumnScope.Content(
    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,

    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    plannedDue: Int,
    topCategory: TopCategory?,
    celebrateGoal: Boolean,
    onGoalCelebrated: () -> Unit,
    tileWave: Boolean,
) {
    Spacer(Modifier.height(24.dp))

    val nav = navigation()
    SearchButton {
        nav.navigateTo(
            screen = SearchScreen
        )
    }

    Spacer(Modifier.height(16.dp))

    QuickAccess(
        theme = theme,
        onSwitchTheme = onSwitchTheme,
        plannedDue = plannedDue,
        tileWave = tileWave,
    )

    Spacer(Modifier.height(40.dp))

    Buffer(
        buffer = buffer,
        currency = currency,
        balance = balance,
        onBufferClick = onBufferClick,
        celebrate = celebrateGoal,
        onCelebrated = onGoalCelebrated,
        animateIn = tileWave,
    )

    Spacer(Modifier.height(16.dp))

    OpenSource()

    Spacer(Modifier.height(12.dp))

    SupportMyWork()

    if (topCategory != null) {
        Spacer(Modifier.height(12.dp))
        Insight(topCategory)
    }

    Spacer(Modifier.weight(1f))
}

@Composable
private fun SearchButton(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.rFull)
            .background(UI.colors.pure)
            .border(1.dp, FernTheme.palette[ColorToken.Outline], UI.shapes.rFull)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(icon = R.drawable.ic_search)

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier.padding(
                vertical = 12.dp,
            ),
            text = stringResource(R.string.search_transactions),
            style = UI.typo.b2.style(
                fontWeight = FontWeight.SemiBold,
                color = UI.colors.pureInverse
            )
        )

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun OpenSource() {
    val uriHandler = LocalUriHandler.current
    LinkCard(
        content = LinkCardData(
            icon = R.drawable.github_logo,
            iconTint = UI.colors.pureInverse,
            title = stringResource(R.string.ivy_wallet_open_source),
            subtitle = Constants.URL_IVY_WALLET_REPO,
            subtitleColor = UI.colors.gray,
            trailingIcon = R.drawable.ic_arrow_right,
            pulseIcon = false,
        ),
        modifier = Modifier.testTag("home_open_source_card"),
    ) {
        openUrl(uriHandler = uriHandler, url = Constants.URL_IVY_WALLET_REPO)
    }
}

// Opens the in-app Support screen, which holds the verified Ko-fi and UPI destinations.
@Composable
private fun SupportMyWork() {
    val nav = navigation()
    LinkCard(
        content = LinkCardData(
            icon = R.drawable.ic_vue_support_heart,
            iconTint = SupportHeart,
            title = stringResource(R.string.support_my_work),
            subtitle = stringResource(R.string.support_my_work_home_description),
            subtitleColor = UI.colors.gray,
            trailingIcon = null,
            pulseIcon = true,
        ),
        modifier = Modifier.testTag(SupportCardTag),
    ) {
        nav.navigateTo(SupportScreen)
    }
}

// A tappable row card of the more menu; the open-source and support cards share it.
@Immutable
private data class LinkCardData(
    @DrawableRes val icon: Int,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val subtitleColor: Color,
    /** e.g. an "opens" arrow for a card that leaves the app; null for none. */
    @DrawableRes val trailingIcon: Int?,
    /** One gentle beat each time the card appears. */
    val pulseIcon: Boolean,
)

@Composable
private fun LinkCard(
    content: LinkCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(interactionSource)
            .clip(UI.shapes.r4)
            .background(UI.colors.pure)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        val beat = remember { Animatable(1f) }
        if (content.pulseIcon) {
            LaunchedEffect(Unit) {
                if (MotionPreferences.reduceMotion) return@LaunchedEffect
                delay(IvyMotion.Duration.Long.toLong()) // after the menu has opened
                beat.animateTo(HeartBeatScale, IvyMotion.standard(IvyMotion.Duration.Short))
                beat.animateTo(1f, IvyMotion.Springs.bouncy())
            }
        }
        IvyIcon(
            modifier = Modifier.graphicsLayer {
                scaleX = beat.value
                scaleY = beat.value
            },
            icon = content.icon,
            tint = content.iconTint,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = content.title,
                style = UI.typo.b2.style(
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = content.subtitle,
                style = UI.typo.c.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = content.subtitleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (content.trailingIcon != null) {
            IvyIcon(icon = content.trailingIcon, tint = UI.colors.gray)
        }
        Spacer(Modifier.width(16.dp))
    }
}

private const val HeartBeatScale = 1.25f

/** One fact about this month, in the space below the cards. */
@Composable
private fun Insight(topCategory: TopCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.r4)
            .background(UI.colors.pure)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IvyIcon(icon = R.drawable.home_more_menu_reports, tint = FernTheme.palette[ColorToken.Accent])
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(R.string.insight_top_category_title),
                style = UI.typo.c.style(fontWeight = FontWeight.ExtraBold, color = UI.colors.gray),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.insight_top_category,
                    topCategory.count,
                    topCategory.name,
                    topCategory.count,
                ),
                style = UI.typo.b2.style(fontWeight = FontWeight.ExtraBold),
            )
        }
    }
}

/** Test tag of the Home menu's "Support my work" card. */
const val SupportCardTag = "home_support_card"

// Same heart colour as the Ko-fi button on the Support screen.
private val SupportHeart = Color(0xFFFF5E5B)

@Composable
private fun ColumnScope.Buffer(
    buffer: Double,
    currency: String,
    balance: Double,
    onBufferClick: () -> Unit,
    celebrate: Boolean,
    onCelebrated: () -> Unit,
    animateIn: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                onBufferClick()
            }
            .testTag("savings_goal_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            text = stringResource(R.string.savings_goal),
            style = UI.typo.b1.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        AmountCurrencyB1(
            amount = buffer,
            currency = currency,
            amountFontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.width(32.dp))
    }

    Spacer(Modifier.height(12.dp))

    Box {
        BufferBattery(
            modifier = Modifier.padding(horizontal = 16.dp),
            buffer = buffer,
            currency = currency,
            balance = balance,
            animateIn = animateIn,
        ) {
            onBufferClick()
        }
        if (celebrate) {
            Confetti(modifier = Modifier.matchParentSize(), onFinished = onCelebrated)
        }
    }
}

@Composable
private fun QuickAccess(
    theme: Theme,
    onSwitchTheme: () -> Unit,
    plannedDue: Int,
    tileWave: Boolean,
) {
    CompositionLocalProvider(LocalTileWave provides tileWave) {
        QuickAccessGrid(theme, onSwitchTheme, plannedDue)
    }
}

// Whether tiles may play their entrance wave; read by every tile of the grid.
private val LocalTileWave = staticCompositionLocalOf { false }

@Composable
private fun QuickAccessGrid(
    theme: Theme,
    onSwitchTheme: () -> Unit,
    plannedDue: Int,
) {
    Column {
        val nav = navigation()

        Text(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(R.string.quick_access),
            style = UI.typo.b2.style()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 0,
                icon = R.drawable.home_more_menu_settings,
                label = stringResource(R.string.settings)
            ) {
                nav.navigateTo(SettingsScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 1,
                icon = R.drawable.home_more_menu_categories,
                label = stringResource(R.string.categories)
            ) {
                nav.navigateTo(CategoriesScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 2,
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                },
                backgroundColor = when (theme) {
                    Theme.LIGHT -> UI.colors.pure
                    Theme.DARK -> UI.colors.pureInverse
                    Theme.AMOLED_DARK -> UI.colors.pureInverse
                    Theme.AUTO -> UI.colors.pure
                },
                tint = when (theme) {
                    Theme.LIGHT -> UI.colors.pureInverse
                    Theme.DARK -> UI.colors.pure
                    Theme.AMOLED_DARK -> UI.colors.pure
                    Theme.AUTO -> UI.colors.pureInverse
                }
            ) {
                onSwitchTheme()
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 3,
                icon = R.drawable.home_more_menu_planned_payments,
                label = stringResource(R.string.planned_payments),
                badge = plannedDue,
            ) {
                nav.navigateTo(PlannedPaymentsScreen)
            }

            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.weight(1f))

            val context = LocalContext.current
//        MoreMenuButton(
//            icon = R.drawable.home_more_menu_reports,
//            label = "Charts"
//        ) {
//            ivyContext.navigateTo(Screen.Charts)
//        }

            // Resolve the RootScreen lazily so the menu can render in previews/screenshots.
            MoreMenuButton(
                wave = 1,
                icon = R.drawable.home_more_menu_share,
                label = stringResource(R.string.share_ivy)
            ) {
                (context as? RootScreen)?.shareIvyWallet()
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 2,
                icon = R.drawable.home_more_menu_reports,
                label = stringResource(R.string.reports),
            ) {
                nav.navigateTo(ReportScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 3,
                icon = R.drawable.home_more_menu_budgets,
                label = stringResource(R.string.budgets),
            ) {
                nav.navigateTo(BudgetScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                wave = 4,
                icon = R.drawable.home_more_menu_loans,
                label = stringResource(R.string.loans),
            ) {
                nav.navigateTo(LoansScreen)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * A Quick-access tile: icon and label are one 48dp+ target that shrinks under the finger with a
 * light tick. On the menu's first opening per app start, tiles fade in as a diagonal wave.
 *
 * @param wave row + column: tiles on the same diagonal enter together, 30ms apart.
 */
@Composable
private fun MoreMenuButton(
    wave: Int,
    @DrawableRes icon: Int,
    label: String,

    backgroundColor: Color = UI.colors.pure,
    tint: Color = UI.colors.pureInverse,
    badge: Int = 0,

    onClick: () -> Unit
) {
    val interactionSource = rememberInteractionSource()
    val view = LocalView.current
    val waveAllowed = LocalTileWave.current
    val enter = remember {
        Animatable(if (!waveAllowed || quickAccessWavePlayed || MotionPreferences.reduceMotion) 1f else 0f)
    }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            delay(wave * QuickAccessWaveStepMillis)
            enter.animateTo(1f, IvyMotion.standard(IvyMotion.Duration.Medium))
        }
        quickAccessWavePlayed = true
    }
    val badgeText = if (badge > 0) pluralStringResource(R.plurals.planned_due_badge, badge, badge) else null
    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * IvyMotion.Stagger.OffsetDp.dp.toPx()
            }
            .pressScale(interactionSource, pressedScale = TilePressedScale)
            .clip(UI.shapes.r4)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
            ) {
                IvyHaptics.tick(view)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                if (badgeText != null) stateDescription = badgeText
            }
            .defaultMinSize(minWidth = 80.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            IvyIcon(
                modifier = Modifier
                    .background(backgroundColor, CircleShape)
                    .padding(14.dp),
                icon = icon,
                tint = tint,
            )
            if (badge > 0) {
                val badgeColor = FernTheme.palette[ColorToken.Overdue]
                Text(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .defaultMinSize(minWidth = 20.dp)
                        .background(badgeColor, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    text = if (badge > MaxBadge) "$MaxBadge+" else badge.toString(),
                    style = UI.typo.nC.style(
                        color = badgeColor.contentColor(),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.defaultMinSize(minWidth = 80.dp),
            text = label,
            style = UI.typo.c.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        )
    }
}

private const val TilePressedScale = 0.92f
private const val QuickAccessWaveStepMillis = 30L
private const val MaxBadge = 9

// shortcut: process-wide, so the wave plays on the menu's first opening per app start only.
private var quickAccessWavePlayed = false

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_Expanded() {
    IvyPreview {
        MoreMenu(
            expanded = true,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {
            },
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {}
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview() {
    IvyPreview {
        var expanded by remember { mutableStateOf(false) }

        MoreMenu(
            expanded = expanded,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            setExpanded = {
                expanded = it
            },
            onSwitchTheme = {},
            onBufferClick = {},
            onCurrencyClick = {}
        )
    }
}
