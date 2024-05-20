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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@RestController
public class RoutingController(
    @Value("\${serviceQueues}") private val serviceQueues: Array<String>,
    @Autowired private val template: RabbitTemplate,
) {
    @RequestMapping("/health")
    fun index(): String {
        // have one internal (outside /api/*) endpoint that indicates
        // that the API gateway itself is fine
        return "Hello, I'm alive!"
    }

    @RequestMapping("/api/mocked/{*path}")
    fun mock(
        @PathVariable path: String,
        @RequestParam params: Map<String, String>,
        @RequestHeader headers: Map<String, String>,
    ): ResponseEntity<String> {
        if (!(headers["cookie"]?.contains("user") ?: false)) {
            var builder = ResponseEntity.status(300)
            builder.header("HX-Redirect", "/login.html")
            return builder.body("")
        }
        val builder = ResponseEntity.status(200)
        if (path.contains("trip_rooms_left")) {
            println(params)
            return builder.body("${(0..6).random()}")
        }
        if (path.contains("create_reservation")) {
            TimeUnit.SECONDS.sleep(3L)
            val reservedUntil =
                DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(60))
            println(params)
            return builder.body(
                """{
                  "id": ${params["id"]},
                  "reserved_until": "$reservedUntil"
                }""",
            )
        }
        if (path.contains("confirm_reservation")) {
            TimeUnit.SECONDS.sleep(3L)
            println(params)
            return builder.body(
                """
                <p>Congratulations, you bought it!</p>
                <button type="button"
                    class="flex select-none items-center gap-3 rounded-lg border border-gray-500 py-3 px-6 text-center align-middle font-sans text-xs font-bold uppercase text-gray-500 transition-all hover:opacity-75 focus:ring focus:ring-gray-200 active:opacity-[0.85] disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                    hx-get="/search.html" hx-target="#container">Go back to tour list</button>
                """,
            )
        }
        if (path.contains("get_trips")) {
            val n: Int = params["n"]?.toInt() ?: 0
            val next = n + 1

            return builder.body(
                """{
	  "name": "${params["search"]}",
	  "n": $next,
	  "id": $n,
	  "description": "Hello, world!"
      }""",
            )
        }
        if (path.contains("trip")) {
            val rooms_left = (0..6).random()
            return builder.body(
                """{
      "id": "${params["id"]}",
      "name": "Trip no. ${params["id"]}",
      "description": "A slightly longer description for trip number ${params["id"]}",
      "hotel_meal_titles": [
        "All inclusive",
        "Wy\u017cywienie zgodnie z programem"
      ],
      "geolocation": {
        "lat": 39.794014,
        "lng": 19.76343
      },
      "hotel_destination_country_title": "Grecja",
      "hotel_destination_city_title": "Korfu",
      "hotel_rooms": [
        {
          "title": "superior",
          "bed_count": 2,
          "extra_bed_count": 1
        },
        {
          "title": "rodzinny",
          "bed_count": 2,
          "extra_bed_count": 2
        }
      ],
      "hotel_rating": 35,
      "hotel_reservation_limit": 98,
      "hotel_reservation_count": ${98-rooms_left},
      "hotel_rooms_left": ${rooms_left},
      "hotel_minimum_age": 10,
      "hotel_max_people_per_reservation": 4
      "duration": 4,
      "start_date": "2024-06-29",
      "end_date": "2024-07-03"
      }""",
            )
        }
        return builder.body("""{ "error": "404" }""")
    }

    @RequestMapping("/api/{serviceName}/{*path}")
    fun serviceRouter(
        @PathVariable serviceName: String,
        @PathVariable path: String,
        @RequestParam params: Map<String, String>,
        @RequestHeader headers: Map<String, String>,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<String> {
        if (serviceName != "auth") {
            if (!(headers["cookie"]?.contains("user") ?: false)) {
                var builder = ResponseEntity.status(300)
                builder.header("HX-Redirect", "/login.html")
                builder.header("Cache-Control", "no-cache")
                return builder.body("")
            }
        }
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
            template.convertSendAndReceive(exchangeName, exchangeName, rawMsg as Any) as String
        val resp = Json.decodeFromString<ResponseMessage>(rawResponse)

        var builder = ResponseEntity.status(resp.status)
        resp.headers.forEach { key, value -> builder = builder.header(key, value) }
        return builder.body(resp.body)
    }
}
