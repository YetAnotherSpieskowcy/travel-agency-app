/*
Snapshot collection.
*/

db = db.getSiblingDB("rsww_184529");

const SNAPSHOT_VALIDATOR = {
  $jsonSchema: {
    bsonType: "object",
    required: ["entity_id", "last_event_id", "data"],
    properties: {
      // Entity ID
      entity_id: {
        bsonType: "string",
      },
      // Entity type, e.g. Bus, Hotel
      entity_type: {
        bsonType: "string",
      },
      // ID of the latest event for the entity when snapshot was made
      last_event_id: {
        bsonType: "number",
      },
      // Data of the aggregate snapshot
      data: {
        bsonType: "object",
      },
    },
  },
};

db.createCollection("snapshots", { validator: SNAPSHOT_VALIDATOR });
