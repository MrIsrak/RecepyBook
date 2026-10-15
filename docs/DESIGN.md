# Design System

RecepyBook uses a minimalist, photo-first visual language. The design system is implemented in `app/src/main/java/app/receptnik/ui/Theme.kt` and `Common.kt`.

## Principles

1. **Content first.** Food photography is the hero; chrome stays quiet.
2. **One accent.** Coral is reserved for emphasis: step numbers, calories, today's date, favorites. Everything else is neutral.
3. **Lines, not shadows.** Separation comes from hairlines and spacing, not elevation.
4. **Generous type and space.** Large titles, 20–24 dp side margins, relaxed line height.
5. **Same experience in every language**, including right-to-left.

## Color

| Token | Light | Dark | Usage |
|---|---|---|---|
| `background` / `surface` | `#F7F6F2` | `#0F0F0E` | Screens |
| `onSurface` | `#141414` | `#F2F1EC` | Primary text |
| `onSurfaceVariant` (`muted`) | `#6E6C66` | `#9C9A93` | Secondary text, meta lines |
| `primary` | `#141414` | `#F2F1EC` | Primary buttons, selected pills |
| `outlineVariant` (`hairline`) | `#E3E1DA` | `#2A2A28` | Dividers, outlines |
| `surfaceContainer` | `#F1F0EB` | `#1A1A19` | Text fields, notices |
| `Accent` (`tertiary`) | `#FF5B37` | `#FF5B37` | Single accent |
| `errorContainer` | `#FDE7E4` | `#3B1412` | Error notices |

Dynamic color (Material You) is intentionally **not** used, to keep the brand consistent.

## Typography

Typeface: **Rubik** (variable, SIL OFL 1.1). It covers Latin, Cyrillic and Hebrew, so all three UI languages share one voice.

| Style | Size / line height | Weight | Usage |
|---|---|---|---|
| `displaySmall` | 34 / 40 | SemiBold | Cook-mode step numbers, calorie hero |
| `headlineLarge` | 30 / 36 | SemiBold | Recipe title |
| `headlineMedium` | 26 / 32 | SemiBold | Tab screen titles |
| `headlineSmall` | 22 / 28 | SemiBold | Section titles |
| `titleMedium` | 16 / 22 | Medium | Card and list titles |
| `bodyLarge` | 16 / 24 | Regular | Ingredients, steps |
| `bodySmall` | 12 / 17 | Regular | Meta lines |
| `labelSmall` | 11 / 14 | Medium, +0.6 tracking | All-caps section labels |

## Shape

| Element | Radius |
|---|---|
| Recipe cards and images | 22–24 dp |
| Hero image (detail) | 32 dp bottom corners |
| Text fields | 18 dp, or fully rounded for search |
| Buttons, pills, FABs | Fully rounded |

## Components

| Component | Description |
|---|---|
| `AppScaffold` | Transparent top bar; large title on tab screens |
| `Pill` | Filter or choice chip: graphite fill when selected, hairline outline otherwise |
| `RecipeCard` | 0.82 aspect photo, title and muted meta line below |
| `RecipeRow` | 60 dp thumbnail, title and meta; for lists and dialogs |
| `SectionLabel` | Small all-caps muted label |
| `Notice` | Soft container for tips, warnings and errors, with an optional text action |
| `CheckDot` | Circular check used for ingredients and shopping items |
| `Stepper` | − value + control for servings |
| `GlassIconButton` | Translucent round button over photos |
| `EmptyState` | Accent dot, title and one line of guidance |

## Iconography

Material Symbols, outlined style. Directional icons use the auto-mirrored variants. The launcher icon is a white bowl with a coral dot on graphite, with a monochrome variant for themed icons.

## Accessibility

- Text contrast meets WCAG AA in both themes. Muted text is used only for secondary information.
- Touch targets are at least 44 dp.
- Icons that act as buttons have content descriptions from string resources.
- All text uses `sp` and respects the system font scale.
