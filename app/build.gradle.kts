plugins {
    id("com.android.application")
    org.jetbrains.kotlin.plugin.compose
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}

android {
    // Fern Wallet's own identity. Kotlin sources keep the upstream `com.ivy.*` packages
    // (see docs/Package-Identity.md); only the generated R/BuildConfig live here.
    namespace = "com.fernwallet.app"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.fernwallet.app"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionName = libs.versions.version.name.get()
        versionCode = libs.versions.version.code.get().toInt()
    }

    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        getByName("debug") {
            // Debug keystore only (local/debug builds, never published). Not a real secret, but
            // kept out of source anyway; falls back to the shared debug password so no setup is
            // needed. Override via -PdebugKeystorePassword or the DEBUG_KEYSTORE_PASSWORD env var.
            val debugKeystorePassword = (findProperty("debugKeystorePassword") as String?)
                ?: System.getenv("DEBUG_KEYSTORE_PASSWORD")
                ?: "IVY7834!DEbug"
            storeFile = file("../debug.jks")
            storePassword = debugKeystorePassword
            keyAlias = "debug"
            keyPassword = debugKeystorePassword
        }

        // Release signing comes only from the environment (CI secrets). The keystore path
        // defaults to ../sign.jks, which is git-ignored and never committed.
        create("release") {
            storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "../sign.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            isDebuggable = false
            isDefault = false

            signingConfig = signingConfigs.getByName("release")

            resValue("string", "app_name", "Fern Wallet")
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            isDebuggable = true
            isDefault = true

            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Fern Wallet Debug")
        }

        create("demo") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            matchingFallbacks.add("release")

            isDebuggable = false
            isDefault = false

            // Demo builds are signed with the public debug key: never distribute them as releases.
            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".demo"
            resValue("string", "app_name", "Fern Wallet Demo")
        }
    }

    val javaVersion = libs.versions.jvm.target.get()
    compileOptions {
        sourceCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
        targetCompatibility = JavaVersion.valueOf("VERSION_$javaVersion")
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    lint {
        disable += "ComposeViewModelInjection"
        checkDependencies = true
        abortOnError = false
        checkReleaseBuilds = false
        htmlReport = true
        htmlOutput = file("${project.rootDir}/build/reports/lint/lint.html")
        xmlReport = true
        xmlOutput = file("${project.rootDir}/build/reports/lint/lint.xml")
        baseline = file("lint-baseline.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.jvm.target.get()))
    }
}

// Keep Hilt's Kotlin metadata reader in sync with our Kotlin compiler (see ivy.hilt.gradle.kts).
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-metadata-jvm") {
            useVersion(libs.versions.kotlin.asProvider().get())
            because("Must be able to read metadata produced by our Kotlin compiler")
        }
    }
}

dependencies {
    implementation(projects.feature.attributions)
    implementation(projects.feature.balance)
    implementation(projects.feature.budgets)
    implementation(projects.feature.categories)
    implementation(projects.feature.contributors)
    implementation(projects.feature.creditCards)
    implementation(projects.feature.disclaimer)
    implementation(projects.feature.editTransaction)
    implementation(projects.feature.exchangeRates)
    implementation(projects.feature.features)
    implementation(projects.feature.home)
    implementation(projects.feature.importData)
    implementation(projects.feature.loans)
    implementation(projects.feature.main)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.piechart)
    implementation(projects.feature.plannedPayments)
    implementation(projects.feature.releases)
    implementation(projects.feature.reports)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)
    implementation(projects.feature.transactions)
    implementation(projects.shared.base)
    implementation(projects.shared.data.core)
    implementation(projects.shared.domain)
    implementation(projects.shared.ui.core)
    implementation(projects.shared.ui.navigation)
    implementation(projects.temp.legacyCode)
    implementation(projects.temp.oldDesign)
    implementation(projects.widget.addTransaction)
    implementation(projects.widget.balance)

    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.kotlin.android)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.arrow)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.activity)
    implementation(libs.bundles.google)
    implementation(libs.datastore)
    implementation(libs.androidx.security)
    implementation(libs.androidx.biometrics)

    implementation(libs.bundles.hilt)
    implementation(libs.material)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    implementation(libs.timber)
    implementation(libs.keval)
    implementation(libs.bundles.opencsv)
    implementation(libs.androidx.work)
    implementation(libs.androidx.recyclerview)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.androidx.work.testing)

    lintChecks(libs.slack.lint.compose)
}
