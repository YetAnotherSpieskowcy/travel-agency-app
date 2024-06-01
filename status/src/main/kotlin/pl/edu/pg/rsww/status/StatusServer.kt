package pl.edu.pg.rsww.status

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Observer(val observerId: String, val tripId: String, val timestamp: String)

public class StatusServer {
    fun getDbCollection(func: (MongoCollection<Observer>) -> Unit) {
        val userName = System.getenv("MONGO_USERNAME")
        val password = System.getenv("MONGO_PASSWORD")
        val dbName = System.getenv("MONGO_DB")
        val host = System.getenv("MONGO_HOSTB")
        val port = System.getenv("MONGO_PORTB")

        val connectionString = "mongodb://$userName:$password@$host:$port/"
        val client = MongoClient.create(connectionString)
        val db = client.getDatabase(dbName)
        val collection = db.getCollection<Observer>("observers")
        func(collection)
        client.close()
    }

    fun updateObserver(
        observerId: String,
        tripId: String,
    ) {
        getDbCollection { collection ->
            val keyfilter = and(eq("tripId", tripId), eq("observerId", observerId))
            val update = set("timestamp", Instant.now())
            collection.updateOne(keyfilter, update, UpdateOptions().upsert(true))
        }
    }

    fun getObserverCount(tripId: String): Long {
        var result: Long = 0
        getDbCollection { collection ->
            val cutoff = Instant.now().minus(15, ChronoUnit.SECONDS)
            val filterGC = lt("timestamp", cutoff)
            collection.deleteMany(filterGC)
            val filter = eq("tripId", tripId)
            result = collection.countDocuments(filter)
        }
        return result
    }

    @RabbitListener(queuesToDeclare = [Queue("#{queueConfig.requests}")])
    fun requestHandler(request: String): String {
        val request = Json.decodeFromString<RequestMessage>(request)
        if (request.path.contains("get_observer_count")) {
            val result = getObserverCount(request.params["tripId"] ?: "").toString()
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        if (request.path.contains("update_observer")) {
            val id = request.params["observerId"] ?: UUID.randomUUID().toString()
            val tripId = request.params["tripId"] ?: ""
            updateObserver(id, tripId)
            val result =
                createHTML().span {
                    attributes["hx-get"] = "/api/status/update_observer?observerId=$id&tripId=$tripId"
                    attributes["hx-trigger"] = "every 5s"
                    attributes["hx-swap"] = "outerHTML"
                }
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
