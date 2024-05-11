package pl.edu.pg.rsww.pricecalculator

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener

public class PriceCalculatorServer {
    val calculator: PriceCalculator = PriceCalculator()

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("calculate_price")) {
            val destLat: String = request.params["destLatitude"] ?: "0"
            val destLong: String = request.params["destLongitude"] ?: "0"
            val fromLat: String = request.params["fromLatitude"] ?: "0"
            val fromLong: String = request.params["fromLongitude"] ?: "0"

            val duration: Int = request.params["duration"]?.toInt() ?: 0
            val numPeople: Int = request.params["numPeople"]?.toInt() ?: 0
            val transportType: String = request.params["transportType"] ?: "none"

            val destGeolocation: Geolocation = Geolocation(destLat, destLong)
            val fromGeolocation: Geolocation = Geolocation(fromLat, fromLong)

            val price = calculator.calculatePrice(destGeolocation, fromGeolocation, duration, numPeople, transportType)

            val resp =
                ResponseMessage(
                    200,
                    emptyMap(),
                    """{
	            "price": $price
                }""",
                )
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
