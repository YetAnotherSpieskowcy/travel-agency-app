package pl.edu.pg.rsww.touroffers

import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document

data class Entity(val entity_id: String, val entity_type: String, val last_event_id: Int, val data: Document)
data class City(val title: String, val country_id: String)

public class TourOffersManager{
    val pageSize: Int = 5
    val userName = System.getenv("MONGO_USERNAME")
    val password = System.getenv("MONGO_PASSWORD")
    val dbName = System.getenv("MONGO_DB")
    val host = System.getenv("MONGO_HOSTB")
    val port = System.getenv("MONGO_PORTB")
    val connectionString = "mongodb://$userName:$password@$host:$port/"

    fun getTourList(destination: String,
                    from: String,
                    departureDate: String,
                    adults: Int,
                    under3: Int,
                    under10: Int,
                    under18: Int): String{

        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        var result = ""
        val fromCities = db.getCollection<Entity>("snapshots").find()
                .toList()
                .filter{ it.entity_type == "City" && (it.data?.getString("title")?.contains(from) ?: false) }
        val destCities = db.getCollection<Entity>("snapshots").find()
                .toList()
                .filter{ it.entity_type == "City" && (it.data?.getString("title")?.contains(destination) ?: false) }

        if (fromCities != null && destCities != null) {

            var n = 0
            for(f in fromCities) {
                for (d in destCities) {
                    val hotels = db.getCollection<Entity>("snapshots").find()
                            .toList()
                            .filter {
                                it.entity_type == "Hotel" && d.entity_id == it.data?.getString("destination_city_id")
                            }
                    for (h in hotels) {
                        result += """
                            <div class="my-3 rounded-md outline-1 box-border border-2 shadow-md flex justify-between gap-x-6 py-5 flex min-w-0 gap-x-4 space-x-4 px-5"
                                hx-get="/api/tour_offers/get_trips" hx-include="[destination='destination', from='from', num_adults='num_adults']"
                                hx-trigger="revealed" hx-swap="afterend" mustache-template="trip">
                                <div>
                                    <p class="break-afer-auto text-sm font-semibold leading-6 text-gray-900">
                                        ${h.data?.getString("title") ?: "Something went wrong..."} </p>
                                    <p class="mt-1 truncate text-xs leading-5 text-gray-500">${d.data?.getString("title")}, ${f.data?.getString("title")}</p>
                                </div>
                                <div>
                                    <button type="button"
                                        class="flex select-none items-center gap-3 rounded-lg border border-blue-500 py-3 px-6 
                                        text-center align-middle font-sans text-xs font-bold uppercase text-blue-500 transition-all 
                                        hover:opacity-75 focus:ring focus:ring-blue-200 active:opacity-[0.85] disabled:pointer-events-none 
                                        disabled:opacity-50 disabled:shadow-none"
                                        hx-get="/api/tour_offers/trip_details/?id=${h.entity_id}" hx-target="#container" mustache-template="trip_details"
                                        hx-swap="innerHTML">Details</button>
                                </div>
                            </div>
                        """.trimIndent()
                    }
                }
                n += 1
            }
        }
        if(result == ""){
            result = """<div>
                <p class="break-afer-auto text-sm font-semibold leading-6 text-gray-900">
                    Brak wyników    
                </p>
            </div>"""
        }
        client.close()
        return result
    }

    fun getTourDetails(id: Int): String{

        return "detail"
    }
}