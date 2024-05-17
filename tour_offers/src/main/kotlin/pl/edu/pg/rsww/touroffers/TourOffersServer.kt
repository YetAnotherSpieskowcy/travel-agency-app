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
            val n: Int = request.params["n"]?.toInt() ?: 0
            val next = n + 1
            val destination = request.params["destination"] ?: ""

            val result = tourOffersManager.getTourList(destination, "", "", 0, 0, 0, 0, n)

            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }

        if (request.path.contains("trip_details")){
            val resp = ResponseMessage(200, emptyMap(), """{
	            "name": "Trip no. ${request.params["id"]}",
	            "description": "A slightly longer description for trip number ${request.params["id"]}"
                }""")
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}