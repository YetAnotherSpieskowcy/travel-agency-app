package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

public class TripReservationController(
    @Autowired private val template: RabbitTemplate,
    @Autowired private val queueConfig: QueueConfig,
) {
    private val activeOrchestrators = HashMap<String, TripReservationOrchestrator>()

    fun bookTrip(msg: BookTripMessage) {
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventTripBooked,
            Json.encodeToString(
                TripBookedEvent(msg.triggeredBy, msg.userId, msg.tripId)
            ),
        )
        // TODO: implement actual logic
    }

    fun cancelBookTrip(msg: CancelBookTripMessage) {
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventTripCanceled,
            Json.encodeToString(
                TripCanceledEvent(msg.triggeredBy, msg.userId, msg.tripId)
            ),
        )
        // TODO: implement actual logic
    }

    fun getPreferences() {
        // TODO: implement actual logic
    }

    fun updateBookingPreferences(msg: UpdateBookingPreferencesMessage) {
        template.convertAndSend(
            queueConfig.base,
            queueConfig.eventBookingPreferencesUpdated,
            Json.encodeToString(
                BookingPreferencesUpdatedEvent(msg.triggeredBy)
            ),
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
            Json.encodeToString(
                PurchaseConfirmedEvent(msg.triggeredBy, msg.userId, msg.tripId)
            ),
        )
        // TODO: implement actual logic
    }

    fun startReservation(message: Message, userId: String, tripId: String) {
        val orchestrator = TripReservationOrchestrator(
            message = message,
            userId = userId,
            tripId = tripId,
            template = template,
            queueConfig = queueConfig,
        )
        orchestrator.onDone = fun() {
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
