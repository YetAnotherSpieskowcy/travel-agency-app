package pl.edu.pg.rsww.touroffers

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.beans.factory.annotation.Value


public class TourOffersServer {
    val tourOffersManager: TourOffersManager = TourOffersManager()

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("get_trips")){
            val destination = request.params["destination"] ?: ""
            val from = request.params["from"] ?: ""
            var departureDate = request.params["dep_date"] ?: ""
            if (departureDate.contains("m") || departureDate.contains("d") || departureDate.contains("r")){
                departureDate = ""
            }
            val adults = request.params["num_adults"]?.toInt() ?: 1
            val under3 = request.params["num_under_3"]?.toInt() ?: 0
            val under10 = request.params["num_under_10"]?.toInt() ?: 0
            val under18 = request.params["num_under_18"]?.toInt() ?: 0


            val result = tourOffersManager.getTourList(destination, from, departureDate, adults, under3, under10, under18)

            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }

        if (request.path.contains("trip_details")){
            val result = tourOffersManager.getTourDetails(request.params["id"] ?: "")
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}