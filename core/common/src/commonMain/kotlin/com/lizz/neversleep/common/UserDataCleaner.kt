package com.lizz.neversleep.common

/**
 * Clears feature-local user data (caches, drafts) when the session changes.
 * Features that cache per-user data bind an implementation in their Koin
 * module; feature:auth collects ALL bindings (Koin getAll) and runs them on
 * login and logout — features never depend on each other.
 */
interface UserDataCleaner {
    suspend fun clearUserData()
}
