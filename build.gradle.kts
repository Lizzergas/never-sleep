plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.room3) apply false
    // Applied at the root to merge per-module coverage into one report.
    alias(libs.plugins.kover)
}

dependencies {
    kover(projects.core.common)
    kover(projects.core.database)
    kover(projects.core.datastore)
    kover(projects.core.designsystem)
    kover(projects.core.model)
    kover(projects.core.navigation)
    kover(projects.core.network)
    kover(projects.core.ui)
    kover(projects.feature.settings)
    kover(projects.feature.showcase)
    kover(projects.app.shared)
    kover(projects.server)
}

// Local equivalent of the CI `quality` job. Deliberately not hooked into
// commits or pushes — run it whenever you want: ./gradlew qualityCheck
val qualityCheck = tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs detekt + ktlintCheck across all modules (same as the CI quality job)."
}

subprojects {
    val modulePath = path
    plugins.withId("io.gitlab.arturbosch.detekt") {
        qualityCheck.configure { dependsOn("$modulePath:detekt") }
    }
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        qualityCheck.configure { dependsOn("$modulePath:ktlintCheck") }
    }
}
