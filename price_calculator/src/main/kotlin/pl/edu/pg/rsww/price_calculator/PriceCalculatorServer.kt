package pl.edu.pg.rsww.price_calculator

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.beans.factory.annotation.Value


public class PriceCalculatorServer {
    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("calculate_price")){

            val resp = ResponseMessage(200, emptyMap(), """{
	            "name": "${request.params["search"]}",
	            "price_with_transport": "100",
                "price_without_transport": "50"
                }""")
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}