import Foundation

/// One local notification the app should have pending.
struct PlannedNotification: Hashable {
    enum Kind: Hashable {
        /// The reminder going off.
        case firing
        /// Re-alert `level` (1...maxEscalations) for a firing nobody has confirmed or snoozed.
        case escalation(level: Int)
    }

    let reminderID: UUID
    /// The firing this notification belongs to; a check-in confirms this firing.
    let firing: Date
    let fireAt: Date
    let kind: Kind
}

/// Decides which notifications to queue. iOS can't run code when a notification is ignored,
/// so each firing's re-alerts are scheduled up front at +10/+20/+30 minutes and removed when
/// the check-in is confirmed or snoozed. They count toward iOS's 64 pending notifications,
/// so the soonest firings get escalation first and later ones fall back to the firing alone.
enum NotificationPlan {

    struct Result: Equatable {
        var notifications: [PlannedNotification]
        /// True when more firings exist than fit in the budget (the app should ask to be opened).
        var truncated: Bool
    }

    static func make(
        reminders: [Reminder],
        logs: [CompletionLog],
        now: Date,
        pausedUntil: Date?,
        budget: Int,
        calendar: Calendar
    ) -> Result {
        let interval = TimeInterval(AppData.escalationMinutes * 60)
        let maxLevel = AppData.maxEscalations
        var result: [PlannedNotification] = []

        func escalations(for reminderID: UUID, firing: Date, after: Date) -> [PlannedNotification] {
            (1...maxLevel).compactMap { level in
                let at = firing.addingTimeInterval(interval * Double(level))
                guard at > after else { return nil }
                return PlannedNotification(reminderID: reminderID, firing: firing, fireAt: at, kind: .escalation(level: level))
            }
        }

        // 1. Firings that already went off recently and are still unanswered keep their
        //    remaining re-alerts, so opening the app without checking in doesn't cancel them.
        let windowStart = now.addingTimeInterval(-interval * Double(maxLevel))
        for reminder in reminders where reminder.isActive {
            for day in [now, calendar.date(byAdding: .day, value: -1, to: now) ?? now] {
                for firing in ReminderSchedule.firings(of: reminder, onDayOf: day, calendar: calendar)
                where firing <= now && firing > windowStart {
                    if let pausedUntil, firing < pausedUntil { continue }
                    // Snoozed: the snooze carries its own re-alerts.
                    if let snoozed = reminder.snoozedFiring, snoozed >= firing { continue }
                    // Answered: any check-in for this reminder since it went off (same rule as Android).
                    if logs.contains(where: { $0.reminderID == reminder.id && $0.completedAt >= firing }) { continue }
                    result += escalations(for: reminder.id, firing: firing, after: now)
                }
            }
        }
        result = Array(result.prefix(budget))

        // 2. Upcoming firings: each with its re-alerts while they fit, then the firing alone.
        let upcoming = ReminderSchedule.upcomingFirings(
            reminders: reminders, after: now, pausedUntil: pausedUntil,
            limit: max(budget, 1), calendar: calendar
        )
        var truncated = upcoming.count >= max(budget, 1)
        for firing in upcoming {
            let slot = PlannedNotification(reminderID: firing.reminderID, firing: firing.date, fireAt: firing.date, kind: .firing)
            let extra = escalations(for: firing.reminderID, firing: firing.date, after: now)
            if result.count + 1 + extra.count <= budget {
                result.append(slot)
                result += extra
            } else if result.count + 1 <= budget {
                result.append(slot)
            } else {
                truncated = true
                break
            }
        }
        return Result(notifications: result, truncated: truncated)
    }

    /// Re-alerts for a snoozed prompt, counted from when the snooze goes off.
    static func snoozeEscalations(reminderID: UUID, firing: Date, snoozeAt: Date) -> [PlannedNotification] {
        let interval = TimeInterval(AppData.escalationMinutes * 60)
        return (1...AppData.maxEscalations).map { level in
            PlannedNotification(
                reminderID: reminderID, firing: firing,
                fireAt: snoozeAt.addingTimeInterval(interval * Double(level)),
                kind: .escalation(level: level)
            )
        }
    }
}
