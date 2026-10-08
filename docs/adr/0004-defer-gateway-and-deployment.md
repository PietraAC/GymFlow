# ADR 0004: Defer gateway and demonstrative deployment

- Status: Accepted
- Date: 2026-10-08

## Context

The current goal is a reproducible local portfolio system. A gateway or public deployment would add operational cost, secret management, TLS, production identity configuration, and another failure boundary.

## Decision

Do not add a gateway, service discovery, Kubernetes, or cloud deployment without a concrete hosting or routing requirement. Keep the Angular development proxy as a local adapter and document the boundary explicitly.

## Consequences

The repository stays focused on demonstrated domain problems. A future deployment must revisit management endpoint security, Keycloak production mode, TLS, CORS, secrets, persistence, and broker/database backups.
