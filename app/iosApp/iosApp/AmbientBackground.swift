import SwiftUI

struct AmbientBackground: View {
    let isActive: Bool
    let isPaused: Bool

    @State private var mode: Float = 0

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30, paused: isPaused)) { timeline in
            GeometryReader { proxy in
                let size = proxy.size
                let time = isPaused
                    ? 0
                    : timeline.date.timeIntervalSinceReferenceDate
                        .truncatingRemainder(dividingBy: 1_200)

                Rectangle()
                    .fill(NeverSleepTheme.canvas)
                    .colorEffect(
                        ShaderLibrary.neverSleepBackground(
                            .float2(Float(size.width), Float(size.height)),
                            .float(Float(time)),
                            .float(mode)
                        )
                    )
                    .ignoresSafeArea()
            }
        }
        .accessibilityHidden(true)
        .onAppear {
            mode = isActive ? 1 : 0
        }
        .onChange(of: isActive) { _, active in
            withAnimation(isPaused ? .linear(duration: 0.12) : NeverSleepTheme.stateTransition) {
                mode = active ? 1 : 0
            }
        }
    }
}
