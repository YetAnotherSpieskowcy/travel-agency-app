/*
Event store for the tour offers service.

Application's state is stored as a series of state-changing events.
Any changes are made by creating a new event from which the entity's (aggregate's) state
can be reconstructed. When the event is saved, it is delivered to all listeners that can,
for example, update a materialized view.

Useful sources:
- https://cqrs.wordpress.com/documents/building-event-storage/
- Patterns, Principles, and Practices of Domain-Driven Design
    by Scott Millett, Nick Tune
  Chapter 22: Event Sourcing

  https://learning.oreilly.com/library/view/patterns-principles-and/9781118714706/c22.xhtml
- https://microservices.io/patterns/data/event-sourcing.html
- https://learn.microsoft.com/en-us/azure/architecture/patterns/event-sourcing
- https://leanpub.com/esversioning/read
*/


--- Create aggregates table for tour offers service
CREATE TABLE IF NOT EXISTS tour_offer_aggregates
(
    --- Bare minimum:
    -- Aggregate ID
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Version number of the aggregate (incremented sequentially)
    version BIGINT NOT NULL DEFAULT 0,

    --- Additional metadata:
    -- Aggregate type
    type VARCHAR NOT NULL
);

--- Create events table representing the event stream for tour offers service
CREATE TABLE IF NOT EXISTS tour_offer_events
(
    --- Bare minimum:
    -- Single-column unique record ID
    id BIGSERIAL PRIMARY KEY,
    -- Aggregate ID
    aggregate_id UUID NOT NULL REFERENCES tour_offer_aggregates (id),
    -- Version number of the aggregate (unique and sequential per aggregate)
    version BIGINT NOT NULL,
    -- Event data blob
    data JSONB NOT NULL,

    --- Additional metadata:
    -- Time when the change was made (event was created)
    change_time TIMESTAMP NOT NULL DEFAULT now(),

    --- Enforce uniqueness
    UNIQUE (aggregate_id, version)
);
