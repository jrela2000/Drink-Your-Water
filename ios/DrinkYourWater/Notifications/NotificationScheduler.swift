import Foundation
import UserNotifications

/// Keeps iOS's pending local notifications in sync with the reminder list.
///
/// iOS keeps at most 64 pending local notifications per app, so instead of one repeating
/// notification per reminder (which can't express interval windows or vacation mode), this
/// schedules the soonest individual firings and tops the queue up whenever the app runs
/// (launch, foreground, background refresh, any edit). If the queue fills before the
/// horizon ends, a last "open the app" notification is queued so reminders don't silently stop.
/// Unanswered check-ins re-alert up to three times; see `NotificationPlan`.
final class NotificationScheduler {
    static let shared = NotificationScheduler()

    static let reminderCategory = "REMINDER"
    static let finalSnoozeCategory = "REMINDER_NO_SNOOZE"
    static let snoozeAction = "SNOOZE"

    static let reminderIDKey = "reminderID"
    static let firingKey = "firing"

    // Identifiers: "slot.<reminder>.<firing>", "nag.<reminder>.<firing>.<level>",
    // "snooze.<reminder>", "snoozenag.<reminder>.<level>", "topup".
    private static let slotPrefix = "slot."
    private static let nagPrefix = "nag."
    private static let snoozePrefix = "snooze."
    private static let snoozeNagPrefix = "snoozenag."
    private static let topUpID = "topup"

    /// Firings plus their re-alerts. Leaves room under iOS's 64 for the top-up notice and
    /// for a few snoozes (each snooze is 1 + its re-alerts).
    private static let planBudget = 44

    private let center = UNUserNotificationCenter.current()

    func registerCategories() {
        let snooze = UNNotificationAction(
            identifier: Self.snoozeAction,
            title: "Snooze \(AppData.snoozeMinutes) min",
            options: []
        )
        let withSnooze = UNNotificationCategory(
            identifier: Self.reminderCategory, actions: [snooze], intentIdentifiers: [], options: []
        )
        let noSnooze = UNNotificationCategory(
            identifier: Self.finalSnoozeCategory, actions: [], intentIdentifiers: [], options: []
        )
        center.setNotificationCategories([withSnooze, noSnooze])
    }

    func requestAuthorization() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    func authorizationStatus() async -> UNAuthorizationStatus {
        await center.notificationSettings().authorizationStatus
    }

    /// Replaces all scheduled firings and re-alerts with the plan for `data`. Snoozes and
    /// their re-alerts are left alone; they're managed by `scheduleSnooze` and `clear`.
    func reschedule(_ data: AppData, now: Date = Date()) async {
        let pending = await center.pendingNotificationRequests()
        let stale = pending.map(\.identifier).filter {
            $0.hasPrefix(Self.slotPrefix) || $0.hasPrefix(Self.nagPrefix) || $0 == Self.topUpID
        }
        center.removePendingNotificationRequests(withIdentifiers: stale)

        guard data.profile.onboardingComplete else { return }

        let plan = NotificationPlan.make(
            reminders: data.reminders,
            logs: data.logs,
            now: now,
            pausedUntil: data.profile.isOnVacation(at: now) ? data.profile.vacationModeEnd : nil,
            budget: Self.planBudget,
            calendar: .current
        )
        let byID = Dictionary(uniqueKeysWithValues: data.reminders.map { ($0.id, $0) })

        for item in plan.notifications {
            guard let reminder = byID[item.reminderID] else { continue }
            let ts = String(Int(item.firing.timeIntervalSince1970))
            let id: String
            let level: Int
            switch item.kind {
            case .firing:
                id = Self.slotPrefix + reminder.id.uuidString + "." + ts
                level = 0
            case .escalation(let n):
                id = Self.nagPrefix + reminder.id.uuidString + "." + ts + "." + String(n)
                level = n
            }
            let content = makeContent(for: reminder, firing: item.firing, data: data, canSnooze: true, escalationLevel: level)
            try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger(at: item.fireAt)))
        }

        if plan.truncated, let last = plan.notifications.last(where: { $0.kind == .firing }) {
            let content = UNMutableNotificationContent()
            content.title = "Keep your reminders coming"
            content.body = "Open Drink Your Water so we can schedule your next check-ins."
            content.sound = .default
            let at = last.fireAt.addingTimeInterval(60)
            try? await center.add(UNNotificationRequest(identifier: Self.topUpID, content: content, trigger: trigger(at: at)))
        }
    }

    /// Schedules the snooze re-fire and its re-alerts, and stops the re-alerts of the
    /// prompt that was snoozed.
    func scheduleSnooze(for reminder: Reminder, firing: Date, data: AppData, now: Date = Date()) async {
        await removePending(for: reminder.id, firedBy: now)

        let canSnoozeAgain = reminder.snoozesUsed(forFiring: firing) < AppData.maxSnoozesPerFiring
        let at = now.addingTimeInterval(TimeInterval(AppData.snoozeMinutes * 60))
        let content = makeContent(for: reminder, firing: firing, data: data, canSnooze: canSnoozeAgain)
        try? await center.add(UNNotificationRequest(
            identifier: Self.snoozePrefix + reminder.id.uuidString, content: content, trigger: trigger(at: at)
        ))
        for item in NotificationPlan.snoozeEscalations(reminderID: reminder.id, firing: firing, snoozeAt: at) {
            guard case .escalation(let level) = item.kind else { continue }
            let nag = makeContent(for: reminder, firing: firing, data: data, canSnooze: canSnoozeAgain, escalationLevel: level)
            try? await center.add(UNNotificationRequest(
                identifier: Self.snoozeNagPrefix + reminder.id.uuidString + "." + String(level),
                content: nag, trigger: trigger(at: item.fireAt)
            ))
        }
    }

    /// Called once a check-in is confirmed: drops its pending snooze, any re-alerts for
    /// firings that already went off, and the reminder's banners.
    func clear(reminderID: UUID, now: Date = Date()) async {
        await removePending(for: reminderID, firedBy: now)
        let delivered = await center.deliveredNotifications()
        let ids = delivered
            .filter { $0.request.content.userInfo[Self.reminderIDKey] as? String == reminderID.uuidString }
            .map(\.request.identifier)
        center.removeDeliveredNotifications(withIdentifiers: ids)
    }

    /// Removes the snooze, snooze re-alerts, and re-alerts of firings at or before `cutoff`.
    /// Re-alerts for future firings stay scheduled.
    private func removePending(for reminderID: UUID, firedBy cutoff: Date) async {
        let rid = reminderID.uuidString
        let pending = await center.pendingNotificationRequests().map(\.identifier)
        let doomed = pending.filter { id in
            if id == Self.snoozePrefix + rid || id.hasPrefix(Self.snoozeNagPrefix + rid + ".") { return true }
            guard id.hasPrefix(Self.nagPrefix + rid + ".") else { return false }
            let parts = id.split(separator: ".")
            guard parts.count == 4, let ts = Double(parts[2]) else { return false }
            return ts <= cutoff.timeIntervalSince1970
        }
        center.removePendingNotificationRequests(withIdentifiers: doomed)
    }

    func removeAll() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }

    private func makeContent(
        for reminder: Reminder, firing: Date, data: AppData, canSnooze: Bool, escalationLevel: Int = 0
    ) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        if escalationLevel > 0 {
            content.title = "Still waiting: \(reminder.text)"
            content.body = "Reminder \(escalationLevel + 1) of \(AppData.maxEscalations + 1). Tap to confirm or snooze."
        } else {
            content.title = reminder.text
            content.body = data.notificationBody(for: reminder)
        }
        content.sound = UNNotificationSound(named: UNNotificationSoundName(data.profile.notificationSound.fileName))
        content.categoryIdentifier = canSnooze ? Self.reminderCategory : Self.finalSnoozeCategory
        content.threadIdentifier = "reminders"
        content.interruptionLevel = .timeSensitive
        content.userInfo = [
            Self.reminderIDKey: reminder.id.uuidString,
            Self.firingKey: firing.timeIntervalSince1970,
        ]
        return content
    }

    private func trigger(at date: Date) -> UNCalendarNotificationTrigger {
        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        return UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    }
}
