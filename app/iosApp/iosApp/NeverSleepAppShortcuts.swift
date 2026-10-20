import AppIntents

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
