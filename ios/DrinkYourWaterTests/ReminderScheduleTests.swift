import XCTest
@testable import DrinkYourWater

final class ReminderScheduleTests: XCTestCase {
    private var calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "America/New_York")!
        return calendar
    }()

    private func date(_ y: Int, _ m: Int, _ d: Int, _ h: Int = 0, _ min: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d, hour: h, minute: min))!
    }

    // 2026-10-05 is a Monday.

    func testDailyReminderFiresOnceAtItsTime() {
        let reminder = Reminder(text: "Water", time: ClockTime(hour: 8, minute: 30))
        XCTAssertEqual(
            ReminderSchedule.firings(of: reminder, onDayOf: date(2026, 10, 5), calendar: calendar),
            [date(2026, 10, 5, 8, 30)]
        )
    }

    func testIntervalReminderRepeatsUntilEndTime() {
        var reminder = Reminder(text: "Water", time: ClockTime(hour: 9, minute: 0), frequency: .every2Hours)
        reminder.endTime = ClockTime(hour: 15, minute: 0)
        let firings = ReminderSchedule.firings(of: reminder, onDayOf: date(2026, 10, 5), calendar: calendar)
        XCTAssertEqual(firings, [9, 11, 13, 15].map { date(2026, 10, 5, $0) })
    }

    func testInactiveWeekdayHasNoFirings() {
        var reminder = Reminder(text: "Water", time: ClockTime(hour: 8, minute: 0))
        reminder.activeDays = [false, true, true, true, true, true, true] // Monday off
        XCTAssertTrue(ReminderSchedule.firings(of: reminder, onDayOf: date(2026, 10, 5), calendar: calendar).isEmpty)
        XCTAssertEqual(ReminderSchedule.firings(of: reminder, onDayOf: date(2026, 10, 6), calendar: calendar).count, 1)
    }

    func testUpcomingSkipsPastFiringsAndSorts() {
        let early = Reminder(text: "Early", time: ClockTime(hour: 8, minute: 0))
        let late = Reminder(text: "Late", time: ClockTime(hour: 20, minute: 0))
        let firings = ReminderSchedule.upcomingFirings(
            reminders: [late, early], after: date(2026, 10, 5, 12), limit: 3, calendar: calendar
        )
        XCTAssertEqual(firings.map(\.date), [date(2026, 10, 5, 20), date(2026, 10, 6, 8), date(2026, 10, 6, 20)])
        XCTAssertEqual(firings.map(\.reminderID), [late.id, early.id, late.id])
    }

    func testUpcomingIgnoresInactiveRemindersAndHonorsVacation() {
        var off = Reminder(text: "Off", time: ClockTime(hour: 9, minute: 0))
        off.isActive = false
        let on = Reminder(text: "On", time: ClockTime(hour: 10, minute: 0))
        let firings = ReminderSchedule.upcomingFirings(
            reminders: [off, on], after: date(2026, 10, 5, 0), pausedUntil: date(2026, 10, 8, 0),
            limit: 2, calendar: calendar
        )
        XCTAssertEqual(firings.map(\.date), [date(2026, 10, 8, 10), date(2026, 10, 9, 10)])
    }

    func testUpcomingRespectsLimit() {
        var hourly = Reminder(text: "Hourly", time: ClockTime(hour: 0, minute: 0), frequency: .every30Minutes)
        hourly.endTime = ClockTime(hour: 23, minute: 30)
        let firings = ReminderSchedule.upcomingFirings(reminders: [hourly], after: date(2026, 10, 5), limit: 56, calendar: calendar)
        XCTAssertEqual(firings.count, 56)
    }

    func testDaylightSavingDayKeepsWallClockTime() {
        // US DST ends 2026-11-01.
        let reminder = Reminder(text: "Water", time: ClockTime(hour: 8, minute: 0))
        let firings = ReminderSchedule.firings(of: reminder, onDayOf: date(2026, 11, 1), calendar: calendar)
        XCTAssertEqual(calendar.component(.hour, from: firings[0]), 8)
    }

    func testSeedDataIsSixDailyCheckIns() {
        let seed = SeedData.initial()
        XCTAssertEqual(seed.reminders.count, 6)
        XCTAssertEqual(ReminderSchedule.plannedCount(reminders: seed.reminders, onDayOf: date(2026, 10, 5), calendar: calendar), 6)
        XCTAssertTrue(seed.logs.isEmpty)
        XCTAssertFalse(seed.profile.onboardingComplete)
    }

    func testClockTimeDisplay() {
        XCTAssertEqual(ClockTime(hour: 0, minute: 5).displayText, "12:05 AM")
        XCTAssertEqual(ClockTime(hour: 13, minute: 0).displayText, "1:00 PM")
        XCTAssertEqual(ClockTime(hour: 12, minute: 30).displayText, "12:30 PM")
    }
}
