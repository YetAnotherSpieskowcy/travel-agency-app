package pl.edu.pg.rsww.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.queue")
public class QueueConfig {
    lateinit var base: String
    val requests get() = "$base.requests"
}
