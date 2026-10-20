import AppIntents
import Observation

enum DisplaySessionDestination: String, AppEnum {
    case session

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Display")
    static let caseDisplayRepresentations: [Self: DisplayRepresentation] = [
        .session: "Display session",
    ]
}

/// Compiled into both targets so WidgetKit can hand execution to the app.
struct StartDisplaySessionIntent: OpenIntent {
    static let title: LocalizedStringResource = "Start Display Session"
    static let description = IntentDescription(
        "Open Never Sleep and keep its display awake. Other apps are not affected."
    )
    static let openAppWhenRun = true

    @Parameter(title: "Display", default: .session)
    var target: DisplaySessionDestination

    @MainActor
    func perform() async throws -> some IntentResult {
        DisplaySessionRequest.shared.requestStart()
        return .result()
    }
}

/// Keeps a cold-launch request until an active scene can consume it.
@MainActor
@Observable
final class DisplaySessionRequest {
    static let shared = DisplaySessionRequest()

    private(set) var revision = 0
    private var pending = false

    func requestStart() {
        pending = true
        revision += 1
    }

    func consume() -> Bool {
        guard pending else { return false }
        pending = false
        return true
    }
}
