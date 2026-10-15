# 0003. Optional AI parsing with a user-supplied key

- Status: Accepted
- Date: 2026-10-15

## Context

Many social media recipes have unstructured captions or keep the recipe only in the video. Rule-based parsing cannot handle these. Hosting an AI service would contradict ADR-0002.

## Decision

- The built-in rule-based parser is always available and is the default.
- If the user enters a Google Gemini API key (free tier available), the app uses Gemini for text, photo and video understanding, requesting structured JSON output.
- The key is stored only in the app's private DataStore and sent only to Google's API.

## Consequences

- The app is fully functional without AI, and better with it, at no cost to the project.
- Users must obtain a key; the Settings screen explains how.
- Output quality depends on an external model. The model name is configurable in case Google renames models.
