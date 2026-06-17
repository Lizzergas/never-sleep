import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("template.quality")
}

dependencies {
    implementation(projects.app.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    testImplementation(libs.kotlin.testJunit)
}

compose.desktop {
    application {
        mainClass = "com.lizz.myapptemplate.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.lizz.myapptemplate"
            packageVersion = "1.0.0"

            modules("jdk.unsupported")

            macOS {
                bundleID = "com.lizz.myapptemplate"
                infoPlist {
                    extraKeysRawXml =
                        """
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>com.lizz.myapptemplate</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>myapptemplate</string>
                                </array>
                            </dict>
                        </array>
                        """.trimIndent()
                }
            }
        }
    }
}
