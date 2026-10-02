import Foundation

/// One upcoming firing of a reminder.
struct ScheduledFiring: Hashable {
    let reminderID: UUID
    let date: Date
}

/// Pure date math for when reminders fire. Mirrors the Android ReminderScheduler rules:
/// a reminder fires at its time on each active weekday, and interval reminders keep
/// firing every N minutes until their end time.
enum ReminderSchedule {

    /// Monday = 0 ... Sunday = 6.
    static func mondayIndex(of date: Date, calendar: Calendar) -> Int {
        (calendar.component(.weekday, from: date) + 5) % 7
    }

    /// All firings of `reminder` on the calendar day containing `day`, ignoring `isActive`.
    static func firings(of reminder: Reminder, onDayOf day: Date, calendar: Calendar) -> [Date] {
        guard reminder.activeDays.count == 7, reminder.activeDays[mondayIndex(of: day, calendar: calendar)] else {
            return []
        }
        guard let first = calendar.date(
            bySettingHour: reminder.time.hour, minute: reminder.time.minute, second: 0, of: day
        ) else { return [] }

        guard let interval = reminder.frequency.intervalMinutes else { return [first] }

        guard reminder.endTime >= reminder.time,
              let last = calendar.date(
                bySettingHour: reminder.endTime.hour, minute: reminder.endTime.minute, second: 0, of: day
              )
        else { return [first] }

        var result: [Date] = []
        var slot = first
        while slot <= last {
            result.append(slot)
            guard let next = calendar.date(byAdding: .minute, value: interval, to: slot) else { break }
            slot = next
        }
        return result
    }

    /// Soonest firings after `now` across all active reminders, sorted by date, at most `limit`.
    /// Firings before `pausedUntil` (vacation mode) are skipped.
    static func upcomingFirings(
        reminders: [Reminder],
        after now: Date,
        pausedUntil: Date? = nil,
        limit: Int,
        horizonDays: Int = 14,
        calendar: Calendar
    ) -> [ScheduledFiring] {
        let active = reminders.filter(\.isActive)
        guard limit > 0, !active.isEmpty else { return [] }

        let today = calendar.startOfDay(for: now)
        var result: [ScheduledFiring] = []

        for offset in 0...horizonDays {
            guard let day = calendar.date(byAdding: .day, value: offset, to: today) else { continue }
            var dayFirings: [ScheduledFiring] = []
            for reminder in active {
                for date in firings(of: reminder, onDayOf: day, calendar: calendar) where date > now {
                    if let pausedUntil, date < pausedUntil { continue }
                    dayFirings.append(ScheduledFiring(reminderID: reminder.id, date: date))
                }
            }
            dayFirings.sort { $0.date < $1.date }
            for firing in dayFirings {
                result.append(firing)
                if result.count == limit { return result }
            }
        }
        return result
    }

    /// How many check-ins are planned on the day containing `day`.
    static func plannedCount(reminders: [Reminder], onDayOf day: Date, calendar: Calendar) -> Int {
        reminders.filter(\.isActive).reduce(0) { total, reminder in
            total + firings(of: reminder, onDayOf: day, calendar: calendar).count
        }
    }
}
