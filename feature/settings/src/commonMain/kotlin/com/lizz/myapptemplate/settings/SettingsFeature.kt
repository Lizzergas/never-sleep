package com.lizz.myapptemplate.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import com.lizz.myapptemplate.navigation.AppDestination
import com.lizz.myapptemplate.navigation.DeepLinkPattern
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.DeepLinkSpec
import com.lizz.myapptemplate.navigation.DestinationKind
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.PrimaryNavigationItem
import com.lizz.myapptemplate.navigation.RouteContentRegistryBuilder
import com.lizz.myapptemplate.navigation.TopBarConfig
import com.lizz.myapptemplate.navigation.TopBarMode
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
    override val destinations = listOf(
        AppDestination(
            route = SettingsRoute,
            id = "settings",
            kind = DestinationKind.TopLevel,
            topBar = TopBarConfig(title = "Settings", mode = TopBarMode.Large),
            primaryNavigation = PrimaryNavigationItem(
                label = "Settings",
                materialIcon = Icons.Default.Settings,
                systemImage = "gearshape.fill",
            ),
        ),
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

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<SettingsRoute> {
            SettingsScreen()
        }
    }
}

val settingsKoinModule: Module = module {
    single { DataStoreSettingsRepository(get()) } binds arrayOf(
        SettingsRepository::class,
        ThemeModeProvider::class,
    )
    viewModelOf(::SettingsViewModel)
}
