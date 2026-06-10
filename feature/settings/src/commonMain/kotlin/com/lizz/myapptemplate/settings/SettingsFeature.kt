package com.lizz.myapptemplate.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.designsystem.ThemeModeProvider
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

@Serializable
data object SettingsRoute : NavKey

object SettingsFeature : FeatureRegistration {

    override val descriptors = listOf(
        FeatureDescriptor(
            id = "settings",
            title = "Settings",
            description = "Theme mode persisted via core:datastore",
            startRoute = SettingsRoute,
        ),
    )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(SettingsRoute::class)
    }

    override fun registerEntries(scope: EntryProviderScope<NavKey>, navigator: Navigator) {
        scope.entry<SettingsRoute> {
            SettingsScreen(onBack = navigator::goBack)
        }
    }
}

val settingsKoinModule: Module = module {
    singleOf(::SettingsRepository) bind ThemeModeProvider::class
    viewModelOf(::SettingsViewModel)
}
