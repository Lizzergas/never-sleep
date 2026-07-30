import Observation
import SwiftUI

enum DisplaySessionDuration: String, CaseIterable, Identifiable {
    case fifteenMinutes
    case thirtyMinutes
    case oneHour
    case untilEnded

    var id: String {
        rawValue
    }

    var title: String {
        switch self {
        case .fifteenMinutes:
            "15 minutes"
        case .thirtyMinutes:
            "30 minutes"
        case .oneHour:
            "1 hour"
        case .untilEnded:
            "Until I end it"
        }
    }

    var timeInterval: TimeInterval? {
        switch self {
        case .fifteenMinutes:
            15 * 60
        case .thirtyMinutes:
            30 * 60
        case .oneHour:
            60 * 60
        case .untilEnded:
            nil
        }
    }
}

@MainActor
@Observable
final class DisplaySessionModel {
    private(set) var startedAt: Date?
    private(set) var endDate: Date?
    var selectedDuration: DisplaySessionDuration = .thirtyMinutes

    @ObservationIgnored
    private let coordinator: IdleTimerCoordinator

    @ObservationIgnored
    private let sceneID: UUID

    @ObservationIgnored
    private let validationDuration: TimeInterval?

    @ObservationIgnored
    private var isSceneActive = false

    init(
        coordinator: IdleTimerCoordinator,
        sceneID: UUID = UUID(),
        validationDuration: TimeInterval? = nil
    ) {
        self.coordinator = coordinator
        self.sceneID = sceneID
        self.validationDuration = validationDuration
    }

    var isSessionRequested: Bool {
        startedAt != nil
    }

    var selectedDurationBinding: Binding<DisplaySessionDuration> {
        Binding(
            get: { self.selectedDuration },
            set: { self.selectedDuration = $0 }
        )
    }

    var statusTitle: String {
        isSessionRequested
            ? "Display session active"
            : "Ready when you are"
    }

    var statusDetail: String {
        if isSessionRequested {
            "The display stays awake while Never Sleep remains in the foreground."
        } else {
            "Start a session when you need this screen to remain visible."
        }
    }

    func accessibilityStatus(at date: Date) -> String {
        if isSessionRequested {
            "Awake session active. \(timerText(at: date))."
        } else {
            "Ready. No display session is active."
        }
    }

    func start(now: Date = Date()) {
        startedAt = now

        let interval = validationDuration ?? selectedDuration.timeInterval
        endDate = interval.map { now.addingTimeInterval($0) }
        syncCoordinator()
    }

    func end() {
        startedAt = nil
        endDate = nil
        syncCoordinator()
    }

    func tick(now: Date = Date()) {
        guard let endDate, now >= endDate else { return }
        end()
    }

    func updateScene(isActive: Bool, now: Date = Date()) {
        tick(now: now)
        isSceneActive = isActive
        syncCoordinator()
    }

    func disconnectScene() {
        isSceneActive = false
        coordinator.remove(sceneID: sceneID)
    }

    func elapsedTime(at date: Date) -> TimeInterval {
        guard let startedAt else { return 0 }
        return max(0, date.timeIntervalSince(startedAt))
    }

    func remainingTime(at date: Date) -> TimeInterval? {
        guard let endDate else { return nil }
        return max(0, endDate.timeIntervalSince(date))
    }

    func timerText(at date: Date) -> String {
        guard isSessionRequested else {
            return selectedDuration.title
        }

        if let remaining = remainingTime(at: date) {
            return "\(format(remaining)) remaining"
        }

        return "\(format(elapsedTime(at: date))) elapsed"
    }

    private func syncCoordinator() {
        coordinator.update(
            sceneID: sceneID,
            wantsAwake: isSessionRequested,
            isSceneActive: isSceneActive
        )
    }

    private func format(_ interval: TimeInterval) -> String {
        let totalSeconds = max(0, Int(interval.rounded(.down)))
        let hours = totalSeconds / 3600
        let minutes = totalSeconds % 3600 / 60
        let seconds = totalSeconds % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }

        return String(format: "%02d:%02d", minutes, seconds)
    }
}

extension DisplaySessionModel {
    static func preview(active: Bool = false) -> DisplaySessionModel {
        let model = DisplaySessionModel(
            coordinator: IdleTimerCoordinator(applyIdleTimerState: { _ in })
        )
        model.updateScene(isActive: true)

        if active {
            model.start(now: Date().addingTimeInterval(-94))
        }

        return model
    }
}
