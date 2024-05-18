package pl.edu.pg.rsww.tripreservations

import java.net.HttpCookie
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

public class TripReservationServer(
    @Autowired private val template: RabbitTemplate,
    @Autowired private val controller: TripReservationController,
    @Autowired private val queueConfig: QueueConfig,
) {

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(payload: String, message: Message) {
        val request = Json.decodeFromString<RequestMessage>(payload)
        val cookieHeader = request.headers["cookie"]
        if (cookieHeader == null) {
            replyUnauthorized(message)
            return
        }
        val cookies = HttpCookie.parse(cookieHeader)
        val userId = cookies.last { cookie -> cookie.name == "user" }?.value
        if (userId == null) {
            replyUnauthorized(message)
            return
        }
        if (request.path == "/start_reservation") {
            controller.startReservation(
                message = message,
                userId = userId,
                tripId = request.params["id"] ?: "",
            )
        }
    }

    private fun replyUnauthorized(message: Message) {
        sendHttpResponse(
            template,
            message,
            """{ "error": "401" }""",
        )
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.transactions}")])
    fun transactionHandler(payload: String, message: Message) {
        when (message.messageProperties.receivedRoutingKey) {
            queueConfig.transactionBookTrip -> {
                val msg = Json.decodeFromString<BookTripMessage>(payload)
                controller.bookTrip(msg)
            }
            queueConfig.transactionCancelBookTrip -> {
                val msg = Json.decodeFromString<CancelBookTripMessage>(payload)
                controller.cancelBookTrip(msg)
            }
            queueConfig.transactionUpdateBookingPreferences -> {
                val msg = Json.decodeFromString<UpdateBookingPreferencesMessage>(payload)
                controller.updateBookingPreferences(msg)
            }
            queueConfig.transactionConfirmPurchase -> {
                val msg = Json.decodeFromString<ConfirmPurchaseMessage>(payload)
                controller.confirmPurchase(msg)
            }
        }
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.events}")])
    fun eventHandler(payload: String, message: Message) {
        println(message.messageProperties.receivedRoutingKey)
        when (message.messageProperties.receivedRoutingKey) {
            queueConfig.eventTripBooked -> {
                val event = Json.decodeFromString<TripBookedEvent>(payload)
                controller.onTripBooked(event)
            }
            queueConfig.externalEventTransportBookedKey -> {
                val event = Json.decodeFromString<TransportBookedEvent>(payload)
                controller.onTransportBooked(event)
            }
            queueConfig.eventBookingPreferencesUpdated -> {
                val event = Json.decodeFromString<BookingPreferencesUpdatedEvent>(payload)
                controller.onBookingPreferencesUpdated(event)
            }
            queueConfig.externalEventPaymentProcessedKey -> {
                val event = Json.decodeFromString<PaymentProcessedEvent>(payload)
                controller.onPaymentProcessed(event)
            }
            queueConfig.eventTripPurchaseConfirmed -> {
                val event = Json.decodeFromString<PurchaseConfirmedEvent>(payload)
                controller.onPurchaseConfirmed(event)
            }
        }
    }
}
