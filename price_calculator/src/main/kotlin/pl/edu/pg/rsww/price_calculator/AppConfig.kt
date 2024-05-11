package pl.edu.pg.rsww.pricecalculator

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {
    @Autowired
    lateinit var queueConfig: QueueConfig

    @Bean
    fun requestsQueue(): Queue {
        return Queue(queueConfig.requests)
    }

    @Bean
    fun requestsExchange(): DirectExchange {
        return DirectExchange(queueConfig.requests)
    }

    @Bean
    fun requestsBinding(
        exchange: DirectExchange,
        queue: Queue,
    ): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(queueConfig.requests)
    }

    @Bean
    fun server(): PriceCalculatorServer {
        return PriceCalculatorServer()
    }
}
