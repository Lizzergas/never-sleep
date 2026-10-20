import Foundation
import ReviewKit

final class UserDefaultsReviewStateStore: ReviewStateStore {
    private let defaults: UserDefaults
    private let key = "never_sleep_review_state"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> ReviewState {
        let values = defaults.dictionary(forKey: key) ?? [:]
        let count = values["completedSessions"] as? Int ?? 0
        return ReviewState(
            completedSessions: Int32(clamping: count),
            requestAttempted: values["requestAttempted"] as? Bool ?? false
        )
    }

    func save(state: ReviewState) -> Bool {
        defaults.set(
            [
                "completedSessions": Int(state.completedSessions),
                "requestAttempted": state.requestAttempted,
            ],
            forKey: key
        )
        return true
    }
}

/// Call on the main actor from an active scene, after other presentation has ended.
@MainActor
final class StoreKitReviewRequester: @MainActor ReviewRequester {
    private let policy: ReviewPolicy
    private let requestReview: () -> Void

    init(policy: ReviewPolicy, requestReview: @escaping () -> Void) {
        self.policy = policy
        self.requestReview = requestReview
    }

    func requestReviewIfEligible() {
        guard policy.claimRequest() else { return }
        // StoreKit decides whether a sheet appears; this records only the attempt.
        requestReview()
    }
}
