CREATE TABLE suggestion_applications (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    suggestion_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES workout_plans(id),
    result_plan_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_suggestion_application_key UNIQUE(identity_subject, idempotency_key),
    CONSTRAINT uq_suggestion_application_suggestion UNIQUE(suggestion_id)
);
CREATE INDEX idx_suggestion_applications_plan ON suggestion_applications(plan_id, created_at DESC);
