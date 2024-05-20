package pl.edu.pg.rsww.touroffers

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.MongoClient
import org.bson.Document

data class Entity(val entity_id: String, val entity_type: String, val last_event_id: Int, val data: Document)

public class TourOffersManager {
    val pageSize: Int = 5
    val userName = System.getenv("MONGO_USERNAME")
    val password = System.getenv("MONGO_PASSWORD")
    val dbName = System.getenv("MONGO_DB")
    val host = System.getenv("MONGO_HOSTB")
    val port = System.getenv("MONGO_PORTB")
    val connectionString = "mongodb://$userName:$password@$host:$port/"

    /*fun GetAvailable(db: MongoDatabase): List<String> {
        val projection = Projection.fields(
                Projections.include(Entity::entity_id.name),
                Projection.excludeId()
        )

        return db.getCollection<Entity>("snapshots").find(Filters.eq(Entity::entity_type.name, "Hotel"))
                .toList()
                .filter{
                    it.data?.getInteger("reservation_count") <= it.data?.getInteger("reservation_limit") ?: false
                }
    }*/

    fun getTourList(
        destination: String,
        from: String,
        departureDate: String,
        adults: Int,
        under3: Int,
        under10: Int,
        under18: Int,
    ): String {
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        var minAge: Int = 18
        val numPeople: Int = adults + under3 + under10 + under18
        if (under3 != 0) {
            minAge = 0
        } else if (under10 != 0) {
            minAge = 3
        } else if (under18 != 0) {
            minAge = 10
        }

        var result = ""

        val destCities =
            db.getCollection<Entity>("snapshots").find(Filters.eq(Entity::entity_type.name, "Tour"))
                .toList()
                .filter {
                    (
                        if (destination != "") {
                            (it.data?.getString("hotel_destination_city_title")?.contains(destination) ?: false) ||
                                (it.data?.getString("hotel_destination_country_title")?.contains(destination) ?: false)
                        } else {
                            true
                        }
                    ) &&
                        (
                            if (from != "") {
                                it.data?.getList(
                                    "hotel_from",
                                    String::class.javaObjectType,
                                )?.any { it.contains(from) } ?: false
                            } else {
                                true
                            }
                        ) &&
                        ((it.data?.getDouble("hotel_minimum_age") ?: 18.0) <= minAge) &&
                        ((it.data?.getDouble("hotel_max_people_per_reservation") ?: 0.0) >= numPeople) &&
                        (if (departureDate != "") it.data?.getString("start_date") == departureDate ?: false else true)
                }

        if (destCities != null) {
            var n = 0
            for (d in destCities) {
                result +=
                    """
                            <div class="my-3 rounded-md outline-1 box-border border-2 shadow-md flex justify-between gap-x-6 py-5 flex min-w-0 gap-x-4 space-x-4 px-5">
                                <div>
                                    <p class="break-afer-auto text-sm font-semibold leading-6 text-gray-900">
                                        ${d.data?.getString("title") ?: "Coś poszło nie tak..."}</p>
                                    <p class="mt-1 truncate text-xs leading-5 text-gray-500">
                                        ${d.data?.getString(
                        "hotel_destination_city_title",
                    ) + "," ?: ""} ${d.data?.getString("hotel_destination_country_title")}</p>
                                </div>
                                <div>
                                    <input type="button"
                                        class="flex select-none items-center gap-3 rounded-lg border border-blue-500 py-3 px-6 
                                        text-center align-middle font-sans text-xs font-bold uppercase text-blue-500 transition-all 
                                        hover:opacity-75 focus:ring focus:ring-blue-200 active:opacity-[0.85] disabled:pointer-events-none 
                                        disabled:opacity-50 disabled:shadow-none"
                                        hx-get="/api/tour_offers/trip_details/?id=${d.entity_id}&num_people=${numPeople}" hx-target="#container" handlebars-template="trip_details"
                                        hx-swap="innerHTML" value="Szczegóły">
                                </div>
                            </div>
                    """.trimIndent()
                n += 1
                // }
            }
        }
        if (result == "") {
            result = """<div>
                <p class="break-afer-auto text-sm font-semibold leading-6 text-gray-900">
                    Brak wyników    
                </p>
            </div>"""
        }
        client.close()
        return result
    }

    fun getTourDetails(id: String, numPeople: Int): String {
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        var result =
            """
            {
            "name": "Coś poszło nie tak...",
            "description": "Brak danych"
            }
            """.trimIndent()

        val tour =
            db.getCollection<Entity>(
                "snapshots",
            ).find(Filters.and(Filters.eq(Entity::entity_type.name, "Tour"), Filters.eq(Entity::entity_id.name, id)))
                .toList()
                .firstOrNull()
        if (tour != null) {
            result =
                """
                {
                "id": "${id}",
                "tour": ${tour.data.toJson()},
                "numPeople": ${numPeople}
                }
                """.trimIndent()
        }
        client.close()
        return result
    }
}
