import Foundation
import UserNotifications

/// Keeps iOS's pending local notifications in sync with the reminder list.
///
/// iOS keeps at most 64 pending local notifications per app, so instead of one repeating
/// notification per reminder (which can't express interval windows or vacation mode), this
/// schedules the soonest individual firings and tops the queue up whenever the app runs
/// (launch, foreground, background refresh, any edit). If the queue fills before the
/// horizon ends, a last "open the app" notification is queued so reminders don't silently stop.
final class NotificationScheduler {
    static let shared = NotificationScheduler()

    static let reminderCategory = "REMINDER"
    static let finalSnoozeCategory = "REMINDER_NO_SNOOZE"
    static let snoozeAction = "SNOOZE"

    static let reminderIDKey = "reminderID"
    static let firingKey = "firing"

    private static let slotPrefix = "slot."
    private static let snoozePrefix = "snooze."
    private static let topUpID = "topup"

    /// Leaves room under the 64 limit for snoozes and the top-up notice.
    private static let maxSlots = 56

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

    /// Replaces all scheduled reminder firings with the soonest ones for `data`.
    func reschedule(_ data: AppData, now: Date = Date()) async {
        let pending = await center.pendingNotificationRequests()
        let stale = pending.map(\.identifier).filter { $0.hasPrefix(Self.slotPrefix) || $0 == Self.topUpID }
        center.removePendingNotificationRequests(withIdentifiers: stale)

        guard data.profile.onboardingComplete else { return }

        let firings = ReminderSchedule.upcomingFirings(
            reminders: data.reminders,
            after: now,
            pausedUntil: data.profile.isOnVacation(at: now) ? data.profile.vacationModeEnd : nil,
            limit: Self.maxSlots,
            calendar: .current
        )
        let byID = Dictionary(uniqueKeysWithValues: data.reminders.map { ($0.id, $0) })

        for firing in firings {
            guard let reminder = byID[firing.reminderID] else { continue }
            let content = makeContent(for: reminder, firing: firing.date, data: data, canSnooze: true)
            let id = Self.slotPrefix + reminder.id.uuidString + "." + String(Int(firing.date.timeIntervalSince1970))
            try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger(at: firing.date)))
        }

        if firings.count == Self.maxSlots, let last = firings.last {
            let content = UNMutableNotificationContent()
            content.title = "Keep your reminders coming"
            content.body = "Open Drink Your Water so we can schedule your next check-ins."
            content.sound = .default
            let at = last.date.addingTimeInterval(60)
            try? await center.add(UNNotificationRequest(identifier: Self.topUpID, content: content, trigger: trigger(at: at)))
        }
    }

    func scheduleSnooze(for reminder: Reminder, firing: Date, data: AppData, now: Date = Date()) async {
        let canSnoozeAgain = reminder.snoozesUsed(forFiring: firing) < AppData.maxSnoozesPerFiring
        let content = makeContent(for: reminder, firing: firing, data: data, canSnooze: canSnoozeAgain)
        let at = now.addingTimeInterval(TimeInterval(AppData.snoozeMinutes * 60))
        let id = Self.snoozePrefix + reminder.id.uuidString
        try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger(at: at)))
    }

    /// Called once a check-in is confirmed: drops its pending snooze and its banners.
    func clear(reminderID: UUID) async {
        center.removePendingNotificationRequests(withIdentifiers: [Self.snoozePrefix + reminderID.uuidString])
        let delivered = await center.deliveredNotifications()
        let ids = delivered
            .filter { $0.request.content.userInfo[Self.reminderIDKey] as? String == reminderID.uuidString }
            .map(\.request.identifier)
        center.removeDeliveredNotifications(withIdentifiers: ids)
    }

    func removeAll() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }

    private func makeContent(for reminder: Reminder, firing: Date, data: AppData, canSnooze: Bool) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = reminder.text
        content.body = data.notificationBody(for: reminder)
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
