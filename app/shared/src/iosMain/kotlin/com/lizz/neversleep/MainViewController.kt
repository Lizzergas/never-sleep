package com.lizz.neversleep

import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.runBlocking

// PascalCase on purpose: consumed from Swift as MainViewControllerKt.MainViewController().
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController() =
    runBlocking {
        val startRoute = resolveAppStartRoute()
        ComposeUIViewController {
            App(startRoute = startRoute)
        }
    }

fun resolveIosStartRouteBlocking(): NavKey =
    runBlocking {
        resolveAppStartRoute()
    }

// PascalCase on purpose: consumed from Swift as MainViewControllerKt.ScreenViewController(...).
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun ScreenViewController(
    route: NavKey,
    onNavigate: (NavKey) -> Unit,
    onGoBack: () -> Unit,
    onSet: (NavKey) -> Unit,
    onActivate: (NavKey) -> Unit,
) = ComposeUIViewController {
    SingleScreenApp(
        route = route,
        onNavigate = onNavigate,
        onGoBack = onGoBack,
        onSet = onSet,
        onActivate = onActivate,
    )
}
