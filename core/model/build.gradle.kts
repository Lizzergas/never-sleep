plugins {
    id("template.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // Exported: DTOs and shared types appear in public signatures
        // of both clients and the server.
        api(libs.kotlinx.serializationJson)
        api(libs.kotlinx.datetime)
    }
}
