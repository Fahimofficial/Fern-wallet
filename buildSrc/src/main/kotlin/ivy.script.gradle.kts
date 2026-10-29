plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.all {
        kotlin.srcDir("build/generated/ksp/$name/kotlin")
    }

    jvmToolchain(catalog.version("jvm-target").toInt())
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.kotlin)

    implementation(catalog.library("kotlinx-serialization-json"))
    testImplementation(libs.bundles.testing)
}
