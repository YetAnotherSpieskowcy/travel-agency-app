package pl.edu.pg.rsww.price_calculator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PriceCalculatorApplication

fun main(args: Array<String>) {
	runApplication<PriceCalculatorApplication>(*args)
}
