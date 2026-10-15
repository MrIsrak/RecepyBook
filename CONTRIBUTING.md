# Contributing to RecepyBook

Thank you for your interest in improving RecepyBook. This guide describes how we work, so that every change is reviewed, tested and releasable.

By participating, you agree to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Table of contents

- [Ways to contribute](#ways-to-contribute)
- [Branching model](#branching-model)
- [Commit messages](#commit-messages)
- [Pull request process](#pull-request-process)
- [Definition of done](#definition-of-done)
- [Code style](#code-style)
- [Localization](#localization)
- [Reporting bugs and requesting features](#reporting-bugs-and-requesting-features)

## Ways to contribute

- Report import failures with the exact link that failed. This is the most valuable feedback we receive.
- Improve the ingredient parser or the food table in `core/`.
- Add or improve translations (see [docs/LOCALIZATION.md](docs/LOCALIZATION.md)).
- Fix bugs and implement issues labelled `good first issue` or `help wanted`.

For larger changes, open an issue first so the approach can be agreed before work starts.

## Branching model

| Branch | Purpose | Merges into |
|---|---|---|
| `main` | Released, stable code. Every release is a tag `vX.Y.Z` on `main`. | — |
| `develop` | Integration branch for the next release. | `main` (via release PR) |
| `feature/<short-name>` | New functionality, branched from `develop`. | `develop` |
| `fix/<short-name>` | Non-urgent bug fixes, branched from `develop`. | `develop` |
| `docs/<short-name>` | Documentation-only changes, branched from `develop`. | `develop` |
| `hotfix/<short-name>` | Urgent fixes to a release, branched from `main`. | `main` and `develop` |

Direct pushes to `main` are not allowed; all changes arrive through pull requests.

## Commit messages

We follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). Write messages in English, in the imperative mood.

```
<type>(<optional scope>): <summary in imperative mood, ≤ 72 chars>

<optional body: what and why, wrapped at 72 chars>

<optional footer: Closes #123, BREAKING CHANGE: ...>
```

| Type | Use for |
|---|---|
| `feat` | A user-visible feature |
| `fix` | A bug fix |
| `perf` | A performance improvement |
| `refactor` | A code change that neither fixes a bug nor adds a feature |
| `test` | Adding or correcting tests |
| `docs` | Documentation only |
| `build` | Build system or dependencies |
| `ci` | CI configuration |
| `chore` | Maintenance that does not fit the above |

Common scopes: `core`, `import`, `data`, `ui`, `i18n`, `release`.

Examples:

```
feat(import): support Threads posts
fix(core): parse "1 1/2 cups" as 1.5 instead of 1
docs: describe the release signing setup
```

## Pull request process

1. Branch from `develop` using the naming scheme above.
2. Keep the PR focused on one logical change.
3. Run the checks locally:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug lintDebug
   ```
4. Add an entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for any user-visible change.
5. Open the PR against `develop` and complete the template.
6. CI must be green. A maintainer reviews the change; address feedback with additional commits rather than force-pushes during review.
7. PRs into `develop` are merged with a merge commit; release PRs into `main` are merged with a merge commit titled `Release X.Y.Z (#N)`.

## Definition of done

A change is complete when:

- [ ] It builds and all unit tests pass in CI.
- [ ] Parser and conversion logic in `core/` is covered by unit tests.
- [ ] UI changes are checked in light and dark themes and in Hebrew (RTL).
- [ ] Every new user-facing string exists in `values`, `values-ru` and `values-iw`.
- [ ] Documentation and the changelog are updated.
- [ ] No secrets, API keys, keystores or personal data are committed.

## Code style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) (`kotlin.code.style=official`); formatting rules are in [.editorconfig](.editorconfig).
- Keep `core/` free of Android dependencies so it stays unit-testable on the JVM.
- Never hard-code user-facing text in Kotlin; use string resources.
- Document public classes and non-obvious functions with KDoc in English.
- Prefer small, composable functions; avoid speculative abstractions.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the full development guide.

## Localization

User-facing strings live in `app/src/main/res/values*/strings.xml`. English is the default locale. See [docs/LOCALIZATION.md](docs/LOCALIZATION.md).

## Reporting bugs and requesting features

Use the [issue templates](https://github.com/MrIsrak/RecepyBook/issues/new/choose). For import problems, always include the link that failed. Report security vulnerabilities privately as described in [SECURITY.md](SECURITY.md).
