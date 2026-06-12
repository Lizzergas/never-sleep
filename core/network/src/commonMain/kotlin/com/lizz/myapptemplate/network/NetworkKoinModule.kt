package com.lizz.myapptemplate.network

import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

/**
 * Provides the app-wide HttpClient. The app's Koin setup must also provide a
 * [NetworkConfig] (see appModule) — override it to point at your API.
 */
val networkKoinModule: Module =
    module {
        single {
            createHttpClient(config = get(), authTokenProvider = getOrNull())
        } withOptions {
            onClose { it?.close() }
        }
    }
