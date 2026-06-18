import buildlogic.QualityFilters
import buildlogic.KotlinAssignmentWrappingCheckTask
import buildlogic.KotlinAssignmentWrappingFormatTask
import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Code quality for a module: detekt + ktlint + kover. Enforced only by the
 * CI `quality` job and the local `./gradlew qualityCheck` aggregate —
 * deliberately NOT wired into commits or pushes.
 */
plugins {
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlinx.kover")
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    // KMP source-set layout (src/<sourceSet>/kotlin)
    source.setFrom(files("src"))
}

extensions.configure<KtlintExtension> {
    filter {
        // KSP/compose-generated sources are their own source roots, so
        // relative-path patterns can't exclude them — a Spec on the absolute
        // path is required (kept config-cache-safe in QualityFilters).
        exclude(QualityFilters.generatedSources)
    }
}

val kotlinAssignmentWrappingFiles =
    layout.projectDirectory.asFileTree.matching {
        include("src/**/*.kt")
        include("*.gradle.kts")
        exclude("build/**")
    }

val kotlinAssignmentWrappingFormat =
    tasks.register<KotlinAssignmentWrappingFormatTask>("kotlinAssignmentWrappingFormat") {
        sourceFiles.from(kotlinAssignmentWrappingFiles)
        maxLineLength.set(120)
    }

val kotlinAssignmentWrappingCheck =
    tasks.register<KotlinAssignmentWrappingCheckTask>("kotlinAssignmentWrappingCheck") {
        sourceFiles.from(kotlinAssignmentWrappingFiles)
        maxLineLength.set(120)
    }

tasks.named("ktlintFormat") {
    finalizedBy(kotlinAssignmentWrappingFormat)
}

tasks.named("ktlintCheck") {
    dependsOn(kotlinAssignmentWrappingCheck)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*ComposableSingletons*",
                    "*_Impl",
                    "*_Impl$*",
                    "*.generated.*",
                    "*ComposeResources*",
                )
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                )
            }
        }
    }
}
