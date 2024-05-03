/*
Event store for the flights service.
*/


CREATE TABLE IF NOT EXISTS flight_aggregates
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,

    type VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS flight_events
(
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES flight_aggregates (id),
    version BIGINT NOT NULL,
    data JSONB NOT NULL,

    change_time TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (aggregate_id, version)
);
