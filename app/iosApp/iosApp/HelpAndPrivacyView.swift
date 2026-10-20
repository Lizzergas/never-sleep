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
                        "Your session continues counting down while you are away. Return before it ends to resume keeping this display awake.",
                        systemImage: "arrow.uturn.backward"
                    )
                    Label(
                        "Pressing the Side button can still lock your device at any time.",
                        systemImage: "button.programmable"
                    )
                }

                Section("Quick access") {
                    Text("In Control Center, tap Add a Control and search for Never Sleep. You can also find Start Display Session in the Shortcuts app.")
                    Text("For Home Screen access, add the Never Sleep widget from the widget gallery.")
                    Text("These actions open Never Sleep and start a session. Your device may ask you to unlock first.")
                }

                Section("Privacy") {
                    Text("The iOS app does not request a display permission, show ads, or collect session data.")
                    Text("Your duration preference and review-prompt eligibility are saved in this app on your device. They may be included in your device backups. We do not send them to a server.")

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
