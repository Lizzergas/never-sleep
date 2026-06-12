plugins {
    id("template.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.common)
        api(libs.androidx.datastore)
        api(libs.androidx.datastore.preferences)
        api(libs.kotlinx.coroutinesCore)
        implementation(libs.kermit)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.koin.android)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.kotlinx.coroutinesTest)
    }
}
