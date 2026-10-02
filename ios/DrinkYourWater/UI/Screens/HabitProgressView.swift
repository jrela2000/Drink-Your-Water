import SwiftUI

struct HabitProgressView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        let now = Date()
        let calendar = Calendar.current
        let logs = store.data.logs
        let current = HabitStats.currentStreak(logs: logs, now: now, calendar: calendar)
        let best = HabitStats.bestStreak(logs: logs, calendar: calendar)
        let week = HabitStats.week(logs: logs, reminders: store.data.reminders, now: now, calendar: calendar)

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        stat("🔥 \(current)", "Current Streak")
                        stat("🏆 \(best)", "Best Streak")
                        stat("💧 \(logs.count)", "Total Check-ins")
                    }

                    WeeklyChart(days: week)

                    VStack(alignment: .leading, spacing: 14) {
                        Text("Streak Milestones").font(.headline).foregroundStyle(Palette.onSurface)
                        milestone("7-Day Water Habit Master", current: current, target: 7)
                        milestone("30-Day Hydration Warrior", current: current, target: 30)
                        milestone("100-Day Centurion", current: current, target: 100)
                    }
                    .card()

                    Text("Recent Confirmations")
                        .font(.title3.bold())
                        .foregroundStyle(Palette.onSurface)
                        .padding(.top, 8)

                    if logs.isEmpty {
                        VStack(spacing: 6) {
                            Image(systemName: "drop").font(.largeTitle).foregroundStyle(Palette.freshBlue)
                            Text("No check-ins recorded yet.").font(.headline).foregroundStyle(Palette.onSurface)
                            Text("Confirm a reminder (or tap Test Check-in on Home) to log your first one.")
                                .font(.caption)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(Palette.onSurfaceVariant)
                        }
                        .frame(maxWidth: .infinity)
                        .card()
                    } else {
                        ForEach(store.recentLogs.prefix(25)) { log in
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(log.reminderText).font(.subheadline.bold()).foregroundStyle(Palette.onSurface)
                                    Text(log.completedAt.formatted(.dateTime.month(.abbreviated).day().hour().minute()))
                                        .font(.caption)
                                        .foregroundStyle(Palette.onSurfaceVariant)
                                }
                                Spacer()
                                Text(log.snoozeCount > 0 ? "Confirmed ✓ (\(log.snoozeCount) snooze)" : "Confirmed ✓")
                                    .font(.caption.bold())
                                    .foregroundStyle(Palette.confirmGreen)
                            }
                            .card(Palette.surface)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .background(Palette.background)
            .navigationTitle("Progress")
        }
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(spacing: 4) {
            Text(value).font(.title3.bold()).foregroundStyle(Palette.onSurface)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text(label).font(.caption2).foregroundStyle(Palette.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Palette.surfaceVariant, in: RoundedRectangle(cornerRadius: 16))
    }

    private func milestone(_ title: String, current: Int, target: Int) -> some View {
        let achieved = current >= target
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: achieved ? "checkmark.seal.fill" : "trophy")
                    .foregroundStyle(achieved ? Palette.confirmGreen : Palette.freshBlue)
                Text(title).font(.subheadline.bold()).foregroundStyle(Palette.onSurface)
                Spacer()
                Text("\(min(current, target))/\(target) d").font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            }
            ProgressView(value: Double(min(current, target)), total: Double(target))
                .tint(achieved ? Palette.confirmGreen : Palette.freshBlue)
        }
    }
}
