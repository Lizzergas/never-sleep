package com.lizz.myapptemplate.navigation

import androidx.navigation3.runtime.NavKey

/** Navigation actions available to feature screens. Implemented by the app shell. */
interface Navigator {
    fun navigate(route: NavKey)

    fun goBack()

    /** Clears the back stack and lands on the app's default start destination. */
    fun resetToStart()
}
