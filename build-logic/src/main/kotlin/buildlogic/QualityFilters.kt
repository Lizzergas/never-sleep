package buildlogic

import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec

/**
 * Filters for lint tasks. Defined in a plain Kotlin file (NOT a .gradle.kts
 * script) so the Spec captures no script object references — lambdas declared
 * inside precompiled script plugins break the configuration cache.
 */
object QualityFilters {
    /** Generated sources (KSP, compose resources) live under build/. */
    val generatedSources: Spec<FileTreeElement> =
        Spec { element -> element.file.path.contains("/build/generated/") }
}
