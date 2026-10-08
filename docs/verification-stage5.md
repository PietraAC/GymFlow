# Stage 5 verification record

Verified locally on October 8, 2026.

## Automated checks

| Check | Result |
| --- | --- |
| Backend Maven verification | 42 tests passed: 13 gym, 14 workout, and 15 assistant |
| Frontend unit tests | 9 tests passed across 4 files |
| Frontend production build | Passed; 349.34 kB initial bundle |
| Authenticated browser journey | Passed in real Chromium |
| Dependency audit | 0 known frontend vulnerabilities |

The browser journey signs in through the local Keycloak realm, saves a student profile, creates a workout draft, and applies a schema-validated deterministic AI suggestion through the real services.

During this verification, the browser test exposed an integration defect that unit tests did not cover:

- requests to `/assistant-api` were missing the access token because the assistant prefix was absent from the HTTP interceptor;

The defect is fixed and covered by the unit/browser checks.

## Operations and diagnostics

- All services expose health, metrics, and Prometheus endpoints in the local profile.
- Assistant generation records request outcome, source, duration, and number of proposed changes.
- Inventory outbox publication and consumption record outcome counters.
- HTTP requests and RabbitMQ messages propagate a correlation ID into structured log context.
- The local startup script reports the frontend and all three service health checks as ready.

## CI boundary

The GitHub Actions workflow runs backend verification, frontend unit tests, the production build, and the authenticated Chromium journey. The workflow is committed as configuration but can only be observed running after the changes are pushed to GitHub; no remote-green result is claimed by this local record.

CI and automated tests use the deterministic offline provider. The optional Gemini adapter requires `AI_PROVIDER=gemini`, a valid `GEMINI_API_KEY`, and account quota. A real local Gemini journey was verified separately with an untracked credential; no provider credential or response payload is stored in the repository.

## Scope decision

An API gateway and demonstrative cloud deployment remain deferred. The current portfolio gains more value from a reproducible local environment, tested service boundaries, visible diagnostics, and explicit architecture decisions than from adding infrastructure without a concrete deployment requirement.
