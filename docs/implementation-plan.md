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

## Stage 3 — Assistant

- Conversations, suggestions, fingerprints, expiration, and confirmed application.
- `TrainingAssistantProvider`, a deterministic demo adapter, and complete output validation.
- Optional Gemini integration through configuration only; no network access in tests; local rate limiting.
- Before/after review panel in Angular so a model never changes a workout directly.

## Stage 4 — Events

- RabbitMQ only for `EquipmentAvailabilityChanged.v1`.
- Transactional outbox in `gym-service`, confirmed publication, and an idempotent consumer in `workout-service`.
- Synchronous REST eligibility validation remains in place for critical mutations.

## Stage 5 — Portfolio hardening

- Continuous integration, browser tests, screenshots, architecture documentation, and a demo walkthrough.
- Evaluate a gateway and demonstrative deployment only when a concrete requirement exists.

## Decisions and assumptions

- The exercise catalog belongs to `gym-service`, keeping equipment requirements and approved content in the same bounded context.
- One local PostgreSQL instance hosts isolated databases without cross-database references; every application has its own credentials.
- External UUIDs are validated through APIs; JPA entities are never shared.
- Available quantity represents administrative inventory, not real-time occupancy.
- Active plans are immutable; future edits use a new draft to preserve validated history.
- A `gym-service` outage does not prevent reading a plan, but it blocks exercise changes and activation.
- The frontend uses Authorization Code + PKCE S256. Password grant exists only as a local manual-testing convenience.
