package pl.edu.pg.rsww.apigateway

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
public class RoutingController(
    @Value("\${serviceQueues}")
    private val serviceQueues: Array<String>,
    @Autowired
    private val template: RabbitTemplate,
) {
    @RequestMapping("/health")
    fun index(): String {
        // have one internal (outside /api/*) endpoint that indicates
        // that the API gateway itself is fine
        return "Hello, I'm alive!"
    }

    @RequestMapping("/api/{serviceName}/{*path}")
    fun serviceRouter(
        @PathVariable serviceName: String,
        @PathVariable path: String,
        @RequestParam params: Map<String, String>,
        @RequestHeader headers: Map<String, String>,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<String> {
        if (!serviceQueues.contains(serviceName)) {
            throw NotFoundException()
        }

        val msg =
            RequestMessage(
                serviceName = serviceName,
                path = path,
                params = params,
                headers = headers,
                body = body ?: "",
            )
        val rawMsg = Json.encodeToString(msg)

        val exchangeName = "$serviceName.requests"
        val rawResponse =
            template.convertSendAndReceive(
                exchangeName,
                exchangeName,
                rawMsg as Any,
            ) as String
        val resp = Json.decodeFromString<ResponseMessage>(rawResponse)

        var builder = ResponseEntity.status(resp.status)
        resp.headers.forEach { key, value ->
            builder = builder.header(key, value)
        }
        return builder.body(resp.body)
    }
}
