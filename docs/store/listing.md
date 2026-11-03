# Store listing: Drink Your Water: Check-ins

Copy for Google Play Console and App Store Connect. Character limits are noted; every field
here fits. Claims only describe what v1.0 does. Update this file when features change.

---

## Shared description

Both stores use the same long description. Paste it as plain text: Play and Apple don't render
Markdown, and the bullet characters below are plain text.

```
Drink Your Water turns reminders into check-ins. When it's time to drink, a full-screen check-in asks one simple question: did you do it? Tap "I Did It" to confirm, or snooze for 10 minutes. No more reminders that slide by unnoticed.

HOW IT WORKS
• Six daily water reminders are set up for you, from your first glass in the morning to an evening wind-down. Change the times, or add your own.
• Each reminder opens a full-screen check-in. Confirm it or snooze it, with up to two 10-minute snoozes per reminder.
• Ignore a check-in and it reminds you again every 10 minutes, up to three more times. Confirm and it stops.
• Watch your streak grow. Your progress screen shows your current and best streak, total check-ins, a 7-day chart of confirmed vs. planned check-ins, and your history.

MAKE IT YOURS
• Reminders once a day, or every 30 minutes, hour, 2 hours or 3 hours within the window you choose, on the days you choose.
• Build your own habits, like stretching, reading, prayer or posture, each with its own reminders and messages.
• Choose your motivation: water facts, positive affirmations, or scripture and faith.
• Four color themes and four calm chimes.
• Vacation Mode pauses reminders for a few days, so a trip doesn't wreck your routine.

PRIVATE BY DESIGN
• No account, no sign-up.
• Everything stays on your phone. The app doesn't collect, send or sell any data.
• No ads, no analytics, no tracking.

Drink Your Water is a habit tool, not medical advice. Don't use it for medication reminders or anything where a missed reminder could cause harm.

Made by an independent developer. Built with the help of AI development tools; the app itself has no AI features.
```

> **Platform notes.** Add one of these as a final bullet under HOW IT WORKS:
> - **Google Play:** `• Optional Strict Mode uses Android screen pinning to keep the check-in in front until you respond.`
> - **App Store:** `• Reminders arrive as Time Sensitive notifications, so they can break through Focus.`

---

## Google Play Console

**Grow > Store presence > Main store listing**

| Field | Limit | Text |
|---|---|---|
| App name | 30 | `Drink Your Water: Check-ins` |
| Short description | 80 | `Water reminders with full-screen check-ins, real streaks and zero tracking.` |
| Full description | 4000 | The shared description above, plus the Google Play note |

**Graphics**
- **App icon:** 512×512 PNG. Export it from the 1024×1024 icon at `ios/DrinkYourWater/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png`.
- **Feature graphic:** `docs/store/feature-graphic.png` (1024×500).
- **Phone screenshots:** 2 to 8, from the Android app: Home, check-in, Progress, Settings.

**Store settings**
- Category: **Health & Fitness**. Tags: Habit tracker, Reminders, Water tracker.
- Contact email: support@jarrodqsimpson.com. Website: https://jarrodqsimpson.com.

**App content** (Policy > App content)
- Privacy policy: https://jarrodqsimpson.com/drink-your-water/privacy
- Ads: **No**.
- App access: **All functionality is available without special access**.
- Content rating: answer **No** to every content question. Expect **Everyone** or **PEGI 3**.
- Target audience: **13+** (13–15, 16–17, 18+). Not designed for children.
- Data safety: **No** data collected or shared. See `docs/legal/README.md`.
- Health apps: choose the fitness/wellness (habit tracking) option. It's not a medical device.
- Government, financial, news: **No**.

---

## App Store Connect

**App Information**

| Field | Limit | Text |
|---|---|---|
| Name | 30 | `Drink Your Water: Check-ins` |
| Subtitle | 30 | `Water reminders that wait` |
| Primary category | | Health & Fitness |
| Secondary category | | Productivity |
| Privacy Policy URL | | https://jarrodqsimpson.com/drink-your-water/privacy |

**1.0 Prepare for Submission**

| Field | Limit | Text |
|---|---|---|
| Promotional text | 170 | `Full-screen check-ins that wait for a yes. Confirm or snooze, build real streaks, and keep every bit of your data on your iPhone.` |
| Description | 4000 | The shared description above, plus the App Store note |
| Keywords | 100 | `hydrate,hydration,reminder,habit,tracker,streak,routine,check in,daily,glass,wellness,self care,goal` |
| Support URL | | https://jarrodqsimpson.com |
| Marketing URL | | *(optional)* |
| Copyright | | `2026 Jarrod Simpson` |

Keywords leave out "drink" and "water": Apple already indexes the words in the app name.

**Age rating:** answer **None** or **No** throughout. Expect **4+**.

**App Review Information**
- Sign-in required: **No**.
- Contact: Jarrod Simpson, support@jarrodqsimpson.com, plus a phone number.
- Notes:

```
No account or sign-in is needed. All data stays on the device; the app makes no network requests.

To see the main feature quickly: finish onboarding, allow notifications, then on the Home screen tap "Test Check-in" on any reminder. That opens the full-screen check-in, where you can confirm or snooze (up to two 10-minute snoozes).

Reminders use Time Sensitive notifications because each one asks the user to confirm a habit at a specific time they chose; users can turn this off in Settings. If a check-in isn't answered, the app re-alerts every 10 minutes up to three times, then stops.

The app is a habit tool and makes no medical claims.
```
