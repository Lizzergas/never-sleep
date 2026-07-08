package com.lizz.neversleep.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.neversleep.auth.data.SessionRepositoryImpl
import com.lizz.neversleep.auth.data.tokenStoragePlatformKoinModule
import com.lizz.neversleep.auth.domain.SessionRepository
import com.lizz.neversleep.auth.domain.ValidateCredentialsUseCase
import com.lizz.neversleep.auth.presentation.AccountScreen
import com.lizz.neversleep.auth.presentation.SessionViewModel
import com.lizz.neversleep.common.UserDataCleaner
import com.lizz.neversleep.navigation.AppDestination
import com.lizz.neversleep.navigation.DeepLinkPattern
import com.lizz.neversleep.navigation.DeepLinkResolution
import com.lizz.neversleep.navigation.DeepLinkSpec
import com.lizz.neversleep.navigation.DestinationKind
import com.lizz.neversleep.navigation.FeatureRegistration
import com.lizz.neversleep.navigation.Navigator
import com.lizz.neversleep.navigation.PrimaryNavigationItem
import com.lizz.neversleep.navigation.RouteContentRegistryBuilder
import com.lizz.neversleep.navigation.TopBarConfig
import com.lizz.neversleep.navigation.TopBarMode
import com.lizz.neversleep.network.AuthTokenProvider
import com.lizz.neversleep.network.clearBearerTokens
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
                scheme = "neversleep",
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
            AccountRouteContent()
        }
    }

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<AccountRoute> {
            AccountRouteContent()
        }
    }
}

@Composable
private fun AccountRouteContent() {
    AccountScreen()
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
