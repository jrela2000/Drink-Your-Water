import BackgroundTasks
import Foundation
import Observation
import UserNotifications

/// A reminder firing the user needs to check in on (shown as the full-screen overlay).
struct CheckIn: Identifiable, Hashable {
    let reminderID: UUID
    let firing: Date

    var id: String { "\(reminderID.uuidString)-\(firing.timeIntervalSince1970)" }
}

/// Single source of truth for app state. Every mutation is saved to disk and re-syncs the
/// scheduled notifications.
@MainActor
@Observable
final class AppStore {
    static let refreshTaskID = "com.aistudio.drinkyourwater.hydra.refresh"

    private(set) var data: AppData
    var activeCheckIn: CheckIn?
    private(set) var notificationStatus: UNAuthorizationStatus = .notDetermined

    @ObservationIgnored private let file: DataFile?
    @ObservationIgnored private let scheduler: NotificationScheduler?

    /// Pass nil for `file`/`scheduler` to run purely in memory (previews, screenshots).
    init(file: DataFile? = DataFile(url: DataFile.defaultURL),
         scheduler: NotificationScheduler? = .shared,
         initialData: AppData? = nil) {
        self.file = file
        self.scheduler = scheduler
        if let initialData {
            data = initialData
        } else if let saved = file?.load() {
            data = saved
        } else {
            data = SeedData.initial()
            try? file?.save(data)
        }
    }

    // MARK: - Reads

    var profile: UserProfile { data.profile }

    var reminders: [Reminder] {
        data.reminders.sorted { ($0.time, $0.text) < ($1.time, $1.text) }
    }

    var customFrameworks: [Framework] { data.frameworks.filter { !$0.isWater } }

    var recentLogs: [CompletionLog] { data.logs.sorted { $0.completedAt > $1.completedAt } }

    func reminder(id: UUID) -> Reminder? { data.reminders.first { $0.id == id } }

    // MARK: - Mutations

    private func update(_ change: (inout AppData) -> Void) {
        change(&data)
        persist()
    }

    private func persist() {
        try? file?.save(data)
        Task { await syncNotifications() }
    }

    func completeOnboarding(style: MotivationalStyle) {
        update {
            $0.profile.onboardingComplete = true
            $0.profile.motivationalStyle = style
        }
    }

    func setReminderActive(_ id: UUID, _ isActive: Bool) {
        update { data in
            guard let i = data.reminders.firstIndex(where: { $0.id == id }) else { return }
            data.reminders[i].isActive = isActive
        }
    }

    func saveReminder(_ reminder: Reminder) {
        update { data in
            if let i = data.reminders.firstIndex(where: { $0.id == reminder.id }) {
                data.reminders[i] = reminder
            } else {
                var new = reminder
                if new.frameworkID == nil {
                    new.frameworkID = data.frameworks.first(where: \.isWater)?.id
                }
                data.reminders.append(new)
            }
        }
    }

    func deleteReminder(_ id: UUID) {
        update { $0.reminders.removeAll { $0.id == id } }
    }

    func createFramework(name: String, theme: OverlayTheme, customMessage: String, items: [(ClockTime, String)]) {
        let framework = Framework(name: name, overlayTheme: theme, customMessage: customMessage, isWater: false)
        update { data in
            data.frameworks.append(framework)
            data.reminders += items.map { time, text in
                Reminder(text: text, time: time, frameworkID: framework.id)
            }
        }
    }

    func deleteFramework(_ id: UUID) {
        update { data in
            data.frameworks.removeAll { $0.id == id }
            data.reminders.removeAll { $0.frameworkID == id }
        }
    }

    func setOverlayTheme(_ theme: OverlayTheme) { update { $0.profile.overlayTheme = theme } }
    func setMotivationalStyle(_ style: MotivationalStyle) { update { $0.profile.motivationalStyle = style } }
    func setNotificationSound(_ sound: NotificationSound) { update { $0.profile.notificationSound = sound } }

    func setVacation(days: Int?) {
        update { data in
            data.profile.vacationModeEnd = days.map { Date().addingTimeInterval(TimeInterval($0) * 86_400) }
        }
    }

    func wipeAllData() {
        scheduler?.removeAll()
        activeCheckIn = nil
        data = SeedData.initial()
        persist()
    }

    // MARK: - Check-ins

    func beginCheckIn(reminderID: UUID, firing: Date = Date()) {
        guard reminder(id: reminderID) != nil else { return }
        activeCheckIn = CheckIn(reminderID: reminderID, firing: firing)
    }

    func snoozesLeft(for checkIn: CheckIn) -> Int {
        guard let reminder = reminder(id: checkIn.reminderID) else { return 0 }
        return max(0, AppData.maxSnoozesPerFiring - reminder.snoozesUsed(forFiring: checkIn.firing))
    }

    func confirm(_ checkIn: CheckIn, now: Date = Date()) {
        guard let reminder = reminder(id: checkIn.reminderID) else { return }
        update { data in
            data.logs.append(CompletionLog(
                reminderID: reminder.id,
                reminderText: reminder.text,
                scheduledFor: checkIn.firing,
                completedAt: now,
                snoozeCount: reminder.snoozesUsed(forFiring: checkIn.firing)
            ))
            if let i = data.reminders.firstIndex(where: { $0.id == reminder.id }) {
                data.reminders[i].snoozeCount = 0
                data.reminders[i].snoozedFiring = nil
            }
        }
        Task { await scheduler?.clear(reminderID: reminder.id) }
    }

    /// Returns false when this firing has no snoozes left.
    @discardableResult
    func snooze(_ checkIn: CheckIn, now: Date = Date()) async -> Bool {
        guard snoozesLeft(for: checkIn) > 0,
              let i = data.reminders.firstIndex(where: { $0.id == checkIn.reminderID })
        else { return false }

        update { data in
            let used = data.reminders[i].snoozesUsed(forFiring: checkIn.firing)
            data.reminders[i].snoozeCount = used + 1
            data.reminders[i].snoozedFiring = checkIn.firing
        }
        await scheduler?.scheduleSnooze(for: data.reminders[i], firing: checkIn.firing, data: data, now: now)
        return true
    }

    // MARK: - Notifications

    func requestNotificationPermission() async {
        guard let scheduler else { return }
        _ = await scheduler.requestAuthorization()
        await syncNotifications()
    }

    /// Re-reads permission and refills the notification queue. Safe to call often.
    func syncNotifications() async {
        guard let scheduler else { return }
        notificationStatus = await scheduler.authorizationStatus()
        await scheduler.reschedule(data)
        scheduleBackgroundRefresh()
    }

    private func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: Self.refreshTaskID)
        request.earliestBeginDate = Date().addingTimeInterval(6 * 3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}
