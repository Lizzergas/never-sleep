import SwiftUI
import ReviewKit

@main
struct NeverSleepApp: App {
    @State private var idleTimerCoordinator = IdleTimerCoordinator()
    private let reviewPolicy = ReviewPolicy(
        store: UserDefaultsReviewStateStore(),
        minimumCompletedSessions: 5
    )

    var body: some Scene {
        WindowGroup {
            SceneRootView(coordinator: idleTimerCoordinator, reviewPolicy: reviewPolicy)
        }
    }
}

private struct SceneRootView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var session: DisplaySessionModel
    @State private var launchComplete = false
    private let reviewPolicy: ReviewPolicy?
    private let request = DisplaySessionRequest.shared

    init(coordinator: IdleTimerCoordinator, reviewPolicy: ReviewPolicy) {
        let environment = ProcessInfo.processInfo.environment
        let disableReviews = environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
            || environment["XCTestConfigurationFilePath"] != nil
            || environment["NEVER_SLEEP_UI_TESTING"] == "1"
            || Self.validationDuration != nil
        let activeReviewPolicy = disableReviews ? nil : reviewPolicy
        self.reviewPolicy = activeReviewPolicy
        _session = State(
            initialValue: DisplaySessionModel(
                coordinator: coordinator,
                validationDuration: Self.validationDuration,
                preferences: .standard,
                onSessionCompleted: { activeReviewPolicy?.recordCompletedSession() }
            )
        )
    }

    var body: some View {
        LaunchExperienceView(onFinished: { launchComplete = true }) {
            ContentView(session: session, reviewPolicy: reviewPolicy, reviewReady: launchComplete)
        }
            .onChange(of: scenePhase, initial: true) { _, phase in
                session.updateScene(isActive: phase == .active)
                consumeStartRequest()
            }
            .onChange(of: request.revision) { _, _ in
                consumeStartRequest()
            }
            .onOpenURL { url in
                guard NeverSleepRoute.matchesStartSession(url) else { return }
                session.start()
            }
            .onDisappear {
                session.disconnectScene()
            }
            .task(id: session.isSessionRequested) {
                guard session.isSessionRequested else { return }
                while !Task.isCancelled, session.isSessionRequested {
                    do {
                        try await Task.sleep(for: .seconds(1))
                    } catch {
                        return
                    }
                    session.tick()
                }
            }
    }

    private func consumeStartRequest() {
        guard scenePhase == .active, request.consume() else { return }
        session.start()
    }

    private static var validationDuration: TimeInterval? {
        #if DEBUG
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
        #else
        return nil
        #endif
    }
}
