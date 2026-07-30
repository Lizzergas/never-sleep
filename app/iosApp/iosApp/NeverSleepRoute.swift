import Foundation

enum NeverSleepRoute {
    static let startSession = URL(string: "neversleep://session/start")!
    static let privacyPolicy = URL(string: "https://neversleep.app/privacy")!

    static func matchesStartSession(_ url: URL) -> Bool {
        url.scheme == startSession.scheme
            && url.host == startSession.host
            && url.path == startSession.path
    }
}
