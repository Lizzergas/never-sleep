import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("template.quality")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.core.review)
    implementation(libs.play.review)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Compose for UI
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    // Material icons for the toggle
    implementation(libs.compose.materialIconsCore)

    // Activity + Compose integration
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
    namespace = "com.lizz.neversleep"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    val admobPropertiesFile = project.file("admob.properties")
    val admobProperties = Properties()
    if (admobPropertiesFile.exists()) {
        admobProperties.load(FileInputStream(admobPropertiesFile))
    }
    val defaultAdmobAppId = admobProperties.getProperty(
        "admob.app.id",
        "ca-app-pub-3940256099942544~3347511713",
    )
    val defaultBannerUnitId = admobProperties.getProperty(
        "admob.banner.unit.id",
        "ca-app-pub-3940256099942544/6300978111",
    )

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.lizz.neversleep"
        // The standalone product supports Android 7; KMP template modules keep
        // their own Android 12 minimum in the shared version catalog.
        minSdk = 24
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()

        // === Versioning for Google Play ===
        // Increment versionCode (integer) for EVERY release / upload to Play Store.
        // versionName is the user-visible string (use semantic versioning).
        versionCode = 5
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = defaultAdmobAppId
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$defaultBannerUnitId\"")
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://neversleep.app/privacy\"",
        )
    }

    // Signing configuration loaded from keystore.properties (see instructions below).
    // NEVER commit actual keystore files or passwords.
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            // These values come from keystore.properties if present.
            // Example keystore.properties (git-ignored):
            // storeFile=keystores/never-sleep-upload.keystore
            // storePassword=yourStorePassword
            // keyAlias=never-sleep-upload
            // keyPassword=yourKeyPassword
            val storeFilePath = keystoreProperties["storeFile"] as? String
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        debug {
            // Debug builds get a different app id so you can install side-by-side with release
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                "\"ca-app-pub-3940256099942544/6300978111\"",
            )
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            // Its JNI keep rule otherwise retains unused path-iteration stubs.
            // proguard-rules.pro verifies that this whole library is discarded.
            optimization.keepRules {
                ignoreFrom("androidx.graphics:graphics-path:1.0.1")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Release preflight below rejects missing credentials. Play App Signing
            // still requires an upload-key signature on the bundle we submit.
            if (keystoreProperties["storeFile"] != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // Exclude only when R8 proves the AndroidX path library is unused.
        // Debug builds retain it for unrestricted tooling and experimentation.
        variant.packaging.jniLibs.excludes
            .add("**/libandroidx.graphics.path.so")
    }
}

val validateReleaseInputs by tasks.registering(Exec::class) {
    group = "verification"
    description = "Rejects missing signing or test/placeholder AdMob IDs in release builds."
    commandLine(
        "python3",
        rootProject.file("scripts/android-release.py").absolutePath,
        "check-config",
    )
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseInputs)
}
