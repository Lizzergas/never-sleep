@testable import NeverSleep
import ReviewKit
import XCTest

@MainActor
final class ReviewPromptTests: XCTestCase {
    func testCompletedSessionsDriveSharedPolicyAndRequestOnlyOnce() {
        let suite = "ReviewPromptTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let policy = ReviewPolicy(
            store: UserDefaultsReviewStateStore(defaults: defaults),
            minimumCompletedSessions: 5
        )
        let session = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: { _ in }),
            onSessionCompleted: { policy.recordCompletedSession() }
        )
        var requests = 0
        let requester = StoreKitReviewRequester(policy: policy) { requests += 1 }

        session.end()
        for _ in 0..<4 {
            session.start()
            session.end()
            session.end()
            requester.requestReviewIfEligible()
            XCTAssertFalse(policy.isEligible())
        }
        XCTAssertEqual(requests, 0)

        session.start()
        session.end()
        XCTAssertTrue(policy.isEligible())
        requester.requestReviewIfEligible()
        requester.requestReviewIfEligible()
        XCTAssertEqual(requests, 1)

        let recreated = ReviewPolicy(
            store: UserDefaultsReviewStateStore(defaults: defaults),
            minimumCompletedSessions: 5
        )
        XCTAssertFalse(recreated.isEligible())
        XCTAssertFalse(recreated.claimRequest())
    }

    func testTimedSessionCountsOnceAndBackgroundingDoesNotCompleteIt() {
        var completions = 0
        let session = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: { _ in }),
            validationDuration: 2,
            onSessionCompleted: { completions += 1 }
        )
        let start = Date(timeIntervalSince1970: 100)
        session.updateScene(isActive: true, now: start)
        session.start(now: start)
        session.updateScene(isActive: false, now: start.addingTimeInterval(1))
        XCTAssertEqual(completions, 0)
        session.tick(now: start.addingTimeInterval(2))
        session.tick(now: start.addingTimeInterval(3))
        session.end()
        XCTAssertEqual(completions, 1)
    }

    func testUnavailableRequesterLeavesRequestPending() {
        let suite = "ReviewPromptTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let policy = ReviewPolicy(
            store: UserDefaultsReviewStateStore(defaults: defaults),
            minimumCompletedSessions: 5
        )
        for _ in 0..<5 { policy.recordCompletedSession() }
        NoOpReviewRequester().requestReviewIfEligible()
        XCTAssertTrue(policy.isEligible())
    }
}
