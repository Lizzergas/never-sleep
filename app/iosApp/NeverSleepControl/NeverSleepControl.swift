import AppIntents
import SwiftUI
import WidgetKit

@main
struct NeverSleepControl: ControlWidget {
    static let kind = "com.lizz.neversleep.control.start-session"

    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: Self.kind) {
            ControlWidgetButton(
                action: OpenURLIntent(URL(string: "neversleep://session/start")!)
            ) {
                Label("Display Session", systemImage: "moon.stars.fill")
                    .controlWidgetActionHint("Start a display session")
            }
        }
        .displayName("Never Sleep")
        .description("Open Never Sleep and start a foreground display session.")
    }
}
