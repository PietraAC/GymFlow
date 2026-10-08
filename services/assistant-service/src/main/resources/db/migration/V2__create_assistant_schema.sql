CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(100) NOT NULL,
    plan_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_conversations_owner ON conversations(identity_subject, created_at DESC);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER','ASSISTANT')),
    text VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);

CREATE TABLE suggestions (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    identity_subject VARCHAR(100) NOT NULL,
    plan_id UUID NOT NULL,
    base_plan_version BIGINT NOT NULL,
    context_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE','EXPIRED')),
    source VARCHAR(20) NOT NULL CHECK (source IN ('DEMO','GEMINI')),
    explanation VARCHAR(2000) NOT NULL,
    observations JSONB NOT NULL,
    proposed_changes JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_suggestions_owner ON suggestions(identity_subject, created_at DESC);
CREATE INDEX idx_suggestions_expiry ON suggestions(expires_at);
