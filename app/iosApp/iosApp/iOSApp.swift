import SwiftUI

@main
struct NeverSleepApp: App {
    @State private var idleTimerCoordinator = IdleTimerCoordinator()

    var body: some Scene {
        WindowGroup {
            SceneRootView(coordinator: idleTimerCoordinator)
        }
    }
}

private struct SceneRootView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var session: DisplaySessionModel

    init(coordinator: IdleTimerCoordinator) {
        _session = State(
            initialValue: DisplaySessionModel(
                coordinator: coordinator,
                validationDuration: Self.validationDuration
            )
        )
    }

    var body: some View {
        LaunchExperienceView {
            ContentView(session: session)
        }
            .onChange(of: scenePhase, initial: true) { _, phase in
                session.updateScene(isActive: phase == .active)
            }
            .onOpenURL { url in
                guard NeverSleepRoute.matchesStartSession(url) else { return }
                session.start()
            }
            .onDisappear {
                session.disconnectScene()
            }
    }

    private static var validationDuration: TimeInterval? {
        guard
            let rawValue = ProcessInfo.processInfo.environment[
                "NEVER_SLEEP_VALIDATION_DURATION_SECONDS"
            ],
            let seconds = TimeInterval(rawValue),
            seconds > 0
        else {
            return nil
        }

        return seconds
    }
}
