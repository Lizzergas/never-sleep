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
}

@MainActor
private final class IdleTimerRecorder {
    private(set) var values: [Bool] = []

    func record(_ value: Bool) {
        values.append(value)
    }
}
