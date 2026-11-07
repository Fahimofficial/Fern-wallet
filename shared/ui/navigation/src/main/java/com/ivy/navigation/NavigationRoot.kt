package com.ivy.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import android.annotation.SuppressLint
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.design.system.motion.IvyMotion
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

/**
 * The screen whose content is being composed. Unlike [Navigation.currentScreen] it doesn't change
 * while the screen animates out, so per-screen effects keyed on it don't restart during transitions.
 * Null outside [NavigationRoot] (previews).
 */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalHostScreen = staticCompositionLocalOf<Screen?> { null }

/**
 * Default screen transition, direction taken from the back-stack order. A screen that can grow
 * out of an element of the previous one (see [sharedContainer]) only fades: the shared bounds
 * carry the motion, so nothing else competes with them.
 */
fun AnimatedContentTransitionScope<NavEntry>.defaultScreenTransition(): ContentTransform {
    val forward = targetState.id >= initialState.id
    val front = if (forward) targetState.screen else initialState.screen
    return IvyMotion.screenTransform(forward = forward, container = front.containerKey() != null)
}

/**
 * Shows the current screen and animates between screens.
 *
 * Back goes through the system's predictive back: when back would simply return to the previous
 * screen, the transition follows the gesture (the previous screen is revealed as the finger moves,
 * releasing finishes the transition from where it is, cancelling plays it back). Back that a modal
 * or a screen intercepts runs when the gesture commits. When the app has nothing to go back to,
 * the handler is disabled so the system can show its own "back to home" preview.
 *
 * @param onUnhandledBack called when a screen's back override declines and there is nowhere to go.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("ComposeModifierMissing")
@Composable
fun NavigationRoot(
    navigation: Navigation,
    transitionSpec: AnimatedContentTransitionScope<NavEntry>.() -> ContentTransform = {
        defaultScreenTransition()
    },
    onUnhandledBack: () -> Unit = {},
    navGraph: @Composable (screen: Screen?) -> Unit
) {
    CompositionLocalProvider(
        LocalNavigation provides navigation,
    ) {
        val entry = navigation.currentEntry
        if (entry == null) {
            // Nothing navigated yet (app start) or previews.
            navGraph(null)
        } else {
            val parentOwner = LocalViewModelStoreOwner.current
            val stores: ScreenViewModelStores? = parentOwner?.let {
                viewModel<ScreenViewModelStores>(viewModelStoreOwner = it)
            }
            val seekState = remember { SeekableTransitionState(entry) }
            LaunchedEffect(entry) {
                // Also finishes a predictive back from the fraction the gesture reached.
                seekState.animateTo(entry)
            }
            PredictiveBack(navigation, seekState, onUnhandledBack)

            // Lets elements of the outgoing and incoming screen connect (see sharedContainer).
            val containerShapes = remember { ContainerShapes() }
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                    LocalContainerShapes provides containerShapes,
                ) {
                    val screens = rememberTransition(seekState, label = "screen")
                    screens.AnimatedContent(
                        transitionSpec = transitionSpec,
                        contentKey = { it.id },
                    ) { shown ->
                        val visibility = transition
                        // Settled unless this screen is arriving in front (back reveals it at once).
                        val enterSettled = remember(shown) {
                            derivedStateOf {
                                val arrivingInFront = screens.targetState.id == shown.id &&
                                    screens.currentState.id < shown.id
                                !arrivingInFront || visibility.currentState == EnterExitState.Visible
                            }
                        }
                        val other = if (shown.id == screens.targetState.id) {
                            screens.currentState
                        } else {
                            screens.targetState
                        }
                        val behind = shown.id < other.id
                        val covered by visibility.transition.animateFloat(
                            transitionSpec = { IvyMotion.recedeSpec() },
                            label = "screen_recede",
                        ) { state ->
                            if (state == EnterExitState.Visible || !behind) 0f else 1f
                        }
                        CompositionLocalProvider(
                            LocalScreenAnimationScope provides this,
                            LocalScreenEnterSettled provides enterSettled,
                        ) {
                            Box(
                                Modifier.applyPreviousScreenRecedeEffect(behind, covered),
                            ) {
                                ScreenContent(shown, navigation, stores, parentOwner, navGraph)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scales and dims the older screen as the newer screen enters, following predictive back progress.
 */
private fun Modifier.applyPreviousScreenRecedeEffect(
    behind: Boolean,
    covered: Float,
): Modifier {
    if (!behind) return this
    return graphicsLayer {
        val scale = 1f - (1f - IvyMotion.RecededScale) * covered
        scaleX = scale
        scaleY = scale
    }.drawWithContent {
        drawContent()
        drawRect(Color.Black, alpha = IvyMotion.RecededDim * covered)
    }
}

@Composable
private fun ScreenContent(
    shown: NavEntry,
    navigation: Navigation,
    stores: ScreenViewModelStores?,
    parentOwner: ViewModelStoreOwner?,
    navGraph: @Composable (screen: Screen?) -> Unit,
) {
    // Legacy screens keep using the Activity's ViewModels (their historic behaviour).
    // Every other screen gets ViewModels of its own, kept while it's on the back stack.
    if (!shown.screen.isLegacy && stores != null && parentOwner != null) {
        val owner = remember(shown, stores, parentOwner) {
            EntryViewModelStoreOwner(stores.storeFor(shown.id), parentOwner)
        }
        DisposableEffect(shown, stores) {
            onDispose {
                // Runs after the exit transition. Release only if it can't be returned to.
                if (!navigation.isAlive(shown)) stores.clear(shown.id)
            }
        }
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalHostScreen provides shown.screen,
        ) {
            navGraph(shown.screen)
        }
    } else {
        CompositionLocalProvider(LocalHostScreen provides shown.screen) {
            navGraph(shown.screen)
        }
    }
}

@Composable
private fun PredictiveBack(
    navigation: Navigation,
    seekState: SeekableTransitionState<NavEntry>,
    onUnhandledBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val latestOnUnhandledBack by rememberUpdatedState(onUnhandledBack)
    PredictiveBackHandler(enabled = navigation.handlesBack()) { progress ->
        val target = navigation.predictiveBackTarget()
        if (target == null) {
            // A modal or the screen decides: act once the gesture commits.
            progress.collect {}
            @Suppress("DEPRECATION")
            if (!navigation.onBackPressed()) latestOnUnhandledBack()
            return@PredictiveBackHandler
        }
        try {
            progress.collect { event ->
                seekState.seekTo(event.progress, targetState = target)
            }
            navigation.back()
        } catch (e: CancellationException) {
            // Gesture cancelled: play the transition back to the screen that stays.
            scope.launch { seekState.animateTo(seekState.currentState) }
            throw e
        }
    }
}

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalScreenEnterSettled = staticCompositionLocalOf<State<Boolean>?> { null }

/**
 * False while the screen is still sliding or growing in front of the previous one; true once it
 * has landed (and always when it is revealed by back, in previews and in tests). Use it to defer
 * heavy content, e.g. a long list, so the transition's frames stay cheap.
 */
@Composable
fun screenEnterSettled(): Boolean = LocalScreenEnterSettled.current?.value ?: true

@Composable
fun navigation(): Navigation {
    return LocalNavigation.current
}

/**
 * Provides a [ViewModel] instance scoped the screen's life.
 * When the user navigates away from the screen (it leaves the back stack) all screen scoped
 * viewModels are destroyed.
 * Does not apply for legacy screens.
 */
@Composable
inline fun <reified T : ViewModel> screenScopedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner provided" }
    val viewModelProvider = factory?.let {
        ViewModelProvider(viewModelStoreOwner, it)
    } ?: ViewModelProvider(viewModelStoreOwner)
    return viewModelProvider[T::class.java]
}
