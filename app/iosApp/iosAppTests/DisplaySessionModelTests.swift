@testable import NeverSleep
import XCTest

@MainActor
final class DisplaySessionModelTests: XCTestCase {
    func testActiveSessionDisablesIdleTimer() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let model = DisplaySessionModel(coordinator: coordinator)

        model.updateScene(isActive: true)
        model.start(now: Date(timeIntervalSince1970: 100))

        XCTAssertTrue(model.isSessionRequested)
        XCTAssertTrue(coordinator.isIdleTimerDisabled)
        XCTAssertEqual(recorder.values, [true])
    }

    func testBackgroundRestoresIdleTimerAndForegroundResumesRequestedSession() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let model = DisplaySessionModel(coordinator: coordinator)
        let start = Date(timeIntervalSince1970: 100)

        model.updateScene(isActive: true, now: start)
        model.start(now: start)
        model.updateScene(isActive: false, now: start.addingTimeInterval(10))

        XCTAssertTrue(model.isSessionRequested)
        XCTAssertFalse(coordinator.isIdleTimerDisabled)

        model.updateScene(isActive: true, now: start.addingTimeInterval(20))

        XCTAssertTrue(coordinator.isIdleTimerDisabled)
        XCTAssertEqual(recorder.values, [true, false, true])
    }

    func testEndingSessionRestoresIdleTimer() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let model = DisplaySessionModel(coordinator: coordinator)

        model.updateScene(isActive: true)
        model.start()
        model.end()

        XCTAssertFalse(model.isSessionRequested)
        XCTAssertFalse(coordinator.isIdleTimerDisabled)
        XCTAssertEqual(recorder.values, [true, false])
    }

    func testTimedSessionEndsAtDeadline() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let model = DisplaySessionModel(coordinator: coordinator, validationDuration: 2)
        let start = Date(timeIntervalSince1970: 100)

        model.updateScene(isActive: true, now: start)
        model.start(now: start)
        model.tick(now: start.addingTimeInterval(1))
        XCTAssertTrue(model.isSessionRequested)

        model.tick(now: start.addingTimeInterval(2))

        XCTAssertFalse(model.isSessionRequested)
        XCTAssertFalse(coordinator.isIdleTimerDisabled)
        XCTAssertEqual(recorder.values, [true, false])
    }

    func testMultipleScenesKeepIdleTimerDisabledUntilLastActiveRequestLeaves() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let first = DisplaySessionModel(coordinator: coordinator)
        let second = DisplaySessionModel(coordinator: coordinator)

        first.updateScene(isActive: true)
        second.updateScene(isActive: true)
        first.start()
        second.start()
        first.updateScene(isActive: false)

        XCTAssertTrue(coordinator.isIdleTimerDisabled)

        second.end()

        XCTAssertFalse(coordinator.isIdleTimerDisabled)
        XCTAssertEqual(recorder.values, [true, false])
    }

    func testInactiveSceneNeverDisablesIdleTimer() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let model = DisplaySessionModel(coordinator: coordinator)

        model.start()

        XCTAssertTrue(model.isSessionRequested)
        XCTAssertFalse(coordinator.isIdleTimerDisabled)
        XCTAssertTrue(recorder.values.isEmpty)
    }

    func testExpiredSessionDoesNotResumeAfterBackground() {
        let recorder = IdleTimerRecorder()
        let model = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: recorder.record),
            validationDuration: 2
        )
        let start = Date(timeIntervalSince1970: 100)
        model.updateScene(isActive: true, now: start)
        model.start(now: start)
        model.updateScene(isActive: false, now: start.addingTimeInterval(1))
        model.updateScene(isActive: true, now: start.addingTimeInterval(3))
        XCTAssertFalse(model.isSessionRequested)
        XCTAssertEqual(recorder.values, [true, false])
    }

    func testRepeatedStartDoesNotExtendSession() {
        let model = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: { _ in }),
            validationDuration: 60
        )
        let start = Date(timeIntervalSince1970: 100)
        model.start(now: start)
        model.start(now: start.addingTimeInterval(20))
        XCTAssertEqual(model.startedAt, start)
        XCTAssertEqual(model.endDate, start.addingTimeInterval(60))
    }

    func testUntilEndedSessionHasNoDeadline() {
        let model = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: { _ in })
        )
        XCTAssertEqual(model.selectedDuration, .untilEnded)
        model.start(now: Date(timeIntervalSince1970: 100))
        model.tick(now: Date(timeIntervalSince1970: 1_000_000))
        XCTAssertTrue(model.isSessionRequested)
        XCTAssertNil(model.endDate)
    }

    func testDurationPersistsWithoutRestoringAnActiveSession() {
        let suite = "NeverSleepTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: { _ in })
        let model = DisplaySessionModel(coordinator: coordinator, preferences: defaults)
        model.selectedDuration = .oneHour
        model.start()
        let restored = DisplaySessionModel(coordinator: coordinator, preferences: defaults)
        XCTAssertEqual(restored.selectedDuration, .oneHour)
        XCTAssertFalse(restored.isSessionRequested)
    }

    func testDisconnectReleasesOnlyItsScene() {
        let recorder = IdleTimerRecorder()
        let coordinator = IdleTimerCoordinator(applyIdleTimerState: recorder.record)
        let first = DisplaySessionModel(coordinator: coordinator)
        let second = DisplaySessionModel(coordinator: coordinator)
        for model in [first, second] {
            model.updateScene(isActive: true)
            model.start()
        }
        first.disconnectScene()
        XCTAssertTrue(coordinator.isIdleTimerDisabled)
        second.disconnectScene()
        XCTAssertFalse(coordinator.isIdleTimerDisabled)
    }

    func testLaunchRequestSurvivesUntilConsumedAndIsConsumedOnlyOnce() {
        let request = DisplaySessionRequest()
        request.requestStart()
        XCTAssertEqual(request.revision, 1)
        XCTAssertTrue(request.consume())
        XCTAssertFalse(request.consume())
        request.requestStart()
        XCTAssertEqual(request.revision, 2)
        XCTAssertTrue(request.consume())
    }

    func testIntentQueuesStartInApp() async throws {
        _ = DisplaySessionRequest.shared.consume()
        _ = try await StartDisplaySessionIntent().perform()
        XCTAssertTrue(DisplaySessionRequest.shared.consume())
        XCTAssertFalse(DisplaySessionRequest.shared.consume())
    }
}

@MainActor
private final class IdleTimerRecorder {
    private(set) var values: [Bool] = []

    func record(_ value: Bool) {
        values.append(value)
    }
}
