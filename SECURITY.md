# Security Policy

## Supported versions

Security fixes are provided for the latest minor release only.

| Version | Supported |
|---|---|
| 1.1.x | ✅ |
| < 1.1 | ❌ |

## Reporting a vulnerability

**Do not open a public issue for security problems.**

Report vulnerabilities privately through [GitHub Security Advisories](https://github.com/MrIsrak/RecepyBook/security/advisories/new). Please include:

- a description of the issue and its potential impact;
- steps to reproduce or a proof of concept;
- the affected app version and Android version.

### What to expect

| Stage | Target time |
|---|---|
| Acknowledgement of your report | within 7 days |
| Initial assessment and severity rating | within 14 days |
| Fix released for confirmed high-severity issues | within 30 days |

We will keep you informed throughout, and credit you in the release notes unless you prefer to remain anonymous. Please allow a reasonable time for a fix before any public disclosure.

## Security model

- **No backend.** The app has no server component and no user accounts.
- **Local data.** Recipes, images and settings are stored in the app's private storage. Backups are plain JSON files that the user saves to a location of their choice. Treat them as personal data.
- **Gemini API key.** Stored in the app's private DataStore and sent only to `generativelanguage.googleapis.com` over HTTPS.
- **Network.** Outbound HTTPS requests are made only in response to a user action (importing a link, or AI parsing). The app opens no listening sockets and collects no analytics.
- **Release signing.** The signing key is held only in GitHub Actions secrets and is never committed (see [docs/SIGNING.md](docs/SIGNING.md)).
- **Supply chain.** Dependencies are pinned to exact versions and updated through Dependabot pull requests that must pass CI.
