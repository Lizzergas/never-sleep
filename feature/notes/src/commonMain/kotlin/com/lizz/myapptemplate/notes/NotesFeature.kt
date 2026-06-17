package com.lizz.myapptemplate.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.common.UserDataCleaner
import com.lizz.myapptemplate.navigation.DeepLinkPattern
import com.lizz.myapptemplate.navigation.DeepLinkResolution
import com.lizz.myapptemplate.navigation.DeepLinkSpec
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.TopLevelDestination
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
    override val topLevelDestination =
        TopLevelDestination(route = NotesRoute, label = "Notes", icon = Icons.Default.Edit)

    override val deepLinks =
        listOf(
            DeepLinkSpec(
                pattern =
                    DeepLinkPattern(
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
            NotesScreen()
        }
    }
}

val notesKoinModule: Module =
    module {
        single<NotesRepository> { NotesRepositoryImpl(httpClient = get(), noteDao = get()) }
        single { NotesUserDataCleaner(get()) } bind UserDataCleaner::class
        factoryOf(::AddNoteUseCase)
        viewModelOf(::NotesViewModel)
    }
