package com.lizz.myapptemplate.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
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
import com.lizz.myapptemplate.notes.data.NotesRepositoryImpl
import com.lizz.myapptemplate.notes.data.NotesUserDataCleaner
import com.lizz.myapptemplate.notes.domain.AddNoteUseCase
import com.lizz.myapptemplate.notes.domain.NotesRepository
import com.lizz.myapptemplate.notes.presentation.NotesScreen
import com.lizz.myapptemplate.notes.presentation.NotesViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

@Serializable
data object NotesRoute : NavKey

object NotesFeature : FeatureRegistration {
    override val destinations = listOf(
        AppDestination(
            route = NotesRoute,
            id = "notes",
            kind = DestinationKind.TopLevel,
            topBar = TopBarConfig(title = "Notes", mode = TopBarMode.Large),
            primaryNavigation = PrimaryNavigationItem(
                label = "Notes",
                materialIcon = Icons.Default.Edit,
                systemImage = "square.and.pencil",
            ),
        ),
    )

    override val deepLinks = listOf(
        DeepLinkSpec(
            pattern = DeepLinkPattern(
                scheme = "myapptemplate",
                host = "open",
                pathSegments = listOf("notes"),
            ),
            buildResolution = {
                DeepLinkResolution(
                    selectedTopLevelRoute = NotesRoute,
                    stack = listOf(NotesRoute),
                )
            },
        ),
    )

    override fun registerRoutes(builder: PolymorphicModuleBuilder<NavKey>) {
        builder.subclass(NotesRoute::class)
    }

    override fun registerEntries(
        scope: EntryProviderScope<NavKey>,
        navigator: Navigator,
    ) {
        scope.entry<NotesRoute> {
            NotesRouteContent()
        }
    }

    override fun registerRouteContent(
        registry: RouteContentRegistryBuilder,
        navigator: Navigator,
    ) {
        registry.entry<NotesRoute> {
            NotesRouteContent()
        }
    }
}

@Composable
private fun NotesRouteContent() {
    NotesScreen()
}

val notesKoinModule: Module = module {
    single<NotesRepository> { NotesRepositoryImpl(httpClient = get(), noteDao = get()) }
    single { NotesUserDataCleaner(get()) } bind UserDataCleaner::class
    factoryOf(::AddNoteUseCase)
    viewModelOf(::NotesViewModel)
}
