# Implementation plan

## Stage 1 — Foundation and inventory (complete)

- Maven monorepo with three independent Spring Boot applications.
- PostgreSQL database and user per service, Flyway, and a local Keycloak realm.
- `gym-service`: gyms, admin memberships, branches, inventory, catalog, and eligibility.
- JWT security with issuer, audience, and role validation; reproducible seeds; OpenAPI; and Problem Details.
- Secure skeleton and health check only for `workout-service` and `assistant-service`.

## Stage 2 — Manual workouts (complete)

- Angular 22 standalone application with reactive forms and PKCE.
- Student profile and transactional workout aggregate in `workout-service`.
- Eligibility validation through the `gym-service` REST API with timeouts and fail-closed mutations.
- Ownership based on JWT `sub`, optimistic concurrency, and admin/student screens.

## Stage 3 — Assistant (complete)

- Conversations, suggestions, fingerprints, expiration, and confirmed application.
- `TrainingAssistantProvider`, a deterministic demo adapter, and complete output validation.
- Optional Gemini integration through configuration only; no network access in tests; local rate limiting.
- The first direct assistant action saved the draft, requested schema-constrained JSON, revalidated it in `workout-service`, and applied it idempotently; Stage 6 expands that contract to complete the requested week.
- The browser never supplies model-created identifiers as trusted workout data; the model cannot set load in kilograms.

## Stage 4 — Events (complete)

- RabbitMQ only for `EquipmentAvailabilityChanged.v1`.
- Transactional outbox in `gym-service`, confirmed publication, and an idempotent consumer in `workout-service`.
- Bounded retries, dead-letter queue, aggregate-version ordering, and a reprocessing runbook.
- Potentially affected non-archived plans are marked for inventory revalidation.
- Synchronous REST eligibility validation remains in place for critical mutations.

## Stage 5 — Portfolio hardening (complete)

- GitHub Actions runs backend tests, frontend tests/build, and the authenticated Chromium journey.
- Playwright verifies profile setup, draft creation, and direct structured AI suggestion application.
- Local Prometheus metrics, domain counters/timers, correlation-aware logs, and message correlation.
- Architecture diagrams, ADRs, sanitized screenshots, and a reproducible demo walkthrough.
- Gateway and demonstrative deployment were evaluated and deliberately deferred because no concrete requirement exists.

## Stage 6 — AI-completed weekly workouts (complete)

- Each plan stores its own goal and target weekly frequency, initially suggested from the student profile.
- The direct assistant action now returns a typed `WORKOUT_COMPLETION` proposal with missing days and additions for existing days.
- Provider output may reference only eligible exercise IDs; it cannot define load, delete existing content, activate a plan, or persist changes directly.
- `workout-service` remains authoritative: it checks ownership, version, expiration, single use, final day count, exercise kinds, and current branch eligibility before applying the proposal.
- Empty and partial drafts are both supported. Existing days/items are preserved and the result remains a draft for human review.
- The editor presents the whole week as expandable day summaries with exercise names and detailed series, repetitions, duration, rest, and optional user-defined load.

## Next stage — Production deployment readiness (not started)

- Select a real hosting target and threat model before introducing a gateway, service discovery, or orchestration.
- Move secrets to a managed secret store; enable TLS, production-mode Keycloak, restricted management endpoints, environment-specific CORS, durable backups, and recovery procedures.
- Define consent, minimization, retention, and deletion rules for context sent to an external AI provider.
- Create a versioned evaluation set for workout-completion quality, safety, schema adherence, and eligible-catalog grounding.
- Add deployment manifests, dashboards, alerts, and SLOs only after the operational requirements are concrete. Kubernetes remains deferred rather than implied by the current local architecture.

## Decisions and assumptions

- The exercise catalog belongs to `gym-service`, keeping equipment requirements and approved content in the same bounded context.
- One local PostgreSQL instance hosts isolated databases without cross-database references; every application has its own credentials.
- External UUIDs are validated through APIs; JPA entities are never shared.
- Available quantity represents administrative inventory, not real-time occupancy.
- Active plans are immutable; future edits use a new draft to preserve validated history.
- A `gym-service` outage does not prevent reading a plan, but it blocks exercise changes and activation.
- The frontend uses Authorization Code + PKCE S256. Password grant exists only as a local manual-testing convenience.
