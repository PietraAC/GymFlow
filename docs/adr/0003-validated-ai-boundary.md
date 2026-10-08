# ADR 0003: Structured AI behind a zero-trust validation boundary

- Status: Accepted
- Date: 2026-10-08

## Context

Model output is probabilistic and cannot be trusted to choose valid identifiers, preserve ownership, or respect current inventory.

## Decision

The browser sends one explicit generation action. `assistant-service` builds server-side context and accepts schema-constrained output from a provider abstraction. `workout-service` fetches the owned suggestion and revalidates plan version, identifiers, shape, idempotency, and eligibility before applying it. Models cannot assign load in kilograms.

## Consequences

Demo and Gemini use the same domain contract. A provider failure never blocks manual editing, and no model response directly mutates a plan.
