# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.1] - 2026-10-15

### Fixed
- TikTok import returned TikTok's generic home page preview ("TikTok | Make Your Day") instead of the video caption. Short-link expansion now stops at the video URL, generic previews are rejected, and two more caption sources are tried: the embed player page and the public tikwm.com API.

## [1.2.0] - 2026-10-15

### Added
- AI video understanding: when the caption has no recipe and a Gemini key is configured, the app downloads the video (TikTok, Instagram, Facebook) or passes the YouTube link to Gemini, and extracts ingredients and steps from speech and on-screen text.
- Import progress stages (fetching, reading, downloading video, watching video).

### Fixed
- AI failures are explained with actionable messages (network/DNS, invalid key, missing model, quota) instead of raw errors, and network failures are retried once.
- Recipe titles imported from link previews no longer start with "@author - ".

### Changed
- All project documentation, code comments, issue and pull request templates, and CI step names are now in English.

- Developer documentation: development guide, recipe import internals, localization guide, design system, release process, architecture decision records, third-party notices and a support policy.

## [1.1.0] - 2026-10-15

### Added
- User interface in English, Russian and Hebrew (full right-to-left layout), with a language picker in Settings.
- New minimalist design: Rubik typeface, warm white / graphite palette with a single coral accent, dark theme, photo-first layouts.
- New app icon.
- AI parsing translates recipes into the current app language.

### Fixed
- TikTok import failed with a JSON parsing error when TikTok rate-limited requests (HTTP 429). Short links are now expanded without downloading the page, requests are retried with backoff, and the caption is read from the video page when oEmbed is unavailable.
- Import errors are shown as clear, localized messages instead of raw exceptions.

## [1.0.0] - 2026-10-15

### Added
- Recipe import from Instagram, TikTok, YouTube, Facebook, Pinterest and websites with schema.org/Recipe markup.
- Receiving links and images through the Android share sheet.
- Preview and editing before a recipe is saved.
- Photo import with on-device text recognition (ML Kit).
- Ingredient parsing for Russian, English and Hebrew: quantities, fractions, ranges and units.
- Calorie and macro calculation from a built-in food table.
- Serving scaling and unit conversion (metric / US).
- Cookbooks.
- Weekly meal planner with daily calorie totals.
- Shopping list grouped by aisle or recipe, with duplicate merging.
- Step-by-step cook mode with timers.
- Search, filters and sorting.
- Backup and restore, including photos.
- Optional AI parsing via Google Gemini with a user-supplied key.

[Unreleased]: https://github.com/MrIsrak/RecepyBook/compare/v1.2.1...HEAD
[1.2.1]: https://github.com/MrIsrak/RecepyBook/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/MrIsrak/RecepyBook/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/MrIsrak/RecepyBook/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/MrIsrak/RecepyBook/releases/tag/v1.0.0
