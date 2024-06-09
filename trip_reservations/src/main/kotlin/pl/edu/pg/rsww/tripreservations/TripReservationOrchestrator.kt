package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timerTask

public class TripReservationOrchestrator(
    val message: Message,
    val userId: String,
    val tripId: String,
    val routeId: String,
    val preferences: PreferencesPayload,
    val template: RabbitTemplate,
    val queueConfig: QueueConfig,
) {
    val sagaId = UUID.randomUUID().toString()
    var state = State.WAITING_TO_START
    var canceled = false
    var completed = false
    var startReservationResponseSent = false
    var onDone: (() -> Unit)? = null

    enum class State {
        WAITING_TO_START,
        STARTED,
        SENT_BOOK_TRIP,
        ACK_BOOK_TRIP,
        SENT_BOOK_TRANSPORT,
        ACK_BOOK_TRANSPORT,
        SENT_UPDATE_BOOKING_PREFERENCES,
        ACK_UPDATE_BOOKING_PREFERENCES,
        SENT_PROCESS_PAYMENT,
        ACK_PROCESS_PAYMENT,
        SENT_CONFIRM_PURCHASE,
        ACK_CONFIRM_PURCHASE,
        DONE,
    }

    val pivotState = State.ACK_PROCESS_PAYMENT

    private fun checkState(newState: State): Boolean {
        val expectedState = State.values()[newState.ordinal - 1]
        if (state != expectedState) {
            try {
                throw RuntimeException("$newState: expected state $expectedState, got $state")
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            revertSaga()
            return true
        }
        return canceled
    }

    val timer = Timer()

    fun start() {
        println("A")
        val newState = State.STARTED
        if (checkState(newState)) {
            println("A2")
            return
        }
        state = newState
        timer.schedule(
            timerTask {
                println("WTF")
                if (!canceled && state < pivotState) {
                    revertSaga()
                }
            },
            60000,
        )
        sendBookTrip()
    }

    fun sendBookTrip() {
        println("B")
        val newState = State.SENT_BOOK_TRIP
        if (checkState(newState)) {
            println("B2")
            return
        }
        template.convertAndSend(
            queueConfig.base,
            queueConfig.transactionBookTrip,
            Json.encodeToString(BookTripMessage(sagaId, userId, tripId)),
        )
        state = newState
    }

    fun ackBookTrip(event: TripBookedEvent) {
        println("C")
        val newState = State.ACK_BOOK_TRIP

        if (checkState(newState)) {
            println("C2")
            return
        }
        if (event.outcome == 0L) {
            revertSaga()
            return
        }
        state = newState
        sendBookTransport()
    }

    fun sendBookTransport() {
        println("D")
        val newState = State.SENT_BOOK_TRANSPORT
        if (checkState(newState)) {
            println("D2")
            return
        }
        if (routeId == "") {
            state = State.ACK_BOOK_TRANSPORT
            sendUpdateBookingPreferences()
            return
        }
        template.convertAndSend(
            queueConfig.externalTransactionBookTransportExchange,
            queueConfig.externalTransactionBookTransportKey,
            Json.encodeToString(BookTransportMessage(sagaId, userId, routeId)),
        )
        state = newState
    }

    fun ackBookTransport(event: TransportBookedEvent) {
        println("E")
        val newState = State.ACK_BOOK_TRANSPORT
        if (checkState(newState)) {
            println("E2")
            return
        }
        if (event.outcome == 0L) {
            revertSaga()
            return
        }

        state = newState
        sendUpdateBookingPreferences()
    }

    fun sendUpdateBookingPreferences() {
        println("F")
        val newState = State.SENT_UPDATE_BOOKING_PREFERENCES
        if (checkState(newState)) {
            println("F2")
            return
        }
        state = newState
        template.convertAndSend(
            queueConfig.base,
            queueConfig.transactionUpdateBookingPreferences,
            // TODO: establish what should be inside this message
            Json.encodeToString(UpdateBookingPreferencesMessage(sagaId, preferences)),
        )
    }

    var continuationMessage: Message? = null

    fun ackUpdateBookingPreferences(event: BookingPreferencesUpdatedEvent) {
        println("G")
        val newState = State.ACK_UPDATE_BOOKING_PREFERENCES
        if (checkState(newState)) {
            println("G2")
            return
        }
        state = newState
        val reservedUntil = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(60))
        sendHttpResponse(
            template,
            message,
            """{
	  "success":${!canceled},
	  "sagaId":"$sagaId",
	  "reserved_until": "$reservedUntil"
	  }""",
        )
        startReservationResponseSent = true
        return
    }

    fun sendProcessPayment() {
        println("H")
        val newState = State.SENT_PROCESS_PAYMENT
        if (checkState(newState)) {
            println("H2")
            return
        }
        template.convertAndSend(
            queueConfig.externalTransactionProcessPaymentExchange,
            queueConfig.externalTransactionProcessPaymentKey,
            Json.encodeToString(ProcessPaymentMessage(sagaId)),
        )
        state = newState
    }

    fun ackProcessPayment(event: PaymentProcessedEvent) {
        println("I")
        val newState = State.ACK_PROCESS_PAYMENT
        if (checkState(newState)) {
            println("I2")
            return
        }
        if (event.success) {
            println("Payment fail")
            revertSaga()
            return
        }
        timer.cancel()

        state = newState
        sendConfirmPurchase()
    }

    fun setTripPurchasedEvent(tripId: String) {
        val pdbName = System.getenv("POSTGRES_DB")
        val phost = System.getenv("POSTGRES_HOSTB")
        val pport = System.getenv("POSTGRES_PORTB")
        Database.connect(
            url = "jdbc:postgresql://$phost:$pport/$pdbName",
            driver = "org.postgresql.Driver",
            user = System.getenv("POSTGRES_USERNAME"),
            password = System.getenv("POSTGRES_PASSWORD"),
        )
        transaction {
            Events.insert {
                it[Events.entityId] = UUID.fromString(tripId)
                it[Events.eventName] = "TripPurchased"
                it[Events.data] = """NA"""
            }
        }
    }

    fun sendConfirmPurchase() {
        println("J")
        val newState = State.SENT_CONFIRM_PURCHASE
        if (checkState(newState)) {
            println("J2")
            return
        }
        //    template.convertAndSend(
        //        queueConfig.externalTransactionProcessPaymentExchange,
        //        queueConfig.externalTransactionProcessPaymentKey,
        //        Json.encodeToString(ProcessPaymentMessage(sagaId)),
        //    )
        state = newState
        ackConfirmPurchase(PurchaseConfirmedEvent("", "", ""))
    }

    fun ackConfirmPurchase(event: PurchaseConfirmedEvent) {
        println("K")
        val newState = State.ACK_CONFIRM_PURCHASE
        if (checkState(newState)) {
            println("K2")
            return
        }
        state = newState
        done()
    }

    fun done() {
        println("L")
        state = State.DONE
        if (canceled && startReservationResponseSent) {
            sendHttpResponse(
                template,
                continuationMessage ?: message,
                """
                <p>Coś poszło nie tak, płatność najpewniej została odrzucona przez operatora.</p>
                <button type="button"
                    class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                    hx-get="/search.html" hx-target="#container">Powróć do wyszukiwarki wycieczek</button>
                """,
            )
        } else if (canceled) {
            sendHttpResponse(
                template,
                continuationMessage ?: message,
                """
                <p>Coś poszło nie tak, najprawdopodobniej skończyły się miejsca.</p>
                <button type="button"
                    class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                    hx-get="/search.html" hx-target="#container">Powróć do wyszukiwarki wycieczek</button>
                """,
            )
        } else {
            setTripPurchasedEvent(tripId)
            sendHttpResponse(
                template,
                continuationMessage ?: message,
                """
                <p>Gratulacje, kupiłeś wycieczkę!</p>
                <button type="button"
                    class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                    hx-get="/search.html" hx-target="#container">Powróć do wyszukiwarki wycieczek</button>
                """,
            )
        }
        onDone?.invoke()
    }

    fun revertSaga() {
        canceled = true
        timer.cancel()
        revertUpdateBookingPreferences()
        revertBookTransport()
        revertBookTrip()
        done()
    }

    fun revertUpdateBookingPreferences() {
        if (state < State.SENT_UPDATE_BOOKING_PREFERENCES) {
            return
        }
    }

    fun revertBookTransport() {
        if (state <= State.SENT_BOOK_TRANSPORT) {
            return
        }
        if (routeId == "") {
            return
        }
        template.convertAndSend(
            queueConfig.externalTransactionBookTransportExchange,
            queueConfig.externalTransactionCancelBookTransportKey,
            Json.encodeToString(BookTransportMessage(sagaId, userId, routeId)),
        )
    }

    fun revertBookTrip() {
        if (state <= State.SENT_BOOK_TRIP) {
            return
        }
        template.convertAndSend(
            queueConfig.base,
            queueConfig.transactionCancelBookTrip,
            Json.encodeToString(CancelBookTripMessage(sagaId, userId, tripId)),
        )
    }
}
