# Release Process

## Versioning

RecepyBook follows [Semantic Versioning](https://semver.org/):

| Change | Bump | Example |
|---|---|---|
| Incompatible data or behavior change | MAJOR | 1.4.2 → 2.0.0 |
| New backward-compatible functionality | MINOR | 1.4.2 → 1.5.0 |
| Backward-compatible bug fixes | PATCH | 1.4.2 → 1.4.3 |

The Android `versionCode` is derived as `MAJOR × 10000 + MINOR × 100 + PATCH` (for example, 1.1.0 → 10100). It is injected by CI, so there is no need to edit `build.gradle.kts`.

## Regular release

1. **Freeze.** Make sure `develop` is green in CI and every change is listed under `[Unreleased]` in `CHANGELOG.md`.
2. **Prepare.** On `develop`, rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`, add an empty `[Unreleased]` section above it, update the comparison links at the bottom, and update the supported-versions table in `SECURITY.md` for minor or major releases. Commit as `chore(release): X.Y.Z`.
3. **Release PR.** Open a PR `develop → main` titled `Release X.Y.Z`. After CI passes and the PR is approved, merge it with a merge commit.
4. **Publish.** Start the release from `main` in either of these ways:
   ```bash
   git checkout main && git pull
   git tag -a vX.Y.Z -m "RecepyBook X.Y.Z"
   git push origin vX.Y.Z
   ```
   or run the **Release** workflow manually from the Actions tab with `version = X.Y.Z`.
5. **Verify.** The workflow runs the tests, builds and signs the APK, extracts the release notes from the changelog, and publishes a GitHub Release with `RecepyBook-X.Y.Z.apk` and `SHA256SUMS.txt`. Install it on a device and smoke-test the import, the language switch and backup.

## Hotfix

1. Branch `hotfix/<name>` from `main`.
2. Fix the problem, add a regression test and a changelog entry under a new PATCH version.
3. Open a PR into `main`; after merging, tag `vX.Y.(Z+1)`.
4. Merge `main` back into `develop` (or cherry-pick the fix) so it is not lost.

## Release checklist

- [ ] CI green on the release commit
- [ ] Changelog section is complete and dated
- [ ] Smoke test: import from TikTok, Instagram and a website; photo import; language switch (EN/RU/HE); backup and restore
- [ ] Release signed with the permanent key (see [SIGNING.md](SIGNING.md))
- [ ] Release notes reviewed on GitHub

## Artifacts

| File | Description |
|---|---|
| `RecepyBook-X.Y.Z.apk` | Signed release APK |
| `SHA256SUMS.txt` | Checksum for verification: `sha256sum -c SHA256SUMS.txt` |
