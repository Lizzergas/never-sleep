plugins {
    id("template.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.datastore)
        implementation(projects.core.designsystem)
        implementation(projects.core.navigation)
        implementation(projects.core.ui)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.kotlinx.coroutinesTest)
        implementation(libs.turbine)
    }
}
