package pl.edu.pg.rsww.pricecalculator

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

public class PriceCalculator {
    val earthRadius = 6371 // in km
    val mealPrices =
        mapOf(
            "All inclusive" to 80,
            "2 posi\u0142ki" to 25,
            "Full board plus" to 40,
            "\u015aniadania" to 15,
            "Bez wy\u017cywienia" to 0,
            "Half board plus" to 30,
            "Wy\u017cywienie zgodnie z programem" to 30,
            "3 posi\u0142ki" to 35,
            "All inclusive soft" to 90,
            "All inclusive light" to 90,
        )

    fun toRadians(deg: Float): Double = deg / 180.0 * PI

    fun calculateDistance(
        location1: Geolocation,
        location2: Geolocation,
    ): Int {
        // acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon2-lon1)) * 6371 = acos(a + b * c) * 6371
        // c = cos(lon2-lon1)
        val a = sin(toRadians(location1.latitude)) * sin(toRadians(location2.latitude))
        val b = cos(toRadians(location1.latitude)) * cos(toRadians(location2.latitude))
        val c = cos(toRadians(location2.longitude - location1.longitude))
        val result = acos(a + b * c) * earthRadius
        val ret = round(result).toInt()
        return ret
    }

    fun calculatePrice(
        destGeolocation: Geolocation,
        fromGeolocation: Geolocation,
        duration: Int,
        numPeople: Int,
        freeSpots: Int,
        transportType: String,
        hotelRating: Float,
        mealType: String,
        multiplier: Float,
    ): Float {
        val distance = calculateDistance(fromGeolocation, destGeolocation)
        var transportPrice = 0
        if (transportType == "flight") {
            transportPrice = 250
        } else if (transportType == "bus") {
            transportPrice = 100
        }
        val mealPrice: Int = mealPrices[mealType] ?: 0

        val price = multiplier * numPeople * (distance + duration * hotelRating * 10 + transportPrice + mealPrice + 100 / freeSpots)

        return price
    }
}
