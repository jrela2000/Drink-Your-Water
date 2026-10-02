import SwiftUI
import UIKit
import UserNotifications

@main
struct DrinkYourWaterApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        let store = appDelegate.store
        WindowGroup {
            RootView()
                .environment(store)
                .environment(\.screenshotTab, appDelegate.screenshot?.tab)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task { await store.syncNotifications() }
            }
        }
        .backgroundTask(.appRefresh(AppStore.refreshTaskID)) {
            await store.syncNotifications()
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    let screenshot = ScreenshotMode.fromLaunchArguments()
    lazy var store: AppStore = screenshot?.makeStore() ?? AppStore()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if screenshot == nil {
            UNUserNotificationCenter.current().delegate = self
            NotificationScheduler.shared.registerCategories()
        }
        _ = store
        return true
    }

    nonisolated private static func parse(_ userInfo: [AnyHashable: Any]) -> (UUID, Date)? {
        guard let idString = userInfo[NotificationScheduler.reminderIDKey] as? String,
              let id = UUID(uuidString: idString),
              let firing = userInfo[NotificationScheduler.firingKey] as? Double
        else { return nil }
        return (id, Date(timeIntervalSince1970: firing))
    }

    /// A reminder arriving while the app is open goes straight to the check-in screen.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        guard let (id, firing) = Self.parse(notification.request.content.userInfo) else {
            completionHandler([.banner, .sound])
            return
        }
        Task { @MainActor in
            self.store.beginCheckIn(reminderID: id, firing: firing)
            completionHandler([.sound])
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let action = response.actionIdentifier
        guard let (id, firing) = Self.parse(response.notification.request.content.userInfo) else {
            completionHandler()
            return
        }
        Task { @MainActor in
            if action == NotificationScheduler.snoozeAction {
                await self.store.snooze(CheckIn(reminderID: id, firing: firing))
            } else if action == UNNotificationDefaultActionIdentifier {
                self.store.beginCheckIn(reminderID: id, firing: firing)
            }
            completionHandler()
        }
    }
}
