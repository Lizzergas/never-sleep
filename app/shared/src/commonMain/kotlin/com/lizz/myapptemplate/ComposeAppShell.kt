package com.lizz.myapptemplate

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.lizz.myapptemplate.designsystem.WindowWidthClass
import com.lizz.myapptemplate.navigation.AppDestination
import com.lizz.myapptemplate.navigation.TopBarMode

/** Bottom bar on compact widths, navigation rail on medium/expanded. */
@Composable
internal fun ComposeAppShell(
    destinations: List<AppDestination>,
    selectedTopLevelRoute: NavKey,
    currentDestination: AppDestination?,
    canNavigateUp: Boolean,
    showShell: Boolean,
    onNavigateUp: () -> Unit,
    onSelect: (AppDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        when {
            !showShell -> content(Modifier.fillMaxSize())
            widthClass == WindowWidthClass.Compact -> Scaffold(
                topBar = {
                    AppTopBar(
                        destination = currentDestination,
                        canNavigateUp = canNavigateUp,
                        onNavigateUp = onNavigateUp,
                    )
                },
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val primaryNavigation = destination.primaryNavigation ?: return@forEach
                            NavigationBarItem(
                                selected = selectedTopLevelRoute == destination.route,
                                onClick = { onSelect(destination) },
                                icon = {
                                    Icon(
                                        primaryNavigation.materialIcon,
                                        contentDescription = primaryNavigation.label,
                                    )
                                },
                                label = { Text(primaryNavigation.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                content(Modifier.fillMaxSize().padding(padding))
            }

            else ->
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            val primaryNavigation = destination.primaryNavigation ?: return@forEach
                            NavigationRailItem(
                                selected = selectedTopLevelRoute == destination.route,
                                onClick = { onSelect(destination) },
                                icon = {
                                    Icon(
                                        primaryNavigation.materialIcon,
                                        contentDescription = primaryNavigation.label,
                                    )
                                },
                                label = { Text(primaryNavigation.label) },
                            )
                        }
                    }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            AppTopBar(
                                destination = currentDestination,
                                canNavigateUp = canNavigateUp,
                                onNavigateUp = onNavigateUp,
                            )
                        },
                    ) { padding ->
                        content(Modifier.fillMaxSize().padding(padding))
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    destination: AppDestination?,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
) {
    if (destination == null || destination.topBar.mode == TopBarMode.Hidden) return

    val navigationIcon: @Composable () -> Unit = {
        if (canNavigateUp) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    }
    val title: @Composable () -> Unit = {
        Text(destination.topBar.title)
    }

    when (destination.topBar.mode) {
        TopBarMode.Large -> LargeTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
        )

        TopBarMode.Inline -> TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
        )

        TopBarMode.Hidden -> Unit
    }
}
