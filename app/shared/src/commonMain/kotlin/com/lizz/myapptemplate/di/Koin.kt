package com.lizz.myapptemplate.di

import com.lizz.myapptemplate.demoKoinModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Permanent app-level DI definitions live here. */
val appModule = module {
    // single { ... } / viewModelOf(::...) for real app code
}

/**
 * Starts Koin once per process. Each platform entry point calls this:
 * - Android: MainApplication.onCreate { initKoin { androidContext(...) } }
 * - Desktop: main() before the Compose application
 * - iOS: iOSApp.init via doInitKoin()
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appModule,
            // TEMPORARY: dependency-verification demo module — remove together with DemoScreen.kt
            demoKoinModule,
        )
    }
}
