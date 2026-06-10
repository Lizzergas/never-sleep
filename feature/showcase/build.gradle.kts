plugins {
    id("template.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.designsystem)
        implementation(projects.core.navigation)
    }
}
