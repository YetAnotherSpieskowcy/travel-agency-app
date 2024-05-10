package pl.edu.pg.rsww.price_calculator

import kotlin.io.*
import kotlin.math.*

public class PriceCalculator{
    val earthRadius = 6371 // in km
    fun toRadians(deg: Float): Double = deg / 180.0 * PI

    fun calculateDistance(location1: Geolocation, location2: Geolocation): Int{
        //acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon2-lon1)) * 6371 = acos(a + b * c) * 6371
        // c = cos(lon2-lon1)
        val a = sin(toRadians(location1.latitude)) * sin(toRadians(location2.latitude))
        val b = cos(toRadians(location1.latitude)) * cos(toRadians(location2.latitude))
        val c = cos(toRadians(location2.longitude - location1.longitude))
        val result = acos(a + b * c) * 6371
        val ret = round(result).toInt()
        println(ret)
        return ret
    }

    fun calculatePrice(destGeolocation: Geolocation,
                       fromGeolocation: Geolocation,
                       duration: Int,
                       numPeople: Int,
                       transportType: String): Int{
        val distance = calculateDistance(fromGeolocation, destGeolocation)
        var transportPrice = 0
        if (transportType == "samolot"){
            transportPrice = 250
        }else if(transportType == "pociag"){
            transportPrice = 150
        }else if(transportType == "bus"){
            transportPrice = 100
        }
        val price = numPeople * (distance + duration * 30 + transportPrice)

        return price
    }
}