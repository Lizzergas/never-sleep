// ============================================================================
// TEMPORARY DEPENDENCY-VERIFICATION SCREEN — DELETE once real app code lands.
// Its only purpose is to prove every baseline dependency compiles on all
// targets and works at runtime: Koin, Navigation3, Room3 (KSP), DataStore,
// Coil 3, Ktor client, FileKit, kotlinx-serialization/datetime, Kermit.
//
// Deletion checklist:
//   DELETE this file
//   DELETE the DemoScreen() call in App.kt (put real content there)
//   DELETE the demoKoinModule registration in di/Koin.kt
//   DELETE app/shared/schemas/ (the DemoDatabase schema export)
//   KEEP   the room3/KSP wiring and -Xexpect-actual-classes flag in
//          app/shared/build.gradle.kts (any real Room database needs them)
//   KEEP   di/Koin.kt and the platform initKoin() calls (MainApplication,
//          desktop main(), iOSApp.init)
// ============================================================================
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.lizz.myapptemplate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

// --- kotlinx-serialization ---------------------------------------------------

@Serializable
data class DemoPayload(val id: Int, val message: String)

// --- DataStore (compile-level check of the preferences API) ------------------

val demoPreferenceKey = booleanPreferencesKey("demo_flag")

// --- Room 3 (the KSP codegen is the real verification here) ------------------

@Entity
data class DemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
)

@Dao
interface DemoDao {
    @Insert
    suspend fun insert(entity: DemoEntity)

    @Query("SELECT * FROM DemoEntity")
    fun observeAll(): Flow<List<DemoEntity>>
}

@Database(entities = [DemoEntity::class], version = 1)
@ConstructedBy(DemoDatabaseConstructor::class)
abstract class DemoDatabase : RoomDatabase() {
    abstract fun demoDao(): DemoDao
}

// The Room compiler generates the `actual` implementations per platform.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object DemoDatabaseConstructor : RoomDatabaseConstructor<DemoDatabase> {
    override fun initialize(): DemoDatabase
}

// --- Koin ---------------------------------------------------------------------

class DemoService {
    fun ping(): String = "pong"
}

class DemoViewModel(private val service: DemoService) : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun increment() {
        _count.update { it + 1 }
        Logger.i { "DemoViewModel.increment -> ${_count.value} (${service.ping()})" }
    }
}

val demoKoinModule = module {
    singleOf(::DemoService)
    viewModelOf(::DemoViewModel)
    // One HttpClient per app, closed when Koin stops — never build clients per screen.
    single {
        HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    } withOptions {
        onClose { it?.close() }
    }
}

// --- Navigation3 routes ---------------------------------------------------------

@Serializable
data object DemoHomeRoute : NavKey

@Serializable
data object DemoDetailRoute : NavKey

// NavKey routes are restored polymorphically from saved state, so every route
// type must be registered in the configuration's serializers module.
private val demoNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DemoHomeRoute::class)
            subclass(DemoDetailRoute::class)
        }
    }
}

// --- UI -------------------------------------------------------------------------

@Composable
fun DemoScreen() {
    // Koin itself is started by each platform entry point via di/initKoin().
    val backStack = rememberNavBackStack(demoNavConfiguration, DemoHomeRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes ViewModels to the nav entry, cleared when the entry is popped.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<DemoHomeRoute> {
                DemoHomePage(onOpenDetail = { backStack.add(DemoDetailRoute) })
            }
            entry<DemoDetailRoute> {
                DemoDetailPage(onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

@Composable
private fun DemoHomePage(onOpenDetail: () -> Unit) {
    val viewModel = koinViewModel<DemoViewModel>()
    val service = koinInject<DemoService>()
    val count by viewModel.count.collectAsState()
    val scope = rememberCoroutineScope()

    var httpStatus by rememberSaveable { mutableStateOf("not requested") }
    var pickedFile by rememberSaveable { mutableStateOf("none") }

    val httpClient = koinInject<HttpClient>()
    val filePicker = rememberFilePickerLauncher { file ->
        pickedFile = file?.name ?: "cancelled"
    }

    val serialized = remember { Json.encodeToString(DemoPayload(1, "hello")) }
    val now = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Dependency baseline check", style = MaterialTheme.typography.titleLarge)
        Text("Serialization: $serialized")
        Text("Datetime: $now")
        Text("Koin service: ${service.ping()}")
        Text("DataStore key: ${demoPreferenceKey.name}")
        Text("Room: DemoDatabase compiles via KSP")

        Button(onClick = viewModel::increment) {
            Text("ViewModel count: $count")
        }
        Button(onClick = {
            scope.launch {
                httpStatus = try {
                    Logger.i { "Ktor GET https://ktor.io" }
                    httpClient.get("https://ktor.io").status.toString()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "failed: ${e.message}"
                }
            }
        }) {
            Text("Ktor GET: $httpStatus")
        }
        Button(onClick = { filePicker.launch() }) {
            Text("FileKit pick: $pickedFile")
        }
        Button(onClick = onOpenDetail) {
            Text("Navigate to detail")
        }

        Text("Coil image:")
        AsyncImage(
            model = "https://avatars.githubusercontent.com/u/878437?s=200&v=4",
            contentDescription = "JetBrains logo via Coil",
            modifier = Modifier.size(96.dp),
        )
    }
}

@Composable
private fun DemoDetailPage(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Navigation3 detail page", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
