package pl.edu.pg.rsww.transports

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Updates.inc
import com.mongodb.kotlin.client.MongoClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

data class Entity(
    val entity_id: String,
    val entity_type: String,
    val last_event_id: Int,
    val data: Document,
)

object Events : Table("events") {
    val id = long("id").autoIncrement("public.seq").uniqueIndex()
    val entityId = uuid("entity_id")
    val eventName = varchar("event_name", 255)
    val data = varchar("data", 255)
}

public class TransportsServer {
    @Autowired lateinit var template: RabbitTemplate

    @Autowired lateinit var queueConfig: QueueConfig

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun processPaymentEventHandler(
        request: String,
        message: Message,
    ) {
        println(message.messageProperties.receivedRoutingKey)
        when (message.messageProperties.receivedRoutingKey) {
            queueConfig.transactionBookTransport -> {
                val payload = Json.decodeFromString<BookTransportMessage>(request)
                val result = updateReservationCounter(payload.routeId, 1)
                println(result)
                template.convertAndSend(
                    "trip_reservations",
                    "trip_reservations.events.transportBooked",
                    Json.encodeToString(
                        TransportBookedEvent(payload.triggeredBy, payload.userId, payload.routeId, result),
                    ),
                )
            }
            queueConfig.transactionCancelBookTransport -> {
                val payload = Json.decodeFromString<BookTransportMessage>(request)
                val result = updateReservationCounter(payload.routeId, -1)
                template.convertAndSend(
                    queueConfig.events,
                    queueConfig.eventTransportBookedEvent,
                    Json.encodeToString(
                        TransportBookedEvent(payload.triggeredBy, payload.userId, payload.routeId, result),
                    ),
                )
            }
        }
    }

    fun updateReservationCounter(
        routeId: String,
        value: Int,
    ): Long {
        val userName = System.getenv("MONGO_USERNAME")
        val password = System.getenv("MONGO_PASSWORD")
        val dbName = System.getenv("MONGO_DB")
        val host = System.getenv("MONGO_HOSTB")
        val port = System.getenv("MONGO_PORTB")

        val connectionString = "mongodb://$userName:$password@$host:$port/"
        val client = MongoClient.create(connectionString)
        val db = client.getDatabase(dbName)
        val collection = db.getCollection<Entity>("snapshots")

        val route = collection.find(eq("entity_id", routeId)).firstOrNull() ?: return 0
        println(routeId)
        var limit = route.data.getDouble("reservation_limit")
        if (limit > 0) {
            limit += (1 - value)
        }

        val filter = and(eq("entity_id", routeId), lt("data.reservation_count", limit))
        val update = inc("data.reservation_count", value)

        val result = collection.updateOne(filter, update).matchedCount
        val pdbName = System.getenv("POSTGRES_DB")
        val phost = System.getenv("POSTGRES_HOSTB")
        val pport = System.getenv("POSTGRES_PORTB")
        Database.connect(
            url = "jdbc:postgresql://$phost:$pport/$pdbName",
            driver = "org.postgresql.Driver",
            user = System.getenv("POSTGRES_USERNAME"),
            password = System.getenv("POSTGRES_PASSWORD"),
        )
        if (result > 0) {
            transaction {
                Events.insert {
                    it[Events.entityId] = UUID.fromString(routeId)
                    it[Events.eventName] = "RouteReservationCountChanged"
                    it[Events.data] = """$value"""
                }
            }
        }
        client.close()
        return result
    }
}
