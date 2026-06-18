plugins {
    id("template.kmp.library")
    id("template.compose")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.androidx.navigation3.runtime)
        api(libs.kotlinx.serializationJson)
        api(libs.compose.runtime)
        // ImageVector for primary navigation icons.
        api(libs.compose.ui)
    }
}
