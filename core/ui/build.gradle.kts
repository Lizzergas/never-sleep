plugins {
    id("template.kmp.library")
    id("template.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        implementation(projects.core.designsystem)
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.junit)
        implementation(libs.kotlin.testJunit)
        implementation(libs.compose.uiTestJunit4)
        implementation(compose.desktop.currentOs)
    }
}
