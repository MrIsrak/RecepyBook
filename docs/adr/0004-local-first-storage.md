# 0004. Local-first storage with Room and JSON backups

- Status: Accepted
- Date: 2026-10-15

## Context

User data must survive without a server, be portable between phones, and remain readable across app versions.

## Decision

- Recipes and related data are stored in Room. Nested values (ingredients, steps, tags, nutrition) are JSON columns, because they are always read together with their recipe.
- Remote images are copied into app storage on save, because CDN URLs expire.
- Backups are a single versioned JSON file that embeds images as base64.
- Persisted values that appear in the UI are stored as language-neutral keys (see ADR-0005).

## Consequences

- Every schema change requires a Room migration and a test; destructive migrations are forbidden.
- Backups can be large when there are many photos.
- Querying inside ingredients requires loading recipes into memory, which is acceptable at personal-collection scale.
