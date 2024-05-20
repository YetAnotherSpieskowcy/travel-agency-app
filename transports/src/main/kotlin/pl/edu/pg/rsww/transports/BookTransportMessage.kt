package pl.edu.pg.rsww.transports

import kotlinx.serialization.Serializable

@Serializable
public data class BookTransportMessage(
    val triggeredBy: String,
    val userId: String,
    val routeId: String,
)
