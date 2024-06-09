package pl.edu.pg.rsww.touroperator

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
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
        requestsQueue: Queue,
    ): Binding {
        return BindingBuilder.bind(requestsQueue).to(exchange).with(queueConfig.requests)
    }

    @Bean
    fun topicExchange(): TopicExchange {
        return TopicExchange(queueConfig.base)
    }

    @Bean
    fun transactionsQueue(): Queue {
        return Queue(queueConfig.transactions)
    }

    @Bean
    fun transactionsBinding(
        exchange: DirectExchange,
        transactionsQueue: Queue,
    ): Binding {
        return BindingBuilder.bind(transactionsQueue).to(DirectExchange("tour_operator")).with(
            "${queueConfig.transactions}.*",
        )
    }

    @Bean
    fun eventsQueue(): Queue {
        return Queue(queueConfig.events)
    }

    @Bean
    fun eventsExchange(): FanoutExchange {
        return FanoutExchange(queueConfig.events)
    }

    @Bean
    fun eventsBinding(
        exchange: FanoutExchange,
        eventsQueue: Queue,
    ): Binding {
        return BindingBuilder.bind(eventsQueue).to(exchange)
    }

    @Bean
    fun eventsTripReservationsBinding(eventsQueue: Queue): Binding {
        return BindingBuilder.bind(eventsQueue).to(TopicExchange("trip_reservations")).with(
            "trip_reservations.events.*",
        )
    }

    @Bean
    fun server(): TourOperatorServer {
        return TourOperatorServer()
    }
}
