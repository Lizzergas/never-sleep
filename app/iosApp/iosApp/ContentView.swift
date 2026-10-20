import SwiftUI
import StoreKit
import ReviewKit

struct ContentView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.requestReview) private var requestReview

    let session: DisplaySessionModel
    var reviewPolicy: ReviewPolicy? = nil
    var reviewReady = false

    @State private var presentedSheet: SheetDestination?

    var body: some View {
        mainContent
            .foregroundStyle(NeverSleepTheme.content)
            .preferredColorScheme(.dark)
            .sheet(item: $presentedSheet, onDismiss: requestReviewIfReady) { destination in
                switch destination {
                case .help:
                    HelpAndPrivacyView()
                }
            }
            .onChange(of: session.isSessionRequested) { _, _ in
                requestReviewIfReady()
            }
            .onChange(of: scenePhase) { _, _ in
                requestReviewIfReady()
            }
            .onChange(of: reviewReady) { _, _ in
                requestReviewIfReady()
            }
    }

    private func requestReviewIfReady() {
        guard reviewReady,
              scenePhase == .active,
              !session.isSessionRequested,
              presentedSheet == nil,
              let reviewPolicy
        else { return }

        StoreKitReviewRequester(policy: reviewPolicy) {
            requestReview()
        }.requestReviewIfEligible()
    }

    private var mainContent: some View {
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
    }

    private func portraitLayout(controlSize: CGFloat) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: dynamicTypeSize.isAccessibilitySize ? 64 : 80)

            brand

            Spacer()
                .frame(height: dynamicTypeSize.isAccessibilitySize ? 28 : NeverSleepTheme.controlSpacing)

            NeverSleepControl(session: session, size: controlSize)

            sessionReadout
                .padding(.top, 24)

            Spacer(minLength: 40)
        }
        .padding(.horizontal, NeverSleepTheme.horizontalPadding)
        .padding(.bottom, 24)
    }

    private func landscapeLayout(controlSize: CGFloat) -> some View {
        HStack(spacing: 56) {
            VStack(alignment: .leading, spacing: 24) {
                brand
                sessionReadout
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
