package com.lizz.myapptemplate.navigation

import androidx.navigation3.runtime.NavKey

/** Navigation actions available to feature screens. Implemented by the app shell. */
interface Navigator {
    fun navigate(route: NavKey)
    fun goBack()
}
