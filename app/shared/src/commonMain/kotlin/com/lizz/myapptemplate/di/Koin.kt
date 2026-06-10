package com.lizz.myapptemplate.di

import com.lizz.myapptemplate.demoKoinModule
import com.lizz.myapptemplate.featureRegistrations
import com.lizz.myapptemplate.navigation.FeatureCatalog
import com.lizz.myapptemplate.showcase.showcaseKoinModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Permanent app-level DI definitions live here. */
val appModule = module {
    // The showcase home screen lists features from this catalog.
    single { FeatureCatalog(featureRegistrations.flatMap { it.descriptors }) }
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
            showcaseKoinModule,
            // TEMPORARY: dependency-verification demo module — remove together with DemoScreen.kt
            demoKoinModule,
        )
    }
}
