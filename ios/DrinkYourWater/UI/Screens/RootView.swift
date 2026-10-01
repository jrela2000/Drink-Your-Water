import SwiftUI

struct RootView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        @Bindable var store = store
        Group {
            if store.profile.onboardingComplete {
                MainTabView()
            } else {
                OnboardingView()
            }
        }
        .tint(Palette.freshBlue)
        .fullScreenCover(item: $store.activeCheckIn) { checkIn in
            CheckInOverlayView(checkIn: checkIn)
                .interactiveDismissDisabled()
        }
    }
}

struct MainTabView: View {
    @Environment(\.screenshotTab) private var screenshotTab
    @State private var tab: AppTab = .home

    var body: some View {
        TabView(selection: $tab) {
            HomeView()
                .tabItem { Label("Home", systemImage: "drop.fill") }
                .tag(AppTab.home)
            HabitProgressView()
                .tabItem { Label("Progress", systemImage: "trophy.fill") }
                .tag(AppTab.progress)
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                .tag(AppTab.settings)
        }
        .onAppear { if let screenshotTab { tab = screenshotTab } }
    }
}
