package com.lizz.neversleep

import android.os.Looper
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import com.lizz.neversleep.review.ReviewPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NeverSleepApplication::class)
class GooglePlayReviewRequesterTest {
    private lateinit var policy: ReviewPolicy

    @Before
    fun setUp() {
        val app: NeverSleepApplication = org.robolectric.RuntimeEnvironment.getApplication() as NeverSleepApplication
        val prefs = app.getSharedPreferences("review_requester_test", 0)
        prefs.edit().clear().commit()
        policy = ReviewPolicy(SharedPreferencesReviewStateStore(prefs))
        repeat(5) { policy.recordCompletedSession() }
    }

    @Test
    fun readyHostRequestsOnlyOnceAcrossPresenters() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val manager = RecordingManager(FakeReviewManager(activity))
        val requester = GooglePlayReviewRequester(activity, policy, { true }, manager)
        requester.requestReviewIfEligible()
        requester.requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        GooglePlayReviewRequester(activity, policy, { true }, manager).requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, manager.launches)
        assertFalse(policy.isEligible())
    }

    @Test
    fun blockedHostDoesNotPrepareOrConsumeRequest() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val manager = RecordingManager(FakeReviewManager(activity))
        GooglePlayReviewRequester(activity, policy, { false }, manager).requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, manager.preparations)
        assertTrue(policy.isEligible())
    }

    @Test
    fun backgroundWhilePreparingDefersUntilResumed() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val manager = RecordingManager(FakeReviewManager(host.get()))
        val pending = TaskCompletionSource<ReviewInfo>()
        manager.nextRequest = pending.task
        val requester = GooglePlayReviewRequester(host.get(), policy, { true }, manager)
        requester.requestReviewIfEligible()
        host.pause().stop()
        pending.setResult(manager.info)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, manager.launches)
        assertTrue(policy.isEligible())
        host.start().resume()
        manager.nextRequest = Tasks.forResult(manager.info)
        requester.requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, manager.launches)
    }

    @Test
    fun failedPreparationLeavesEligibilityForANewHost() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val manager = RecordingManager(FakeReviewManager(activity))
        manager.nextRequest = Tasks.forException(IllegalStateException("Play unavailable"))
        GooglePlayReviewRequester(activity, policy, { true }, manager).requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, manager.launches)
        assertTrue(policy.isEligible())
        manager.nextRequest = Tasks.forResult(manager.info)
        GooglePlayReviewRequester(activity, policy, { true }, manager).requestReviewIfEligible()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, manager.launches)
    }

    private class RecordingManager(
        private val delegate: ReviewManager,
    ) : ReviewManager by delegate {
        val info: ReviewInfo = delegate.requestReviewFlow().result
        var nextRequest: Task<ReviewInfo> = Tasks.forResult(info)
        var preparations = 0
        var launches = 0

        override fun requestReviewFlow(): Task<ReviewInfo> {
            preparations++
            return nextRequest
        }

        override fun launchReviewFlow(
            activity: android.app.Activity,
            reviewInfo: ReviewInfo,
        ): Task<Void> {
            launches++
            return delegate.launchReviewFlow(activity, reviewInfo)
        }
    }
}
