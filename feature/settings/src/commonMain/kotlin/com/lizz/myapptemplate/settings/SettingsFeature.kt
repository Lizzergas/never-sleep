package com.lizz.myapptemplate.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import com.lizz.myapptemplate.navigation.DeepLinkPattern
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.DeepLinkSpec
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.settings.data.DataStoreSettingsRepository
import com.lizz.myapptemplate.settings.domain.SettingsRepository
import com.lizz.myapptemplate.settings.presentation.SettingsScreen
import com.lizz.myapptemplate.settings.presentation.SettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

@Serializable
data object SettingsRoute : NavKey

object SettingsFeature : FeatureRegistration {
    override val topLevelDestination =
        TopLevelDestination(
            route = SettingsRoute,
            label = "Settings",
            icon = Icons.Default.Settings
        )

    override val deepLinks = listOf(
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("settings"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = SettingsRoute,
                    stack = listOf(SettingsRoute),
                )
            },
        ),
    )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(SettingsRoute::class)
    }

    override fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    ) {
        scope.entry<SettingsRoute> {
            SettingsScreen()
        }
    }
}

val settingsKoinModule: Module =
    module {
        single { DataStoreSettingsRepository(get()) } binds arrayOf(
            SettingsRepository::class,
            ThemeModeProvider::class
        )
        viewModelOf(::SettingsViewModel)
    }
