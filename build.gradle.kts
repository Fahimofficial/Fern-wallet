// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Security floors for the build classpath of the root project, which subprojects inherit
// (see gradle/libs.versions.toml). Plugins are declared below so their dependencies resolve here.
buildscript {
    dependencies {
        classpath(platform(libs.build.classpath.jackson.bom))
        constraints {
            classpath(libs.build.classpath.guava)
        }
    }
}

plugins {
    // Run with:
    // ./gradlew detekt // Simple report in the console
    // ./gradlew detektFormat // To check with enabled auto-correction
    id("ivy.detekt")
    id("com.jraska.module.graph.assertion")

    alias(libs.plugins.gradleWrapperUpgrade)

    alias(libs.plugins.koverPlugin)

    // Applied in :app; declared here so it is loaded with the constraints above.
    alias(libs.plugins.cyclonedx) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    kover {
        reports {
            filters {
                excludes {
                    classes(
                        "*Activity",
                        "*Activity\$*",
                        "*.BuildConfig",
                        "dagger.hilt.*",
                        "hilt_aggregated_deps.*",
                        "*.Hilt_*"
                    )
                    annotatedBy("@Composable")
                }
            }
        }
    }
}

wrapperUpgrade {
    gradle {
        create("fernWallet") {
            repo.set("Fahimofficial/Fern-wallet")
            baseBranch.set("main")
        }
    }
}
