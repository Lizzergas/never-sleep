import SwiftUI

struct HelpAndPrivacyView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("How it works") {
                    Label(
                        "Never Sleep keeps this display awake only while a session is active and the app is in the foreground.",
                        systemImage: "moon.stars"
                    )
                    Label(
                        "Leaving the app restores normal Auto-Lock behavior. Your system setting is never changed.",
                        systemImage: "lock.shield"
                    )
                    Label(
                        "Pressing the Side button can still lock your device at any time.",
                        systemImage: "button.programmable"
                    )
                }

                Section("Privacy") {
                    Text("The iOS app does not request a display permission, show ads, or collect session data.")

                    Link(destination: NeverSleepRoute.privacyPolicy) {
                        Label("Read the privacy policy", systemImage: "hand.raised")
                    }
                }

                Section {
                    Button("Done") {
                        dismiss()
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("dismissHelpButton")
                }
            }
            .navigationTitle("Help & Privacy")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium, .large])
    }
}
