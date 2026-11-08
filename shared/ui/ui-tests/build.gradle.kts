// Compose UI tests of app screens, run on the JVM with Robolectric (no emulator needed).
// Test-only: nothing here ships. Kept out of the feature modules because Paparazzi and
// Robolectric don't mix in one module.
plugins {
    org.jetbrains.kotlin.plugin.compose
    id("ivy.module")
    id("ivy.compose")
}

android {
    namespace = "com.ivy.uitests"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    constraints {
        // Robolectric's Bouncy Castle transitive dependency must stay on a patched release.
        add("testImplementation", "org.bouncycastle:bcprov-jdk18on:1.86") {
            because("Security floor for the test runtime; the older 1.80 release has critical advisories.")
        }
    }

    testImplementation(projects.feature.home)
    testImplementation(projects.feature.main)
    testImplementation(projects.shared.base)
    testImplementation(projects.shared.ui.core)
    testImplementation(projects.shared.ui.navigation)
    testImplementation(projects.temp.legacyCode)
    testImplementation(projects.temp.oldDesign)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    // Robolectric downloads its Android jars under the home folder (~/.m2 and a lock file there);
    // ROBOLECTRIC_DEPS_DIR moves both where home isn't writable (sandboxes). CI needs nothing.
    System.getenv("ROBOLECTRIC_DEPS_DIR")?.let { dir ->
        systemProperty("maven.repo.local", "$dir/repository")
        systemProperty("user.home", dir)
    }
}
