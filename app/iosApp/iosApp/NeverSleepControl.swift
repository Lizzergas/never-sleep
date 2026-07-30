import SwiftUI

struct NeverSleepControl: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let session: DisplaySessionModel
    let size: CGFloat

    @State private var mode: Float = 0

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30)) { timeline in
            Button {
                if session.isSessionRequested {
                    session.end()
                } else {
                    session.start()
                }
            } label: {
                ZStack {
                    controlFace(time: timeline.date.timeIntervalSinceReferenceDate)
                    CelestialMark(
                        isActive: session.isSessionRequested,
                        reduceMotion: reduceMotion
                    )

                    VStack {
                        Spacer()

                        Text(session.isSessionRequested ? "AWAKE" : "READY")
                            .font(.system(size: 21, weight: .black, design: .rounded))
                            .tracking(3.6)
                            .foregroundStyle(NeverSleepTheme.content)

                        Spacer()
                            .frame(height: size * 0.19)
                    }
                    .padding(.horizontal, 20)
                }
                .frame(width: size, height: size)
                .contentShape(Circle())
            }
            .buttonStyle(OrbButtonStyle(reduceMotion: reduceMotion))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                session.isSessionRequested
                    ? "End Display Session"
                    : "Start Display Session"
            )
            .accessibilityValue(session.accessibilityStatus(at: timeline.date))
            .accessibilityHint(
                session.isSessionRequested
                    ? "Restores normal Auto-Lock behavior"
                    : "Keeps this display awake while Never Sleep remains open"
            )
            .accessibilityIdentifier("startEndSessionButton")
        }
        .frame(width: size, height: size)
        .sensoryFeedback(.impact(weight: .medium, intensity: 0.7), trigger: session.isSessionRequested)
        .onAppear {
            mode = session.isSessionRequested ? 1 : 0
        }
        .onChange(of: session.isSessionRequested) { _, active in
            withAnimation(reduceMotion ? .linear(duration: 0.12) : NeverSleepTheme.stateTransition) {
                mode = active ? 1 : 0
            }
        }
    }

    private func controlFace(time: TimeInterval) -> some View {
        Circle()
            .fill(NeverSleepTheme.panel)
            .colorEffect(
                ShaderLibrary.neverSleepControlFace(
                    .float2(Float(size), Float(size)),
                    .float(Float(reduceMotion ? 0 : time)),
                    .float(mode)
                )
            )
            .overlay {
                Circle()
                    .strokeBorder(
                        session.isSessionRequested
                            ? NeverSleepTheme.neverAccent.opacity(0.32)
                            : NeverSleepTheme.normalAccent.opacity(0.18),
                        lineWidth: 1
                    )
            }
            .shadow(color: NeverSleepTheme.shadow.opacity(0.72), radius: 18, y: 10)
    }
}

private struct CelestialMark: View {
    let isActive: Bool
    let reduceMotion: Bool

    var body: some View {
        ZStack {
            SunMark()
                .opacity(isActive ? 0 : 1)
                .scaleEffect(isActive ? 0.84 : 1)

            MoonMark()
                .opacity(isActive ? 1 : 0)
                .scaleEffect(isActive ? 1 : 0.84)
        }
        .frame(width: 104, height: 104)
        .offset(y: -26)
        .animation(
            reduceMotion ? .linear(duration: 0.12) : NeverSleepTheme.stateTransition,
            value: isActive
        )
        .accessibilityHidden(true)
    }
}

private struct SunMark: View {
    var body: some View {
        ZStack {
            ForEach(0..<8, id: \.self) { index in
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [
                                NeverSleepTheme.normalAccent.opacity(0.9),
                                NeverSleepTheme.normalAccent.opacity(0),
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .frame(width: 4, height: 20)
                    .offset(y: -42)
                    .rotationEffect(.degrees(Double(index) * 45))
            }

            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            NeverSleepTheme.content,
                            NeverSleepTheme.normalAccent,
                            NeverSleepTheme.sunCore,
                        ],
                        center: .center,
                        startRadius: 0,
                        endRadius: 34
                    )
                )
                .frame(width: 66, height: 66)
                .shadow(color: NeverSleepTheme.normalAccent.opacity(0.58), radius: 18)
        }
    }
}

private struct MoonMark: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [
                            NeverSleepTheme.cyanAccent,
                            NeverSleepTheme.neverAccent,
                            NeverSleepTheme.neverAmbient,
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 76, height: 76)
                .shadow(color: NeverSleepTheme.neverAmbient.opacity(0.72), radius: 20)

            Circle()
                .fill(NeverSleepTheme.panel)
                .frame(width: 68, height: 68)
                .offset(x: 24, y: -10)

            Circle()
                .fill(NeverSleepTheme.cyanAccent.opacity(0.9))
                .frame(width: 4, height: 4)
                .offset(x: -45, y: 28)

            Circle()
                .fill(NeverSleepTheme.neverAccent.opacity(0.9))
                .frame(width: 3, height: 3)
                .offset(x: 41, y: 21)
        }
    }
}

private struct OrbButtonStyle: ButtonStyle {
    let reduceMotion: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.96 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(
                configuration.isPressed ? NeverSleepTheme.pressIn : NeverSleepTheme.pressOut,
                value: configuration.isPressed
            )
    }
}
