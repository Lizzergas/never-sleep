import SwiftUI
import WidgetKit

struct NeverSleepHomeWidget: Widget {
    let kind = "com.lizz.neversleep.widget.start-session"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SessionWidgetProvider()) { _ in
            Button(intent: StartDisplaySessionIntent()) {
                VStack(alignment: .leading, spacing: 12) {
                    Image(systemName: "moon.stars.fill")
                        .font(.title)
                        .foregroundStyle(Color(red: 0.62, green: 0.55, blue: 1))
                    Spacer(minLength: 0)
                    Text("Never Sleep")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("Open & start a session")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.75))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .containerBackground(Color(red: 0.05, green: 0.05, blue: 0.1), for: .widget)
            .accessibilityLabel("Open Never Sleep and start a display session")
        }
        .configurationDisplayName("Display Session")
        .description("Open Never Sleep and start keeping its display awake.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct SessionWidgetEntry: TimelineEntry {
    let date: Date
}

private struct SessionWidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> SessionWidgetEntry {
        SessionWidgetEntry(date: .now)
    }

    func getSnapshot(in context: Context, completion: @escaping (SessionWidgetEntry) -> Void) {
        completion(SessionWidgetEntry(date: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SessionWidgetEntry>) -> Void) {
        completion(Timeline(entries: [SessionWidgetEntry(date: .now)], policy: .never))
    }
}
