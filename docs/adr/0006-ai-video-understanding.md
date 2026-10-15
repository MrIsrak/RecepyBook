# 0006. Extract recipes from videos with Gemini

- Status: Accepted
- Date: 2026-10-15

## Context

User testing showed that many TikTok and Instagram recipes keep the ingredients and steps only in the video (voice-over or on-screen text). The caption holds a tagline, so caption parsing returns nothing. Competing apps solve this server-side; we have no server (ADR-0002).

## Decision

When caption-based parsing finds no recipe and the user has configured a Gemini key (ADR-0003), the app:

1. obtains the video: the YouTube URL is passed directly; for TikTok, Instagram and Facebook a direct file URL is extracted and downloaded (≤ 150 MB) to the cache;
2. uploads it through the Gemini Files API (resumable protocol) and waits for `ACTIVE`;
3. asks the model for the same structured JSON recipe as for text, adding the caption as context;
4. deletes the uploaded file and the local copy.

## Consequences

- Video-only recipes become importable on-device, at no cost to the project.
- Imports take longer (typically 15–60 s); the UI shows progress stages.
- Video URLs depend on undocumented page data and may break when platforms change; the feature is best-effort and always falls back to the caption result.
- Videos are processed under the user's own Gemini account; this is disclosed in PRIVACY.md.
