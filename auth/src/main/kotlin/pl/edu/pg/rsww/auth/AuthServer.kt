package pl.edu.pg.rsww.auth

import com.mongodb.kotlin.client.MongoClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener

data class User(val user_id: String, val user_name: String)

public class AuthServer {
    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("login")) {
            val userName = System.getenv("MONGO_USERNAME")
            val password = System.getenv("MONGO_PASSWORD")
            val dbName = System.getenv("MONGO_DB")
            val host = System.getenv("MONGO_HOSTB")
            val port = System.getenv("MONGO_PORTB")
            val connectionString = "mongodb://$userName:$password@$host:$port/"
            val client = MongoClient.create(connectionString = connectionString)
            val db = client.getDatabase(databaseName = dbName)
            val user =
                db.getCollection<User>("users")
                    .find()
                    .toList()
                    .filter { it.user_name == request.params["name"] }
                    .firstOrNull()
            val resp =
                user?.let {
                    ResponseMessage(
                        200,
                        mapOf("Set-Cookie" to """user=${user.user_id}; Path=/""", "HX-Redirect" to "/"),
                        "",
                    )
                }
                    ?: run {
                        ResponseMessage(
                            200,
                            emptyMap(),
                            "Username not found",
                        )
                    }
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path.contains("logout")) {
            val resp =
                ResponseMessage(
                    200,
                    mapOf(
                        "Set-Cookie" to """user=; Path=/; Expires=Thu, 31 Oct 2021 07:28:00 GMT;""",
                        "HX-Redirect" to "/login.html",
                    ),
                    "",
                )
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
