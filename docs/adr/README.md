# Architecture Decision Records

We record significant technical decisions as ADRs, following [Michael Nygard's format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-no-backend.md) | No backend; all processing on device or via user-owned keys | Accepted |
| [0003](0003-optional-ai-with-user-key.md) | Optional AI parsing with a user-supplied Gemini key | Accepted |
| [0004](0004-local-first-storage.md) | Local-first storage with Room and JSON backups | Accepted |
| [0005](0005-in-app-localization.md) | In-app localization with language-neutral persisted data | Accepted |
| [0006](0006-ai-video-understanding.md) | Extract recipes from videos with Gemini | Accepted |

## Process

1. Copy the template below to `NNNN-short-title.md`, using the next number.
2. Open a PR with the ADR in status *Proposed*.
3. Once merged, set the status to *Accepted*. To replace an ADR, write a new one and mark the old one *Superseded by NNNN*. Never edit an accepted decision in place.

```markdown
# NNNN. Title

- Status: Proposed | Accepted | Superseded by NNNN
- Date: YYYY-MM-DD

## Context
## Decision
## Consequences
```
