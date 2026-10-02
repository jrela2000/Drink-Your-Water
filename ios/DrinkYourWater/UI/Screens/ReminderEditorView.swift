import SwiftUI

extension ClockTime {
    /// Today at this time, for DatePicker bindings.
    var asDate: Date {
        Calendar.current.date(bySettingHour: hour, minute: minute, second: 0, of: Date()) ?? Date()
    }

    init(date: Date) {
        let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
        self.init(hour: parts.hour ?? 0, minute: parts.minute ?? 0)
    }
}

struct ReminderEditorView: View {
    let existing: Reminder?

    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var draft: Reminder
    @State private var confirmDelete = false

    private static let dayLetters = ["M", "T", "W", "T", "F", "S", "S"]

    init(existing: Reminder?) {
        self.existing = existing
        _draft = State(initialValue: existing ?? Reminder(text: "Water Hydration", time: ClockTime(hour: 10, minute: 0)))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Reminder") {
                    TextField("Reminder title / habit", text: $draft.text)
                    DatePicker(
                        draft.frequency == .daily ? "Time" : "Starts at",
                        selection: Binding(get: { draft.time.asDate }, set: { draft.time = ClockTime(date: $0) }),
                        displayedComponents: .hourAndMinute
                    )
                }

                Section("Repeat") {
                    Picker("Frequency", selection: $draft.frequency) {
                        ForEach(ReminderFrequency.allCases) { Text($0.label).tag($0) }
                    }
                    if draft.frequency != .daily {
                        DatePicker(
                            "Until",
                            selection: Binding(get: { draft.endTime.asDate }, set: { draft.endTime = ClockTime(date: $0) }),
                            displayedComponents: .hourAndMinute
                        )
                    }
                    HStack {
                        ForEach(0..<7, id: \.self) { i in
                            Button {
                                draft.activeDays[i].toggle()
                            } label: {
                                Text(Self.dayLetters[i])
                                    .font(.subheadline.bold())
                                    .frame(width: 34, height: 34)
                                    .foregroundStyle(draft.activeDays[i] ? .white : Palette.onSurfaceVariant)
                                    .background(draft.activeDays[i] ? Palette.freshBlue : Palette.surfaceVariant, in: Circle())
                            }
                            .buttonStyle(.plain)
                            .frame(maxWidth: .infinity)
                        }
                    }
                }

                if existing != nil {
                    Section {
                        Button("Delete Reminder", role: .destructive) { confirmDelete = true }
                    }
                }
            }
            .navigationTitle(existing == nil ? "New Reminder" : "Edit Reminder")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        draft.text = draft.text.trimmingCharacters(in: .whitespacesAndNewlines)
                        store.saveReminder(draft)
                        dismiss()
                    }
                    .disabled(!isValid)
                }
            }
            .confirmationDialog("Delete this reminder?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    store.deleteReminder(draft.id)
                    dismiss()
                }
            }
        }
    }

    private var isValid: Bool {
        !draft.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && draft.activeDays.contains(true)
            && (draft.frequency == .daily || draft.endTime >= draft.time)
    }
}
