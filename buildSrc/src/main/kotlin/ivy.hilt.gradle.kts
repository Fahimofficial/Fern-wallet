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
}
