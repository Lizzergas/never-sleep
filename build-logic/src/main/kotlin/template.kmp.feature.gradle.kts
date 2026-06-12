import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Convention for feature:* modules: KMP library + Compose + serialization,
 * with the UI/DI/navigation baseline every feature needs. A feature build
 * file should only declare its core:* dependencies on top of this.
 */
plugins {
    id("template.kmp.library")
    id("template.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun lib(alias: String) = libs.findLibrary(alias).get()

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(lib("compose-runtime"))
        implementation(lib("compose-foundation"))
        implementation(lib("compose-material3"))
        implementation(lib("compose-ui"))
        implementation(lib("compose-uiToolingPreview"))
        implementation(lib("compose-materialIconsCore"))
        implementation(lib("androidx-lifecycle-viewmodelCompose"))
        implementation(lib("androidx-lifecycle-runtimeCompose"))

        implementation(project.dependencies.platform(lib("koin-bom")))
        implementation(lib("koin-core"))
        implementation(lib("koin-compose"))
        implementation(lib("koin-composeViewmodel"))

        implementation(lib("jetbrains-navigation3-ui"))
        implementation(lib("androidx-lifecycle-viewmodelNavigation3"))

        implementation(lib("kermit"))
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(lib("kotlinx-coroutinesTest"))
    }
}
