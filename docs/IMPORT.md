# Recipe Import

This document explains how RecepyBook extracts recipes from links and photos, and how to troubleshoot failures.

## Pipeline

```
input text ──► first URL ──► platform strategy ──► Source(text, title, image, author[, structured])
                                   │ (on failure)
                                   └──► generic link preview (og: tags, crawler user agent)

Source ──► structured recipe?  ──yes──► use it
             │ no
             ├─► Gemini configured? ──yes──► GeminiClient.extract()
             └─► CaptionParser.parse()
                                   │
                                   ▼
                  RecipeDraft ──► nutrition (NutritionDb) ──► review screen
```

If the input contains no URL and is longer than 20 characters, it is parsed directly as recipe text.

## Platform strategies

| Platform | Strategy (in order) | Notes |
|---|---|---|
| **TikTok** | 1. Expand short links (`vt.`/`vm.tiktok.com`) via `Location` headers<br>2. oEmbed API (`/oembed?url=`), full caption<br>3. Video page: embedded JSON (`__UNIVERSAL_DATA_FOR_REHYDRATION__`, `SIGI_STATE`), desktop then crawler user agent<br>4. `og:` tags | oEmbed is rate-limited (HTTP 429) per IP; steps 3–4 cover this case |
| **Instagram** | 1. Embed page `/p/{code}/embed/captioned/` (caption, image)<br>2. `og:` tags via crawler user agent, keeping the longer caption | Private posts are not accessible |
| **YouTube** | oEmbed (title, thumbnail, channel) and `shortDescription` from the watch page | Supports `watch`, `youtu.be`, `shorts`, `embed`, `live` URLs |
| **Facebook** | `og:` tags via crawler user agent | Often limited without sign-in |
| **Pinterest** | Follow the pin's outbound link; if that site has schema.org markup, import from it, otherwise use the pin's `og:` tags | |
| **Websites** | schema.org `Recipe` JSON-LD (including `@graph` and `HowToSection`); otherwise article text from `article`/`main` | Site nutrition is per serving and is multiplied by servings |

All HTTP requests go through `Http`. Responses are capped at 4 MB, and 429/5xx responses are retried with linear backoff.

## Parsing

### Built-in parser (`CaptionParser`)

1. Converts keycap emoji (1️⃣) to numbers and moves mid-line headers ("…in 15 minutes! Ingredients: …") onto their own lines.
2. Detects section headers in Russian, English and Hebrew ("Ingredients", "Ингредиенты", "מצרכים", "Method", "Приготовление", "אופן הכנה", …).
3. Stops at calls to action and hashtags ("Save this", "Follow for", "#…").
4. Without headers, classifies lines heuristically: ingredient-like lines first, then numbered or long lines as steps.
5. Extracts servings and total time from the intro text.

### AI parser (`GeminiClient`)

Used only when the user has saved a Gemini API key. The model is asked for a strict JSON schema (title, servings, time, ingredients as `quantity unit product` strings, steps, nutrition per serving, cuisine, meal type, tags). If "Translate recipes" is on, output is requested in the current app language. If the call fails, the error is shown and the built-in parser result is used instead.

## Nutrition

`NutritionDb.calculate()` matches each ingredient to a food by its longest matching stem, converts the amount to grams (unit factor × density, or piece weight), and sums the values per 100 g. The local result replaces a site or AI estimate when at least 80% of quantified ingredients are matched. The source is stored as a code: `calc:all`, `calc:m/n`, `site` or `ai`.

## Error handling

Failures are mapped to `ImportException.Reason` and shown as localized messages:

| Reason | Typical cause | Message shown (EN) |
|---|---|---|
| `RATE_LIMITED` | HTTP 429 after retries | "The site is temporarily limiting requests…" (with *Try again*) |
| `NOT_FOUND` | 404 / 410 | "Post not found — it may be deleted or private." |
| `BLOCKED` | 401 / 403 | "…doesn't allow reading this post without signing in…" |
| `SITE_ERROR` | Other HTTP errors | "The site returned an error…" |
| `BAD_RESPONSE` | Unexpected format, unparsable JSON | "Couldn't read the recipe from this page…" |
| — | No connectivity / timeout | "No internet connection" / "The site took too long to respond" |

Non-fatal problems (no ingredients found, unreadable photo) are reported as `ImportWarning`s on the review screen.

## Troubleshooting

| Symptom | What to try |
|---|---|
| "Temporarily limiting requests" | Wait 1–2 minutes; switch between Wi-Fi and mobile data. |
| Title imported, but no ingredients | The recipe is probably only in the video. Add a Gemini key, or type the ingredients. |
| Instagram post fails | Check that the account is public. Otherwise copy the caption and paste it as text. |
| Wrong quantities | Report the exact caption line in an issue; it becomes a parser test case. |

## Adding a new platform

1. Add a value to `LinkImporter.Platform` and a host check in `platformOf()`.
2. Implement a `private suspend fun <platform>(url): Source`. Use `Http.getWithRetry` and throw `ImportException` with a specific reason.
3. Prefer official or public endpoints (oEmbed, embed pages, `og:` tags) over scraping private APIs.
4. Document the strategy in the table above and add the platform to the README.
