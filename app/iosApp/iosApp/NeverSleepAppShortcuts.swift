import AppIntents

struct StartDisplaySessionIntent: AppIntent {
    static let title: LocalizedStringResource = "Start Display Session"
    static let description = IntentDescription(
        "Open Never Sleep and start a foreground display session."
    )

    func perform() async throws -> some IntentResult & OpensIntent {
        .result(opensIntent: OpenURLIntent(NeverSleepRoute.startSession))
    }
}

struct NeverSleepAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartDisplaySessionIntent(),
            phrases: [
                "Start a display session in \(.applicationName)",
                "Keep this screen awake with \(.applicationName)",
            ],
            shortTitle: "Start Display Session",
            systemImageName: "moon.stars.fill"
        )
    }
}
