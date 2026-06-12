package com.lizz.myapptemplate.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Lets a feature replace the app's start destination (e.g. feature:onboarding
 * until its seen-flag is set). Bound in the owning feature's Koin module; the
 * shell looks it up with a fallback, so the feature stays removable.
 */
interface StartRouteOverride {
    /** Route to start at instead of the default, or null to keep the default. */
    suspend fun startRoute(): NavKey?
}
