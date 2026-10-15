<div align="center">

# RecepyBook

**A free, ad-free Android recipe manager. Save recipes from Instagram, TikTok and YouTube in one tap, plan your week and build your shopping list.**

[![CI](https://github.com/MrIsrak/RecepyBook/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/MrIsrak/RecepyBook/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/MrIsrak/RecepyBook)](https://github.com/MrIsrak/RecepyBook/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Languages](https://img.shields.io/badge/UI-English%20·%20Русский%20·%20עברית-555)

[**Download APK**](https://github.com/MrIsrak/RecepyBook/releases/latest) · [Features](#features) · [Installation](#installation) · [Documentation](#documentation) · [Contributing](CONTRIBUTING.md)

</div>

---

## Overview

Most home recipes now live on social media: a short video, with the ingredients buried in a caption between emojis and hashtags. Subscription apps such as ReciMe solve this for a monthly fee. **RecepyBook does the same for free**, with no ads and no account. All data stays on the device.

## Features

| Area | Capability |
|---|---|
| **Link import** | Instagram, TikTok, YouTube (incl. Shorts), Facebook, Pinterest, and any website with schema.org/Recipe markup. Share a post to RecepyBook and it extracts the caption, parses ingredients and steps, and saves the cover image. |
| **Review before saving** | A preview shows what was recognized (ingredient and step counts, time, calories). Every field is editable. |
| **Photo import** | Screenshots and cookbook pages, using on-device text recognition. |
| **Nutrition** | Calories and macros per serving from a built-in table of ~130 foods named in English, Russian and Hebrew. |
| **Scaling & units** | Scale to any number of servings; show amounts as written, in metric, or in US units. |
| **Cookbooks** | Group recipes into collections with emoji and photo covers. |
| **Meal planner** | Weekly breakfast / lunch / dinner / snack planning with daily calorie totals; add a whole week to the shopping list in one tap. |
| **Shopping list** | Grouped by store aisle or by recipe; duplicates are merged; share to WhatsApp or Telegram. |
| **Cook mode** | Large type, one step per page, screen kept awake, one-tap timers detected from the step text. |
| **Search & sort** | By title, ingredient, tag or cuisine; newest, A–Z, quickest, lowest calorie, recently cooked. |
| **Languages** | English, Russian and Hebrew (full right-to-left layout), switchable in Settings. |
| **Backup** | Export everything, including photos, to a single file and restore it on another phone. |

### Optional AI parsing

The built-in parser handles captions that contain an ingredient list. With a **free** Google Gemini API key (*Settings → Smart parsing*), the app can also:
- parse captions with no explicit list;
- read photos in any language;
- translate recipes into the app language;
- give a closer calorie estimate.

The key is stored only on the device.

## Installation

1. Open the [latest release](https://github.com/MrIsrak/RecepyBook/releases/latest) on your phone.
2. Download `RecepyBook-x.y.z.apk`.
3. Open the file and allow installation from this source if prompted.

**Requirements:** Android 8.0 (API 26) or later. Each release ships a `SHA256SUMS.txt` for verification.

> **Note:** until a permanent release signing key is configured, each version must be installed after uninstalling the previous one. Export a backup first (*Settings → Save backup*). See [docs/SIGNING.md](docs/SIGNING.md).

## Building from source

Requires JDK 17 and the Android SDK (Android Studio Ladybug or later recommended).

```bash
git clone https://github.com/MrIsrak/RecepyBook.git
cd RecepyBook
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the full developer guide.

## Documentation

| Document | Contents |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | Modules, data flow, persistence model |
| [Development guide](docs/DEVELOPMENT.md) | Setup, build, testing, code style |
| [Recipe import](docs/IMPORT.md) | How each platform is handled; troubleshooting |
| [Localization](docs/LOCALIZATION.md) | Adding strings and languages; RTL rules |
| [Design system](docs/DESIGN.md) | Colors, typography, components |
| [Releasing](docs/RELEASING.md) | Versioning, release process, hotfixes |
| [Release signing](docs/SIGNING.md) | Setting up a permanent signing key |
| [Architecture decisions](docs/adr/README.md) | ADR log |
| [Third-party notices](docs/THIRD_PARTY_NOTICES.md) | Open-source components and licenses |

## Tech stack

Kotlin 2.0 · Jetpack Compose · Material 3 · Room · DataStore · OkHttp · Jsoup · Coil · ML Kit · kotlinx.serialization · AppCompat per-app locales

## Project status

Active development. See [CHANGELOG.md](CHANGELOG.md) for release history and the [issue tracker](https://github.com/MrIsrak/RecepyBook/issues) for planned work.

## Known limitations

- Some Instagram and Facebook posts are not readable without signing in. In that case the app falls back to the link preview, which usually contains the full caption.
- When a recipe is only spoken in a video and is absent from the caption, ingredients must be entered manually.
- Calorie values are estimates derived from a food table, not laboratory data.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md) first. Security issues: see [SECURITY.md](SECURITY.md).

## Support

See [SUPPORT.md](SUPPORT.md).

## Privacy

No accounts, analytics or ads. See [PRIVACY.md](PRIVACY.md).

## License

Released under the [MIT License](LICENSE). © 2026 MrIsrak.
The Rubik typeface is licensed under the [SIL Open Font License 1.1](docs/licenses/Rubik-OFL.txt).
