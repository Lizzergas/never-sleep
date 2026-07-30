import SwiftUI

struct ContentView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.scenePhase) private var scenePhase

    let session: DisplaySessionModel

    @State private var presentedSheet: SheetDestination?

    var body: some View {
        ZStack {
            AmbientBackground(
                isActive: session.isSessionRequested,
                isPaused: reduceMotion || scenePhase != .active
            )

            GeometryReader { proxy in
                let landscape = proxy.size.width > proxy.size.height
                let controlSize = preferredControlSize(in: proxy.size, landscape: landscape)

                ScrollView {
                    Group {
                        if landscape && !dynamicTypeSize.isAccessibilitySize {
                            landscapeLayout(controlSize: controlSize)
                        } else {
                            portraitLayout(controlSize: controlSize)
                        }
                    }
                    .frame(
                        minHeight: proxy.size.height,
                        alignment: landscape ? .center : .top
                    )
                    .frame(maxWidth: .infinity)
                }
                .scrollBounceBehavior(.basedOnSize)
                .scrollIndicators(.hidden)
            }

            helpButton
        }
        .foregroundStyle(NeverSleepTheme.content)
        .preferredColorScheme(.dark)
        .sheet(item: $presentedSheet) { destination in
            switch destination {
            case .help:
                HelpAndPrivacyView()
            }
        }
        .task(id: session.isSessionRequested) {
            guard session.isSessionRequested else { return }

            while !Task.isCancelled, session.isSessionRequested {
                try? await Task.sleep(for: .seconds(1))
                session.tick()
            }
        }
    }

    private func portraitLayout(controlSize: CGFloat) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: dynamicTypeSize.isAccessibilitySize ? 92 : 146)

            brand

            Spacer()
                .frame(height: dynamicTypeSize.isAccessibilitySize ? 28 : NeverSleepTheme.controlSpacing)

            NeverSleepControl(session: session, size: controlSize)

            sessionReadout
                .padding(.top, 24)

            Spacer(minLength: 72)

            footer
        }
        .padding(.horizontal, NeverSleepTheme.horizontalPadding)
        .padding(.bottom, 24)
    }

    private func landscapeLayout(controlSize: CGFloat) -> some View {
        HStack(spacing: 56) {
            VStack(alignment: .leading, spacing: 24) {
                brand
                sessionReadout
                footer
                    .frame(maxWidth: 300, alignment: .leading)
            }

            NeverSleepControl(session: session, size: controlSize)
        }
        .frame(maxWidth: 720)
        .padding(.horizontal, 72)
        .padding(.vertical, 32)
    }

    private var brand: some View {
        Text("never sleep")
            .font(.system(size: 18, weight: .medium, design: .rounded))
            .tracking(5.2)
            .foregroundStyle(
                session.isSessionRequested
                    ? NeverSleepTheme.contentMuted
                    : NeverSleepTheme.normalInk.opacity(0.82)
            )
            .animation(NeverSleepTheme.stateTransition, value: session.isSessionRequested)
            .accessibilityAddTraits(.isHeader)
            .accessibilityIdentifier("brandWordmark")
    }

    private var sessionReadout: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            if session.isSessionRequested {
                Text(session.timerText(at: context.date))
                    .font(.system(.subheadline, design: .monospaced, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(NeverSleepTheme.contentMuted)
                    .accessibilityIdentifier("sessionStatus")
            } else {
                Menu {
                    Picker("Session duration", selection: session.selectedDurationBinding) {
                        ForEach(DisplaySessionDuration.allCases) { duration in
                            Text(duration.title).tag(duration)
                        }
                    }
                } label: {
                    HStack(spacing: 7) {
                        Text(session.selectedDuration.title)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(NeverSleepTheme.normalInk.opacity(0.72))
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(NeverSleepTheme.normalInk.opacity(0.78))
                    .frame(minWidth: 140, minHeight: 44)
                    .contentShape(Rectangle())
                }
                .accessibilityLabel("Session duration")
                .accessibilityValue(session.selectedDuration.title)
                .accessibilityIdentifier("sessionDurationPicker")
            }
        }
    }

    private var footer: some View {
        VStack(spacing: 9) {
            TimelineView(.periodic(from: .now, by: 60)) { context in
                Text(context.date, format: .dateTime.hour().minute())
                    .font(.system(.caption, design: .monospaced, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(NeverSleepTheme.contentSubtle)
                    .accessibilityLabel(
                        "Current time, \(context.date.formatted(date: .omitted, time: .shortened))"
                    )
            }

            Text(
                session.isSessionRequested
                    ? "Awake while this window remains active. Uses more battery."
                    : "Auto-Lock stays unchanged until you begin."
            )
            .font(.footnote)
            .foregroundStyle(NeverSleepTheme.contentSubtle)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityIdentifier("sessionFootnote")
        }
    }

    private var helpButton: some View {
        VStack {
            HStack {
                Spacer()

                Button {
                    presentedSheet = .help
                } label: {
                    Image(systemName: "info.circle")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(NeverSleepTheme.contentMuted)
                        .frame(width: 44, height: 44)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Help and privacy")
                .accessibilityIdentifier("helpAndPrivacyButton")
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
    }

    private func preferredControlSize(in size: CGSize, landscape: Bool) -> CGFloat {
        if dynamicTypeSize.isAccessibilitySize {
            return NeverSleepTheme.compactControlSize
        }

        if landscape {
            return min(NeverSleepTheme.regularControlSize, size.height * 0.62)
        }

        return min(NeverSleepTheme.regularControlSize, size.width * 0.66)
    }
}

private enum SheetDestination: String, Identifiable {
    case help

    var id: String {
        rawValue
    }
}

#Preview("Ready") {
    ContentView(session: .preview())
}

#Preview("Active") {
    ContentView(session: .preview(active: true))
}
