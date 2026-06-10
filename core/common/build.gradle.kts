plugins {
    id("template.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutinesCore)
        // Logging facade for all shared code.
        api(libs.kermit)
    }
}
