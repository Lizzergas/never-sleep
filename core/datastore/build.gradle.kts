plugins {
    id("template.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.androidx.datastore)
        api(libs.androidx.datastore.preferences)
        api(libs.kotlinx.coroutinesCore)
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
