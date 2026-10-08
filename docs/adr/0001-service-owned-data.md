# ADR 0001: Service-owned data and contracts

- Status: Accepted
- Date: 2026-10-08

## Context

GymFlow needs independently evolvable gym, workout, and assistant boundaries without pretending that one shared schema is three services.

## Decision

Each service owns its database, credentials, migrations, entities, and API contract. Cross-service references are UUID values validated through HTTP APIs. JPA entities and foreign keys never cross service databases.

## Consequences

Ownership and failure boundaries stay explicit. Cross-service reads cost a network call and require timeout and degraded-mode decisions.
