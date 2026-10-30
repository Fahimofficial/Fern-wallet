enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack builds artifacts from mutable git tags, so it may serve only the one group we
        // need from it (detekt-explicit), and that group only from JitPack.
        exclusiveContent {
            forRepository { maven(url = "https://jitpack.io") }
            filter { includeGroup("com.github.Ivy-Apps") }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "FernWallet"
include(":app")
include(":ci-actions:base")
include(":ci-actions:compose-stability")
include(":ci-actions:issue-assign")
include(":ci-actions:issue-create-comment")
include(":ci-actions:pr-description-check")
include(":feature:accounts")
include(":feature:attributions")
include(":feature:balance")
include(":feature:budgets")
include(":feature:categories")
include(":feature:contributors")
include(":feature:credit-cards")
include(":feature:disclaimer")
include(":feature:edit-transaction")
include(":feature:exchange-rates")
include(":feature:features")
include(":feature:home")
include(":feature:import-data")
include(":feature:loans")
include(":feature:main")
include(":feature:onboarding")
include(":feature:piechart")
include(":feature:planned-payments")
include(":feature:releases")
include(":feature:reports")
include(":feature:search")
include(":feature:settings")
include(":feature:transactions")
include(":shared:base")
include(":shared:data:core")
include(":shared:data:core-testing")
include(":shared:data:model")
include(":shared:data:model-testing")
include(":shared:domain")
include(":shared:ui:core")
include(":shared:ui:navigation")
include(":shared:ui:testing")
include(":temp:legacy-code")
include(":temp:old-design")
include(":widget:add-transaction")
include(":widget:balance")
include(":widget:shared-base")
