plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "com.lizz.myapptemplate"
version = "1.0.0"
application {
    mainClass = "com.lizz.myapptemplate.ApplicationKt"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.ktor)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.kotlin.testJunit)
}