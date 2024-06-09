package pl.edu.pg.rsww.pricecalculator

import kotlin.test.Test
import kotlin.test.assertEquals

internal class PriceCalculatorTest {
    val calculator: PriceCalculator = PriceCalculator()

    @Test
    fun testCalculateDistanceAllPositive() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected = 1513
        assertEquals(expected, calculator.calculateDistance(fromLocation, destLocation))
    }

    @Test
    fun testCalculateDistanceOneNegative() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(-33.0f, 151.0f)

        val expected = 15675
        assertEquals(expected, calculator.calculateDistance(fromLocation, destLocation))
    }

    @Test
    fun testCalculatePricePlane3Meals() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected: Float = 2 * (1513 + 7 * 5.0f * 10 + 250 + 80 + 100 / 2)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, 2, "flight", 5.0f, "All inclusive", 1.0f))
    }

    @Test
    fun testCalculatePriceBus() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected: Float = 2 * (1513 + 7 * 3.5f * 10 + 100 + 35 + 100 / 2)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, 2, "bus", 3.5f, "3 posi\u0142ki", 1.0f))
    }

    @Test
    fun testCalculatePriceNoTransport() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected: Float = 2 * (1513 + 7 * 4.0f * 10 + 0 + 25 + 100 / 2)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, 2, "", 4.0f, "2 posi\u0142ki", 1.0f))
    }

    @Test
    fun testCalculatePriceMorePeople() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected: Float = 4 * (1513 + 7 * 5.0f * 10 + 250 + 80 + 100 / 1)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 4, 1, "flight", 5.0f, "All inclusive", 1.0f))
    }

    @Test
    fun testCalculatePriceMultiplier() {
        val fromLocation: Geolocation = Geolocation(54.0f, 18.0f)
        val destLocation: Geolocation = Geolocation(41.0f, 12.0f)

        val expected: Float = 1.5f * 4 * (1513 + 7 * 5.0f * 10 + 250 + 80 + 100 / 1)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 4, 1, "flight", 5.0f, "All inclusive", 1.5f))
    }
}
