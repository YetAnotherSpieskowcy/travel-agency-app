package pl.edu.pg.rsww.status

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Observer(val observerId: String, val tripId: String, val timestamp: String)

object Events : Table("events") {
    val id = long("id").autoIncrement("public.seq").uniqueIndex()
    val entityId = uuid("entity_id")
    val eventName = varchar("event_name", 255)
    val data = varchar("data", 255)
    val change_time = timestamp("change_time")
}

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

    fun getPurchaseStatus(tripId: String): Long {
        val pdbName = System.getenv("POSTGRES_DB")
        val phost = System.getenv("POSTGRES_HOSTB")
        val pport = System.getenv("POSTGRES_PORTB")
        Database.connect(
            url = "jdbc:postgresql://$phost:$pport/$pdbName",
            driver = "org.postgresql.Driver",
            user = System.getenv("POSTGRES_USERNAME"),
            password = System.getenv("POSTGRES_PASSWORD"),
        )
        var count: Long = 0
        transaction {
            val result =
                Events.select {
                    (Events.change_time greaterEq Instant.now().minus(10, ChronoUnit.SECONDS)) and
                        (Events.entityId eq UUID.fromString(tripId))
                }
            count = result.count()
        }
        return count
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
        if (request.path.contains("poll_purchase_event")) {
            val tripId = request.params["tripId"] ?: ""
            val count = getPurchaseStatus(tripId)
            var result = ""
            if (count > 0) {
                result = "Inny klient właśnie zakupił tę wycieczkę!"
            }
            val resp = ResponseMessage(200, emptyMap(), result)
            val rawResp = Json.encodeToString(resp)
            return rawResp
        }
        return Json.encodeToString(ResponseMessage(200, emptyMap(), """{ "error": "404" }"""))
    }
}
