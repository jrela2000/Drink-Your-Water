import XCTest
@testable import DrinkYourWater

final class HabitStatsTests: XCTestCase {
    private let calendar = Calendar(identifier: .gregorian)
    private let reminderID = UUID()

    private func day(_ offset: Int, hour: Int = 9, from base: Date) -> Date {
        let start = calendar.startOfDay(for: base)
        return calendar.date(byAdding: DateComponents(day: offset, hour: hour), to: start)!
    }

    private func log(_ at: Date) -> CompletionLog {
        CompletionLog(reminderID: reminderID, reminderText: "Water", scheduledFor: at, completedAt: at, snoozeCount: 0)
    }

    private let now = Date(timeIntervalSince1970: 1_790_000_000)

    func testNoLogsMeansNoStreak() {
        XCTAssertEqual(HabitStats.currentStreak(logs: [], now: now, calendar: calendar), 0)
        XCTAssertEqual(HabitStats.bestStreak(logs: [], calendar: calendar), 0)
    }

    func testStreakCountsConsecutiveDaysIncludingToday() {
        let logs = [0, -1, -2, -4].map { log(day($0, from: now)) }
        XCTAssertEqual(HabitStats.currentStreak(logs: logs, now: now, calendar: calendar), 3)
    }

    func testStreakSurvivesUntilTodaysFirstCheckIn() {
        let logs = [-1, -2].map { log(day($0, from: now)) }
        XCTAssertEqual(HabitStats.currentStreak(logs: logs, now: now, calendar: calendar), 2)
    }

    func testStreakBreaksAfterAMissedDay() {
        let logs = [-2, -3].map { log(day($0, from: now)) }
        XCTAssertEqual(HabitStats.currentStreak(logs: logs, now: now, calendar: calendar), 0)
    }

    func testMultipleCheckInsPerDayCountOnce() {
        let logs = [log(day(0, hour: 8, from: now)), log(day(0, hour: 12, from: now)), log(day(-1, from: now))]
        XCTAssertEqual(HabitStats.currentStreak(logs: logs, now: now, calendar: calendar), 2)
        XCTAssertEqual(HabitStats.completions(onDayOf: now, logs: logs, calendar: calendar), 2)
    }

    func testBestStreakFindsLongestRun() {
        let logs = [-10, -9, -8, -7, -3, -2].map { log(day($0, from: now)) }
        XCTAssertEqual(HabitStats.bestStreak(logs: logs, calendar: calendar), 4)
    }

    func testWeekIsSevenDaysEndingToday() {
        let reminders = SeedData.initial().reminders
        let logs = [log(day(0, from: now)), log(day(-6, from: now)), log(day(-7, from: now))]
        let week = HabitStats.week(logs: logs, reminders: reminders, now: now, calendar: calendar)
        XCTAssertEqual(week.count, 7)
        XCTAssertTrue(week.last!.isToday)
        XCTAssertEqual(week.map(\.completed), [1, 0, 0, 0, 0, 0, 1])
        XCTAssertEqual(week.first!.planned, 6)
    }
}
