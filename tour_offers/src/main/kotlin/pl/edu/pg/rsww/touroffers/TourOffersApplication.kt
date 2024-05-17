package pl.edu.pg.rsww.touroffers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class TourOffersApplication

fun main(args: Array<String>) {
	runApplication<TourOffersApplication>(*args)
}
