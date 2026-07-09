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
    private const val NEVER_SLEEP_THRESHOLD_MS = 3_600_000

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENABLED, false)) return true
        val current = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                DEFAULT_NORMAL_TIMEOUT_MS,
            )
        } catch (e: Exception) {
            DEFAULT_NORMAL_TIMEOUT_MS
        }
        return current >= NEVER_SLEEP_THRESHOLD_MS
    }

    fun toggle(context: Context): Boolean =
        if (isEnabled(context)) {
            setNormal(context)
        } else {
            setNeverSleep(context)
        }

    fun setNeverSleep(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            // Can't show UI from here easily; caller should handle opening settings
            return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                DEFAULT_NORMAL_TIMEOUT_MS,
            )
        } catch (e: Exception) {
            DEFAULT_NORMAL_TIMEOUT_MS
        }
        prefs
            .edit()
            .putInt(KEY_PREVIOUS_TIMEOUT, previous)
            .putBoolean(KEY_ENABLED, true)
            .apply()

        val success = try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                NEVER_SLEEP_TIMEOUT_MS,
            )
        } catch (e: Exception) {
            false
        }
        return success
    }

    fun setNormal(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getInt(KEY_PREVIOUS_TIMEOUT, DEFAULT_NORMAL_TIMEOUT_MS)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()

        val success = try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                previous,
            )
        } catch (e: Exception) {
            false
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
}
