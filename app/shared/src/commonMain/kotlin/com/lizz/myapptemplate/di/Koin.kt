package com.lizz.myapptemplate.di

import com.lizz.myapptemplate.auth.authKoinModule
import com.lizz.myapptemplate.connectivity.connectivityPlatformKoinModule
import com.lizz.myapptemplate.database.databasePlatformKoinModule
import com.lizz.myapptemplate.datastore.datastorePlatformKoinModule
import com.lizz.myapptemplate.defaultServerBaseUrl
import com.lizz.myapptemplate.featureRegistrations
import com.lizz.myapptemplate.navigation.FeatureCatalog
import com.lizz.myapptemplate.network.NetworkConfig
import com.lizz.myapptemplate.network.networkKoinModule
import com.lizz.myapptemplate.onboarding.onboardingKoinModule
import com.lizz.myapptemplate.settings.settingsKoinModule
import com.lizz.myapptemplate.showcase.showcaseKoinModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Permanent app-level DI definitions live here. */
val appModule =
    module {
        // The showcase home screen lists features from this catalog.
        single { FeatureCatalog(featureRegistrations.flatMap { it.descriptors }) }
        // Point this at your API; the template default is the local sample server.
        single { NetworkConfig(baseUrl = defaultServerBaseUrl()) }
    }

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
        modules(
            appModule,
            authKoinModule,
            connectivityPlatformKoinModule,
            databasePlatformKoinModule,
            datastorePlatformKoinModule,
            networkKoinModule,
            onboardingKoinModule,
            settingsKoinModule,
            showcaseKoinModule,
        )
    }
}
