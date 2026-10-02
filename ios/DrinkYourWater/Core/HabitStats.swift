import Foundation

struct DaySummary: Hashable, Identifiable {
    let day: Date
    let label: String
    let completed: Int
    let planned: Int
    let isToday: Bool

    var id: Date { day }
}

/// Stats derived from the completion log, so they can never drift out of sync with it.
/// A day counts toward a streak when at least one check-in was confirmed that day.
enum HabitStats {

    static func completions(onDayOf day: Date, logs: [CompletionLog], calendar: Calendar) -> Int {
        logs.filter { calendar.isDate($0.completedAt, inSameDayAs: day) }.count
    }

    /// Consecutive days with a check-in, ending today. If nothing is logged yet today the
    /// streak is still alive and counts back from yesterday.
    static func currentStreak(logs: [CompletionLog], now: Date, calendar: Calendar) -> Int {
        let days = Set(logs.map { calendar.startOfDay(for: $0.completedAt) })
        var day = calendar.startOfDay(for: now)
        if !days.contains(day) {
            guard let yesterday = calendar.date(byAdding: .day, value: -1, to: day) else { return 0 }
            day = yesterday
        }
        var streak = 0
        while days.contains(day) {
            streak += 1
            guard let previous = calendar.date(byAdding: .day, value: -1, to: day) else { break }
            day = previous
        }
        return streak
    }

    static func bestStreak(logs: [CompletionLog], calendar: Calendar) -> Int {
        let days = Set(logs.map { calendar.startOfDay(for: $0.completedAt) }).sorted()
        var best = 0
        var run = 0
        var previous: Date?
        for day in days {
            if let previous,
               let expected = calendar.date(byAdding: .day, value: 1, to: previous),
               calendar.isDate(expected, inSameDayAs: day) {
                run += 1
            } else {
                run = 1
            }
            best = max(best, run)
            previous = day
        }
        return best
    }

    /// The last seven days ending today, oldest first.
    static func week(
        logs: [CompletionLog], reminders: [Reminder], now: Date, calendar: Calendar
    ) -> [DaySummary] {
        let today = calendar.startOfDay(for: now)
        let symbols = calendar.shortWeekdaySymbols
        return (0..<7).reversed().compactMap { back in
            guard let day = calendar.date(byAdding: .day, value: -back, to: today) else { return nil }
            let weekday = calendar.component(.weekday, from: day)
            return DaySummary(
                day: day,
                label: symbols[weekday - 1],
                completed: completions(onDayOf: day, logs: logs, calendar: calendar),
                planned: ReminderSchedule.plannedCount(reminders: reminders, onDayOf: day, calendar: calendar),
                isToday: back == 0
            )
        }
    }
}
