plugins {
    id("template.kmp.library")
}

kotlin {
    // This small product module also supports the standalone Android app.
    android {
        minSdk = 24
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ReviewKit"
            isStatic = false
            binaryOption("bundleId", "com.lizz.neversleep.review")
        }
    }
}
