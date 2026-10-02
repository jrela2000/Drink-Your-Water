import SwiftUI

/// Four-step wizard for a custom habit framework (name, schedule, look, review).
struct FrameworkBuilderView: View {
    var onActivated: (() -> Void)? = nil

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    private struct Row: Identifiable {
        let id = UUID()
        var time: ClockTime
        var text: String
    }

    private static let maxRows = 5

    @State private var step = 1
    @State private var name = ""
    @State private var rows: [Row] = [
        Row(time: ClockTime(hour: 9, minute: 0), text: "Morning Habit Check-in"),
        Row(time: ClockTime(hour: 14, minute: 0), text: "Afternoon Practice Check-in"),
    ]
    @State private var theme: OverlayTheme = .midnightWater
    @State private var message = "Take a deep breath and complete your habit."

    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(spacing: 6) {
                    ForEach(1...4, id: \.self) { i in
                        Capsule()
                            .fill(i <= step ? Palette.freshBlue : Palette.outline.opacity(0.4))
                            .frame(height: 6)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)

                ScrollView {
                    Group {
                        switch step {
                        case 1: nameStep
                        case 2: scheduleStep
                        case 3: lookStep
                        default: reviewStep
                        }
                    }
                    .padding(20)
                }

                footerButton
                    .padding(20)
            }
            .background(Palette.background)
            .navigationTitle(["Framework Name", "Habit Reminders", "Check-in Look", "Review"][step - 1])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(step == 1 ? "Cancel" : "Back") {
                        if step == 1 { dismiss() } else { withAnimation { step -= 1 } }
                    }
                }
            }
        }
    }

    // MARK: Steps

    private var nameStep: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Name Your Custom Habit Framework")
                .font(.title2.bold())
                .foregroundStyle(Palette.onBackground)
            TextField("Framework name", text: $name)
                .textFieldStyle(.roundedBorder)
                .onChange(of: name) { _, new in if new.count > 30 { name = String(new.prefix(30)) } }
            Text("\(name.count)/30").font(.caption).foregroundStyle(Palette.onSurfaceVariant)
            Text("Popular ideas").font(.headline).foregroundStyle(Palette.onSurface).padding(.top, 8)
            ForEach(SeedData.frameworkSuggestions, id: \.self) { suggestion in
                Button { name = suggestion } label: {
                    Text(suggestion)
                        .font(.subheadline)
                        .foregroundStyle(Palette.onSurface)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Palette.surfaceVariant, in: Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var scheduleStep: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Set Your Daily Check-in Schedule")
                .font(.title2.bold())
                .foregroundStyle(Palette.onBackground)
            Text("Up to \(Self.maxRows) daily reminders (time & prompt message)")
                .font(.subheadline)
                .foregroundStyle(Palette.onSurfaceVariant)

            ForEach($rows) { $row in
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        DatePicker("Time", selection: Binding(
                            get: { row.time.asDate }, set: { row.time = ClockTime(date: $0) }
                        ), displayedComponents: .hourAndMinute)
                        if rows.count > 1 {
                            Button(role: .destructive) {
                                rows.removeAll { $0.id == row.id }
                            } label: {
                                Image(systemName: "trash").foregroundStyle(Palette.dangerRed)
                            }
                            .accessibilityLabel("Remove reminder")
                        }
                    }
                    TextField("Reminder message", text: $row.text)
                        .textFieldStyle(.roundedBorder)
                }
                .card()
            }

            if rows.count < Self.maxRows {
                Button {
                    rows.append(Row(time: ClockTime(hour: 18, minute: 0), text: "Evening Habit Check-in"))
                } label: {
                    Label("Add Reminder (\(rows.count)/\(Self.maxRows))", systemImage: "plus.circle.fill")
                }
            }
        }
    }

    private var lookStep: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Live Check-in Preview")
                .font(.title2.bold())
                .foregroundStyle(Palette.onBackground)
            PhonePreview(theme: theme, title: rows.first?.text ?? trimmedName, message: message)
                .frame(maxWidth: .infinity)
            HStack(spacing: 8) {
                ForEach(OverlayTheme.allCases) { option in
                    Button { theme = option } label: {
                        Text(option.title.components(separatedBy: " ").first ?? option.title)
                            .font(.caption.bold())
                            .foregroundStyle(theme == option ? .white : Palette.onSurface)
                            .frame(maxWidth: .infinity, minHeight: 36)
                            .background(theme == option ? Palette.freshBlue : Palette.surfaceVariant, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            TextField("Custom motivational message", text: $message, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(2...4)
        }
    }

    private var reviewStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Review Framework")
                .font(.title2.bold())
                .foregroundStyle(Palette.onBackground)
            VStack(alignment: .leading, spacing: 8) {
                Text(trimmedName).font(.title3.bold()).foregroundStyle(Palette.freshBlue)
                Text("Theme: \(theme.title)").font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                Text("“\(message)”").font(.caption).italic().foregroundStyle(Palette.onSurfaceVariant)
                Divider().padding(.vertical, 4)
                Text("Daily check-ins").font(.subheadline.bold()).foregroundStyle(Palette.onSurface)
                ForEach(rows.sorted { $0.time < $1.time }) { row in
                    Text("• \(row.time.displayText): \(row.text)")
                        .font(.subheadline)
                        .foregroundStyle(Palette.onSurfaceVariant)
                }
            }
            .card()
        }
    }

    // MARK: Footer

    private var canContinue: Bool {
        switch step {
        case 1: return !trimmedName.isEmpty
        case 2: return !rows.isEmpty && rows.allSatisfy { !$0.text.trimmingCharacters(in: .whitespaces).isEmpty }
        default: return true
        }
    }

    private var footerButton: some View {
        Button(step < 4 ? ["Next: Add Reminders", "Next: Customize Look", "Next: Review"][step - 1] : "Activate My Framework") {
            if step < 4 {
                withAnimation { step += 1 }
            } else {
                store.createFramework(
                    name: trimmedName,
                    theme: theme,
                    customMessage: message.trimmingCharacters(in: .whitespacesAndNewlines),
                    items: rows.map { ($0.time, $0.text.trimmingCharacters(in: .whitespaces)) }
                )
                onActivated?()
                dismiss()
            }
        }
        .buttonStyle(PrimaryButtonStyle())
        .disabled(!canContinue)
        .opacity(canContinue ? 1 : 0.5)
    }
}

/// A miniature phone showing what the check-in screen will look like.
struct PhonePreview: View {
    let theme: OverlayTheme
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Label("CHECK-IN", systemImage: "lock.fill")
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(.white.opacity(0.15), in: Capsule())
            Image(systemName: "drop.fill")
                .font(.system(size: 28))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(.white.opacity(0.2), in: Circle())
            Text(title)
                .font(.footnote.bold())
                .multilineTextAlignment(.center)
                .foregroundStyle(.white)
            Text(message)
                .font(.caption2)
                .multilineTextAlignment(.center)
                .foregroundStyle(Palette.iceTeal)
                .padding(8)
                .background(.black.opacity(0.25), in: RoundedRectangle(cornerRadius: 10))
            RoundedRectangle(cornerRadius: 8)
                .fill(Palette.confirmGreen)
                .frame(height: 26)
                .overlay(Text("Confirm ✓").font(.caption2.bold()).foregroundStyle(.white))
        }
        .padding(16)
        .frame(width: 180, height: 320)
        .background(theme.gradient, in: RoundedRectangle(cornerRadius: 28))
        .overlay(RoundedRectangle(cornerRadius: 28).stroke(.black.opacity(0.8), lineWidth: 6))
        .accessibilityElement(children: .combine)
    }
}
