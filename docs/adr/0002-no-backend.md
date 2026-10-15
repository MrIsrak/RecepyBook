# 0002. No backend

- Status: Accepted
- Date: 2026-10-15

## Context

Competing apps rely on a server to fetch social media posts and run AI parsing, and fund it through subscriptions. RecepyBook's goal is to be free permanently, private, and maintainable by volunteers with no operating budget.

## Decision

The app has no server component. Link import runs on the device against public endpoints (oEmbed, embed pages, link previews, schema.org markup). AI features call the provider directly with a key the user owns.

## Consequences

- No hosting costs, no accounts, and no personal data held by the project.
- Import is subject to per-device rate limits and platform changes; strategies need fallbacks (see `docs/IMPORT.md`).
- There is no cross-device sync; backups are files the user manages.
