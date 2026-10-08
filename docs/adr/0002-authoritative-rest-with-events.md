# ADR 0002: Authoritative REST validation with event-driven notification

- Status: Accepted
- Date: 2026-10-08

## Context

Inventory changes must flag workout plans, but eventual delivery cannot prove that equipment is still available at mutation time.

## Decision

`EquipmentAvailabilityChanged.v1` notifies `workout-service` and marks plans for revalidation. Saving, activation, and suggestion application continue to synchronously query `gym-service`. Publication uses a transactional outbox and consumption is idempotent and version-aware.

## Consequences

The user gets proactive warnings without weakening correctness. The system accepts at-least-once delivery and maintains retry and DLQ operations.
