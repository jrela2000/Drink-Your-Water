# Subscriptions (RevenueCat + Google Play Billing)

Water reminders are free. Custom habit frameworks (medications, movement, study, prayer, ...)
need a subscription. The code is in `app/.../hydra/billing` and `ui/screens/PaywallScreen.kt`.

## How it fits together

```
App  --(RevenueCat SDK)-->  RevenueCat  <--(receipts, webhooks)-->  Google Play Billing
```

- You create the **subscription products** in Play Console (the store owns the money).
- RevenueCat connects to Play with a service account, checks purchases, and tells the app which
  **entitlements** are active. The app never validates receipts itself.
- The app asks "which entitlement is active?" and unlocks that tier. Tier limits:
  Starter 2, Builder 4, Full Stack 6 custom habits.

## Names the code expects

| Thing | Value |
|---|---|
| Entitlement ids | `starter`, `builder`, `fullstack` |
| Offering | the **current** offering |
| Package identifiers | `<tier>_monthly`, `<tier>_yearly` (e.g. `builder_yearly`) |
| Prices (iOS design) | Starter $2.99 / $23.99, Builder $5.99 / $47.99, Full Stack $8.99 / $71.99 |

## Turning billing on

1. Play Console: set up the merchant account, then create three subscriptions
   (`starter`, `builder`, `fullstack`), each with a monthly and a yearly base plan.
2. RevenueCat: add the Android app (package `com.aistudio.drinkyourwater.hydra`), connect Play,
   import the products, create the three entitlements, attach products, and build the offering
   with the six packages above.
3. Copy the app's **public SDK key** (`goog_...`) into the GitHub secret
   `REVENUECAT_ANDROID_KEY`, or pass `-PrevenuecatAndroidKey=...` locally.
4. With a blank key the app behaves as before: the builder is locked and shows "coming soon".

Before shipping a build with billing on: update the Play **Data safety** form (RevenueCat
receives purchase history and an anonymous app user id), the privacy policy and terms, and add
a Play license tester to try a purchase.
