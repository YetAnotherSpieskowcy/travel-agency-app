/*
Materialized read-only views (stored as collections) for the tour offers service.
These are created in order to cache read models / projections of the event stream.
It can be especially useful when creating views listing all matching entities.

Writes can only happen through events (event-sourcing)
and the view is updated by the service listening to those events.

Useful sources:
- https://microservices.io/patterns/data/cqrs.html
- https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs
- https://danielwhittaker.me/2014/10/05/build-master-details-view-using-cqrs-event-sourcing/
*/

// db.createCollection("tour_offer_view_<view_name>", {...})
