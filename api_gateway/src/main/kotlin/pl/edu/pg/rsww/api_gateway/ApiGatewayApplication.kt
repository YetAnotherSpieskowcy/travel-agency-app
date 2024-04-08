package pl.edu.pg.rsww.api_gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
	runApplication<ApiGatewayApplication>(*args)
}
