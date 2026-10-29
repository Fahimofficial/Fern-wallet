import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.kotlin.android")
}

android {
    // Kotlin
    val javaVersion = catalog.version("jvm-target")
    compileOptions {
        sourceCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
        targetCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
    }

    // Android
    compileSdk = catalog.version("compile-sdk").toInt()
    defaultConfig {
        minSdk = catalog.version("min-sdk").toInt()
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(catalog.version("jvm-target")))
    }
}

gradle.projectsEvaluated {
    // Increase tests Heap Size because of Kotest property-based tests
    tasks.withType<Test> {
        maxHeapSize = "2048m"
    }
}

dependencies {
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.kotlin)
    implementation(catalog.bundle("kotlin-android"))
    implementation(libs.timber)

    testImplementation(libs.bundles.testing)
}
