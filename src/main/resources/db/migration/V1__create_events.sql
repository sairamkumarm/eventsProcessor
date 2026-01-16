CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_time TIMESTAMPTZ NOT NULL,
    received_time TIMESTAMPTZ NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    factory_id VARCHAR(64) NOT NULL,
    line_id VARCHAR(64) NOT NULL,
    duration_ms BIGINT NOT NULL,
    defect_count INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

