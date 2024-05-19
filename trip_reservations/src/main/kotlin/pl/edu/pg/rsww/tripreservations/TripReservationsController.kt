package pl.edu.pg.rsww.tripreservations

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Updates.inc
import com.mongodb.kotlin.client.MongoClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

data class Entity(
    val entity_id: String,
    val entity_type: String,
    val last_event_id: Int,
    val data: Document,
)

public class TripReservationController(
    @Autowired private val template: RabbitTemplate,
    @Autowired private val queueConfig: QueueConfig,
) {
    private val activeOrchestrators = HashMap<String, TripReservationOrchestrator>()

    fun updateReservationCounter(
        tripId: String,
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

        val tourFilter = eq("entity_id", tripId)
        val tour = collection.find(tourFilter).firstOrNull() ?: return 0
        val hotelId = tour.data.getString("hotel")
        val hotel = collection.find(eq("entity_id", hotelId)).firstOrNull() ?: return 0
        var limit = hotel.data.getDouble("reservation_limit")
        if (limit > 0) {
            limit += (1 - value)
        }

        val filter = and(eq("entity_id", hotelId), lt("data.reservation_count", limit))
        val update = inc("data.reservation_count", value)
        return collection.updateOne(filter, update).matchedCount
    }

    fun bookTrip(msg: BookTripMessage) {
        val result = updateReservationCounter(msg.tripId, 1)

        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventTripBooked,
            Json.encodeToString(TripBookedEvent(msg.triggeredBy, msg.userId, msg.tripId, result)),
        )
    }

    fun cancelBookTrip(msg: CancelBookTripMessage) {
        val result = updateReservationCounter(msg.tripId, -1)
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventTripCanceled,
            Json.encodeToString(TripCanceledEvent(msg.triggeredBy, msg.userId, msg.tripId)),
        )
    }

    fun getPreferences() {
        // TODO: implement actual logic
    }

    fun updateBookingPreferences(msg: UpdateBookingPreferencesMessage) {
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventBookingPreferencesUpdated,
            Json.encodeToString(BookingPreferencesUpdatedEvent(msg.triggeredBy)),
        )
        // TODO: implement actual logic
    }

    fun confirmPurchase(msg: ConfirmPurchaseMessage) {
        val orchestrator = activeOrchestrators[msg.triggeredBy]
        if (orchestrator == null) {
            return
        }
        sendHttpResponse(template, orchestrator.message, "{}")
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventTripPurchaseConfirmed,
            Json.encodeToString(PurchaseConfirmedEvent(msg.triggeredBy, msg.userId, msg.tripId)),
        )
        // TODO: implement actual logic
    }

    fun startReservation(
        message: Message,
        userId: String,
        tripId: String,
    ) {
        val orchestrator =
            TripReservationOrchestrator(
                message = message,
                userId = userId,
                tripId = tripId,
                template = template,
                queueConfig = queueConfig,
            )
        orchestrator.onDone =

            fun() {
                activeOrchestrators.remove(orchestrator.sagaId)
            }
        activeOrchestrators[orchestrator.sagaId] = orchestrator
        orchestrator.start()
    }

    fun onTripBooked(event: TripBookedEvent) {
        val orchestrator = activeOrchestrators[event.triggeredBy]
        if (orchestrator != null) {
            orchestrator.ackBookTrip(event)
        }
    }

    fun onTransportBooked(event: TransportBookedEvent) {
        val orchestrator = activeOrchestrators[event.triggeredBy]
        if (orchestrator != null) {
            orchestrator.ackBookTransport(event)
        }
    }

    fun onBookingPreferencesUpdated(event: BookingPreferencesUpdatedEvent) {
        val orchestrator = activeOrchestrators[event.triggeredBy]
        if (orchestrator != null) {
            orchestrator.ackUpdateBookingPreferences(event)
        }
    }

    fun onPaymentProcessed(event: PaymentProcessedEvent) {
        val orchestrator = activeOrchestrators[event.triggeredBy]
        if (orchestrator != null) {
            orchestrator.ackProcessPayment(event)
        }
    }

    fun onPurchaseConfirmed(event: PurchaseConfirmedEvent) {
        val orchestrator = activeOrchestrators[event.triggeredBy]
        if (orchestrator != null) {
            orchestrator.ackConfirmPurchase(event)
        }
    }
}
