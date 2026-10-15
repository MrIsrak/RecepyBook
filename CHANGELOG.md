# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- All project documentation, code comments, issue and pull request templates, and CI step names are now in English.

### Added
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

[Unreleased]: https://github.com/MrIsrak/RecepyBook/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/MrIsrak/RecepyBook/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/MrIsrak/RecepyBook/releases/tag/v1.0.0
