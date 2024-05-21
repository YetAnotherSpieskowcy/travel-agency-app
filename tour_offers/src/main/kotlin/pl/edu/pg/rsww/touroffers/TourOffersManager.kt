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

        val available =
            db.getCollection<Entity>("snapshots").find(Filters.eq(Entity::entity_type.name, "Hotel"))
                .toList()
                .filter {
                    it.data.getDouble("reservation_count") < it.data.getDouble("reservation_limit")
                }

        val destCities =
            db.getCollection<Entity>("snapshots").find(
                Filters.and(
                    Filters.eq(Entity::entity_type.name, "Tour"),
                    Filters.lte("data.hotel_minimum_age", minAge),
                    Filters.gte("data.hotel_max_people_per_reservation", numPeople),
                ),
            )
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
                        (if (departureDate != "") it.data?.getString("start_date") == departureDate ?: false else true) &&
                        available.any { a -> a.entity_id == it.data?.getString("hotel") }
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
                                        hx-get="/api/tour_offers/trip_details/?id=${d.entity_id}&numPeople=$numPeople" hx-target="#container" handlebars-template="trip_details"
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

    fun getTourDetails(
        id: String,
        numPeople: Int,
    ): String? {
        val data = getTourData(id, numPeople)

        val result = data?.toJson()
        return result
    }

    fun getTourAvailableRooms(
        id: String,
        numPeople: Int,
    ): String {
        val data = getTourData(id, numPeople)

        val result = data?.getInteger("availableRooms", 0)

        return "$result"
    }

    private fun getTourData(
        id: String,
        numPeople: Int,
    ): Document? {
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        var result: Document? = null

        val tour =
            db.getCollection<Entity>(
                "snapshots",
            ).find(Filters.and(Filters.eq(Entity::entity_type.name, "Tour"), Filters.eq(Entity::entity_id.name, id)))
                .toList()
                .firstOrNull()
        if (tour != null) {
            val hotel =
                db.getCollection<Entity>("snapshots").find(Filters.eq(Entity::entity_id.name, tour.data.getString("hotel")))
                    .toList()
                    .firstOrNull()
            val id = id
            val limit = hotel?.data?.getDouble("reservation_limit") ?: 0.0
            val count = hotel?.data?.getDouble("reservation_count") ?: limit
            val availableRooms = (limit - count).toInt()
            result =
                tour.data.append("numPeople", numPeople)
                    .append("availableRooms", availableRooms)
                    .append("id", id)
        }
        client.close()
        return result
    }

    fun getTransportOptions(
        id: String,
        numPeople: Int,
        default: String,
    ): String {
        val client = MongoClient.create(connectionString = connectionString)
        val db = client.getDatabase(databaseName = dbName)

        val tour =
            db.getCollection<Entity>(
                "snapshots",
            ).find(Filters.and(Filters.eq(Entity::entity_type.name, "Tour"), Filters.eq(Entity::entity_id.name, id)))
                .toList()
                .firstOrNull()

        if (tour == null) {
            client.close()
            return generateOptions(listOf(), default)
        }

        val busRouteIds =
            tour.data.getList(
                "hotel_bus_routes",
                Document::class.javaObjectType,
            ).map { it.getString("id") }
        val flightRouteIds =
            tour.data.getList(
                "hotel_flight_routes",
                Document::class.javaObjectType,
            ).map { it.getString("id") }

        val routes =
            db.getCollection<Entity>(
                "snapshots",
            ).find(
                Filters.or(
                    Filters.and(
                        Filters.eq(Entity::entity_type.name, "BusRoute"),
                        Filters.`in`(Entity::entity_id.name, busRouteIds),
                    ),
                    Filters.and(
                        Filters.eq(Entity::entity_type.name, "FlightRoute"),
                        Filters.`in`(Entity::entity_id.name, flightRouteIds),
                    ),
                ),
            ).toList().filter {
                val limit = it?.data?.getDouble("reservation_limit") ?: 0.0
                val count = it?.data?.getDouble("reservation_count") ?: limit
                val availableSpots = limit - count
                availableSpots >= numPeople
            }

        val result = generateOptions(routes, default)

        client.close()
        return result
    }

    private fun generateOptions(
        routes: List<Entity>,
        default: String,
    ): String {
        return buildString {
            val optionValue = "own"
            append("<option")
            append(" value='$optionValue'")
            if (default == optionValue) {
                append(" selected='selected'")
            }
            append(">")
            append("Transport we własnym zakresie")
            append("</option>")
            for (route in routes) {
                val rawType =
                    when (route.entity_type) {
                        "BusRoute" -> "bus"
                        "FlightRoute" -> "flight"
                        else -> throw IllegalStateException(
                            "routes query only includes above 2 types",
                        )
                    }
                val displayType =
                    when (route.entity_type) {
                        "BusRoute" -> "Autokar"
                        "FlightRoute" -> "Lot"
                        else -> throw IllegalStateException(
                            "routes query only includes above 2 types",
                        )
                    }
                val city =
                    if (route.entity_type == "BusRoute") {
                        route.data.getString("origin_bus_stop_city")
                    } else {
                        route.data.getString("origin_airport_city")
                    }
                val limit = route?.data?.getDouble("reservation_limit") ?: 0.0
                val count = route?.data?.getDouble("reservation_count") ?: limit
                val freeSeats = (limit - count).toInt()
                val optionValue = "${rawType}_${route.entity_id}"
                append("<option")
                append(" value='$optionValue'")
                if (default == optionValue) {
                    append(" selected='selected'")
                }
                append(">")
                append(displayType)
                append(" z ")
                append(city)
                append(" (")
                append("$freeSeats")
                append(" wolne miejsca)")
                append("</option>")
            }
        }
    }
}
