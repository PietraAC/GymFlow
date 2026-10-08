# Security Policy

GymFlow is a portfolio and learning project. It is not a production deployment template.

## Supported versions

Security fixes are applied to the latest commit on the `main` branch.

## Reporting a vulnerability

Please do not open a public issue containing credentials, tokens, personal data, or detailed exploitation steps. Use GitHub's private vulnerability reporting feature when it is available for this repository, or contact the repository owner privately through their GitHub profile.

Include the affected component, reproduction conditions, expected impact, and a minimal proof of concept that does not expose third-party data.

## Repository safety

- Real secrets belong only in the ignored local `.env` file or an external secret manager.
- `.env.example` contains placeholders, not operational credentials.
- Credentials in the imported Keycloak realm are public local demo accounts and must never be reused.
- The local Keycloak server runs in development mode.
- AI provider keys must be supplied through environment variables and must never appear in source code, tests, fixtures, screenshots, or logs.
- Automated tests must use the deterministic demo AI adapter rather than an external provider.
- Real-provider use must send only the minimum context required for the feature and must not include secrets, access tokens, free-form medical data, or direct database records.
- Model output is untrusted input: schema validation, ownership/version checks, current catalog eligibility, expiration, and idempotency are enforced before a workout draft can change.
