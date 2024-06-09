package pl.edu.pg.rsww.touroperator

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.queue")
public class QueueConfig {
    lateinit var base: String
    val requests get() = "$base.requests"
    val events get() = "$base.events"
    val transactions get() = "$base.transactions"

    val eventPaymentProcessed get() = "$events.paymentProcessed"
    val transactionProcessPayment get() = "$transactions.processPayment"

    val externalTransactionBookTransportExchange
        get() = "transports"
    val externalTransactionBookTransportKey
        get() = "$externalTransactionBookTransportExchange.requests.bookTransport"
    val externalEventTransportBookedKey
        get() = "trip_reservations.events.transportBooked"

    val externalTransactionBookTripExchange
        get() = "trip_reservations"
    val externalTransactionBookTripKey
        get() = "$externalTransactionBookTripExchange.transactions.bookTrip"
    val externalEventTripBookedKey
        get() = "trip_reservations.events.tripBooked"
    val externalTransactionChangeTripMultiplierExchange
        get() = "trip_reservations"
    val externalTransactionChangeTripMultiplierKey
        get() = "$externalTransactionChangeTripMultiplierExchange.transactions.changeTripMultiplier"
    val externalEventTripMultiplierChangedKey
        get() = "trip_reservations.events.tripMultiplierChanged"
}
