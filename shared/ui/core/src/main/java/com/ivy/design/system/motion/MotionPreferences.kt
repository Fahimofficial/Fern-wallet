package com.ivy.design.system.motion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The system "Remove animations" setting (animator duration scale = 0), refreshed on every resume.
 *
 * Compose tweens and springs already finish at once when the scale is 0. Check this for motion the
 * scale doesn't cover: `delay()`-based staggers, count-ups, one-shot pulses and frame loops.
 * Snapshot state, so composables that read it update when the user changes the setting.
 */
object MotionPreferences {
    var reduceMotion: Boolean by mutableStateOf(false)
}
