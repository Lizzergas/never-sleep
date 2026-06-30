import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("template.quality")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            binaryOption("bundleId", "com.lizz.myapptemplate.shared")
            isStatic = true
        }
    }

    jvm()

    android {
        namespace = "com.lizz.myapptemplate.app.shared"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kotlinx.coroutinesAndroid)
        }
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.common)
            api(projects.core.designsystem)
            implementation(projects.core.connectivity)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            api(projects.core.navigation)
            api(projects.core.network)
            implementation(projects.core.ui)
            implementation(projects.feature.auth)
            implementation(projects.feature.notes)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.settings)
            implementation(projects.feature.showcase)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)

            // DI
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)

            // Navigation
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)

            // Baseline libs ready for app code (image loading, file pickers) —
            // not yet exercised by the showcase.
            implementation(libs.coil.compose)
            implementation(libs.coil.networkKtor3)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogsCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        jvmTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.testJunit)
            implementation(libs.compose.uiTestJunit4)
            implementation(compose.desktop.currentOs)
            // collectAsStateWithLifecycle needs a Main dispatcher in JVM tests.
            implementation(libs.kotlinx.coroutinesSwing)
            // Real client <-> server e2e test spins up the template server in-process.
            implementation(projects.server)
            implementation(libs.ktor.serverNetty)
            implementation(libs.ktor.clientMock)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
