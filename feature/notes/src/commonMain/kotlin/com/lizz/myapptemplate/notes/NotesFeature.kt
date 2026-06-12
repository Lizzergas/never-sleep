package com.lizz.myapptemplate.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.navigation.FeatureDescriptor
import com.lizz.myapptemplate.navigation.FeatureRegistration
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.TopLevelDestination
import com.lizz.myapptemplate.notes.data.NotesRepositoryImpl
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
import org.koin.dsl.module

@Serializable
data object NotesRoute : NavKey

object NotesFeature : FeatureRegistration {
    override val topLevelDestination =
        TopLevelDestination(route = NotesRoute, label = "Notes", icon = Icons.Default.Edit)

    override val descriptors =
        listOf(
            FeatureDescriptor(
                id = "notes",
                title = "Notes",
                description = "The reference feature: full chain from server to UI (copy me)",
                startRoute = NotesRoute,
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
            NotesScreen(onBack = navigator::goBack)
        }
    }
}

val notesKoinModule: Module =
    module {
        single<NotesRepository> { NotesRepositoryImpl(httpClient = get(), noteDao = get()) }
        factoryOf(::AddNoteUseCase)
        viewModelOf(::NotesViewModel)
    }
