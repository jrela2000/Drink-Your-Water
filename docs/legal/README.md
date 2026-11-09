# Legal pages: Drink Your Water

Drafts of the **Privacy Policy** and **Terms of Use** for the Android and iPhone apps.
They describe what the code does today. Update them whenever the apps start collecting or
sending anything.

> These are plain-language drafts, not legal advice. Have them reviewed if you add accounts,
> payments, analytics, ads, AI features or anything that sends data off the phone.

## Details used

| Field | Value |
|---|---|
| Publisher | Jarrod Simpson |
| Contact | support@jarrodqsimpson.com |
| Website | https://jarrodqsimpson.com |
| Privacy Policy URL | https://jarrodqsimpson.com/drink-your-water/privacy |
| Terms URL | https://jarrodqsimpson.com/drink-your-water/terms |
| Effective date | November 9, 2026 |
| Governing law | State of Illinois, USA |
| Store name | Drink Your Water: Check-ins |

If you publish the pages at different addresses, update both URLs in both documents. Update
the effective date whenever either document changes.

## Facts the drafts rely on (re-check if the code changes)

- **Network access (Android):** the manifest declares `INTERNET`, `ACCESS_NETWORK_STATE` and
  `com.android.vending.BILLING` for the RevenueCat subscription SDK. Network calls happen only
  when the RevenueCat key is set (billing on); reminders and history are never sent.
- **Third-party SDKs:** RevenueCat (subscriptions only). No analytics, ads, crash-reporting or
  tracking libraries.
- **iPhone:** a Base44-backed account (email/password or Google) stores reminders, frameworks,
  profile settings and check-in history on Base44. Settings > Delete Account removes those
  records and signs out; removing the login itself is by email request.
- **On-device storage:**
  - Android: a Room database plus one SharedPreferences flag for Strict Mode.
  - iPhone: one JSON file in Application Support.
- **Phone backups:**
  - Android: `allowBackup="true"`, so Android Backup can include app data.
  - iPhone: Application Support is included in iCloud and device backups.
- **Deletion:** in-app delete-all, or uninstall.
- **AI:** used to build the app (Anthropic's Claude, Google AI Studio). The app has no AI
  features.

## Store privacy answers that match these drafts

- **Apple App Store, App Privacy:** must now declare account data (email), user content
  and purchases/identifiers handled by Base44 and RevenueCat. "Data Not Collected" is no
  longer accurate for the iPhone app.
- **Google Play, Data safety (updated Nov 9, 2026):** collects Purchase history and Device or
  other IDs (RevenueCat), required, app functionality, not shared (service provider),
  encrypted in transit. Deletion question left blank.
- **Both stores:** add the Privacy Policy URL to the store listing, and link both pages from
  the app's project page on jarrodqsimpson.com.

## Not covered

- **The web demo** at drinkyourwater.base44.app is hosted by Base44, and these drafts don't
  cover it. If it stays public, check what Base44 collects, then extend the policy or give
  the demo its own notice.

## Paste-ready web pages

`web/` has HTML versions of both documents, built from the Markdown files by `web/build.py`
(re-run it after any edit: `python3 docs/legal/web/build.py docs/legal`).

- **`privacy-snippet.html` / `terms-snippet.html`:** paste into a "Custom HTML" or "Embed code"
  block in your site builder (WordPress, Squarespace, Wix and others). Styles are scoped so they
  won't affect the rest of your site.
- **`privacy.html` / `terms.html`:** complete pages, including dark mode, if you upload files to
  your host directly. For example, upload `privacy.html` as `drink-your-water/privacy/index.html`.
