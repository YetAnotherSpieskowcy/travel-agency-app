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
            try {
                val destLat: Float = request.params["destLatitude"]?.toFloat() ?: 0.0f
                val destLong: Float = request.params["destLongitude"]?.toFloat() ?: 0.0f
                val fromLat: Float = request.params["fromLatitude"]?.toFloat() ?: 0.0f
                val fromLong: Float = request.params["fromLongitude"]?.toFloat() ?: 0.0f

                val duration: Int = request.params["duration"]?.toInt() ?: 0
                val numPeople: Int = request.params["numPeople"]?.toInt() ?: 0
                var freeSpots: Int = request.params["freeSpots"]?.toInt() ?: 1
                if (freeSpots == 0) {
                    freeSpots = 1
                }
                var transportType: String = (request.params["route_id"]
                        ?: "").split("_", limit = 2)[0].ifEmpty { "none" }
                transportType = request.params["transportType"] ?: transportType
                val hotelRating: Int = request.params["hotelRating"]?.toInt() ?: 30
                val mealType: String = request.params["mealType"] ?: "Bez wy\u017cywienia"

                val destGeolocation: Geolocation = Geolocation(destLat, destLong)
                val fromGeolocation: Geolocation = Geolocation(fromLat, fromLong)

                val price =
                        calculator.calculatePrice(
                                destGeolocation,
                                fromGeolocation,
                                duration,
                                numPeople,
                                freeSpots,
                                transportType,
                                hotelRating / 10.0f,
                                mealType,
                        )

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
            } catch (e: Exception){
                val resp =
                        ResponseMessage(
                                200,
                                emptyMap(),
                                """{
	            "price": 3721
                }""",
                        )
                return Json.encodeToString(resp)
            }
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
