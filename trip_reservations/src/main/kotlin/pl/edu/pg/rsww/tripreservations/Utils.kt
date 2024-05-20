package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate

fun sendHttpResponse(
    template: RabbitTemplate,
    message: Message,
    body: String,
) {
    val messageProperties = message.messageProperties
    val replyTo = messageProperties.replyToAddress
    val correlationId = messageProperties.correlationId
    template.convertAndSend(
        replyTo.exchangeName,
        replyTo.routingKey,
        Json.encodeToString(ResponseMessage(200, emptyMap(), body)),
    ) {
        it.messageProperties.correlationId = correlationId
        it
    }
}
