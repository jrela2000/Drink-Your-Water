# Drink Your Water 💧

> **Full-Screen Lock Overlay Accountability & Hydration Habit Tracker**

[![Live Sample](https://img.shields.io/badge/Live_Sample-Try_in_Browser-0077B6?style=for-the-badge&logo=android)](https://ais-pre-uu73w2zheoinds5l5cxbod-651608772304.us-east1.run.app)

**Drink Your Water** is a modern Android application built with **Jetpack Compose** and **Kotlin** that transforms habit building into a gentle yet unmissable accountability experience. Through full-screen lock screen overlay alerts, users are prompted to drink water or complete custom daily habits before unlocking their device.

---

## 🌐 Sample App Online

You can sample the web app directly in your browser:

👉 **[Launch Live Web Sample](https://drinkyourwater.base44.app)**

---

## ✨ Key Features

- **🔒 Signature Lock Screen Overlay**: High-accountability full-screen overlay that pops up on schedule with animated water drop pulses and one-tap completion or snooze options.
- **🛠️ Custom Habit Framework Builder**: Multi-step wizard to build custom lock screen overlay schedules for medications, study breaks, daily prayer, vitamins, and post-work movement.
- **📊 Streak & Progress Analytics**:
  - Radial progress ring tracking daily water intake goals.
  - Flame streak counters and milestone achievement trophies (7-day, 30-day, 100-day).
  - Weekly hydration breakdown charts and detailed lock screen confirmation logs.
- **🎨 Personalization & Themes**:
  - **Color Themes**: Midnight Water, Ice Teal, Deep Ocean, and Cool Mint.
  - **Motivational Vibe**: Choose between scientific Water Facts, Positive Affirmations, or Faith Scripture.
  - **Notification Chimes**: Crystal Drops, Zen Stream, Ocean Breeze, and Soft Bell.
- **🏖️ Vacation Mode & Privacy**: Pause notifications anytime for 3 days or wipe local data instantly from Settings.
- **⚡ 100% Local & Offline-First**: Built with Room Database so all habit records remain private and available without internet dependency.

---

## 🔒 How the Lock Screen Works

**How does the lock screen work?**
When a reminder fires, Drink Your Water shows a full-screen overlay that stays in front until you confirm the habit or use one of your two snoozes. The Home and Recent Apps buttons are disabled for the duration using Android's built-in **screen pinning** feature — the same mechanism Android itself offers for focus and kiosk-style apps.

**What permissions does the app need?**
Drink Your Water needs **Screen pinning** turned on in your phone's Settings (under Security, or search "pin" in Settings search — the exact menu varies by phone). We don't require any special or sensitive permission for this — screen pinning is a standard Android feature you control and can turn off at any time. We don't use Accessibility Services or "draw over other apps" permissions, and we don't block or monitor your use of other apps.

**Can I still get out if I really need to (e.g. a phone call)?**
Yes. Android's screen pinning always keeps one exit gesture available system-wide (hold Back + Recents together, or swipe-up-and-hold on gesture navigation) so you can never be fully locked out of your phone — that's an OS-level guarantee, not something any app can remove. In everyday use, though, there's no visible "X" or back-button shortcut out of the reminder screen; only confirming the habit or using a snooze will dismiss it normally.

See [docs/lock-screen-mechanism.md](docs/lock-screen-mechanism.md) for the full technical rationale, including why this doesn't (and can't) work the same way iOS app blockers like Pray Focus do.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material Design 3 ("Sleek Interface" theme, edge-to-edge layout)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Navigation Compose
- **Local Persistence**: Android Room Database with KSP (Kotlin Symbol Processing)
- **Testing**: Robolectric-backed JVM unit tests, plus instrumented tests on-device (Roborazzi is wired up for screenshot testing when a suite is added)

---

## 🚀 Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/jrela2000/Drink-Your-Water.git
   cd Drink-Your-Water
   ```

2. **Build debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run tests**:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

All rights reserved. No open-source license is granted at this time.
