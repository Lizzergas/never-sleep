package com.lizz.neversleep

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NeverSleepApplication::class)
class NeverSleepReviewTest {
    private lateinit var app: NeverSleepApplication

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as NeverSleepApplication
        app
            .getSharedPreferences("never_sleep_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        setCanWrite(true)
        Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)
    }

    private fun setCanWrite(allowed: Boolean) {
        shadowOf(app.getSystemService(AppOpsManager::class.java)).setMode(
            AppOpsManager.OPSTR_WRITE_SETTINGS,
            Process.myUid(),
            app.packageName,
            if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_ERRORED,
        )
    }

    @Test
    fun fiveSuccessfulCyclesBecomeEligibleAndRestoreTimeout() {
        repeat(4) {
            assertTrue(NeverSleepController.setNeverSleep(app))
            assertTrue(NeverSleepController.setNormal(app))
            assertFalse(app.reviewPolicy.isEligible())
        }
        assertTrue(NeverSleepController.setNeverSleep(app))
        assertFalse(app.reviewPolicy.isEligible())
        assertTrue(NeverSleepController.setNormal(app))
        assertTrue(app.reviewPolicy.isEligible())
        assertEquals(
            30_000,
            Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT),
        )
    }

    @Test
    fun repeatedOffAndPermissionFailuresDoNotCount() {
        repeat(5) { NeverSleepController.setNormal(app) }
        setCanWrite(false)
        repeat(5) {
            assertFalse(NeverSleepController.setNeverSleep(app))
            assertFalse(NeverSleepController.setNormal(app))
        }
        assertFalse(app.reviewPolicy.isEligible())
    }

    @Test
    fun repeatedOnDoesNotCreateExtraSessions() {
        repeat(5) { NeverSleepController.setNeverSleep(app) }
        NeverSleepController.setNormal(app)
        assertFalse(app.reviewPolicy.isEligible())
    }

    @Test
    fun screenshotSessionsAndTheirLaterManualOffDoNotCount() {
        repeat(5) {
            NeverSleepController.setNeverSleep(app, countForReview = false)
            NeverSleepController.setNormal(app)
        }
        repeat(5) {
            NeverSleepController.setNeverSleep(app)
            NeverSleepController.setNormal(app, countForReview = false)
        }
        assertFalse(app.reviewPolicy.isEligible())
    }

    @Test
    fun externalTimeoutChangesDoNotCompleteSession() {
        repeat(5) {
            NeverSleepController.setNeverSleep(app)
            Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60_000)
            assertFalse(NeverSleepController.isEnabled(app))
            NeverSleepController.setNormal(app)
        }
        assertFalse(app.reviewPolicy.isEligible())
    }

    @Test
    fun persistedReviewStateIsVisibleToANewPolicy() {
        repeat(5) {
            NeverSleepController.toggle(app)
            NeverSleepController.toggle(app)
        }
        val recreated = com.lizz.neversleep.review.ReviewPolicy(
            SharedPreferencesReviewStateStore(
                app.getSharedPreferences("never_sleep_prefs", Context.MODE_PRIVATE),
            ),
        )
        assertTrue(recreated.claimRequest())
        assertFalse(app.reviewPolicy.isEligible())
    }
}
