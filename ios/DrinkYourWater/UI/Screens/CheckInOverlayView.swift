import SwiftUI

/// The full-screen "lock" check-in. There's deliberately no close button and it can't be
/// swiped away: the only ways out are confirming or using one of the two snoozes.
/// (iOS doesn't let any app block the Home gesture; see ios/README.md.)
struct CheckInOverlayView: View {
    let checkIn: CheckIn

    @Environment(AppStore.self) private var store
    @State private var pulse = false
    @State private var confirmed = false

    var body: some View {
        let reminder = store.reminder(id: checkIn.reminderID)
        let theme = reminder.map { store.data.overlayTheme(for: $0) } ?? store.profile.overlayTheme
        let copy = reminder.map { store.data.overlayCopy(for: $0) } ?? store.profile.motivationalStyle.overlayCopy
        let snoozesLeft = store.snoozesLeft(for: checkIn)
        let snoozesUsed = AppData.maxSnoozesPerFiring - snoozesLeft

        ZStack {
            theme.gradient.ignoresSafeArea()

            VStack {
                VStack(spacing: 16) {
                    Label("CHECK-IN REQUIRED", systemImage: "lock.fill")
                        .font(.caption.bold())
                        .tracking(1)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(.white.opacity(0.15), in: Capsule())

                    TimelineView(.everyMinute) { context in
                        Text(context.date.formatted(date: .omitted, time: .shortened))
                            .font(.system(size: 52, weight: .bold, design: .rounded))
                            .foregroundStyle(.white)
                    }
                }
                .padding(.top, 32)

                Spacer()

                VStack(spacing: 24) {
                    ZStack {
                        Circle()
                            .fill(RadialGradient(colors: [Palette.iceTeal, .clear], center: .center, startRadius: 0, endRadius: 60))
                            .frame(width: 120, height: 120)
                            .scaleEffect(pulse ? 1.06 : 0.94)
                        Circle()
                            .fill(.white.opacity(0.2))
                            .frame(width: 84, height: 84)
                        Image(systemName: "drop.fill")
                            .font(.system(size: 44))
                            .foregroundStyle(.white)
                    }
                    .onAppear {
                        withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) { pulse = true }
                    }

                    Text(reminder?.text ?? "Hydration Reminder")
                        .font(.title.bold())
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.white)

                    Text(copy)
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Palette.iceTeal)
                        .padding(20)
                        .frame(maxWidth: .infinity)
                        .background(.black.opacity(0.25), in: RoundedRectangle(cornerRadius: 20))
                }

                Spacer()

                if confirmed {
                    VStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 64))
                            .foregroundStyle(Palette.confirmGreen)
                        Text("Check-in Confirmed!")
                            .font(.title2.bold())
                            .foregroundStyle(.white)
                    }
                    .transition(.scale.combined(with: .opacity))
                    .padding(.bottom, 40)
                } else {
                    VStack(spacing: 12) {
                        Button {
                            store.confirm(checkIn)
                            withAnimation(.spring) { confirmed = true }
                            Task {
                                try? await Task.sleep(for: .seconds(1.2))
                                store.activeCheckIn = nil
                            }
                        } label: {
                            Label("I Did It — Confirm ✓", systemImage: "checkmark.circle.fill")
                        }
                        .buttonStyle(PrimaryButtonStyle(color: Palette.confirmGreen))

                        Button {
                            Task {
                                if await store.snooze(checkIn) { store.activeCheckIn = nil }
                            }
                        } label: {
                            Label(
                                snoozesLeft > 0
                                    ? "Snooze \(AppData.snoozeMinutes) min (\(snoozesUsed)/\(AppData.maxSnoozesPerFiring) used)"
                                    : "No snoozes left — confirm to continue",
                                systemImage: "moon.zzz.fill"
                            )
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.9))
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(Palette.snoozeGray.opacity(0.35), in: RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(snoozesLeft == 0)
                    }
                    .padding(.bottom, 24)
                }
            }
            .padding(.horizontal, 24)
        }
        .sensoryFeedback(.success, trigger: confirmed)
    }
}
