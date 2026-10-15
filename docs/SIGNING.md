# Release Signing

Android allows an app to be updated in place only if the new version is signed with **the same key** as the installed one. Releases must therefore be signed with a permanent key.

## Current behavior

The `Release` workflow looks for signing secrets. If they are missing, it signs the APK with an ephemeral debug key and emits a warning. Such an APK installs normally, but **cannot be installed over a previous version**: users must uninstall first, which deletes their data unless they have exported a backup.

## One-time setup

1. **Generate a keystore** (requires a JDK):
   ```bash
   keytool -genkeypair -v \
     -keystore release.jks -alias recepybook \
     -keyalg RSA -keysize 4096 -validity 36500
   ```
2. **Encode it** for storage as a secret:
   ```bash
   base64 -w0 release.jks > release.jks.b64     # macOS: base64 -i release.jks -o release.jks.b64
   ```
3. **Add repository secrets** under *Settings → Secrets and variables → Actions → New repository secret*:

   | Secret | Value |
   |---|---|
   | `SIGNING_KEYSTORE_BASE64` | Contents of `release.jks.b64` |
   | `SIGNING_STORE_PASSWORD` | Keystore password |
   | `SIGNING_KEY_ALIAS` | `recepybook` |
   | `SIGNING_KEY_PASSWORD` | Key password |

4. **Back up** `release.jks` and both passwords in a password manager or another secure offline location. **If the key is lost, existing installations can never be updated.**
5. Delete the local `release.jks.b64`.

## Migration from debug-signed releases

The first release signed with the permanent key cannot replace a debug-signed installation. Announce this in the release notes: users should export a backup, uninstall, install the new version and restore. All later releases update in place.

## Rules

- Never commit `*.jks`, `*.keystore` or encoded keys; `.gitignore` excludes them.
- Rotate the secrets only if they are compromised. Changing the key breaks in-place updates again.
- Only maintainers have access to repository secrets.
