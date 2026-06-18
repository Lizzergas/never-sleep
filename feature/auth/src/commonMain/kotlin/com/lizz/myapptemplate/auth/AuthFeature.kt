package com.lizz.myapptemplate.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.data.SessionRepositoryImpl
import com.lizz.myapptemplate.auth.data.tokenStoragePlatformKoinModule
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.ValidateCredentialsUseCase
import com.lizz.myapptemplate.auth.presentation.AccountScreen
import com.lizz.myapptemplate.auth.presentation.SessionViewModel
import com.lizz.myapptemplate.common.UserDataCleaner
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
import com.lizz.myapptemplate.network.AuthTokenProvider
import com.lizz.myapptemplate.network.clearBearerTokens
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.binds
import org.koin.dsl.module

@Serializable
data object AccountRoute : NavKey

object AuthFeature : FeatureRegistration {
    override val destinations = listOf(
        AppDestination(
            route = AccountRoute,
            id = "account",
            kind = DestinationKind.TopLevel,
            topBar = TopBarConfig(title = "Account", mode = TopBarMode.Large),
            primaryNavigation = PrimaryNavigationItem(
                label = "Account",
                materialIcon = Icons.Default.Person,
                systemImage = "person.fill",
            ),
        ),
    )

    override val deepLinks = listOf(
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("account"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = AccountRoute,
                    stack = listOf(AccountRoute),
                )
            },
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
            AccountScreen()
        }
    }

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<AccountRoute> {
            AccountScreen()
        }
    }
}

val authKoinModule: Module = module {
    includes(tokenStoragePlatformKoinModule)
    single {
        // Lazy lookups: the app HttpClient itself depends on this
        // repository (as AuthTokenProvider), so resolve at call time.
        val koin = getKoin()
        SessionRepositoryImpl(
            config = get(),
            storage = get(),
            onSessionChanged = {
                koin.get<HttpClient>().clearBearerTokens()
                koin.getAll<UserDataCleaner>().forEach { it.clearUserData() }
            },
        )
    } binds arrayOf(SessionRepository::class, AuthTokenProvider::class)
    factory { ValidateCredentialsUseCase() }
    viewModelOf(::SessionViewModel)
}
