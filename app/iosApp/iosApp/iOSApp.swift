import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        KoinIosKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
