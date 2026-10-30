import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Target the same bytecode as the Android modules, built by whichever JDK runs Gradle
// (instead of a toolchain that needs that exact JDK installed).
val bytecodeTarget = catalog.version("jvm-target")

java {
    sourceCompatibility = JavaVersion.toVersion(bytecodeTarget)
    targetCompatibility = JavaVersion.toVersion(bytecodeTarget)
}

kotlin {
    sourceSets.all {
        kotlin.srcDir("build/generated/ksp/$name/kotlin")
    }

    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(bytecodeTarget))
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.kotlin)

    implementation(catalog.library("kotlinx-serialization-json"))
    testImplementation(libs.bundles.testing)
}
