package com.lizz.neversleep

import android.content.Context
import android.provider.Settings

object NeverSleepController {
    private const val PREFS_NAME = "never_sleep_prefs"
    private const val KEY_PREVIOUS_TIMEOUT = "previous_timeout"
    private const val KEY_ENABLED = "never_sleep_enabled"
    private const val KEY_ADS_DISABLED = "ads_disabled"
    private const val DEFAULT_NORMAL_TIMEOUT_MS = 30_000
    private const val NEVER_SLEEP_TIMEOUT_MS = 1_000_000_000

    @Synchronized
    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return false

        val current = readTimeout(context) ?: return true
        val stillApplied = current >= NEVER_SLEEP_TIMEOUT_MS
        if (!stillApplied) {
            // The user, device policy, or OEM changed the timeout outside the app.
            // Stop claiming the feature is active, but keep the previous value so a
            // later enable can replace it with the device's current timeout.
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        }
        return stillApplied
    }

    @Synchronized
    fun toggle(context: Context): Boolean =
        if (isEnabled(context)) {
            setNormal(context)
        } else {
            setNeverSleep(context)
        }

    @Synchronized
    fun setNeverSleep(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            return false
        }
        if (isEnabled(context)) return true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = readTimeout(context) ?: DEFAULT_NORMAL_TIMEOUT_MS
        val hadStoredPrevious = prefs.contains(KEY_PREVIOUS_TIMEOUT)
        val storedPrevious = prefs.getInt(KEY_PREVIOUS_TIMEOUT, DEFAULT_NORMAL_TIMEOUT_MS)

        // Journal ownership before changing the system value. If the process stops
        // between these operations, isEnabled() reconciles the flag with the real
        // timeout on the next read.
        val prepared = prefs
            .edit()
            .putInt(KEY_PREVIOUS_TIMEOUT, previous)
            .putBoolean(KEY_ENABLED, true)
            .commit()
        if (!prepared) return false

        val success = try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                NEVER_SLEEP_TIMEOUT_MS,
            )
        } catch (e: Exception) {
            false
        }
        if (!success) {
            val rollback = prefs.edit().putBoolean(KEY_ENABLED, false)
            if (hadStoredPrevious) {
                rollback.putInt(KEY_PREVIOUS_TIMEOUT, storedPrevious)
            } else {
                rollback.remove(KEY_PREVIOUS_TIMEOUT)
            }
            rollback.commit()
        }
        return success
    }

    @Synchronized
    fun setNormal(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!isEnabled(context)) return true

        val previous = prefs.getInt(KEY_PREVIOUS_TIMEOUT, DEFAULT_NORMAL_TIMEOUT_MS)
        val success = try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                previous,
            )
        } catch (e: Exception) {
            false
        }
        if (success) {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        }
        return success
    }

    fun areAdsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_ADS_DISABLED, false)
    }

    fun setAdsEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADS_DISABLED, !enabled).apply()
    }

    private fun readTimeout(context: Context): Int? =
        try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                DEFAULT_NORMAL_TIMEOUT_MS,
            )
        } catch (e: Exception) {
            null
        }
}
