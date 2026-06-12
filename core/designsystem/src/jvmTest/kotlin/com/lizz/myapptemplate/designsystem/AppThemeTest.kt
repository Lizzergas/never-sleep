package com.lizz.myapptemplate.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AppThemeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun darkModeResolvesDarkScheme() {
        var resolvedSurface: Color? = null
        rule.setContent {
            AppTheme(themeMode = ThemeMode.Dark) {
                resolvedSurface = MaterialTheme.colorScheme.surface
            }
        }
        rule.waitForIdle()
        assertEquals(DarkColorScheme.surface, resolvedSurface)
    }

    @Test
    fun lightModeResolvesLightScheme() {
        var resolvedSurface: Color? = null
        rule.setContent {
            AppTheme(themeMode = ThemeMode.Light) {
                resolvedSurface = MaterialTheme.colorScheme.surface
            }
        }
        rule.waitForIdle()
        assertEquals(LightColorScheme.surface, resolvedSurface)
    }

    @Test
    fun spacingTokensAreExposedInsideTheme() {
        var md: Dp? = null
        rule.setContent {
            AppTheme(themeMode = ThemeMode.Light) {
                md = Theme.spacing.md
            }
        }
        rule.waitForIdle()
        assertEquals(Spacing().md, md)
    }
}
