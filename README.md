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

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material Design 3 ("Sleek Interface" theme, edge-to-edge layout)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Navigation Compose
- **Local Persistence**: Android Room Database with KSP (Kotlin Symbol Processing)
- **Testing**: Robolectric JVM unit tests & Roborazzi screenshot verification

---

## 🚀 Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/drink-your-water.git
   cd drink-your-water
   ```

2. **Build debug APK**:
   ```bash
   gradle assembleDebug
   ```

3. **Run tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
