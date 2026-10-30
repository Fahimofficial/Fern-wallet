package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.design.system.motion.IvyMotion

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

/**
 * The screen whose content is being composed. Unlike [Navigation.currentScreen] it doesn't change
 * while the screen animates out, so per-screen effects keyed on it don't restart during transitions.
 * Null outside [NavigationRoot] (previews).
 */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalHostScreen = staticCompositionLocalOf<Screen?> { null }

/** Default screen transition: shared-axis X, direction taken from the back-stack order. */
fun AnimatedContentTransitionScope<NavEntry>.defaultScreenTransition(): ContentTransform =
    IvyMotion.screenTransform(forward = targetState.id >= initialState.id)

@SuppressLint("ComposeModifierMissing")
@Composable
fun NavigationRoot(
    navigation: Navigation,
    transitionSpec: AnimatedContentTransitionScope<NavEntry>.() -> ContentTransform = {
        defaultScreenTransition()
    },
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
            AnimatedContent(
                targetState = entry,
                transitionSpec = transitionSpec,
                contentKey = { it.id },
                label = "screen",
            ) { shown ->
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
        }
    }
}

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
