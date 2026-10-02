import SwiftUI
import UIKit

struct HomeView: View {
    @Environment(AppStore.self) private var store
    @State private var editing: Reminder?
    @State private var isAdding = false
    @State private var isBuilding = false

    var body: some View {
        let now = Date()
        let calendar = Calendar.current
        let planned = ReminderSchedule.plannedCount(reminders: store.data.reminders, onDayOf: now, calendar: calendar)
        let done = HabitStats.completions(onDayOf: now, logs: store.data.logs, calendar: calendar)
        let streak = HabitStats.currentStreak(logs: store.data.logs, now: now, calendar: calendar)

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    ProgressRing(completed: done, total: planned, streak: streak)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)

                    statusBanners

                    builderBanner

                    Text("Today's Reminders (\(store.reminders.count))")
                        .font(.title3.bold())
                        .foregroundStyle(Palette.onSurface)
                        .padding(.top, 8)

                    ForEach(store.reminders) { reminder in
                        ReminderCard(
                            reminder: reminder,
                            onToggle: { store.setReminderActive(reminder.id, $0) },
                            onTestCheckIn: { store.beginCheckIn(reminderID: reminder.id) },
                            onEdit: { editing = reminder }
                        )
                    }

                    if store.customFrameworks.isEmpty {
                        Text("Explore Custom Habit Check-ins")
                            .font(.title3.bold())
                            .foregroundStyle(Palette.onSurface)
                            .padding(.top, 16)
                        ForEach(["Medication & Supplement Schedule", "Post-Study Movement & Stretch"], id: \.self) { name in
                            Button { isBuilding = true } label: { suggestionCard(name) }
                                .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .background(Palette.background)
            .navigationTitle("Drink Your Water")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { isAdding = true } label: { Image(systemName: "plus.circle.fill").font(.title2) }
                        .accessibilityLabel("Add reminder")
                }
            }
            .sheet(item: $editing) { ReminderEditorView(existing: $0) }
            .sheet(isPresented: $isAdding) { ReminderEditorView(existing: nil) }
            .fullScreenCover(isPresented: $isBuilding) { FrameworkBuilderView() }
        }
    }

    @ViewBuilder
    private var statusBanners: some View {
        if store.notificationStatus == .denied {
            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            } label: {
                banner(icon: "bell.slash.fill", tint: Palette.dangerRed,
                       title: "Notifications are off",
                       text: "Reminders can't reach you. Tap to turn them on in Settings.")
            }
            .buttonStyle(.plain)
        } else if store.notificationStatus == .notDetermined {
            Button {
                Task { await store.requestNotificationPermission() }
            } label: {
                banner(icon: "bell.badge.fill", tint: Palette.freshBlue,
                       title: "Turn on reminders",
                       text: "Allow notifications so your check-ins arrive on time.")
            }
            .buttonStyle(.plain)
        }
        if store.profile.isOnVacation(at: Date()), let end = store.profile.vacationModeEnd {
            banner(icon: "beach.umbrella.fill", tint: Palette.iceTeal,
                   title: "Vacation mode",
                   text: "Reminders paused until \(end.formatted(date: .abbreviated, time: .shortened)).")
        }
    }

    private func banner(icon: String, tint: Color, title: String, text: String) -> some View {
        HStack(spacing: 12) {
            IconBadge(systemName: icon, tint: tint, size: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.bold()).foregroundStyle(Palette.onSurface)
                Text(text).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            }
        }
        .card(tint.opacity(0.12))
    }

    private var builderBanner: some View {
        Button { isBuilding = true } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Build Custom Habit Framework")
                        .font(.headline)
                        .foregroundStyle(Palette.onSurface)
                    Text("Medications, movement, study, prayer & check-in accountability.")
                        .font(.caption)
                        .foregroundStyle(Palette.onSurfaceVariant)
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                Text("Create +")
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Palette.freshBlue, in: RoundedRectangle(cornerRadius: 12))
            }
            .card(Palette.freshBlue.opacity(0.12))
        }
        .buttonStyle(.plain)
    }

    private func suggestionCard(_ name: String) -> some View {
        HStack(spacing: 12) {
            IconBadge(systemName: "plus", tint: Palette.iceTeal, size: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(.headline).foregroundStyle(Palette.onSurface)
                Text("Custom Habit Check-in • Tap to Build").font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(Palette.onSurfaceVariant)
        }
        .card(Palette.surface)
    }
}
