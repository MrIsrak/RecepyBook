# Localization

RecepyBook ships in **English** (default), **Russian** and **Hebrew**. Users can change the language in *Settings → Language*, independently of the system language.

## Resource layout

| Locale | Directory | Notes |
|---|---|---|
| English | `res/values/strings.xml` | Default and fallback; every key must exist here |
| Russian | `res/values-ru/strings.xml` | |
| Hebrew | `res/values-iw/strings.xml` | Android uses the legacy `iw` code for Hebrew |

Supported locales are declared in `res/xml/locales_config.xml` (used by the Android 13+ per-app language settings) and in the in-app picker in `SettingsScreen`.

## Rules

1. **No user-facing text in Kotlin.** UI code uses `stringResource()`, and non-UI code returns codes (enums, keys) that the UI maps to strings.
2. **Plurals** use `<plurals>` and `pluralStringResource()`. Russian needs `one`/`few`/`many`/`other`; Hebrew needs `one`/`two`/`other`.
3. **Placeholders** use positional arguments (`%1$d`, `%2$s`) whenever there is more than one, so translators can reorder them.
4. **Every key must exist in all three files.** Reviewers check this, and the PR template includes it.
5. **Units, numbers and dates** are formatted from the current locale: `Units.display(key, lang)`, `QtyFormat.format(q, lang)` (comma decimal separator in Russian), and `DateTimeFormatter` with `uiLocale()`.
6. **Store keys, not labels.** Persisted values (aisles, nutrition notes) are language-neutral keys, so data survives a language switch.

## Right-to-left (Hebrew)

- `android:supportsRtl="true"` is set; Compose mirrors layouts automatically.
- Use `Icons.AutoMirrored.*` for directional icons (back, forward, sort, lists).
- Use `start`/`end` paddings, never `left`/`right`.
- Check every UI change in Hebrew before merging.

## Recipe content

Recipe text is user content and is **not** translated by the UI. The parsers understand ingredient syntax and section headers in all three languages. When a Gemini key is set and *Translate recipes* is on, imported recipes are translated into the current app language.

## Adding a new language

1. Create `res/values-<code>/strings.xml` with every key from `values/strings.xml`, including all plural quantities for that language ([CLDR plural rules](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)).
2. Add `<locale android:name="<code>"/>` to `res/xml/locales_config.xml`.
3. Add the language to the picker in `SettingsScreen.kt`.
4. Add unit labels to `Units.labels` and a branch in `Lang.of()` and `Units.display()`.
5. Optionally, add parser keywords (section headers, units, food stems) to `CaptionParser`, `Units` and `NutritionDb`, with tests.
6. Add the language to `languageName()` in `ReceptnikApp.kt` for AI translation.
7. Make sure the Rubik font covers the script; if not, document the fallback font.
