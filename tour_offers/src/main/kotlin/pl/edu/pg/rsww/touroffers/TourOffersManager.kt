package pl.edu.pg.rsww.touroffers

import com.mongodb.kotlin.client.MongoClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class Entity(val entity_id: String, val entity_type: String, val last_event_id: Int)
data class City(val title: String, val country_id: String)

public class TourOffersManager{
    val pageSize: Int = 5
    val userName = System.getenv("MONGO_USERNAME")
    val password = System.getenv("MONGO_PASSWORD")
    val dbName = System.getenv("MONGO_DB")
    val host = System.getenv("MONGO_HOSTB")
    val port = System.getenv("MONGO_PORTB")

    fun getTourDetails(id: Int): String{

        return "detail"
    }

    fun getTourList(destination: String,
                    from: String,
                    departureDate: String,
                    adults: Int,
                    under3: Int,
                    under10: Int,
                    under18: Int,
                    page: Int): String{

        val connectionString = "mongodb://$userName:$password@$host:$port/"
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        val n = 0
        val doc = db.getCollection<Any>("snapshots").find()
                .toList()
                .count()
                //.filter{ it.entity_type == "City" }
                //.firstOrNull()
        var result = """{
                "name": $doc,
                "n": $n,
                "id": "",
                "description": "brak wynikow"
            }"""
        /*if (doc != null) {
            val (entity_id, _, _, data) = doc
            result = """{
                "name": "wynik",
                "n": $n,
                "id": "${entity_id}",
                "description": "test"
            }"""
        }*/
        client.close()
        return result
    }
}