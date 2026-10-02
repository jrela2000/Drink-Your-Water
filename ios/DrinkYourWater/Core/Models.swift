import Foundation

/// A wall-clock time of day (no date, no time zone), e.g. 8:30 AM.
struct ClockTime: Codable, Hashable, Comparable {
    var hour: Int
    var minute: Int

    init(hour: Int, minute: Int) {
        self.hour = min(max(hour, 0), 23)
        self.minute = min(max(minute, 0), 59)
    }

    var minutesSinceMidnight: Int { hour * 60 + minute }

    static func < (lhs: ClockTime, rhs: ClockTime) -> Bool {
        lhs.minutesSinceMidnight < rhs.minutesSinceMidnight
    }

    /// "8:30 AM" style, independent of the device locale so it matches the Android app.
    var displayText: String {
        let h12 = hour % 12 == 0 ? 12 : hour % 12
        let suffix = hour < 12 ? "AM" : "PM"
        return String(format: "%d:%02d %@", h12, minute, suffix)
    }
}

enum ReminderFrequency: String, Codable, CaseIterable, Identifiable {
    case daily
    case every30Minutes = "30min"
    case everyHour = "1hr"
    case every2Hours = "2hr"
    case every3Hours = "3hr"

    var id: String { rawValue }

    /// nil means the reminder fires once a day at its time.
    var intervalMinutes: Int? {
        switch self {
        case .daily: return nil
        case .every30Minutes: return 30
        case .everyHour: return 60
        case .every2Hours: return 120
        case .every3Hours: return 180
        }
    }

    var label: String {
        switch self {
        case .daily: return "Once a day"
        case .every30Minutes: return "Every 30 min"
        case .everyHour: return "Every hour"
        case .every2Hours: return "Every 2 hours"
        case .every3Hours: return "Every 3 hours"
        }
    }
}

enum OverlayTheme: String, Codable, CaseIterable, Identifiable {
    case midnightWater = "midnight-water"
    case iceTeal = "ice-teal"
    case deepOcean = "deep-ocean"
    case coolMint = "cool-mint"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .midnightWater: return "Midnight Water"
        case .iceTeal: return "Ice Teal"
        case .deepOcean: return "Deep Ocean"
        case .coolMint: return "Cool Mint"
        }
    }

    /// Top and bottom colors of the lock overlay gradient, as 0xRRGGBB.
    var gradientHex: (top: UInt32, bottom: UInt32) {
        switch self {
        case .midnightWater: return (0x0D3B6B, 0x1A6FA8)
        case .iceTeal: return (0x0F4C5C, 0x48CAE4)
        case .deepOcean: return (0x03045E, 0x0077B6)
        case .coolMint: return (0x1B4332, 0x52B788)
        }
    }
}

enum MotivationalStyle: String, Codable, CaseIterable, Identifiable {
    case waterFacts
    case affirmations
    case scripture

    var id: String { rawValue }

    var title: String {
        switch self {
        case .waterFacts: return "Water Facts"
        case .affirmations: return "Positive Affirmations"
        case .scripture: return "Scripture & Faith"
        }
    }

    var shortTitle: String {
        switch self {
        case .waterFacts: return "Water Facts"
        case .affirmations: return "Affirmations"
        case .scripture: return "Scripture"
        }
    }

    var subtitle: String {
        switch self {
        case .waterFacts: return "Scientific trivia & health benefits of proper hydration"
        case .affirmations: return "Mindful encouraging self-talk and habit focus"
        case .scripture: return "Inspirational spiritual quotes and verses"
        }
    }

    /// Copy shown on the full-screen check-in.
    var overlayCopy: String {
        switch self {
        case .waterFacts: return "Drinking water boosts brain energy, clears skin, and fuels physical endurance!"
        case .affirmations: return "I nourish my body with pure water and stay focused on my daily vitality."
        case .scripture: return "Let anyone who is thirsty come to me and drink. (John 7:37)"
        }
    }

    /// Copy shown in the notification banner.
    var notificationBody: String {
        switch self {
        case .waterFacts: return "Tap to check in and confirm your glass."
        case .affirmations: return "You show up for yourself. Every single time."
        case .scripture: return "\"Let anyone who is thirsty come to me and drink.\" - John 7:37"
        }
    }
}

enum NotificationSound: String, Codable, CaseIterable, Identifiable {
    case tone1, tone2, tone3, tone4

    var id: String { rawValue }

    var title: String {
        switch self {
        case .tone1: return "Crystal Drops"
        case .tone2: return "Glacier Chime"
        case .tone3: return "Ocean Ripple"
        case .tone4: return "Serene Water Bell"
        }
    }

    var subtitle: String {
        switch self {
        case .tone1: return "Gentle water droplet bell chime"
        case .tone2: return "Crisp high-frequency mountain chime"
        case .tone3: return "Deep soothing ambient wave synth"
        case .tone4: return "Balanced soft acoustic tone"
        }
    }

    /// Bundled audio file used as the notification sound.
    var fileName: String { "\(rawValue).wav" }
}

struct Reminder: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var text: String
    var time: ClockTime
    var frequency: ReminderFrequency = .daily
    /// Last allowed firing time of day for interval reminders. Ignored for `.daily`.
    var endTime: ClockTime = ClockTime(hour: 22, minute: 0)
    /// Monday-first, 7 entries.
    var activeDays: [Bool] = Array(repeating: true, count: 7)
    var isActive: Bool = true
    var frameworkID: UUID?
    /// Snoozes used for the firing at `snoozedFiring`. A new firing starts with a full allowance.
    var snoozeCount: Int = 0
    var snoozedFiring: Date?

    func snoozesUsed(forFiring firing: Date) -> Int {
        snoozedFiring == firing ? snoozeCount : 0
    }

    var scheduleSummary: String {
        switch frequency {
        case .daily: return "\(time.displayText) • Once a day"
        default: return "\(time.displayText)–\(endTime.displayText) • \(frequency.label)"
        }
    }
}

struct Framework: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var name: String
    var overlayTheme: OverlayTheme = .midnightWater
    var customMessage: String = ""
    var isWater: Bool = false
}

struct CompletionLog: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var reminderID: UUID
    var reminderText: String
    var scheduledFor: Date
    var completedAt: Date
    var snoozeCount: Int
}

struct UserProfile: Codable, Hashable {
    var onboardingComplete: Bool = false
    var motivationalStyle: MotivationalStyle = .waterFacts
    var overlayTheme: OverlayTheme = .midnightWater
    var notificationSound: NotificationSound = .tone1
    var vacationModeEnd: Date?

    func isOnVacation(at now: Date) -> Bool {
        guard let end = vacationModeEnd else { return false }
        return end > now
    }
}

/// Everything the app persists. Saved as a single JSON file; the data set is tiny.
struct AppData: Codable, Hashable {
    static let currentSchemaVersion = 1

    var schemaVersion: Int = AppData.currentSchemaVersion
    var profile = UserProfile()
    var reminders: [Reminder] = []
    var frameworks: [Framework] = []
    var logs: [CompletionLog] = []

    static let maxSnoozesPerFiring = 2
    static let snoozeMinutes = 10

    func framework(for reminder: Reminder) -> Framework? {
        guard let id = reminder.frameworkID else { return nil }
        return frameworks.first { $0.id == id }
    }

    /// Custom frameworks carry their own theme/message; water reminders follow the profile.
    func overlayTheme(for reminder: Reminder) -> OverlayTheme {
        if let fw = framework(for: reminder), !fw.isWater { return fw.overlayTheme }
        return profile.overlayTheme
    }

    func overlayCopy(for reminder: Reminder) -> String {
        if let fw = framework(for: reminder), !fw.isWater, !fw.customMessage.isEmpty {
            return fw.customMessage
        }
        return profile.motivationalStyle.overlayCopy
    }

    func notificationBody(for reminder: Reminder) -> String {
        if let fw = framework(for: reminder), !fw.isWater, !fw.customMessage.isEmpty {
            return fw.customMessage
        }
        return profile.motivationalStyle.notificationBody
    }
}
