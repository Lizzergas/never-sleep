plugins {
    id("template.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        api(libs.ktor.clientCore)
        api(libs.ktor.clientAuth)
        implementation(libs.ktor.clientContentNegotiation)
        implementation(libs.ktor.clientLogging)
        implementation(libs.ktor.serializationKotlinxJson)
        implementation(libs.kermit)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.clientOkhttp)
    }
    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.clientDarwin)
    }
    sourceSets.jvmMain.dependencies {
        implementation(libs.ktor.clientOkhttp)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.ktor.clientMock)
        implementation(libs.kotlinx.coroutinesTest)
    }
}
