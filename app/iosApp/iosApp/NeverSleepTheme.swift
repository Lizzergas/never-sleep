import SwiftUI

// Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
enum NeverSleepTheme {
    // Shared visual language with the shipping Android app.
    static let canvas = Color(red: 13 / 255, green: 13 / 255, blue: 26 / 255)
    static let launchCanvas = Color(red: 4 / 255, green: 5 / 255, blue: 15 / 255)
    static let panel = Color(red: 24 / 255, green: 24 / 255, blue: 42 / 255)
    static let panelStrong = Color(red: 34 / 255, green: 34 / 255, blue: 58 / 255)
    static let content = Color(red: 241 / 255, green: 239 / 255, blue: 248 / 255)
    static let contentMuted = Color(red: 195 / 255, green: 190 / 255, blue: 206 / 255)
    static let contentSubtle = Color(red: 169 / 255, green: 163 / 255, blue: 183 / 255)
    static let divider = Color(red: 61 / 255, green: 56 / 255, blue: 80 / 255)
    static let normalAccent = Color(red: 255 / 255, green: 183 / 255, blue: 77 / 255)
    static let normalAmbient = Color(red: 74 / 255, green: 96 / 255, blue: 128 / 255)
    static let normalInk = Color(red: 45 / 255, green: 24 / 255, blue: 18 / 255)
    static let sunCore = Color(red: 1, green: 0.50, blue: 0.02)
    static let neverAccent = Color(red: 158 / 255, green: 140 / 255, blue: 255 / 255)
    static let neverAmbient = Color(red: 124 / 255, green: 77 / 255, blue: 255 / 255)
    static let cyanAccent = Color(red: 75 / 255, green: 224 / 255, blue: 245 / 255)
    static let shadow = Color(red: 5 / 255, green: 4 / 255, blue: 9 / 255)

    static let compactControlSize: CGFloat = 224
    static let regularControlSize: CGFloat = 256
    static let horizontalPadding: CGFloat = 24
    static let controlSpacing: CGFloat = 42
    static let stateTransition = Animation.timingCurve(0.65, 0, 0.35, 1, duration: 1.1)
    static let pressIn = Animation.timingCurve(0.7, 0, 0.84, 0, duration: 0.1)
    static let pressOut = Animation.timingCurve(0.16, 1, 0.3, 1, duration: 0.15)
}
