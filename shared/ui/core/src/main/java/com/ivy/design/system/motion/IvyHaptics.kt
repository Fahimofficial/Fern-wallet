package com.ivy.design.system.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * The few moments that earn a haptic. Each fires on the same frame as the visual change it
 * belongs to, and only for a meaningful event (a gesture commits, a toggle flips, a value steps).
 *
 * `View.performHapticFeedback` follows the user's system "touch feedback" setting and needs
 * no permission. Newer, more specific constants are used where available, with the closest
 * older constant as fallback (minSdk 28).
 */
object IvyHaptics {
    /** A drag crossed the point where releasing (or the gesture itself) commits an action. */
    fun gestureCommit(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
        )
    }

    /** A switch flipped on or off. */
    fun toggle(view: View, on: Boolean) {
        view.performHapticFeedback(
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF

                else -> HapticFeedbackConstants.CLOCK_TICK
            }
        )
    }

    /** A value moved one discrete step (e.g. previous/next month). */
    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}
