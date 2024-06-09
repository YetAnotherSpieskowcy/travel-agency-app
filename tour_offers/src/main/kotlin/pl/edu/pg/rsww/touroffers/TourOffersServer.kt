package pl.edu.pg.rsww.touroffers

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener

public class TourOffersServer {
    val tourOffersManager: TourOffersManager = TourOffersManager()

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("get_trips")) {
            val destination = request.params["destination"] ?: ""
            val from = request.params["from"] ?: ""
            var departureDate = request.params["dep_date"] ?: ""
            if (departureDate.contains("m") || departureDate.contains("d") || departureDate.contains("r")) {
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

        if (request.path.contains("trip_details")) {
            val result =
                tourOffersManager.getTourDetails(
                    request.params["id"] ?: "",
                    request.params["numPeople"]?.toInt() ?: 0,
                )
            if (result == null) {
                val resp =
                    ResponseMessage(
                        200,
                        mapOf("HX-Retarget" to "#container"),
                        """
                    <p>Wycieczka wydaje się być już niedostępna.</p>
                    <button type="button"
                        class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                        hx-get="/search.html" hx-target="#container">Powróć do wyszukiwarki wycieczek</button>
                    """,
                    )
                val rawResp = Json.encodeToString(resp)
                return rawResp
            }
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path.contains("dest_preferences")) {
            val resp = ResponseMessage(200, emptyMap(), tourOffersManager.getTripPreferences())
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path.contains("detail_preferences")) {
            val tripId = request.params["tripId"] ?: ""
            val resp = ResponseMessage(200, emptyMap(), tourOffersManager.getDetailPreferences(tripId))
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }

        if (request.path.contains("trip_available_rooms")) {
            val result =
                tourOffersManager.getTourAvailableRooms(
                    request.params["id"] ?: "",
                    request.params["numPeople"]?.toInt() ?: 0,
                )
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }

        if (request.path.contains("transport_options")) {
            val result =
                tourOffersManager.getTransportOptions(
                    request.params["id"] ?: "",
                    request.params["numPeople"]?.toInt() ?: 0,
                    request.params["route_id"] ?: "",
                )
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
