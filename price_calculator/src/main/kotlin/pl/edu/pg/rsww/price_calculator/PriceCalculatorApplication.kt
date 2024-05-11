package pl.edu.pg.rsww.pricecalculator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PriceCalculatorApplication

fun main(args: Array<String>) {
    runApplication<PriceCalculatorApplication>(*args)
}
