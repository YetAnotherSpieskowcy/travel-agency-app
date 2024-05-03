/*
Snapshot collection for flights service.
*/

const SNAPSHOT_VALIDATOR = {
    $jsonSchema: {
        bsonType: "object",
        required: ["aggregate_id", "version", "data"],
        properties: {
            // Aggregate ID
            aggregate_id: {
                bsonType: "string",
            },
            // Version number of the aggregate when snapshot was made
            version: {
                bsonType: "long",
            },
            // Data of the aggregate snapshot
            data: {
                bsonType: "object",
            },
        },
    },
}

db.createCollection("flight_snapshots", {validator: SNAPSHOT_VALIDATOR});
