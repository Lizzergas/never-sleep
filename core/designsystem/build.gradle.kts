plugins {
    id("template.kmp.library")
    id("template.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.compose.runtime)
        api(libs.compose.foundation)
        api(libs.compose.material3)
        api(libs.compose.ui)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.junit)
        implementation(libs.kotlin.testJunit)
        implementation(compose.desktop.uiTestJUnit4)
        implementation(compose.desktop.currentOs)
    }
}
