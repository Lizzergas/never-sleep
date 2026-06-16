import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Base convention for every Kotlin Multiplatform library module in the template:
 * Android library + iOS (arm64, simulator arm64) + Desktop JVM targets,
 * namespace derived from the module path, kotlin-test in commonTest.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("template.quality")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    android {
        namespace = "com.lizz.myapptemplate" +
                project.path.replace(":", ".").replace("-", "")
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
    }

    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.findLibrary("kotlin-test").get())
    }
}
