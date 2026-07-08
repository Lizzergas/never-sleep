package com.lizz.neversleep.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatform

/**
 * Looks up an OPTIONAL Koin binding (the cross-feature contract pattern:
 * ThemeModeProvider, StartRouteOverride, ...). Returns null both when the
 * binding is absent (feature removed) and when Koin isn't running at all
 * (@Previews, Content-level tests) — the runCatching is what keeps previews
 * alive; don't "simplify" it away.
 */
@Composable
inline fun <reified T : Any> rememberOptionalKoin(): T? =
    remember {
        runCatching { KoinPlatform.getKoin().getOrNull<T>() }.getOrNull()
    }
