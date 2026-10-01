import SwiftUI

enum AppTab: String, Hashable {
    case home, progress, settings
}

private struct ScreenshotTabKey: EnvironmentKey {
    static let defaultValue: AppTab? = nil
}

extension EnvironmentValues {
    /// Set only in screenshot mode, to open a specific tab.
    var screenshotTab: AppTab? {
        get { self[ScreenshotTabKey.self] }
        set { self[ScreenshotTabKey.self] = newValue }
    }
}

/// Debug-only launch mode used by CI to capture simulator screenshots with sample data:
/// `-screenshot onboarding|home|progress|settings|checkin`. Data stays in memory and no
/// notifications are scheduled. Compiled out of Release builds.
struct ScreenshotMode {
    let screen: String

    var tab: AppTab? { AppTab(rawValue: screen) ?? (screen == "checkin" ? .home : nil) }

    static func fromLaunchArguments() -> ScreenshotMode? {
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: "-screenshot"), i + 1 < args.count else { return nil }
        return ScreenshotMode(screen: args[i + 1])
        #else
        return nil
        #endif
    }

    @MainActor
    func makeStore() -> AppStore {
        var data = SeedData.initial()
        if screen != "onboarding" {
            data.profile.onboardingComplete = true
            data.logs = Self.sampleLogs(for: data.reminders)
        }
        let store = AppStore(file: nil, scheduler: nil, initialData: data)
        if screen == "checkin", let first = store.reminders.first {
            store.beginCheckIn(reminderID: first.id)
        }
        return store
    }

    /// Nine days of check-ins, a few missed, ending with two today.
    private static func sampleLogs(for reminders: [Reminder]) -> [CompletionLog] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let perDay = [5, 6, 4, 6, 6, 5, 6, 6, 2]
        var logs: [CompletionLog] = []
        for (index, count) in perDay.enumerated() {
            let daysBack = perDay.count - 1 - index
            guard let day = calendar.date(byAdding: .day, value: -daysBack, to: today) else { continue }
            for reminder in reminders.prefix(count) {
                guard let at = calendar.date(
                    bySettingHour: reminder.time.hour, minute: reminder.time.minute + 4, second: 0, of: day
                ) else { continue }
                logs.append(CompletionLog(
                    reminderID: reminder.id, reminderText: reminder.text,
                    scheduledFor: at, completedAt: at, snoozeCount: index % 3 == 0 ? 1 : 0
                ))
            }
        }
        return logs
    }
}
