import XCTest
@testable import DrinkYourWater

final class NotificationPlanTests: XCTestCase {
    private var calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/New_York")!
        return calendar
    }()

    private func date(_ h: Int, _ m: Int = 0, day: Int = 5) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 10, day: day, hour: h, minute: m))!
    }

    private func plan(_ reminders: [Reminder], logs: [CompletionLog] = [], now: Date, budget: Int = 44,
                      pausedUntil: Date? = nil) -> NotificationPlan.Result {
        NotificationPlan.make(reminders: reminders, logs: logs, now: now, pausedUntil: pausedUntil,
                              budget: budget, calendar: calendar)
    }

    func testUpcomingFiringGetsThreeReAlertsTenMinutesApart() {
        let r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        let items = plan([r], now: date(8)).notifications.filter { $0.firing == date(9) }

        XCTAssertEqual(items.map(\.fireAt), [date(9), date(9, 10), date(9, 20), date(9, 30)])
        XCTAssertEqual(items.map(\.kind), [.firing, .escalation(level: 1), .escalation(level: 2), .escalation(level: 3)])
    }

    func testUnansweredFiringKeepsRemainingReAlertsWhenAppOpens() {
        let r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        let items = plan([r], now: date(9, 15)).notifications.filter { $0.firing == date(9) }

        XCTAssertEqual(items.map(\.fireAt), [date(9, 20), date(9, 30)])
    }

    func testConfirmedFiringStopsEscalating() {
        let r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        let log = CompletionLog(reminderID: r.id, reminderText: "Water", scheduledFor: date(9, 5),
                                completedAt: date(9, 5), snoozeCount: 0)
        let items = plan([r], logs: [log], now: date(9, 15)).notifications.filter { $0.firing == date(9) }

        XCTAssertTrue(items.isEmpty)
    }

    func testSnoozedFiringLeavesReAlertsToTheSnooze() {
        var r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        r.snoozeCount = 1
        r.snoozedFiring = date(9)
        let items = plan([r], now: date(9, 5)).notifications.filter { $0.firing == date(9) }

        XCTAssertTrue(items.isEmpty)
    }

    func testBudgetGivesSoonestFiringsReAlertsThenFiringsAlone() {
        let r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        let result = plan([r], now: date(8), budget: 10)

        XCTAssertEqual(result.notifications.count, 10)
        // Two days with re-alerts (8 notifications), then two plain firings.
        XCTAssertEqual(result.notifications.filter { $0.kind == .firing }.count, 4)
        XCTAssertEqual(Array(result.notifications.suffix(2)).map(\.kind), [.firing, .firing])
        XCTAssertTrue(result.truncated)
    }

    func testVacationSkipsFiringsAndTheirReAlerts() {
        let r = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0))
        let result = plan([r], now: date(9, 5), pausedUntil: date(0, day: 7))

        XCTAssertTrue(result.notifications.allSatisfy { $0.firing >= date(0, day: 7) })
    }

    func testSnoozeReAlertsCountFromTheSnooze() {
        let id = UUID()
        let items = NotificationPlan.snoozeEscalations(reminderID: id, firing: date(9), snoozeAt: date(9, 15))

        XCTAssertEqual(items.map(\.fireAt), [date(9, 25), date(9, 35), date(9, 45)])
        XCTAssertTrue(items.allSatisfy { $0.firing == date(9) })
    }
}
