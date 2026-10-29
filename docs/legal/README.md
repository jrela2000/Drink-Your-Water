# Legal pages: Drink Your Water

Drafts of the **Privacy Policy** and **Terms of Use** for the Android and iPhone apps.
They describe what the code does today. Update them whenever the apps start collecting or
sending anything.

> These are plain-language drafts, not legal advice. Have them reviewed if you add accounts,
> payments, analytics, ads, AI features or anything that sends data off the phone.

## Before publishing: fill in the placeholders

| Placeholder | What to put |
|---|---|
| `[STUDIO OR LEGAL NAME]` | Your name or business name, as it appears on your Play Console and App Store accounts |
| `[CONTACT EMAIL]` | A support address you'll actually check (stores require one) |
| `[WEBSITE URL]` | e.g. `https://jarrodqsimpson.com` |
| `[PRIVACY POLICY URL]`, `[TERMS URL]` | Where each page is published on jarrodqsimpson.com |
| `[EFFECTIVE DATE]` | The day you publish |
| `[STATE / COUNTRY]` | Where you or your business are based, for governing law |

## Facts the drafts rely on (re-check if the code changes)

- **No network access:** the Android manifest has no `INTERNET` permission, and neither app
  makes network requests.
- **No third-party SDKs:** there are no analytics, ads, crash-reporting or tracking libraries.
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

- **Apple App Store, App Privacy:** "Data Not Collected." No tracking.
- **Google Play, Data safety:**
  - Does the app collect or share any required user data types? **No.**
  - Is all user data encrypted in transit? Not applicable, because nothing is transmitted.
  - Can users request that data be deleted? Data stays on the device and is deleted in the
    app or by uninstalling.
- **Both stores:** add the Privacy Policy URL to the store listing, and link both pages from
  the app's project page on jarrodqsimpson.com.

## Not covered

- **The web demo** at drinkyourwater.base44.app is hosted by Base44, and these drafts don't
  cover it. If it stays public, check what Base44 collects, then extend the policy or give
  the demo its own notice.
