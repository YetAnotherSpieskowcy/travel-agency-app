package pl.edu.pg.rsww.touroperator

import kotlin.random.Random
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.MongoClient
import org.bson.Document
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import java.util.ArrayDeque
import java.util.UUID

data class Entity(
    val entity_id: String,
    val entity_type: String,
    val last_event_id: Int,
    val data: Document,
)

public class TourOperatorServer {
    val userName = System.getenv("MONGO_USERNAME")
    val password = System.getenv("MONGO_PASSWORD")
    val dbName = System.getenv("MONGO_DB")
    val host = System.getenv("MONGO_HOSTB")
    val port = System.getenv("MONGO_PORTB")
    val connectionString = "mongodb://$userName:$password@$host:$port/"

    val recentChanges = ArrayDeque<String>(10)
    val pendingChanges = HashMap<String, String>()
    var changeSimulatorEnabled = false

    @Autowired
    lateinit var template: RabbitTemplate

    @Autowired
    lateinit var queueConfig: QueueConfig

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path == "/recent_changes") {
            var body = "Operator wycieczek nie wykonał jeszcze żadnych zmian."
            if (recentChanges.size != 0) {
                body = buildString {
                    append("<ul>")
                    for (change in recentChanges) {
                        append("<li>")
                        append(change)
                        append("</li>")
                    }
                    append("</ul>")
                }
            }
            val resp = ResponseMessage(200, mapOf("Content-Type" to "text/html"), body)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path == "/enable_change_simulator") {
            changeSimulatorEnabled = true
            val resp = ResponseMessage(200, mapOf("Content-Type" to "text/html"), "Włączono")
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path == "/disable_change_simulator") {
            changeSimulatorEnabled = false
            val resp = ResponseMessage(200, mapOf("Content-Type" to "text/html"), "Wyłączono")
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.transactions}")])
    fun processPaymentEventHandler(
        request: String,
        message: Message,
    ) {
        println(message.messageProperties.receivedRoutingKey)
        when (message.messageProperties.receivedRoutingKey) {
            queueConfig.transactionProcessPayment -> {
                TimeUnit.SECONDS.sleep(3L)
                val payload = Json.decodeFromString<ProcessPaymentMessage>(request)
                template.convertAndSend(
                    queueConfig.events,
                    queueConfig.eventPaymentProcessed,
                    Json.encodeToString(
                        PaymentProcessedEvent(payload.triggeredBy, (0..1).random() == 1),
                    ),
                )
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    fun dataChangeSimulator() {
        if (!changeSimulatorEnabled) {
            return
        }
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        // TODO: change multiplier of a fixed tour for presentation
        // (+ exclude it from below changes)

        val available =
            db.getCollection<Entity>("snapshots").find(Filters.eq(Entity::entity_type.name, "Hotel"))
                .toList()
                .filter {
                    it.data.getDouble("reservation_count") < it.data.getDouble("reservation_limit")
                }

        val trips =
            db.getCollection<Entity>("snapshots").find(
                Filters.and(
                    Filters.eq(Entity::entity_type.name, "Tour"),
                ),
            )
                .toList()
                .shuffled()
                .filter {
                    available.any { a -> a.entity_id == it.data?.getString("hotel") }
                }

        for (trip in trips.take(3)) {
            val bookTripMsg = BookTripMessage(
                UUID.randomUUID().toString(), "unimportant", trip.entity_id
            )
            val title = trip.data.getString("title")
            pendingChanges[bookTripMsg.triggeredBy] = (
                "Liczba rezerwacji wycieczki '$title' (${trip.entity_id}) zwiększona o 1"
            )
            template.convertAndSend(
                queueConfig.externalTransactionBookTripExchange,
                queueConfig.externalTransactionBookTripKey,
                Json.encodeToString(bookTripMsg),
            )
            // TODO: multiplier change
            val newMultiplier = Random.nextDouble(1.0, 5.0)
        }

        // Not entirely sure if this is something that tour operator should generate
        /*template.convertAndSend(
            queueConfig.externalTransactionBookTransportExchange,
            queueConfig.externalTransactionBookTransportKey,
            Json.encodeToString(BookTransportMessage("unimportant", "unimportant", "<TODO>")),
        )*/
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.events}")])
    fun eventHandler(
        payload: String,
        message: Message,
    ) {
        when (message.messageProperties.receivedRoutingKey) {
            queueConfig.externalEventTripBookedKey -> {
                val event = Json.decodeFromString<TripBookedEvent>(payload)
                onTripBooked(event)
            }
            // Not entirely sure if this is something that tour operator should generate
            /*queueConfig.externalEventTransportBookedKey -> {
                val event = Json.decodeFromString<TransportBookedEvent>(payload)
                onTransportBooked(event)
            }*/
        }
    }

    fun onTripBooked(event: TripBookedEvent) {
        if (event.outcome == 0L) {
            return
        }

        updateRecentChanges(event.triggeredBy)
    }

    fun onTransportBooked(event: TransportBookedEvent) {
        if (event.outcome == 0L) {
            return
        }

        updateRecentChanges(event.triggeredBy)
    }

    fun updateRecentChanges(triggeredBy: String) {
        val changeDescription = pendingChanges.remove(triggeredBy)
        if (changeDescription == null) {
            return
        }

        // store to recent changes
        while (recentChanges.size > 10) {
            recentChanges.removeLast()
        }
        recentChanges.addFirst(changeDescription)
    }
}
