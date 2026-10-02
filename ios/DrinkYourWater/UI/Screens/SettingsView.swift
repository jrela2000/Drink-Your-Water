import SwiftUI
import UIKit

struct SettingsView: View {
    @Environment(AppStore.self) private var store
    @State private var showSounds = false
    @State private var confirmWipe = false

    var body: some View {
        let profile = store.profile
        let onVacation = profile.isOnVacation(at: Date())

        NavigationStack {
            List {
                Section {
                    HStack(spacing: 12) {
                        IconBadge(systemName: "checkmark.seal.fill", tint: Palette.confirmGreen)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("All Habit Reminders Active").font(.headline)
                            Text("Water hydration plus any custom habits you build — free for now")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section("Check-in Screen") {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Color Theme")
                        HStack(spacing: 8) {
                            ForEach(OverlayTheme.allCases) { theme in
                                Button { store.setOverlayTheme(theme) } label: {
                                    VStack(spacing: 6) {
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(theme.gradient)
                                            .frame(height: 44)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(Palette.freshBlue, lineWidth: profile.overlayTheme == theme ? 3 : 0)
                                            )
                                        Text(theme.title.components(separatedBy: " ").first ?? theme.title)
                                            .font(.caption2)
                                            .foregroundStyle(profile.overlayTheme == theme ? Palette.freshBlue : .secondary)
                                    }
                                }
                                .buttonStyle(.plain)
                                .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .padding(.vertical, 4)

                    Picker("Motivational Copy", selection: Binding(
                        get: { profile.motivationalStyle },
                        set: { store.setMotivationalStyle($0) }
                    )) {
                        ForEach(MotivationalStyle.allCases) { Text($0.shortTitle).tag($0) }
                    }

                    Button { showSounds = true } label: {
                        LabeledContent("Notification Chime", value: profile.notificationSound.title)
                    }
                    .foregroundStyle(.primary)
                }

                Section {
                    Toggle(isOn: Binding(
                        get: { onVacation },
                        set: { store.setVacation(days: $0 ? 3 : nil) }
                    )) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Vacation Mode")
                            Text(onVacation ? "Reminders paused" : "Pause all reminders for 3 days")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .tint(Palette.freshBlue)

                    Button {
                        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                    } label: {
                        LabeledContent("Notifications", value: notificationStatusText)
                    }
                    .foregroundStyle(.primary)
                } header: {
                    Text("Reminders")
                } footer: {
                    Text("Reminders are delivered as Time Sensitive notifications so they can break through Focus modes. Tapping one opens the full-screen check-in.")
                }

                Section {
                    Button("Delete All Data", role: .destructive) { confirmWipe = true }
                } header: {
                    Text("Your Data")
                } footer: {
                    Text("Everything is stored only on this iPhone. Nothing is sent to a server. Deleting erases your habits, history and streaks and restarts onboarding.")
                }

                Section {
                    LabeledContent("Version", value: Bundle.main.appVersion)
                }
            }
            .scrollContentBackground(.hidden)
            .background(Palette.background)
            .navigationTitle("Settings")
            .sheet(isPresented: $showSounds) {
                SoundPickerSheet(selected: profile.notificationSound) { store.setNotificationSound($0) }
            }
            .confirmationDialog(
                "Delete all data?",
                isPresented: $confirmWipe,
                titleVisibility: .visible
            ) {
                Button("Delete Everything", role: .destructive) { store.wipeAllData() }
            } message: {
                Text("This permanently erases all custom habit frameworks, check-in history and streaks.")
            }
        }
    }

    private var notificationStatusText: String {
        switch store.notificationStatus {
        case .authorized, .provisional, .ephemeral: return "On"
        case .denied: return "Off"
        default: return "Not set up"
        }
    }
}

extension Bundle {
    var appVersion: String {
        let version = infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }
}
