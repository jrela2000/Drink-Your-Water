# The lock screen: how it works and why

This documents the reasoning behind the reminder lock overlay's "can't skip it" behavior,
implemented in commit `63a6b96` ("Make the lock overlay actually resist being skipped").
Keep this around for two reasons: so a future contributor doesn't accidentally "fix" the
missing exit button back in, and so the explanation below can be reused for the website
(e.g. a "How does the lock work?" / "What permissions does the app need?" FAQ entry,
the way [Pray Focus](https://prayfocus.app) — the app this feature is modeled on — documents
theirs).

## Website-ready copy

Use/adapt this directly for an FAQ page:

> **How does the lock screen work?**
> When a reminder fires, Drink Your Water shows a full-screen overlay that stays in front
> until you confirm the habit or use one of your two snoozes. The Home and Recent Apps
> buttons are disabled for the duration using Android's built-in **screen pinning**
> feature — the same mechanism Android itself offers for focus and kiosk-style apps.
>
> **What permissions does the app need?**
> Drink Your Water needs **Screen pinning** turned on in your phone's Settings (under
> Security, or search "pin" in Settings search — the exact menu varies by phone). We don't
> require any special or sensitive permission for this — screen pinning is a standard
> Android feature you control and can turn off at any time. We don't use Accessibility
> Services or "draw over other apps" permissions, and we don't block or monitor your use of
> other apps.
>
> **Can I still get out if I really need to (e.g. a phone call)?**
> Yes. Android's screen pinning always keeps one exit gesture available system-wide (hold
> Back + Recents together, or swipe-up-and-hold on gesture navigation) so you can never be
> fully locked out of your phone — that's an OS-level guarantee, not something any app can
> remove. In everyday use, though, there's no visible "X" or back-button shortcut out of the
> reminder screen; only confirming the habit or using a snooze will dismiss it normally.

## Why this exists

The product goal (per the founder) is to mimic [Pray Focus](https://prayfocus.app)'s "locks
your phone" accountability mechanic. Before this change, the lock overlay *looked* locked
but wasn't: it had an explicit "X" close button, the system Back button worked normally, and
tapping "Snooze" silently exited with nothing persisted or rescheduled. None of that actually
held anyone accountable to anything.

## Why not copy Pray Focus's exact mechanism

Pray Focus's own FAQ says: *"you will be asked to grant Screen Time permission (Family
Controls)"* — that's **Apple's iOS-only API** for blocking specific other apps (their example:
TikTok, Instagram, Facebook) until an action is completed. Two things follow from that:

1. **There is no Android equivalent.** No public, Play-policy-safe API lets a third-party
   Android app block *other specific apps* the way iOS's Family Controls does.
2. **It's a different mechanic anyway.** Pray Focus reactively blocks distracting apps when
   you try to open them. Drink Your Water proactively interrupts on a *schedule* (the
   reminder fires, then locks) — closer to an alarm clock than an app blocker.

## The Android options that were actually considered

| Approach | What it needs | Risk |
|---|---|---|
| **Screen pinning (`startLockTask()`/`stopLockTask()`)** — what's implemented | Nothing beyond a standard, user-toggleable OS setting (Settings → Security → Screen pinning). No manifest permission, no Play declaration. | Low. Standard public API, widely used by kiosk/focus apps, no special review scrutiny. |
| `USE_FULL_SCREEN_INTENT` (heads-up screen like an incoming call/alarm) | A manifest permission Play reviews specifically | On Android 14+, Play restricts this to calling/alarm-category apps. Real rejection risk for a hydration app. |
| `AccessibilityService` + "draw over other apps" overlay (how app-blockers like Forest/Opal work on Android) | Two permissions the user must manually grant, plus a Play declaration form justifying the Accessibility Service use | Materially higher risk — Accessibility Service misuse is one of the most common Play rejection reasons, and this is also a different feature (blocking *other* apps), which wasn't the ask. |

Screen pinning was chosen because it directly serves the actual mechanic (don't let the user
leave the reminder screen without acting) with no elevated permissions and no Play review
risk, while the other two either don't fit the mechanic or bring risk disproportionate to
what a hydration/habit reminder app needs.

## What changed in code

- `LockOverlayScreen` calls `Activity.startLockTask()` when it appears and
  `Activity.stopLockTask()` when it's dismissed (via `DisposableEffect`), pinning the app for
  the overlay's lifetime.
- The "X" exit button is gone. A `BackHandler` swallows the system Back button while the
  overlay is showing.
- Only two ways out remain: **Confirm** (logs completion, resets the snooze count) or
  **Snooze**, capped at 2 uses per reminder firing. Snooze now actually persists to the
  database and schedules a real re-prompt in 10 minutes — previously it did neither.

## Known limitation

`startLockTask()` silently does nothing if "Screen pinning" is off in the device's system
settings — there's no way for a third-party app to turn that system setting on for the user.
If it's off, the overlay still loses its exit button and ignores Back, but Home/Recents
remain unaffected. Worth prompting the user to enable it during onboarding in a future
iteration (there's no way to deep-link directly to the screen-pinning settings page; the best
available option is deep-linking to `Settings.ACTION_SECURITY_SETTINGS` and pointing the user
from there).
