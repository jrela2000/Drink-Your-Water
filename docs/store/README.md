# Store assets and releases

| File | What it is |
|---|---|
| `listing.md` | Listing text for Google Play and the App Store, with character counts that fit |
| `feature-graphic.png` | Google Play feature graphic, 1024×500, RGB with no transparency |
| `feature-graphic.html` | Source for the graphic. Open it at 1024×500 and screenshot it to rebuild |

## Signed Android release (no Android Studio)

The **Android Release** workflow builds the signed `.aab` that Play Console asks for. It signs
with your upload key, `my-upload-key.jks`, which you keep on your computer and in GitHub
secrets. Never commit it.

### One-time setup

1. **Base64-encode the keystore.** This turns the file into text you can paste into a secret.
   - **Windows (PowerShell)**, in the folder holding the file:
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\my-upload-key.jks")) | Set-Clipboard
     ```
   - **Mac:** `base64 -i my-upload-key.jks | pbcopy`

   Either command copies the text to your clipboard.
2. In GitHub, go to **Settings → Secrets and variables → Actions → New repository secret**
   and add:

   | Secret | Value |
   |---|---|
   | `ANDROID_KEYSTORE_BASE64` | The text from step 1 |
   | `ANDROID_KEYSTORE_PASSWORD` | The keystore password you chose when you created the key |
   | `ANDROID_KEY_PASSWORD` | The key password. It's often the same as the keystore password |
   | `ANDROID_KEY_ALIAS` | Optional. Only needed if your key isn't named `upload` |

   Don't paste any of these into a chat, an issue or a commit.

### Each release

1. Go to **Actions → Android Release → Run workflow**. Enter the version name (`1.0` for the
   first release) and run it.
2. When it finishes, the run's summary shows the version code and the key's SHA-256
   fingerprint. Download the **android-release-aab** artifact and unzip it to get the `.aab`.
3. In Play Console, open **Test and release → Testing → Closed testing**, create a release and
   upload the `.aab`.

The version code is 100 + the run number, so each build is higher than the last. If the run
fails, its error says which secret is wrong: for example a bad password, a wrong key name, or
a keystore that isn't valid base64.

**Check the key once.** The SHA-256 in the run summary should match **Play Console → Test and
release → App integrity → Upload key certificate**. If Play says the bundle is signed with the
wrong key, the keystore in the secret isn't the one registered with Play. If you've never
uploaded a bundle for this app, Play registers the key from your first upload, so there's
nothing to compare yet.
