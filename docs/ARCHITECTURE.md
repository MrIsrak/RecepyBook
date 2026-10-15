# Architecture

This document describes how RecepyBook is structured and how data moves through it. Significant decisions and their rationale are recorded as [ADRs](adr/README.md).

## System context

RecepyBook is a single-module Android application with **no backend**. It talks to the outside world only when the user imports a recipe or opts into AI parsing.

```
                 ┌────────────────────────── Device ──────────────────────────┐
  Share sheet ──►│  RecepyBook                                                │
  (link/image)   │   ├─ UI (Compose) ◄──► Repository ◄──► Room / DataStore    │
                 │   └─ Importers ──► core parsers ──► RecipeDraft            │
                 └──────┬──────────────────────────────┬──────────────────────┘
                        │ HTTPS (on user action)       │ HTTPS (opt-in, user key)
                        ▼                              ▼
       Social networks, oEmbed, recipe websites   Google Gemini API
```

## Layers

```
ui ──► data ──► core
 │               ▲
 └──► importer ──┘
```

| Package | Responsibility | Android dependency |
|---|---|---|
| `app.receptnik.core` | Pure parsing and domain logic: ingredient parsing, caption parsing, units, nutrition table, language codes | **None** (JVM unit-testable) |
| `app.receptnik.importer` | Fetching and extracting recipes from links and photos; HTTP; Gemini client | Yes |
| `app.receptnik.data` | Persistence (Room, DataStore), image storage, backup/restore, repository | Yes |
| `app.receptnik.ui` | Jetpack Compose screens, navigation, theme, localization helpers | Yes |
| `app.receptnik` | `Application`, `MainActivity`, manual dependency container | Yes |

Rule: **`core` must never import Android or UI code.** This keeps the most complex logic fast to test (see [DEVELOPMENT.md](DEVELOPMENT.md#testing)).

## Key components

### core

- **`IngredientParser`** turns a line into `Ingredient(quantity, quantityMax, unit, name, note)`. It supports quantity-first ("200 g flour") and quantity-last ("Flour — 200 g") forms, Unicode and ASCII fractions, ranges, group headers ("For the sauce:"), and notes such as "to taste", in Russian, English and Hebrew.
- **`CaptionParser`** turns free text (social captions, video descriptions, OCR output) into a `RecipeDraft`. It detects section headers, including headers embedded mid-line, falls back to heuristics when there are none, and strips trailing hashtags and calls to action.
- **`Units` / `QtyFormat`** hold the unit registry with multilingual aliases, metric/US conversion, and localized display labels and number formatting.
- **`NutritionDb`** is a table of ~130 foods (word stems in three languages) with kcal and macros per 100 g, piece weight, density and store aisle. `calculate()` returns totals plus a coverage ratio.
- **`Lang`** normalizes language codes (`iw` → `he`).

### importer

- **`LinkImporter`** detects the platform and runs a platform strategy, with a generic link-preview fallback. It then parses the result with Gemini (if configured) or `CaptionParser`, and computes nutrition. See [IMPORT.md](IMPORT.md).
- **`PhotoImporter`** uses Gemini vision if a key is configured, otherwise on-device ML Kit OCR followed by `CaptionParser`.
- **`Http`** wraps OkHttp. It enforces a 4 MB response cap, expands short links via `Location` headers, retries on 429/5xx, and maps failures to `ImportException.Reason`.
- **`GeminiClient`** calls the REST `generateContent` endpoint and requests structured JSON output.

Importers never produce user-facing text. They return **codes** (`ImportWarning`, `ImportException.Reason`, `NutritionNote`) that the UI maps to localized strings.

### data

- **Room database** (`receptnik.db`, schema version 1):

  | Table | Notes |
  |---|---|
  | `recipes` | Ingredients, steps, tags and nutrition stored as JSON columns |
  | `cookbooks` | Name and emoji |
  | `recipe_cookbook` | Many-to-many link, cascade delete |
  | `meal_plan` | `epochDay`, `slot`, `recipeId`, `servings` |
  | `grocery` | Name, quantity, unit, aisle key, source recipe, checked flag |

- **DataStore** holds settings: Gemini key and model, translation toggle, default units and grocery grouping. The UI language is stored by AppCompat (`autoStoreLocales`).
- **`ImageStore`** copies remote images into `filesDir/images` on save, because social-media CDN URLs expire.
- **`Repository`** is the single entry point for the UI. It merges duplicate grocery items and handles backup export/import (JSON with base64 images).

### ui

- One `MainActivity` (an `AppCompatActivity`, for per-app locales) hosts a Navigation Compose graph: four tabs (Recipes, Plan, Shopping, Cookbooks) plus detail, cook mode, import, editor and settings screens.
- ViewModels are used where state must survive configuration changes (import and editor). Other screens collect `Flow`s from the repository directly.
- `Theme.kt` defines the design system (see [DESIGN.md](DESIGN.md)).

## Main flows

### Import via share sheet

1. Android delivers `ACTION_SEND` to `MainActivity`, which stores it as `Shared.Text` or `Shared.Image`.
2. `AppNav` navigates to `import?text=…` or `import?image=…`.
3. `ImportViewModel` calls `LinkImporter.import()` or `PhotoImporter.import()`.
4. The resulting `RecipeDraft` fills the `EditorState`, and the user reviews and edits it.
5. On save, `Repository.saveDraft()` persists the image locally, recomputes nutrition and inserts the recipe.

### Shopping list

`Repository.addRecipeToGrocery()` scales quantities by servings, merges items with the same name and unit, and assigns an aisle key through `NutritionDb.aisleFor()`.

## Data compatibility

- Aisle values are stable keys (`veg`, `dairy`, …). Version 1.0 stored Russian labels; `Aisles.normalize()` maps them at read time.
- Nutrition notes are codes (`site`, `ai`, `calc:all`, `calc:m/n`); legacy free-text notes are displayed as is.
- Any future Room schema change **must** ship a migration (see [ADR-0004](adr/0004-local-first-storage.md)).

## Cross-cutting concerns

| Concern | Approach |
|---|---|
| Localization | All UI text in resources (`values`, `values-ru`, `values-iw`); see [LOCALIZATION.md](LOCALIZATION.md) |
| Errors | Typed reasons mapped to localized messages; no raw exceptions shown to users |
| Concurrency | Kotlin coroutines; I/O on `Dispatchers.IO`; Room and DataStore are Flow-based |
| Security & privacy | See [SECURITY.md](../SECURITY.md) and [PRIVACY.md](../PRIVACY.md) |
