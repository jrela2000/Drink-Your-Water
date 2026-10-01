# Drink Your Water — iOS

Native SwiftUI version of the app (iPhone, iOS 17+). It's a separate codebase from the
Android app in `/app`, built to match its screens, copy and behavior.

## Run it on your Mac

You need a Mac with Xcode 16 or newer.

```bash
brew install xcodegen      # one time
cd ios
xcodegen                   # creates DrinkYourWater.xcodeproj from project.yml
open DrinkYourWater.xcodeproj
```

Pick an iPhone simulator and press ⌘R. To run on your own iPhone, select the
**DrinkYourWater** target → **Signing & Capabilities** → choose your Team, then plug the
phone in and pick it as the run destination.

The `.xcodeproj` is generated and git-ignored. Edit `project.yml` for project settings
(bundle ID, version, capabilities) and re-run `xcodegen`.

Tests: ⌘U in Xcode. CI builds, tests and captures screenshots on every push that touches
`ios/` (see `.github/workflows/ios.yml`; screenshots are attached to each run as an artifact).

## How the "lock" works on iPhone

iOS doesn't allow any app to take over the lock screen or block the Home gesture. Android's
screen pinning has no iPhone equivalent. So on iOS:

1. Each check-in is a **Time Sensitive notification** with the chosen chime. Time Sensitive
   alerts break through Focus modes and notification summaries.
2. Tapping it (or the reminder arriving while the app is open) opens the **full-screen
   check-in**. It has no close button and can't be swiped away: the only ways out are
   **Confirm** or one of **two 10-minute snoozes** per reminder firing.
3. The notification also has a **Snooze 10 min** button. After two snoozes the re-fired
   alert has no snooze button left.

### The Pray Focus–style upgrade (phase 2)

The founder notes in `docs/lock-screen-mechanism.md` say this feature is modeled on Pray Focus.
On iPhone, Pray Focus uses Apple's **Screen Time API** (FamilyControls, ManagedSettings and
DeviceActivity) to **block chosen apps until you complete the habit**. Drink Your Water can do
the same on iOS: when a check-in fires, shield Instagram/TikTok/etc. until you confirm.

That needs the **Family Controls (Distribution)** entitlement, which Apple grants by request
and which can take a few weeks. Request it now from the Apple Developer account (Certificates,
Identifiers & Profiles → your App ID → Additional Capabilities → Family Controls) so it's ready
for a 1.1 release. v1.0 ships without it.

## Notification limits

iOS keeps at most 64 scheduled notifications per app. `NotificationScheduler` queues the next
56 individual check-ins and refills the queue whenever the app opens, comes to the foreground
or gets a background refresh. With the default six daily water reminders that covers more than
a week. If someone sets many interval reminders and doesn't open the app for a long time, a
last "open the app to keep reminders coming" notification fires instead of reminders silently
stopping.

## Code map

| Folder | What's there |
|---|---|
| `Core/` | Plain Swift models and logic: data model, schedule math, streak/stats, seed data, JSON file storage |
| `Store/AppStore.swift` | App state. Every change saves to disk and re-syncs notifications |
| `Notifications/` | Scheduling Time Sensitive notifications, snoozes and the 64-notification window |
| `UI/` | SwiftUI screens: onboarding, home, progress, settings, check-in overlay, framework builder |
| `App/` | App entry point, notification tap/snooze handling, screenshot mode for CI |
| `Resources/` | App icon, the four chime sounds, privacy manifest |

All data stays on the device in a single JSON file in Application Support. Nothing goes to a
server, no accounts, no analytics, no tracking.

## Differences from the Android build (deliberate)

- **Six default reminders fire once a day each.** On Android each seeded reminder also
  carries a `1hr` interval, so the scheduler repeats every one of them hourly until 10 PM
  (about 50 alerts a day). iOS adds a **Once a day** frequency and uses it for the defaults.
- **Stats are real.** Android seeds a 3-day streak, 7-day best and 18 completions on first
  launch, and the weekly chart is hard-coded. Streaks, totals and the weekly chart are computed
  here from actual check-ins. A day counts toward the streak when at least one check-in was
  confirmed.
- **Chimes actually play.** The Android sound setting is saved but never used. iOS bundles
  four tones and uses the selected one for notifications, with a preview in Settings.
- **Snoozes are per firing.** Android only resets the snooze count on Confirm, so a reminder
  you snoozed twice and then ignored stays at "no snoozes left" forever. Here each new firing
  starts with two snoozes.

## Before submitting to the App Store

- [ ] Apple Developer Program membership ($99/year) and a Team selected in Xcode.
- [ ] Confirm the bundle ID. It's `com.aistudio.drinkyourwater.hydra` to match Android.
      It can't be changed once the app record exists in App Store Connect, so change it in
      `project.yml` first if you want your own (for example `com.yourname.drinkyourwater`).
- [ ] Create the app in App Store Connect and archive from Xcode (Product → Archive →
      Distribute → App Store Connect), then test with TestFlight.
- [ ] Privacy policy URL (required even though no data leaves the phone). The App Privacy
      section can declare **Data Not Collected**.
- [ ] Screenshots for the 6.9" iPhone size (the CI screenshots use the largest available
      simulator and can be a starting point).
- [ ] Age rating questionnaire, category (Health & Fitness), support URL, description.
