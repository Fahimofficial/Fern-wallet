package com.ivy.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Stack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One visit of a [Screen] on the back stack.
 *
 * The same screen can be visited more than once (e.g. two transaction lists), so screen-scoped state
 * such as ViewModels is keyed by [id], not by the screen. Ids only grow, which also tells the
 * transition whether a navigation goes forward (newer entry) or back (older entry).
 */
@Immutable
class NavEntry internal constructor(
    val screen: Screen,
    val id: Long,
) {
    override fun toString(): String = "NavEntry(id=$id, screen=$screen)"
}

@Stable
@Singleton
class Navigation @Inject constructor() {
  var currentEntry: NavEntry? by mutableStateOf(null)
    private set

  val currentScreen: Screen?
    get() = currentEntry?.screen

  /** Bumped whenever something that decides how back behaves changes (read by [handlesBack]). */
  private var backStateVersion by mutableIntStateOf(0)

  @Deprecated("Legacy code. Don't use it, please.")
  val modalBackHandling: Stack<ModalBackHandler> = ObservableStack { backStateVersion++ }

  @Deprecated("Legacy code. Use Compose `BackHandler {}` instead.")
  val onBackPressed: MutableMap<Screen, () -> Boolean> = mutableStateMapOf()

  /**
   * Side effects to run when the user goes back from a screen (e.g. restore the status bar).
   * Unlike [onBackPressed] they can't consume back, so the screen keeps the predictive back
   * gesture.
   */
  val beforeBack: MutableMap<Screen, () -> Unit> = mutableMapOf()

  private val backStack: Stack<NavEntry> = ObservableStack { backStateVersion++ }
  private var lastEntry: NavEntry? = null
  private var nextId = 0L

  val lastScreen: Screen?
    get() = lastEntry?.screen

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

  @Synchronized
  fun navigateTo(screen: Screen) {
    lastEntry?.let { backStack.push(it) }
    val current = currentEntry
    // Navigating to the screen that's already shown keeps it as it is (no new visit, no
    // transition), like before entries existed; back from it is then a visual no-op too.
    switchTo(if (current?.screen == screen) current else NavEntry(screen = screen, id = nextId++))
  }

  @Synchronized
  fun backStackEmpty(): Boolean = backStack.empty()

  @Deprecated("Legacy code. Don't use it, please.")
  fun onBackPressed(): Boolean {
    if (modalBackHandling.isNotEmpty()) {
      return modalBackHandling.peek().onBackPressed()
    }
    val specialHandling = onBackPressed.getOrDefault(currentScreen) { false }.invoke()
    return specialHandling || back()
  }

  @Synchronized
  fun back(): Boolean {
    if (!backStack.empty()) {
      currentScreen?.let { beforeBack[it]?.invoke() }
      switchTo(backStack.pop())
      return true
    }
    return false
  }

  /**
   * True when back is handled inside the app (a modal, a screen override or the back stack);
   * false when it should go to the system (leave the app). Observable from composition.
   */
  @Suppress("DEPRECATION")
  fun handlesBack(): Boolean {
    backStateVersion // subscribe
    return modalBackHandling.isNotEmpty() || onBackPressed.containsKey(currentScreen) ||
      !backStack.empty()
  }

  /** True while a legacy modal handles back. Observable from composition. */
  @Suppress("DEPRECATION")
  fun hasOpenModal(): Boolean {
    backStateVersion // subscribe
    return modalBackHandling.isNotEmpty()
  }

  /**
   * The entry back would return to, when back simply pops the stack: no modal and no screen
   * override can intercept it. Only then can the back gesture preview the previous screen.
   */
  @Suppress("DEPRECATION")
  @Synchronized
  fun predictiveBackTarget(): NavEntry? {
    if (modalBackHandling.isNotEmpty() || onBackPressed.containsKey(currentScreen)) return null
    return if (backStack.empty()) null else backStack.peek()
  }

  /**
   * True while [entry] is shown or can still be returned to.
   * Screen-scoped state of entries for which this is false can be released.
   */
  @Synchronized
  fun isAlive(entry: NavEntry): Boolean =
    entry === currentEntry || entry === lastEntry || backStack.any { it === entry }

  private fun switchTo(entry: NavEntry) {
    lastEntry = entry
    currentEntry = entry
  }

  @Synchronized
  fun resetBackStack() {
    backStack.clear()
    lastEntry = null
  }
}

/** A [Stack] that reports every change, so back-handling state can be observed. */
private class ObservableStack<T>(private val onChange: () -> Unit) : Stack<T>() {
  override fun push(item: T): T = super.push(item).also { onChange() }

  @Synchronized
  override fun pop(): T = super.pop().also { onChange() }

  @Synchronized
  override fun add(element: T): Boolean = super.add(element).also { onChange() }

  override fun remove(element: T): Boolean = super.remove(element).also { onChange() }

  @Synchronized
  override fun removeAt(index: Int): T = super.removeAt(index).also { onChange() }

  override fun clear() {
    super.clear()
    onChange()
  }
}
