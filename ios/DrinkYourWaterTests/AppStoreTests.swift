import XCTest
@testable import DrinkYourWater

@MainActor
final class AppStoreTests: XCTestCase {
    private func makeStore() -> AppStore {
        var data = SeedData.initial()
        data.profile.onboardingComplete = true
        return AppStore(file: nil, scheduler: nil, initialData: data)
    }

    func testSnoozeIsCappedAtTwoPerFiring() async {
        let store = makeStore()
        let checkIn = CheckIn(reminderID: store.reminders[0].id, firing: Date(timeIntervalSince1970: 1_000))

        XCTAssertEqual(store.snoozesLeft(for: checkIn), 2)
        let first = await store.snooze(checkIn)
        let second = await store.snooze(checkIn)
        let third = await store.snooze(checkIn)
        XCTAssertTrue(first)
        XCTAssertTrue(second)
        XCTAssertFalse(third)
        XCTAssertEqual(store.snoozesLeft(for: checkIn), 0)
    }

    func testNextFiringGetsAFreshSnoozeAllowance() async {
        let store = makeStore()
        let id = store.reminders[0].id
        let first = CheckIn(reminderID: id, firing: Date(timeIntervalSince1970: 1_000))
        await store.snooze(first)
        await store.snooze(first)

        let next = CheckIn(reminderID: id, firing: Date(timeIntervalSince1970: 90_000))
        XCTAssertEqual(store.snoozesLeft(for: next), 2)
    }

    func testConfirmLogsCompletionAndResetsSnoozes() async {
        let store = makeStore()
        let reminder = store.reminders[0]
        let checkIn = CheckIn(reminderID: reminder.id, firing: Date(timeIntervalSince1970: 1_000))
        await store.snooze(checkIn)

        store.confirm(checkIn, now: Date(timeIntervalSince1970: 2_000))

        XCTAssertEqual(store.data.logs.count, 1)
        XCTAssertEqual(store.data.logs[0].reminderText, reminder.text)
        XCTAssertEqual(store.data.logs[0].snoozeCount, 1)
        XCTAssertEqual(store.reminder(id: reminder.id)?.snoozeCount, 0)
        XCTAssertEqual(store.snoozesLeft(for: checkIn), 2)
    }

    func testSaveReminderInsertsThenUpdates() {
        let store = makeStore()
        var reminder = Reminder(text: "New", time: ClockTime(hour: 7, minute: 15))
        store.saveReminder(reminder)
        XCTAssertEqual(store.reminders.count, 7)
        XCTAssertEqual(store.reminders.first?.text, "New")
        XCTAssertNotNil(store.reminder(id: reminder.id)?.frameworkID, "new reminders join the water framework")

        reminder.text = "Edited"
        reminder.time = ClockTime(hour: 23, minute: 0)
        store.saveReminder(reminder)
        XCTAssertEqual(store.reminders.count, 7)
        XCTAssertEqual(store.reminders.last?.text, "Edited")
    }

    func testCustomFrameworkUsesItsOwnThemeAndMessage() {
        let store = makeStore()
        store.createFramework(
            name: "Vitamins", theme: .coolMint, customMessage: "Take your vitamins.",
            items: [(ClockTime(hour: 9, minute: 0), "Morning vitamins")]
        )
        let reminder = store.reminders.first { $0.text == "Morning vitamins" }!
        XCTAssertEqual(store.data.overlayTheme(for: reminder), .coolMint)
        XCTAssertEqual(store.data.overlayCopy(for: reminder), "Take your vitamins.")

        let water = store.reminders.first { $0.text.contains("Morning Hydration") }!
        XCTAssertEqual(store.data.overlayTheme(for: water), store.profile.overlayTheme)

        store.deleteFramework(store.customFrameworks[0].id)
        XCTAssertNil(store.reminders.first { $0.text == "Morning vitamins" })
    }

    func testWipeRestoresSeedAndRestartsOnboarding() async {
        let store = makeStore()
        let checkIn = CheckIn(reminderID: store.reminders[0].id, firing: Date())
        store.confirm(checkIn)
        store.wipeAllData()
        XCTAssertTrue(store.data.logs.isEmpty)
        XCTAssertFalse(store.profile.onboardingComplete)
        XCTAssertEqual(store.reminders.count, 6)
    }

    func testDataFileRoundTrip() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathComponent("data.json")
        let file = DataFile(url: url)
        XCTAssertNil(file.load())

        var data = SeedData.initial()
        data.profile.vacationModeEnd = Date(timeIntervalSince1970: 1_800_000_000)
        try file.save(data)
        XCTAssertEqual(file.load(), data)
    }
}
