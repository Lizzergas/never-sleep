package com.lizz.myapptemplate.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.network.AuthTokenProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

@Serializable
data object AccountRoute : NavKey

object AuthFeature : FeatureRegistration {
    override val descriptors =
        listOf(
            FeatureDescriptor(
                id = "account",
                title = "Account",
                description = "JWT auth: register, login, 401 auto-refresh, secure token storage",
                startRoute = AccountRoute,
            ),
        )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(AccountRoute::class)
    }

    override fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    ) {
        scope.entry<AccountRoute> {
            AccountScreen(onBack = navigator::goBack)
        }
    }
}

val authKoinModule: Module =
    module {
        includes(tokenStoragePlatformKoinModule)
        single { AuthRepository(config = get(), storage = get()) } bind AuthTokenProvider::class
        viewModelOf(::SessionViewModel)
    }
