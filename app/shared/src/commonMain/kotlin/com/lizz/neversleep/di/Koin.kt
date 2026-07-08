package com.lizz.neversleep.di

import com.lizz.neversleep.DeepLinkCoordinator
import com.lizz.neversleep.IosDeepLinkCommandBridge
import com.lizz.neversleep.appDeepLinkRegistry
import com.lizz.neversleep.auth.AccountRoute
import com.lizz.neversleep.auth.authKoinModule
import com.lizz.neversleep.connectivity.connectivityPlatformKoinModule
import com.lizz.neversleep.database.databasePlatformKoinModule
import com.lizz.neversleep.datastore.datastorePlatformKoinModule
import com.lizz.neversleep.defaultServerBaseUrl
import com.lizz.neversleep.featureRegistrations
import com.lizz.neversleep.navigation.FeatureCatalog
import com.lizz.neversleep.network.NetworkConfig
import com.lizz.neversleep.network.networkKoinModule
import com.lizz.neversleep.notes.notesKoinModule
import com.lizz.neversleep.onboarding.onboardingKoinModule
import com.lizz.neversleep.settings.settingsKoinModule
import com.lizz.neversleep.showcase.showcaseKoinModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Permanent app-level DI definitions live here. */
val appModule = module {
    // The showcase home screen lists features from this catalog.
    // (Top-level destinations live in the shell's bar/rail, not here.)
    single { FeatureCatalog(featureRegistrations.flatMap { it.descriptors }) }
    single { appDeepLinkRegistry() }
    single { DeepLinkCoordinator(get()) }
    single { IosDeepLinkCommandBridge(registry = get(), accountRoute = AccountRoute) }
    // Point this at your API; the template default is the local sample server.
    single { NetworkConfig(baseUrl = defaultServerBaseUrl()) }
}

private val coreModules: List<Module> = listOf(
    appModule,
    connectivityPlatformKoinModule,
    databasePlatformKoinModule,
    datastorePlatformKoinModule,
    networkKoinModule,
)

private val featureModules: List<Module> = listOf(
    authKoinModule,
    notesKoinModule,
    onboardingKoinModule,
    settingsKoinModule,
    showcaseKoinModule,
)

private val appModules: List<Module> = coreModules + featureModules

/**
 * THE feature plug-in point for DI. Each feature contributes one Koin module.
 * Starts once per process from each platform entry point:
 * - Android: MainApplication.onCreate { initKoin { androidContext(...) } }
 * - Desktop: main() before the Compose application
 * - iOS: iOSApp.init via doInitKoin()
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules)
    }
}
