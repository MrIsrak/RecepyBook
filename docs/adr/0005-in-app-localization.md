# 0005. In-app localization with language-neutral persisted data

- Status: Accepted
- Date: 2026-10-15

## Context

The app serves English, Russian and Hebrew speakers, often bilingual, who may want an app language that differs from the system language. Version 1.0 stored some UI labels (store aisles, nutrition notes) as Russian text in the database.

## Decision

- Use Android string resources (`values`, `values-ru`, `values-iw`) with English as the default.
- Use AppCompat per-app locales (`AppCompatDelegate.setApplicationLocales`) with `autoStoreLocales`, and declare `locales_config.xml` for Android 13+ system settings.
- Store keys or codes in the database and map them to strings at display time. Legacy Russian values are normalized when read.
- Format units, numbers and dates from the current locale. Support RTL through auto-mirrored icons and start/end layout.

## Consequences

- Switching language never changes stored data.
- `MainActivity` extends `AppCompatActivity`, which adds the AppCompat dependency.
- Every new string must be added in three files (enforced in review).
