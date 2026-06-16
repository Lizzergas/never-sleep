plugins {
    id("template.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.common)
        implementation(projects.core.designsystem)
        implementation(projects.core.model)
        implementation(projects.core.navigation)
        implementation(projects.core.network)
        implementation(projects.core.ui)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.kvault)
        implementation(libs.koin.android)
    }
    sourceSets.iosMain.dependencies {
        implementation(libs.kvault)
    }
    sourceSets.jvmTest.dependencies {
        implementation(compose.desktop.currentOs)
        implementation(libs.compose.uiTestJunit4)
        implementation(libs.kotlinx.coroutinesTest)
        implementation(libs.turbine)
        implementation(libs.ktor.clientMock)
    }
}
