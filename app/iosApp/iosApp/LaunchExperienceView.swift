import SwiftUI

struct LaunchExperienceView<Content: View>: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var phase = LaunchPhase.resting

    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ZStack {
            content
                .allowsHitTesting(phase == .finished)
                .accessibilityHidden(phase != .finished)

            if phase != .finished {
                launchLayer
                    .transition(.identity)
                    .accessibilityHidden(true)
            }
        }
        .task {
            await playLaunchSequence()
        }
    }

    private var launchLayer: some View {
        ZStack {
            NeverSleepTheme.launchCanvas
                .ignoresSafeArea()

            launchAtmosphere

            VStack(spacing: 18) {
                launchMark

                Text("n e v e r   s l e e p")
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundStyle(NeverSleepTheme.contentMuted)
                    .opacity(phase == .resting ? 0.72 : 1)
            }
            .offset(y: -22)
        }
        .opacity(phase == .departing ? 0 : 1)
        .scaleEffect(phase == .departing ? 1.025 : 1)
        .blur(radius: phase == .departing && !reduceMotion ? 5 : 0)
    }

    private var launchAtmosphere: some View {
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            NeverSleepTheme.neverAmbient.opacity(0.26),
                            NeverSleepTheme.cyanAccent.opacity(0.08),
                            .clear,
                        ],
                        center: .center,
                        startRadius: 0,
                        endRadius: 170
                    )
                )
                .frame(width: 340, height: 340)
                .scaleEffect(phase == .resting ? 0.72 : 1)
                .opacity(phase == .resting ? 0 : 1)
                .blur(radius: 14)

            Circle()
                .stroke(
                    AngularGradient(
                        colors: [
                            .clear,
                            NeverSleepTheme.cyanAccent.opacity(0.52),
                            .clear,
                            NeverSleepTheme.neverAccent.opacity(0.46),
                            .clear,
                        ],
                        center: .center
                    ),
                    lineWidth: 1
                )
                .frame(width: 166, height: 166)
                .rotationEffect(phase == .resting ? .degrees(-18) : .degrees(8))
                .scaleEffect(phase == .resting ? 0.84 : 1)
                .opacity(phase == .resting ? 0 : 0.74)
        }
        .offset(y: -42)
    }

    private var launchMark: some View {
        Image("SplashMark")
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .frame(width: 132, height: 132)
            .scaleEffect(markScale)
            .opacity(phase == .resting ? 0.9 : 1)
            .shadow(
                color: NeverSleepTheme.cyanAccent.opacity(
                    phase == .resting ? 0 : 0.24
                ),
                radius: phase == .resting ? 0 : 20
            )
    }

    private var markScale: CGFloat {
        switch phase {
        case .resting:
            0.94
        case .illuminated:
            1
        case .departing:
            reduceMotion ? 1 : 1.08
        case .finished:
            1
        }
    }

    @MainActor
    private func playLaunchSequence() async {
        if reduceMotion {
            try? await Task.sleep(for: .milliseconds(120))
            guard !Task.isCancelled else { return }
            withAnimation(.easeOut(duration: 0.18)) {
                phase = .departing
            }
            try? await Task.sleep(for: .milliseconds(180))
            guard !Task.isCancelled else { return }
            phase = .finished
            return
        }

        try? await Task.sleep(for: .milliseconds(45))
        guard !Task.isCancelled else { return }

        withAnimation(.timingCurve(0.16, 1, 0.3, 1, duration: 0.52)) {
            phase = .illuminated
        }

        try? await Task.sleep(for: .milliseconds(560))
        guard !Task.isCancelled else { return }

        withAnimation(.timingCurve(0.65, 0, 0.35, 1, duration: 0.42)) {
            phase = .departing
        }

        try? await Task.sleep(for: .milliseconds(420))
        guard !Task.isCancelled else { return }
        phase = .finished
    }
}

private enum LaunchPhase {
    case resting
    case illuminated
    case departing
    case finished
}

#Preview {
    LaunchExperienceView {
        Color.orange
            .ignoresSafeArea()
    }
}
