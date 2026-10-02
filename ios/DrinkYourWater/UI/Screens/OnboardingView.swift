import SwiftUI

struct OnboardingView: View {
    @Environment(AppStore.self) private var store
    @State private var step = 1
    @State private var style: MotivationalStyle = .waterFacts
    @State private var isBuilding = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 6) {
                ForEach(1...6, id: \.self) { i in
                    Capsule()
                        .fill(i == step ? Palette.freshBlue : Palette.outline.opacity(0.5))
                        .frame(width: i == step ? 28 : 8, height: 8)
                }
            }
            .animation(.easeInOut, value: step)
            .padding(.top, 16)

            Group {
                switch step {
                case 1: welcome
                case 2: howItWorks
                case 3: preloaded
                case 4: notifications
                case 5: vibe
                default: custom
                }
            }
            .padding(24)
            .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading)).combined(with: .opacity))
            .id(step)
        }
        .background(Palette.background.ignoresSafeArea())
        .fullScreenCover(isPresented: $isBuilding) {
            FrameworkBuilderView(onActivated: { store.completeOnboarding(style: style) })
        }
    }

    private func go(_ next: Int) {
        withAnimation(.easeInOut(duration: 0.3)) { step = next }
    }

    private func header(_ icon: String, _ title: String, _ text: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 56))
                .foregroundStyle(Palette.freshBlue)
                .frame(width: 120, height: 120)
                .background(Palette.freshBlue.opacity(0.12), in: Circle())
            Text(title)
                .font(.title.bold())
                .multilineTextAlignment(.center)
                .foregroundStyle(Palette.onBackground)
            Text(text)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(Palette.onSurfaceVariant)
        }
    }

    private var welcome: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "drop.fill")
                .font(.system(size: 64))
                .foregroundStyle(.white)
                .frame(width: 140, height: 140)
                .background(LinearGradient(colors: [Palette.iceTeal, Palette.midnightWater], startPoint: .top, endPoint: .bottom), in: Circle())
            Text("Drink Your Water")
                .font(.largeTitle.bold())
                .foregroundStyle(Palette.onBackground)
            Text("Your body has been waiting.")
                .font(.title3)
                .foregroundStyle(Palette.freshBlue)
            Text("Real accountability through full-screen check-in reminders. Confirm the habit to clear it.")
                .multilineTextAlignment(.center)
                .foregroundStyle(Palette.onSurfaceVariant)
            Spacer()
            Button("Start With Water (Free)") { go(3) }
                .buttonStyle(PrimaryButtonStyle())
            Button("Show Me How It Works") { go(2) }
                .foregroundStyle(Palette.freshBlue)
        }
    }

    private var howItWorks: some View {
        VStack(spacing: 24) {
            Spacer()
            header("lock.iphone", "Full-Screen Check-ins",
                   "When a reminder fires, tap it and a calming full-screen check-in appears. It only clears when you confirm you did the habit — or use one of two snoozes.")
            Spacer()
            Button("Got It, Next") { go(3) }
                .buttonStyle(PrimaryButtonStyle())
        }
    }

    private var preloaded: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("6 Preloaded Hydrations")
                .font(.title.bold())
                .foregroundStyle(Palette.onBackground)
            Text("Balanced throughout your day automatically")
                .foregroundStyle(Palette.onSurfaceVariant)
            VStack(spacing: 10) {
                ForEach(store.reminders) { reminder in
                    HStack(spacing: 12) {
                        Image(systemName: "drop.fill").foregroundStyle(Palette.freshBlue)
                        Text(reminder.time.displayText)
                            .font(.subheadline.monospacedDigit().bold())
                            .foregroundStyle(Palette.onSurface)
                            .frame(width: 76, alignment: .leading)
                        Text(reminder.text)
                            .font(.subheadline)
                            .foregroundStyle(Palette.onSurface)
                        Spacer()
                    }
                    .padding(12)
                    .background(Palette.surfaceVariant, in: RoundedRectangle(cornerRadius: 14))
                }
            }
            Spacer()
            Button("Activate Water Reminders") { go(4) }
                .buttonStyle(PrimaryButtonStyle())
        }
    }

    private var notifications: some View {
        VStack(spacing: 24) {
            Spacer()
            header("bell.badge.fill", "Turn On Reminder Alerts",
                   "Allow notifications so your check-ins arrive on schedule. They're marked Time Sensitive so they can reach you during Focus modes.")
            Spacer()
            Button("Allow Notifications") {
                Task {
                    await store.requestNotificationPermission()
                    go(5)
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            Button("Set Up Later") { go(5) }
                .foregroundStyle(Palette.freshBlue)
        }
    }

    private var vibe: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Choose Your Motivational Vibe")
                .font(.title.bold())
                .foregroundStyle(Palette.onBackground)
            Text("Copy displayed on your check-in screen")
                .foregroundStyle(Palette.onSurfaceVariant)
            ForEach(MotivationalStyle.allCases) { option in
                Button { style = option } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(option.title).font(.headline).foregroundStyle(Palette.onSurface)
                            Text(option.subtitle).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                        }
                        Spacer()
                        Image(systemName: style == option ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(style == option ? Palette.freshBlue : Palette.outline)
                    }
                    .card(style == option ? Palette.freshBlue.opacity(0.15) : Palette.surfaceVariant)
                }
                .buttonStyle(.plain)
            }
            Spacer()
            Button("Continue") { go(6) }
                .buttonStyle(PrimaryButtonStyle())
        }
    }

    private var custom: some View {
        VStack(spacing: 24) {
            Spacer()
            header("square.stack.3d.up.fill", "Custom Habit Frameworks",
                   "Water is free! You can also build check-in schedules for medications, movement, study time, prayer, and vitamins.")
            Spacer()
            Button("Build a Custom Habit Framework") { isBuilding = true }
                .buttonStyle(PrimaryButtonStyle())
            Button("Start With Water Only") { store.completeOnboarding(style: style) }
                .foregroundStyle(Palette.freshBlue)
        }
    }
}
