plugins {
    id("template.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.connectivity)
        implementation(projects.core.designsystem)
        implementation(projects.core.model)
        implementation(projects.core.navigation)
        implementation(projects.core.network)
        implementation(projects.core.ui)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.kotlinx.coroutinesTest)
        implementation(libs.turbine)
        implementation(libs.ktor.clientMock)
    }
}
