package com.lizz.myapptemplate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.auth.domain.SessionRepository
import com.lizz.myapptemplate.auth.domain.SessionState
import com.lizz.myapptemplate.designsystem.AppTheme
import com.lizz.myapptemplate.navigation.Navigator
import com.lizz.myapptemplate.navigation.RouteContentHost
import com.lizz.myapptemplate.navigation.RouteContentRegistry
import com.lizz.myapptemplate.ui.OfflineBanner
import com.lizz.myapptemplate.ui.rememberOptionalKoin

@Composable
fun SingleScreenApp(
    route: NavKey,
    onNavigate: (NavKey) -> Unit,
    onGoBack: () -> Unit,
    onSet: (NavKey) -> Unit,
    onActivate: (NavKey) -> Unit,
) {
    val themeMode = rememberThemeMode()
    val navigator = remember(onNavigate, onGoBack, onSet, onActivate) {
        IosCallbackNavigator(
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onSet = onSet,
            onActivate = onActivate,
        )
    }
    val routeContentRegistry = remember(navigator) {
        appRouteContentRegistry(navigator = navigator)
    }

    AppTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val isOnline = rememberOnlineStatus()
            Column(modifier = Modifier.fillMaxSize()) {
                OfflineBanner(
                    visible = !isOnline,
                    modifier = Modifier.fillMaxWidth(),
                )
                NativePendingDeepLinkEffect(
                    onNavigate = onNavigate,
                    onSet = onSet,
                    onActivate = onActivate,
                )
                AppRouteContentContainer(modifier = Modifier.weight(1f)) {
                    ScreenContent(
                        route = route,
                        registry = routeContentRegistry,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ScreenContent(
    route: NavKey,
    registry: RouteContentRegistry,
) {
    RouteContentHost(
        route = route,
        registry = registry,
        unsupported = { UnsupportedRoute(route = route) },
    )
}

@Composable
private fun NativePendingDeepLinkEffect(
    onNavigate: (NavKey) -> Unit,
    onSet: (NavKey) -> Unit,
    onActivate: (NavKey) -> Unit,
) {
    val repository = rememberOptionalKoin<SessionRepository>()
    val session = repository
        ?.sessionState
        ?.collectAsStateWithLifecycle(initialValue = SessionState.Unknown)
        ?.value

    LaunchedEffect(session) {
        if (session is SessionState.LoggedIn) {
            consumePendingNativeDeepLinkAfterLogin()?.let { command ->
                applyIosNavigationCommand(
                    command = command,
                    onNavigate = onNavigate,
                    onSet = onSet,
                    onActivate = onActivate,
                )
            }
        }
    }
}

private fun applyIosNavigationCommand(
    command: IosNavigationCommand,
    onNavigate: (NavKey) -> Unit,
    onSet: (NavKey) -> Unit,
    onActivate: (NavKey) -> Unit,
) {
    if (command.isFullScreen) {
        onSet(command.stack.last())
        return
    }
    onActivate(command.selectedTopLevelRoute)
    onSet(command.selectedTopLevelRoute)
    command.stack.drop(1).forEach(onNavigate)
}

private class IosCallbackNavigator(
    private val onNavigate: (NavKey) -> Unit,
    private val onGoBack: () -> Unit,
    private val onSet: (NavKey) -> Unit,
    private val onActivate: (NavKey) -> Unit,
) : Navigator {
    override fun navigate(route: NavKey) {
        onNavigate(route)
    }

    override fun goBack() {
        onGoBack()
    }

    override fun resetToStart() {
        onActivate(defaultStartRoute)
        onSet(defaultStartRoute)
    }
}

@Composable
private fun UnsupportedRoute(route: NavKey) {
    Text(
        text = "Unsupported route: $route",
        style = MaterialTheme.typography.bodyMedium,
    )
}
