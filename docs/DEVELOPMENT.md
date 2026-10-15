# Development Guide

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | Platform 35, Build Tools 35 |
| Android Studio | Ladybug (2024.2) or later, recommended |
| Gradle | Provided by the wrapper (`./gradlew`) |

## Getting started

```bash
git clone https://github.com/MrIsrak/RecepyBook.git
cd RecepyBook
git checkout develop
./gradlew assembleDebug
```

Open the project in Android Studio and run the `app` configuration on a device or emulator (API 26+).

## Project layout

```
app/
├── src/main/java/app/receptnik/
│   ├── core/        Pure Kotlin: parsers, units, nutrition (no Android imports)
│   ├── importer/    Network import, OCR, Gemini
│   ├── data/        Room, DataStore, images, backup
│   └── ui/          Compose screens and theme
├── src/main/res/
│   ├── values/      English (default) strings
│   ├── values-ru/   Russian
│   ├── values-iw/   Hebrew
│   └── font/        Rubik variable font (OFL)
└── src/test/        JVM unit tests for core/
docs/                Project documentation and ADRs
.github/             CI, release workflow, templates, Dependabot
```

## Common tasks

| Task | Command |
|---|---|
| Unit tests | `./gradlew testDebugUnitTest` |
| Debug APK | `./gradlew assembleDebug` |
| Lint | `./gradlew lintDebug` |
| Release APK (local, debug-signed) | `./gradlew assembleRelease -PversionName=1.2.0 -PversionCode=10200` |
| Everything CI runs | `./gradlew testDebugUnitTest assembleDebug lintDebug` |

Reports are written to `app/build/reports/`.

## Testing

### Strategy

| Level | Scope | Location |
|---|---|---|
| Unit (JVM) | All logic in `core/`: parsing, units, nutrition, localization helpers | `app/src/test/` |
| Manual | Import against real links; UI in light/dark themes and RTL | Per-PR checklist |

Logic that can be expressed without Android belongs in `core/` and **must** be unit-tested. When fixing a parser bug, add a test with the exact input that failed.

### Writing a parser test

```kotlin
@Test fun quantityLast() {
    val i = IngredientParser.parse("Flour — 200 g")
    assertEquals(200.0, i.quantity)
    assertEquals("g", i.unit)
    assertEquals("Flour", i.name)
}
```

## Coding standards

- Kotlin [coding conventions](https://kotlinlang.org/docs/coding-conventions.html); `.editorconfig` defines formatting (4-space indent, 140-column limit).
- KDoc in English for public types and non-obvious functions. Explain *why*, not *what*.
- No user-facing strings in Kotlin: use `stringResource(R.string.…)` in UI code, and return codes from non-UI code.
- Use `pluralStringResource` for counts; never concatenate numbers with nouns.
- Use auto-mirrored icons (`Icons.AutoMirrored.*`) for anything directional, so RTL layouts render correctly.
- Do network and disk work on `Dispatchers.IO`; never block the main thread.
- Pin exact dependency versions in `app/build.gradle.kts`; upgrades arrive through Dependabot PRs.

## Continuous integration

`.github/workflows/ci.yml` runs on pushes to `main` and `develop` and on every pull request:

1. Unit tests
2. Debug build
3. Lint
4. Upload of reports and the debug APK as workflow artifacts

If the build fails, Gradle and compiler errors are surfaced as annotations on the run summary.

## Debugging import

- Use **Add recipe → From a link** with the failing URL. Errors are mapped to readable messages (see [IMPORT.md](IMPORT.md#error-handling)).
- For parser issues, copy the caption text and reproduce it in a unit test against `CaptionParser.parse()`.
- Network behavior differs by region and IP: social networks rate-limit aggressively. Always retest after a minute before concluding that something is broken.

## Database changes

The Room schema is at version 1. Any change to an `@Entity`:

1. Increment `version` in `AppDatabase`.
2. Add a `Migration` and register it in `Room.databaseBuilder(...).addMigrations(...)`.
3. Add a migration test.
4. Never use `fallbackToDestructiveMigration()`: users' recipes exist only on their devices.
