import SwiftUI

struct ReminderCard: View {
    let reminder: Reminder
    let onToggle: (Bool) -> Void
    let onTestCheckIn: () -> Void
    let onEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                IconBadge(systemName: "drop.fill", tint: reminder.isActive ? Palette.freshBlue : Palette.snoozeGray)
                VStack(alignment: .leading, spacing: 2) {
                    Text(reminder.text)
                        .font(.headline)
                        .foregroundStyle(Palette.onSurface)
                    Label(reminder.scheduleSummary, systemImage: "clock")
                        .font(.caption)
                        .foregroundStyle(Palette.onSurfaceVariant)
                }
                Spacer(minLength: 8)
                Toggle("Active", isOn: Binding(get: { reminder.isActive }, set: onToggle))
                    .labelsHidden()
                    .tint(Palette.freshBlue)
            }

            HStack {
                Button(action: onTestCheckIn) {
                    Label("Test Check-in", systemImage: "lock.fill")
                        .font(.caption.bold())
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Palette.freshBlue, in: RoundedRectangle(cornerRadius: 12))
                }
                Spacer()
                Button(action: onEdit) {
                    Image(systemName: "pencil")
                        .foregroundStyle(Palette.onSurfaceVariant)
                        .padding(8)
                }
                .accessibilityLabel("Edit reminder")
            }
        }
        .card()
        .opacity(reminder.isActive ? 1 : 0.7)
    }
}
