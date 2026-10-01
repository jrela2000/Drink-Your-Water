import Foundation

enum SeedData {

    /// The free water framework and its six daily check-ins, matching the Android app.
    /// Stats start at zero: they're computed from real check-ins only.
    static func initial() -> AppData {
        let water = Framework(
            name: "Water Hydration",
            overlayTheme: .midnightWater,
            customMessage: "",
            isWater: true
        )
        let schedule: [(Int, Int, String)] = [
            (8, 0, "Morning Hydration (First Glass)"),
            (10, 30, "Post-Breakfast Water Refresh"),
            (13, 0, "Mid-Day Hydration Boost"),
            (15, 30, "Afternoon Energy Hydration"),
            (18, 30, "Pre-Dinner Glass of Water"),
            (21, 0, "Evening Wind-Down Hydration"),
        ]
        let reminders = schedule.map { hour, minute, text in
            Reminder(text: text, time: ClockTime(hour: hour, minute: minute), frameworkID: water.id)
        }
        return AppData(profile: UserProfile(), reminders: reminders, frameworks: [water], logs: [])
    }

    static let frameworkSuggestions = [
        "Medication Schedule",
        "Post-Study Movement",
        "Daily Prayer & Grace",
        "Vitamin & Supplement Routine",
        "Deep Focus Reset",
    ]
}
