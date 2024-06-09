package pl.edu.pg.rsww.touroperator

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.MongoClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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

    // Trip details:
    // - Title: Hotel AP Cabanas Beach & Nature
    // - To: Algarve
    // - Date: 2024-06-30
    val specialTripId = "a4610c19-4fad-4166-b852-ac9c81747634"

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
                body =
                    buildString {
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

        // change multiplier of a fixed trip to allow us to present the feature more easily
        val specialTrip =
            db.getCollection<Entity>(
                "snapshots",
            )
                .find(
                    Filters.and(
                        Filters.eq(Entity::entity_type.name, "Tour"),
                        Filters.eq(Entity::entity_id.name, specialTripId),
                    ),
                )
                .toList()
                .firstOrNull()

        if (specialTrip == null) {
            println("Could not fetch the special trip")
            return
        }

        changeMultiplier(specialTrip)

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
                    Filters.ne(Entity::entity_id.name, specialTripId),
                ),
            )
                .toList()
                .shuffled()
                .filter {
                    available.any { a -> a.entity_id == it.data?.getString("hotel") }
                }

        for (trip in trips.take(3)) {
            bookTrip(trip)
            changeMultiplier(trip)
        }

        // Not entirely sure if this is something that tour operator should generate

        /*template.convertAndSend(
            queueConfig.externalTransactionBookTransportExchange,
            queueConfig.externalTransactionBookTransportKey,
            Json.encodeToString(BookTransportMessage("unimportant", "unimportant", "<TODO>")),
        )*/
    }

    fun changeMultiplier(trip: Entity) {
        val newMultiplier = Random.nextDouble(1.0, 5.0)
        val msg =
            ChangeTripMultiplierMessage(
                UUID.randomUUID().toString(),
                trip.entity_id,
                newMultiplier,
            )
        val title = trip.data.getString("title")
        pendingChanges[msg.triggeredBy] = (
            "Mnożnik dla wycieczki '$title' (${trip.entity_id}) zmieniony na $newMultiplier"
        )
        template.convertAndSend(
            queueConfig.externalTransactionChangeTripMultiplierExchange,
            queueConfig.externalTransactionChangeTripMultiplierKey,
            Json.encodeToString(msg),
        )
    }

    fun bookTrip(trip: Entity) {
        val msg =
            BookTripMessage(
                UUID.randomUUID().toString(),
                "unimportant",
                trip.entity_id,
            )
        val title = trip.data.getString("title")
        pendingChanges[msg.triggeredBy] = (
            "Liczba rezerwacji wycieczki '$title' (${trip.entity_id}) zwiększona o 1"
        )
        template.convertAndSend(
            queueConfig.externalTransactionBookTripExchange,
            queueConfig.externalTransactionBookTripKey,
            Json.encodeToString(msg),
        )
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
            queueConfig.externalEventTripMultiplierChangedKey -> {
                val event = Json.decodeFromString<TripMultiplierChangedEvent>(payload)
                onTripMultiplierChanged(event)
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

    fun onTripMultiplierChanged(event: TripMultiplierChangedEvent) {
        if (event.outcome == 0L) {
            return
        }

        updateRecentChanges(event.triggeredBy)
    }

    /*fun onTransportBooked(event: TransportBookedEvent) {
        if (event.outcome == 0L) {
            return
        }

        updateRecentChanges(event.triggeredBy)
    }*/

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
