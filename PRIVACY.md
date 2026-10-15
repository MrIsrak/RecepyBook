# Privacy Policy

_Last updated: 15 October 2026 (video understanding added)_

RecepyBook does not collect, store on any server, or share personal data with third parties.

## Data stored on your device

Recipes, photos, cookbooks, meal plans, shopping lists and settings (including your Gemini API key, if you add one). All of this is kept in the app's private storage and is deleted when the app is uninstalled.

## Network requests

The app connects to the internet only when you take an action:

| Action | Destination | Data sent |
|---|---|---|
| Importing a link | The link you shared, plus public oEmbed endpoints of TikTok and YouTube | The link URL |
| Importing a TikTok link when TikTok's own endpoints do not respond | The public tikwm.com API | The TikTok video link |
| Saving an imported recipe | The image host of that recipe | A request for the cover image |
| AI parsing (only with your own key) | Google Gemini API (`generativelanguage.googleapis.com`) | The post text or photo being parsed |
| AI video understanding (only with your own key, when the caption has no recipe) | The platform's video CDN, then the Gemini Files API | The video is downloaded to a temporary file, uploaded to your Gemini account, and deleted from both places after processing |

When AI parsing is used, the data is processed under the [Gemini API Terms of Service](https://ai.google.dev/gemini-api/terms).

## What we do not do

No accounts, advertising, analytics, tracking or crash reporting.

## Backups

A backup is a regular file that you choose to save and store yourself. It contains your recipes and photos; keep it somewhere you trust.

## Children

The app does not knowingly collect any data from anyone, including children.

## Changes

Changes to this policy are recorded in this file and in the [changelog](CHANGELOG.md).

## Contact

Questions about this policy: open an [issue](https://github.com/MrIsrak/RecepyBook/issues) or contact the maintainer, [@MrIsrak](https://github.com/MrIsrak).
