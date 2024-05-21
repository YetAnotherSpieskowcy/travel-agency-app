package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.net.HttpCookie

public class TripReservationServer(
    @Autowired private val template: RabbitTemplate,
    @Autowired private val controller: TripReservationController,
    @Autowired private val queueConfig: QueueConfig,
) {
    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(
        payload: String,
        message: Message,
    ) {
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
                tripId = request.params["trip_id"] ?: "",
                routeId = request.params["route_id"] ?: "",
            )
            return
        }
        if (request.path == "/confirm_reservation") {
            if (controller.activeOrchestrators[request.params["sagaId"]] == null) {
                sendHttpResponse(
                    template,
                    message,
                    """
                    <p>Rezerwacja nie mogła zostać znaleziona lub wygasła.</p>
                    <button type="button"
                        class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                        hx-get="/search.html" hx-target="#container">Powróć do wyszukiwarki wycieczek</button>
                    """,
                )
                return
            }

            controller.activeOrchestrators[request.params["sagaId"]]?.continuationMessage = message
            controller.activeOrchestrators[request.params["sagaId"]]?.sendProcessPayment()
            return
        }
        sendHttpResponse(template, message, """{"error":"404"}""")
    }

    private fun replyUnauthorized(message: Message) {
        sendHttpResponse(
            template,
            message,
            """{ "error": "401" }""",
        )
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.transactions}")])
    fun transactionHandler(
        payload: String,
        message: Message,
    ) {
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
    fun eventHandler(
        payload: String,
        message: Message,
    ) {
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
