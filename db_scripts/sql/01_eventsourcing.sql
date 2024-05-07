/*
Event store.

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


--- Create events table representing the event stream
CREATE TABLE IF NOT EXISTS events
(
    -- Single-column unique record ID
    id BIGSERIAL PRIMARY KEY,
    -- Entity ID
    entity_id UUID NOT NULL,
    -- Event name, e.g. BusCreated, HotelReservationCountChanged
    event_name VARCHAR NOT NULL,
    -- Event data blob - JSON object for *Created events, (JSON) integer for *Changed events
    data JSONB NOT NULL,

    change_time TIMESTAMP NOT NULL DEFAULT now(),
);
