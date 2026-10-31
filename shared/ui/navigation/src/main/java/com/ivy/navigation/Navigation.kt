package com.ivy.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Stack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class Navigation @Inject constructor() {
  var currentScreen: Screen? by mutableStateOf(null)
    private set

  @Deprecated("Legacy code. Don't use it, please.")
  val modalBackHandling: Stack<ModalBackHandler> = Stack()

  @Deprecated("Legacy code. Use Compose `BackHandler {}` instead.")
  val onBackPressed: MutableMap<Screen, () -> Boolean> = mutableMapOf()

  private val backStack: Stack<Screen> = Stack()
  var lastScreen: Screen? = null
    private set

  @Deprecated("Legacy code. Don't use it, please.")
  data class ModalBackHandler(
    val id: UUID,
    val onBackPressed: () -> Boolean
  )

  @Deprecated("Legacy code. Don't use it, please.")
  fun lastModalBackHandlerId(): UUID? {
    return if (modalBackHandling.isEmpty()) {
      null
    } else {
      modalBackHandling.peek().id
    }
  }

  fun navigateTo(screen: Screen) {
    if (lastScreen != null) {
      backStack.push(lastScreen)
    }
    switchScreen(screen)
  }

  fun backStackEmpty() = backStack.empty()

  private fun popBackStack() {
    backStack.pop()
  }

  @Deprecated("Legacy code. Don't use it, please.")
  fun onBackPressed(): Boolean {
    if (modalBackHandling.isNotEmpty()) {
      return modalBackHandling.peek().onBackPressed()
    }
    val specialHandling = onBackPressed.getOrDefault(currentScreen) { false }.invoke()
    return specialHandling || back()
  }

  fun back(): Boolean {
    if (!backStack.empty()) {
      switchScreen(backStack.pop())
      return true
    }
    return false
  }

  private fun switchScreen(screen: Screen) {
    this.currentScreen = screen
    lastScreen = screen
  }

  /** The back stack, oldest first, ending with the current screen. */
  fun snapshot(): List<Screen> = backStack.toList() + listOfNotNull(currentScreen)

  /** Replaces the whole back stack with [screens] (oldest first); the last one becomes current. */
  fun restore(screens: List<Screen>) {
    if (screens.isEmpty()) return
    resetBackStack()
    screens.dropLast(1).forEach { backStack.push(it) }
    switchScreen(screens.last())
  }

  fun resetBackStack() {
    while (!backStackEmpty()) {
      popBackStack()
    }
    lastScreen = null
  }
}
