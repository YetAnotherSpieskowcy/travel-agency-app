package pl.edu.pg.rsww.touroperator

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

public class TourOperatorServer {
    @Autowired
    lateinit var template: RabbitTemplate

    @Autowired
    lateinit var queueConfig: QueueConfig

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
}
