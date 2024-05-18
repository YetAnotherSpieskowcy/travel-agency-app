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
}
