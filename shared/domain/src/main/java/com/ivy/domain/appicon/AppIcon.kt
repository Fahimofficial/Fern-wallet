package com.ivy.domain.appicon

/**
 * Selectable launcher icons. Each maps to an `<activity-alias>` in the app manifest
 * ([aliasClassName]); [Default] is the one enabled out of the box.
 */
enum class AppIcon(val aliasClassName: String) {
    Default("com.ivy.wallet.LauncherDefault"),
    Mono("com.ivy.wallet.LauncherMono"),
    Graphite("com.ivy.wallet.LauncherGraphite"),
    Midnight("com.ivy.wallet.LauncherMidnight"),
}
