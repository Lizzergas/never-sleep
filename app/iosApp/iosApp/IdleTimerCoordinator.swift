import Observation
import OSLog
import UIKit

@MainActor
@Observable
final class IdleTimerCoordinator {
    private(set) var isIdleTimerDisabled = false

    private let applyIdleTimerState: @MainActor (Bool) -> Void
    private var activeSceneRequests: Set<UUID> = []
    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "com.lizz.neversleep",
        category: "IdleTimer"
    )

    init() {
        applyIdleTimerState = {
            UIApplication.shared.isIdleTimerDisabled = $0
        }
    }

    init(applyIdleTimerState: @escaping @MainActor (Bool) -> Void) {
        self.applyIdleTimerState = applyIdleTimerState
    }

    func update(sceneID: UUID, wantsAwake: Bool, isSceneActive: Bool) {
        if wantsAwake, isSceneActive {
            activeSceneRequests.insert(sceneID)
        } else {
            activeSceneRequests.remove(sceneID)
        }

        applyAggregateState()
    }

    func remove(sceneID: UUID) {
        activeSceneRequests.remove(sceneID)
        applyAggregateState()
    }

    private func applyAggregateState() {
        let shouldDisableIdleTimer = !activeSceneRequests.isEmpty
        guard shouldDisableIdleTimer != isIdleTimerDisabled else { return }

        isIdleTimerDisabled = shouldDisableIdleTimer
        applyIdleTimerState(shouldDisableIdleTimer)
        logger.notice("Idle timer disabled: \(shouldDisableIdleTimer, privacy: .public)")
    }
}
