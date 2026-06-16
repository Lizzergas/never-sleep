import buildlogic.QualityFilters
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
