package pl.edu.pg.rsww.tripreservations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.queue")
public class QueueConfig {
    lateinit var base: String
    val requests get() = "$base.requests"
    val events get() = "$base.events"
    val transactions get() = "$base.transactions"

    val transactionBookTrip get() = "$transactions.bookTrip"
    val transactionCancelBookTrip get() = "$transactions.cancelBookTrip"
    val transactionGetPreferences get() = "$transactions.getPreferences"
    val transactionUpdateBookingPreferences get() = "$transactions.updateBookingPreferences"
    val transactionConfirmPurchase get() = "$transactions.confirmPurchase"
    val eventTripBooked get() = "$events.tripBooked"
    val eventTripCanceled get() = "$events.tripCanceled"
    val eventTripPurchaseConfirmed get() = "$events.tripPurchaseConfirmed"
    val eventBookingPreferencesUpdated get() = "$events.bookingPreferencesUpdated"

    val externalTransactionBookTransportExchange get() = "transport_reservations.requests"
    val externalTransactionBookTransportKey get() = "${externalTransactionBookTransportExchange}.bookTransport"
    val externalEventTransportBookedExchange get() = "transport_reservations.events"
    val externalEventTransportBookedKey get() = "${externalEventTransportBookedExchange}.transportBooked"

    val externalTransactionProcessPaymentExchange get() = "tour_operator"
    val externalTransactionProcessPaymentKey get() = "${externalTransactionProcessPaymentExchange}.transactions.processPayment"
    val externalEventPaymentProcessedExchange get() = "tour_operator.events"
    val externalEventPaymentProcessedKey get() = "${externalEventPaymentProcessedExchange}.paymentProcessed"
}
