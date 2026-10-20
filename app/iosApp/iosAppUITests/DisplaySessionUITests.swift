import XCTest

final class DisplaySessionUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testSingleTapAndDurationPersistence() {
        let app = makeApp()
        app.launch()
        let toggle = app.buttons["startEndSessionButton"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        let duration = app.buttons["sessionDurationPicker"]
        duration.tap()
        app.buttons["15 minutes"].tap()
        XCTAssertEqual(duration.value as? String, "15 minutes")
        capture("01-ready")

        toggle.tap()
        XCTAssertEqual(toggle.label, "End Display Session")
        XCTAssertTrue(app.staticTexts["sessionStatus"].exists)
        capture("02-active")

        app.buttons["helpAndPrivacyButton"].tap()
        XCTAssertTrue(app.staticTexts["How it works"].waitForExistence(timeout: 5))
        capture("03-help")

        app.terminate()
        app.launch()
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        XCTAssertEqual(toggle.label, "Start Display Session")
        XCTAssertEqual(duration.value as? String, "15 minutes")
    }

    @MainActor
    func testTimedSessionEndsAutomatically() {
        let app = makeApp()
        app.launchEnvironment["NEVER_SLEEP_VALIDATION_DURATION_SECONDS"] = "5"
        app.launch()
        let toggle = app.buttons["startEndSessionButton"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        toggle.tap()
        XCTAssertEqual(toggle.label, "End Display Session")
        let ended = NSPredicate(format: "label == %@", "Start Display Session")
        expectation(for: ended, evaluatedWith: toggle)
        waitForExpectations(timeout: 12)
        XCTAssertTrue(app.buttons["sessionDurationPicker"].exists)
    }

    @MainActor
    func testSessionResumesAfterBackgrounding() {
        let app = makeApp()
        app.launch()
        let toggle = app.buttons["startEndSessionButton"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        toggle.tap()
        XCUIDevice.shared.press(.home)
        app.activate()
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        XCTAssertEqual(toggle.label, "End Display Session")
        toggle.tap()
        XCTAssertEqual(toggle.label, "Start Display Session")
    }

    @MainActor
    private func makeApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["NEVER_SLEEP_UI_TESTING"] = "1"
        return app
    }

    @MainActor
    private func capture(_ name: String) {
        // Let the 1.1-second state transition finish before capturing artwork.
        Thread.sleep(forTimeInterval: 1.3)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
