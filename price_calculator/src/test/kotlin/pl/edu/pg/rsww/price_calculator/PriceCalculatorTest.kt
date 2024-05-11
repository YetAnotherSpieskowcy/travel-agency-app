package pl.edu.pg.rsww.pricecalculator

import kotlin.test.*

internal class PriceCalculatorTest {
    val calculator: PriceCalculator = PriceCalculator()

    @Test
    fun testCalculateDistanceAllPositive() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected = 1513
        assertEquals(expected, calculator.calculateDistance(fromLocation, destLocation))
    }

    @Test
    fun testCalculateDistanceOneNegative() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("-33", "151")

        val expected = 15675
        assertEquals(expected, calculator.calculateDistance(fromLocation, destLocation))
    }

    @Test
    fun testCalculatePricePlane() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected: Int = 2 * (1513 + 7 * 30 + 250)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, "plane"))
    }

    @Test
    fun testCalculatePriceTrain() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected: Int = 2 * (1513 + 7 * 30 + 150)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, "train"))
    }

    @Test
    fun testCalculatePriceBus() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected: Int = 2 * (1513 + 7 * 30 + 100)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, "bus"))
    }

    @Test
    fun testCalculatePriceNoTransport() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected: Int = 2 * (1513 + 7 * 30 + 0)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 2, ""))
    }

    @Test
    fun testCalculatePriceMorePeople() {
        val fromLocation: Geolocation = Geolocation("54", "18")
        val destLocation: Geolocation = Geolocation("41", "12")

        val expected: Int = 4 * (1513 + 7 * 30 + 250)

        assertEquals(expected, calculator.calculatePrice(destLocation, fromLocation, 7, 4, "plane"))
    }
}
