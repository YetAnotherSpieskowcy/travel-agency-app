package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class ProcessPaymentMessage(
    val triggeredBy: String,
)
