import AppIntents
import SwiftUI
import WidgetKit

@main
struct NeverSleepWidgets: WidgetBundle {
    var body: some Widget {
        NeverSleepControl()
        NeverSleepHomeWidget()
    }
}

struct NeverSleepControl: ControlWidget {
    static let kind = "com.lizz.neversleep.control.start-session"

    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: Self.kind) {
            ControlWidgetButton(
                action: StartDisplaySessionIntent()
            ) {
                Label("Display Session", systemImage: "moon.stars.fill")
                    .controlWidgetActionHint("Start a display session")
            }
        }
        .displayName("Never Sleep")
        .description("Open Never Sleep and start a foreground display session.")
    }
}
