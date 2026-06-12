plugins {
    id("template.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.connectivity)
        implementation(projects.core.database)
        implementation(projects.core.designsystem)
        implementation(projects.core.navigation)
        implementation(projects.core.network)
        implementation(projects.core.ui)
    }
}
