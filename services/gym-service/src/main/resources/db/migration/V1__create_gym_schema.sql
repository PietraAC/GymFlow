CREATE TABLE gyms (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE gym_admin_memberships (
    id UUID PRIMARY KEY,
    gym_id UUID NOT NULL REFERENCES gyms(id),
    identity_subject VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_gym_admin_membership UNIQUE (gym_id, identity_subject)
);

CREATE TABLE gym_units (
    id UUID PRIMARY KEY,
    gym_id UUID NOT NULL REFERENCES gyms(id),
    name VARCHAR(120) NOT NULL,
    city VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE equipment_types (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE unit_equipment (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES gym_units(id),
    equipment_type_id UUID NOT NULL REFERENCES equipment_types(id),
    total_quantity INTEGER NOT NULL CHECK (total_quantity >= 0),
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0 AND available_quantity <= total_quantity),
    notes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_unit_equipment UNIQUE (unit_id, equipment_type_id)
);

CREATE TABLE exercises (
    id UUID PRIMARY KEY,
    name VARCHAR(140) NOT NULL UNIQUE,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('STRENGTH', 'WARMUP', 'STRETCHING')),
    movement_pattern VARCHAR(80) NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    instructions VARCHAR(1500) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE exercise_primary_muscle_groups (
    exercise_id UUID NOT NULL REFERENCES exercises(id),
    muscle_group VARCHAR(60) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group)
);

CREATE TABLE exercise_secondary_muscle_groups (
    exercise_id UUID NOT NULL REFERENCES exercises(id),
    muscle_group VARCHAR(60) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group)
);

CREATE TABLE exercise_requirement_options (
    id UUID PRIMARY KEY,
    exercise_id UUID NOT NULL REFERENCES exercises(id),
    position INTEGER NOT NULL CHECK (position >= 0),
    CONSTRAINT uk_exercise_requirement_position UNIQUE (exercise_id, position)
);

CREATE TABLE exercise_requirement_equipment (
    requirement_option_id UUID NOT NULL REFERENCES exercise_requirement_options(id),
    equipment_type_id UUID NOT NULL REFERENCES equipment_types(id),
    PRIMARY KEY (requirement_option_id, equipment_type_id)
);

CREATE INDEX idx_membership_subject ON gym_admin_memberships(identity_subject);
CREATE INDEX idx_unit_gym ON gym_units(gym_id);
CREATE INDEX idx_inventory_unit ON unit_equipment(unit_id);
CREATE INDEX idx_requirement_exercise ON exercise_requirement_options(exercise_id);

