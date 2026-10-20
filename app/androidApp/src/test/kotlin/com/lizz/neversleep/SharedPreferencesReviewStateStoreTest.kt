package com.lizz.neversleep

import android.content.SharedPreferences
import com.lizz.neversleep.review.ReviewPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NeverSleepApplication::class)
class SharedPreferencesReviewStateStoreTest {
    @Test
    fun failedCommitRestoresMemoryCacheAndKeepsRequestEligible() {
        val app: NeverSleepApplication = RuntimeEnvironment.getApplication() as NeverSleepApplication
        val preferences = app.getSharedPreferences("review_failure_test", 0)
        preferences.edit().clear().commit()
        val realPolicy = ReviewPolicy(SharedPreferencesReviewStateStore(preferences))
        repeat(5) { realPolicy.recordCompletedSession() }

        val failingPreferences = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun putInt(
                        key: String,
                        value: Int,
                    ): SharedPreferences.Editor {
                        editor.putInt(key, value)
                        return this
                    }

                    override fun putBoolean(
                        key: String,
                        value: Boolean,
                    ): SharedPreferences.Editor {
                        editor.putBoolean(key, value)
                        return this
                    }

                    override fun commit(): Boolean {
                        // Model Android's memory update even when commit returns false.
                        editor.commit()
                        return false
                    }
                }
            }
        }
        val policy = ReviewPolicy(SharedPreferencesReviewStateStore(failingPreferences))
        assertFalse(policy.claimRequest())
        assertTrue(realPolicy.isEligible())
        assertTrue(realPolicy.claimRequest())
    }
}
