plugins {
    id("ivy.kotlin-android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
}

// Dagger/Hilt read Kotlin class metadata with kotlin-metadata-jvm. The version they ship with
// can lag behind our Kotlin compiler and then fails with "Provided Metadata instance has version X".
// Keep it aligned with the Kotlin version we compile with.
val kotlinVersion = catalog.version("kotlin")
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
            useVersion(kotlinVersion)
            because("Must be able to read metadata produced by our Kotlin compiler")
        }
    }
}

dependencies {
    implementation(libs.bundles.hilt)
    ksp(catalog.library("hilt-compiler"))

    // hilt-work 1.4.0 depends on work-runtime 2.3.4. Modules that don't add WorkManager
    // themselves (e.g. shared:data:core) would otherwise get 2.3.4 in their test APKs, and its
    // ForceStopRunnable crashes on API 31+ because it creates a PendingIntent without
    // FLAG_IMMUTABLE/FLAG_MUTABLE. The app already resolves the catalog version.
    constraints {
        implementation("androidx.work:work-runtime:${catalog.version("androidx-work")}") {
            because("work-runtime < 2.7 crashes on Android 12+ (PendingIntent mutability flags)")
        }
    }
}
